package br.ufrn.middleware.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Envelope de resposta neutro de protocolo. */
public final class Response {
    private final int status;
    private final Map<String, String> headers;
    private final String body;

    public Response(int status, Map<String, String> headers, String body) {
        this.status = status;
        this.headers = headers == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.body = body == null ? "" : body;
    }

    public int status() { return status; }
    public Map<String, String> headers() { return headers; }
    public String body() { return body; }

    public static Response json(int status, String body) {
        return new Response(status, Map.of("Content-Type", "application/json; charset=utf-8"), body);
    }

    public static Response text(int status, String body) {
        return new Response(status, Map.of("Content-Type", "text/plain; charset=utf-8"), body);
    }
}
