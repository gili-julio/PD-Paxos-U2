package br.ufrn.middleware.protocol.udp;

import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.protocol.HttpUtil;
import br.ufrn.middleware.protocol.ProtocolPlugin;
import br.ufrn.middleware.protocol.Request;
import br.ufrn.middleware.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * UDP Protocol Plug-In with HTTP-like envelope. Each datagram carries a full
 * request (request-line + headers + body) and the matching reply is sent back
 * to the source address. Useful for the lightweight Paxos relay path.
 *
 * <p>Datagram size is bounded by {@link #BUFFER_SIZE}; payloads must fit in one
 * UDP packet.
 */
public final class UdpProtocolPlugin implements ProtocolPlugin {
    private static final Logger log = LoggerFactory.getLogger(UdpProtocolPlugin.class);
    private static final int BUFFER_SIZE = 64 * 1024;

    private volatile DatagramSocket socket;
    private volatile ExecutorService workers;
    private volatile Thread receiver;

    @Override public String name() { return "udp"; }

    @Override public void start(int port, Function<Request, Response> dispatcher) throws IOException {
        this.socket = new DatagramSocket(port);
        this.workers = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("udp-worker-", 0).factory());
        this.receiver = new Thread(() -> recvLoop(dispatcher), "udp-receiver");
        this.receiver.start();
        log.info("UDP plug-in listening on port {}", port);
    }

    private void recvLoop(Function<Request, Response> dispatcher) {
        byte[] buf = new byte[BUFFER_SIZE];
        while (!socket.isClosed()) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);
                byte[] copy = new byte[pkt.getLength()];
                System.arraycopy(pkt.getData(), 0, copy, 0, pkt.getLength());
                var src = pkt.getSocketAddress();
                workers.submit(() -> handle(copy, src, dispatcher));
            } catch (IOException e) {
                if (!socket.isClosed()) log.warn("UDP recv failed", e);
            }
        }
    }

    private void handle(byte[] data, java.net.SocketAddress source, Function<Request, Response> dispatcher) {
        try {
            Request req = decode(data);
            Response resp = dispatcher.apply(req);
            byte[] out = encode(resp);
            socket.send(new DatagramPacket(out, out.length, source));
        } catch (Exception e) {
            log.warn("UDP request failed", e);
            try {
                byte[] out = encode(Response.json(500, "{\"error\":\"" + e.getMessage() + "\"}"));
                socket.send(new DatagramPacket(out, out.length, source));
            } catch (IOException ignored) {}
        }
    }

    private static Request decode(byte[] data) {
        String text = new String(data, StandardCharsets.UTF_8);
        String[] lines = text.split("\r\n|\n", -1);
        if (lines.length == 0) throw new IllegalArgumentException("Empty datagram");
        String[] parts = lines[0].split(" ");
        if (parts.length < 2) throw new IllegalArgumentException("Bad request line: " + lines[0]);

        HttpMethod method = HttpUtil.parseMethod(parts[0]);
        String[] pq = HttpUtil.splitPathQuery(parts[1]);
        var headers = new LinkedHashMap<String, String>();

        int i = 1;
        for (; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) { i++; break; }
            int colon = line.indexOf(':');
            if (colon > 0) headers.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
        }
        StringBuilder body = new StringBuilder();
        for (; i < lines.length; i++) {
            if (body.length() > 0) body.append('\n');
            body.append(lines[i]);
        }
        return new Request(method, pq[0], HttpUtil.parseQuery(pq[1]), headers, body.toString());
    }

    private static byte[] encode(Response resp) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.0 ").append(resp.status()).append(' ')
                .append(HttpUtil.statusText(resp.status())).append("\r\n");
        Map<String, String> hdrs = new LinkedHashMap<>(resp.headers());
        hdrs.putIfAbsent("Content-Type", "application/json; charset=utf-8");
        for (var e : hdrs.entrySet()) sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
        sb.append("\r\n").append(resp.body());
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override public void stop() {
        if (socket != null && !socket.isClosed()) socket.close();
        if (workers != null) workers.shutdownNow();
        if (receiver != null) receiver.interrupt();
    }
}
