package br.ufrn.middleware.protocol;

import br.ufrn.middleware.annotation.HttpMethod;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Helpers de parsing HTTP. */
public final class HttpUtil {
    private HttpUtil() {}

    public static HttpMethod parseMethod(String token) {
        return HttpMethod.valueOf(token.toUpperCase());
    }

    public static String[] splitPathQuery(String target) {
        int q = target.indexOf('?');
        if (q < 0) return new String[] { target, "" };
        return new String[] { target.substring(0, q), target.substring(q + 1) };
    }

    public static Map<String, String> parseQuery(String query) {
        Map<String, String> out = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return out;
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String k = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
            String v = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            out.put(k, v);
        }
        return out;
    }

    public static String statusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            default  -> "Status";
        };
    }
}
