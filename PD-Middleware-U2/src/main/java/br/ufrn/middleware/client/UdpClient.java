package br.ufrn.middleware.client;

import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.protocol.HttpUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** {@link RemoteClient} that speaks the HTTP-like envelope used by {@link br.ufrn.middleware.protocol.udp.UdpProtocolPlugin}. */
public final class UdpClient implements RemoteClient {
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int TIMEOUT_MS = 5_000;

    @Override public String protocol() { return "udp"; }

    @Override
    public String call(String host, int port, HttpMethod method, String path,
                       Map<String, String> headers, String body) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            byte[] reqBytes = encode(method, path, headers, body);
            socket.send(new DatagramPacket(reqBytes, reqBytes.length,
                    InetAddress.getByName(host), port));

            byte[] buf = new byte[BUFFER_SIZE];
            DatagramPacket reply = new DatagramPacket(buf, buf.length);
            socket.receive(reply);

            String text = new String(reply.getData(), 0, reply.getLength(), StandardCharsets.UTF_8);
            int sep = text.indexOf("\r\n\r\n");
            int code = 200;
            String bodyOut = text;
            if (sep > 0) {
                String head = text.substring(0, sep);
                bodyOut = text.substring(sep + 4);
                String[] lines = head.split("\r\n");
                if (lines.length > 0) {
                    String[] sp = lines[0].split(" ", 3);
                    if (sp.length >= 2) code = Integer.parseInt(sp[1]);
                }
            }
            if (code >= 400) throw new RemoteCallException(code, HttpUtil.statusText(code) + ": " + bodyOut);
            return bodyOut;
        } catch (RemoteCallException re) {
            throw re;
        } catch (Exception e) {
            throw new RemoteCallException("UDP call failed", e);
        }
    }

    private static byte[] encode(HttpMethod method, String path,
                                 Map<String, String> headers, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.name()).append(' ').append(path).append(" HTTP/1.0\r\n");
        Map<String, String> hdrs = new LinkedHashMap<>(headers == null ? Map.of() : headers);
        hdrs.putIfAbsent("Content-Type", "application/json; charset=utf-8");
        for (var e : hdrs.entrySet()) sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
        sb.append("\r\n");
        if (body != null) sb.append(body);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
