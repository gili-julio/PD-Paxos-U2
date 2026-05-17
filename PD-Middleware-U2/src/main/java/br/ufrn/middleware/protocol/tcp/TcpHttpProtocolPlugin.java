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
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * TCP Protocol Plug-In implementing HTTP/1.1. JMeter's HTTP Sampler talks
 * directly to this plug-in. Each connection is handled on a virtual thread,
 * so concurrency scales with load without tuning a pool.
 */
public final class TcpHttpProtocolPlugin implements ProtocolPlugin {
    private static final Logger log = LoggerFactory.getLogger(TcpHttpProtocolPlugin.class);

    private volatile ServerSocket server;
    private volatile ExecutorService workers;
    private volatile Thread acceptor;

    @Override public String name() { return "tcp"; }

    @Override public void start(int port, Function<Request, Response> dispatcher) throws IOException {
        this.server = new ServerSocket(port);
        this.workers = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("tcp-worker-", 0).factory());
        this.acceptor = new Thread(() -> acceptLoop(dispatcher), "tcp-acceptor");
        this.acceptor.start();
        log.info("TCP plug-in listening on port {}", port);
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

            String line = in.readLine();
            if (line == null || line.isBlank()) return;
            String[] parts = line.split(" ");
            if (parts.length < 3) { writeError(rawOut, 400, "Malformed request line"); return; }

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

            Request req = new Request(method, path, query, headers, body);
            Response resp = dispatcher.apply(req);
            writeResponse(rawOut, resp);
        } catch (Exception e) {
            log.warn("TCP request failed", e);
        }
    }

    private static void writeResponse(OutputStream raw, Response resp) throws IOException {
        byte[] body = resp.body().getBytes(StandardCharsets.UTF_8);
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(raw, StandardCharsets.UTF_8));
        w.write("HTTP/1.1 " + resp.status() + " " + HttpUtil.statusText(resp.status()) + "\r\n");
        Map<String, String> out = new LinkedHashMap<>(resp.headers());
        out.putIfAbsent("Content-Type", "application/json; charset=utf-8");
        out.put("Content-Length", String.valueOf(body.length));
        out.put("Connection", "close");
        for (var e : out.entrySet()) w.write(e.getKey() + ": " + e.getValue() + "\r\n");
        w.write("\r\n");
        w.flush();
        raw.write(body);
        raw.flush();
    }

    private static void writeError(OutputStream raw, int status, String message) throws IOException {
        writeResponse(raw, Response.json(status, "{\"error\":\"" + message.replace("\"", "'") + "\"}"));
    }

    @Override public void stop() {
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        if (workers != null) workers.shutdownNow();
        if (acceptor != null) acceptor.interrupt();
    }
}
