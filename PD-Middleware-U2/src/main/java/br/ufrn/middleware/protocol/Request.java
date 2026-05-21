package br.ufrn.middleware.protocol;

import br.ufrn.middleware.annotation.HttpMethod;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Envelope de requisicao neutro de protocolo. */
public final class Request {
    private final HttpMethod method;
    private final String path;
    private final Map<String, String> query;
    private final Map<String, String> headers;
    private final String body;

    public Request(HttpMethod method, String path, Map<String, String> query,
                   Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.query = query == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(query));
        this.headers = headers == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.body = body == null ? "" : body;
    }

    public HttpMethod method() { return method; }
    public String path() { return path; }
    public Map<String, String> query() { return query; }
    public Map<String, String> headers() { return headers; }
    public String body() { return body; }

    public String header(String name) {
        for (var e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
        }
        return null;
    }
}
