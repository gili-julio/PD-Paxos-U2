package br.ufrn.middleware.client;

import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.protocol.HttpUtil;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Cliente HTTP/1.1 TCP com pool keep-alive por destino. */
public final class TcpHttpClient implements RemoteClient {
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int IDLE_MAX_PER_HOST = 64;

    private final ConcurrentHashMap<String, Deque<PooledConn>> pools = new ConcurrentHashMap<>();

    @Override public String protocol() { return "tcp"; }

    @Override
    public String call(String host, int port, HttpMethod method, String path,
                       Map<String, String> headers, String body) {
        String key = host + ":" + port;
        Deque<PooledConn> pool = pools.computeIfAbsent(key, k -> new ArrayDeque<>());

        for (int attempt = 0; attempt < 2; attempt++) {
            PooledConn conn = borrow(pool, host, port);
            try {
                String response = doCall(conn, host, port, method, path, headers, body);
                if (conn.keepAlive) returnToPool(pool, conn);
                else conn.closeQuiet();
                return response;
            } catch (RemoteCallException re) {
                conn.closeQuiet();
                if (attempt == 0 && re.status() == -1 && conn.fromPool) continue;
                throw re;
            } catch (Exception e) {
                conn.closeQuiet();
                if (attempt == 0 && conn.fromPool) continue;
                throw new RemoteCallException("TCP call failed", e);
            }
        }
        throw new RemoteCallException(-1, "Unreachable");
    }

    private PooledConn borrow(Deque<PooledConn> pool, String host, int port) {
        PooledConn conn;
        synchronized (pool) {
            conn = pool.pollFirst();
        }
        if (conn != null && conn.isUsable()) {
            conn.fromPool = true;
            return conn;
        }
        if (conn != null) conn.closeQuiet();
        return connect(host, port);
    }

    private void returnToPool(Deque<PooledConn> pool, PooledConn conn) {
        synchronized (pool) {
            if (pool.size() >= IDLE_MAX_PER_HOST) {
                conn.closeQuiet();
                return;
            }
            conn.fromPool = false;
            pool.offerFirst(conn);
        }
    }

    private static PooledConn connect(String host, int port) {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            s.setSoTimeout(READ_TIMEOUT_MS);
            s.setTcpNoDelay(true);
            return new PooledConn(s);
        } catch (Exception e) {
            try { s.close(); } catch (Exception ignored) {}
            throw new RemoteCallException("TCP connect failed: " + host + ":" + port, e);
        }
    }

    private String doCall(PooledConn conn, String host, int port, HttpMethod method, String path,
                          Map<String, String> headers, String body) throws Exception {
        byte[] bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);

        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(conn.out, StandardCharsets.UTF_8));
        w.write(method.name() + " " + path + " HTTP/1.1\r\n");
        Map<String, String> hdrs = new LinkedHashMap<>(headers == null ? Map.of() : headers);
        hdrs.putIfAbsent("Host", host + ":" + port);
        hdrs.putIfAbsent("Content-Type", "application/json; charset=utf-8");
        hdrs.put("Content-Length", String.valueOf(bodyBytes.length));
        hdrs.putIfAbsent("Connection", "keep-alive");
        for (var e : hdrs.entrySet()) w.write(e.getKey() + ": " + e.getValue() + "\r\n");
        w.write("\r\n");
        w.flush();
        if (bodyBytes.length > 0) {
            conn.out.write(bodyBytes);
            conn.out.flush();
        }

        String statusLine = readLine(conn.in);
        if (statusLine == null) throw new RemoteCallException(-1, "Empty response");
        String[] sp = statusLine.split(" ", 3);
        if (sp.length < 2) throw new RemoteCallException(-1, "Malformed status line: " + statusLine);
        int code = Integer.parseInt(sp[1]);

        Map<String, String> respHeaders = new LinkedHashMap<>();
        String h;
        while ((h = readLine(conn.in)) != null && !h.isEmpty()) {
            int colon = h.indexOf(':');
            if (colon > 0) respHeaders.put(h.substring(0, colon).trim(), h.substring(colon + 1).trim());
        }

        StringBuilder bodyBuf = new StringBuilder();
        String cl = respHeaders.get("Content-Length");
        if (cl != null) {
            int n = Integer.parseInt(cl);
            byte[] buf = new byte[n];
            int read = 0;
            while (read < n) {
                int r = conn.in.read(buf, read, n - read);
                if (r < 0) break;
                read += r;
            }
            bodyBuf.append(new String(buf, 0, read, StandardCharsets.UTF_8));
        }

        String connHdr = respHeaders.get("Connection");
        conn.keepAlive = connHdr == null || !"close".equalsIgnoreCase(connHdr.trim());

        if (code >= 400) {
            throw new RemoteCallException(code, HttpUtil.statusText(code) + ": " + bodyBuf);
        }
        return bodyBuf.toString();
    }

    private static String readLine(BufferedInputStream in) throws Exception {
        var bo = new java.io.ByteArrayOutputStream();
        int prev = -1;
        while (true) {
            int c = in.read();
            if (c < 0) {
                if (bo.size() == 0) return null;
                break;
            }
            if (prev == '\r' && c == '\n') {
                byte[] arr = bo.toByteArray();
                return new String(arr, 0, arr.length - 1, StandardCharsets.UTF_8);
            }
            bo.write(c);
            prev = c;
        }
        return bo.toString(StandardCharsets.UTF_8);
    }

    private static final class PooledConn {
        final Socket socket;
        final BufferedInputStream in;
        final OutputStream out;
        boolean keepAlive = true;
        boolean fromPool = false;

        PooledConn(Socket s) throws Exception {
            this.socket = s;
            this.in = new BufferedInputStream(s.getInputStream());
            this.out = s.getOutputStream();
        }

        boolean isUsable() {
            return !socket.isClosed() && socket.isConnected()
                    && !socket.isInputShutdown() && !socket.isOutputShutdown();
        }

        void closeQuiet() {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }
}
