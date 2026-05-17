package br.ufrn.middleware.client;

import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.protocol.HttpUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** {@link RemoteClient} that speaks HTTP/1.1 over TCP. Matches {@link br.ufrn.middleware.protocol.tcp.TcpHttpProtocolPlugin}. */
public final class TcpHttpClient implements RemoteClient {

    @Override public String protocol() { return "tcp"; }

    @Override
    public String call(String host, int port, HttpMethod method, String path,
                       Map<String, String> headers, String body) {
        try (Socket s = new Socket(host, port);
             OutputStream rawOut = s.getOutputStream();
             InputStream rawIn = s.getInputStream()) {

            byte[] bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(rawOut, StandardCharsets.UTF_8));
            w.write(method.name() + " " + path + " HTTP/1.1\r\n");
            Map<String, String> hdrs = new LinkedHashMap<>(headers == null ? Map.of() : headers);
            hdrs.putIfAbsent("Host", host + ":" + port);
            hdrs.putIfAbsent("Content-Type", "application/json; charset=utf-8");
            hdrs.put("Content-Length", String.valueOf(bodyBytes.length));
            hdrs.put("Connection", "close");
            for (var e : hdrs.entrySet()) w.write(e.getKey() + ": " + e.getValue() + "\r\n");
            w.write("\r\n");
            w.flush();
            rawOut.write(bodyBytes);
            rawOut.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(rawIn, StandardCharsets.UTF_8));
            String statusLine = in.readLine();
            if (statusLine == null) throw new RemoteCallException(-1, "Empty response");
            String[] sp = statusLine.split(" ", 3);
            int code = Integer.parseInt(sp[1]);

            Map<String, String> respHeaders = new LinkedHashMap<>();
            String h;
            while ((h = in.readLine()) != null && !h.isEmpty()) {
                int colon = h.indexOf(':');
                if (colon > 0) respHeaders.put(h.substring(0, colon).trim(), h.substring(colon + 1).trim());
            }

            StringBuilder bodyBuf = new StringBuilder();
            String cl = respHeaders.get("Content-Length");
            if (cl != null) {
                int n = Integer.parseInt(cl);
                char[] buf = new char[n];
                int read = 0;
                while (read < n) {
                    int r = in.read(buf, read, n - read);
                    if (r < 0) break;
                    read += r;
                }
                bodyBuf.append(buf, 0, read);
            } else {
                char[] tmp = new char[4096];
                int r;
                while ((r = in.read(tmp)) > 0) bodyBuf.append(tmp, 0, r);
            }
            if (code >= 400) {
                throw new RemoteCallException(code, HttpUtil.statusText(code) + ": " + bodyBuf);
            }
            return bodyBuf.toString();
        } catch (RemoteCallException re) {
            throw re;
        } catch (Exception e) {
            throw new RemoteCallException("TCP call failed", e);
        }
    }
}
