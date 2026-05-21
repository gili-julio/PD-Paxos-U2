package br.ufrn.middleware.protocol.tcp;

import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.protocol.HttpUtil;
import br.ufrn.middleware.protocol.ProtocolPlugin;
import br.ufrn.middleware.protocol.Request;
import br.ufrn.middleware.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Plug-In TCP HTTP/1.1 com keep-alive. Cada conexao e tratada em uma virtual
 * thread que faz loop lendo multiplos requests do mesmo socket ate o cliente
 * fechar (Connection: close) ou ficar ocioso ({@link #IDLE_TIMEOUT_MS}).
 *
 * <p>Keep-alive aliviar portas efemeras TCP no cliente (TIME_WAIT acumulado
 * sem isso → BindException sob carga).
 */
public final class TcpHttpProtocolPlugin implements ProtocolPlugin {
    private static final Logger log = LoggerFactory.getLogger(TcpHttpProtocolPlugin.class);
    private static final int IDLE_TIMEOUT_MS = 30_000;

    private volatile ServerSocket server;
    private volatile ExecutorService workers;
    private volatile Thread acceptor;

    @Override public String name() { return "tcp"; }

    @Override public void start(int port, Function<Request, Response> dispatcher) throws IOException {
        this.server = new ServerSocket(port, 1000);
        this.workers = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("tcp-worker-", 0).factory());
        this.acceptor = new Thread(() -> acceptLoop(dispatcher), "tcp-acceptor");
        this.acceptor.start();
        log.info("TCP plug-in listening on port {} (keep-alive)", port);
    }

    private void acceptLoop(Function<Request, Response> dispatcher) {
        while (!server.isClosed()) {
            try {
                Socket s = server.accept();
                workers.submit(() -> handle(s, dispatcher));
            } catch (IOException e) {
                if (!server.isClosed()) log.warn("accept failed", e);
            }
        }
    }

    private void handle(Socket s, Function<Request, Response> dispatcher) {
        try (s;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             OutputStream rawOut = s.getOutputStream()) {

            s.setSoTimeout(IDLE_TIMEOUT_MS);

            while (!s.isClosed()) {
                Request req;
                try {
                    req = readRequest(in);
                } catch (SocketTimeoutException e) {
                    return; // idle close, normal
                }
                if (req == null) return; // EOF: client fechou

                Response resp = dispatcher.apply(req);

                boolean keepAlive = wantsKeepAlive(req);
                writeResponse(rawOut, resp, keepAlive);

                if (!keepAlive) return;
            }
        } catch (Exception e) {
            log.warn("TCP request failed: {}", e.toString());
        }
    }

    /** Le um request completo do BufferedReader. Devolve null em EOF. */
    private static Request readRequest(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null) return null;
        if (line.isBlank()) {
            // alguns clientes enviam linha vazia entre requests; pula
            line = in.readLine();
            if (line == null) return null;
        }
        String[] parts = line.split(" ");
        if (parts.length < 3) throw new IOException("Malformed request line: " + line);

        HttpMethod method = HttpUtil.parseMethod(parts[0]);
        String[] pq = HttpUtil.splitPathQuery(parts[1]);
        String path = pq[0];
        Map<String, String> query = HttpUtil.parseQuery(pq[1]);

        Map<String, String> headers = new LinkedHashMap<>();
        String h;
        while ((h = in.readLine()) != null && !h.isEmpty()) {
            int colon = h.indexOf(':');
            if (colon > 0) headers.put(h.substring(0, colon).trim(), h.substring(colon + 1).trim());
        }

        String body = "";
        String cl = headers.get("Content-Length");
        if (cl != null) {
            int n = Integer.parseInt(cl);
            if (n > 0) {
                char[] buf = new char[n];
                int read = 0;
                while (read < n) {
                    int r = in.read(buf, read, n - read);
                    if (r < 0) break;
                    read += r;
                }
                body = new String(buf, 0, read);
            }
        }
        return new Request(method, path, query, headers, body);
    }

    /** HTTP/1.1: keep-alive default, exceto se Connection: close explicito. */
    private static boolean wantsKeepAlive(Request req) {
        String conn = req.header("Connection");
        if (conn == null) return true;
        return !"close".equalsIgnoreCase(conn.trim());
    }

    private static void writeResponse(OutputStream raw, Response resp, boolean keepAlive) throws IOException {
        byte[] body = resp.body().getBytes(StandardCharsets.UTF_8);
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(raw, StandardCharsets.UTF_8));
        w.write("HTTP/1.1 " + resp.status() + " " + HttpUtil.statusText(resp.status()) + "\r\n");
        Map<String, String> out = new LinkedHashMap<>(resp.headers());
        out.putIfAbsent("Content-Type", "application/json; charset=utf-8");
        out.put("Content-Length", String.valueOf(body.length));
        out.put("Connection", keepAlive ? "keep-alive" : "close");
        if (keepAlive) out.put("Keep-Alive", "timeout=30");
        for (var e : out.entrySet()) w.write(e.getKey() + ": " + e.getValue() + "\r\n");
        w.write("\r\n");
        w.flush();
        raw.write(body);
        raw.flush();
    }

    @Override public void stop() {
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        if (workers != null) workers.shutdownNow();
        if (acceptor != null) acceptor.interrupt();
    }
}
