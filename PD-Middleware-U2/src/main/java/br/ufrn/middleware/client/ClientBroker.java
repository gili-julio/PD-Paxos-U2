package br.ufrn.middleware.client;

import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.broker.Marshaller;
import br.ufrn.middleware.identification.AbsoluteObjectReference;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/** Facade do lado cliente. Escolhe RemoteClient pelo protocolo do AOR. */
public final class ClientBroker {
    private final Marshaller marshaller;
    private final Map<String, RemoteClient> clients = new HashMap<>();

    public ClientBroker(Marshaller marshaller) {
        this.marshaller = marshaller;
        register(new TcpHttpClient());
        register(new UdpClient());
    }

    public void register(RemoteClient c) { clients.put(c.protocol().toLowerCase(), c); }

    public String callRaw(AbsoluteObjectReference target, HttpMethod method,
                          String methodPath, Map<String, String> headers, String body) {
        RemoteClient c = clients.get(target.protocol().toLowerCase());
        if (c == null) throw new RemoteCallException(-1,
                "No client for protocol " + target.protocol());
        String fullPath = "/" + target.objectId().value() + ensureLead(methodPath);
        return c.call(target.host(), target.port(), method, fullPath, headers, body);
    }

    public <T> T call(AbsoluteObjectReference target, HttpMethod method, String methodPath,
                      Map<String, String> headers, Object body, Type returnType) {
        String json = body == null ? "" : marshaller.toJson(body);
        String resp = callRaw(target, method, methodPath, headers, json);
        if (returnType == void.class || returnType == Void.class) return null;
        return marshaller.fromJson(resp, returnType);
    }

    private static String ensureLead(String p) {
        if (p == null || p.isEmpty()) return "";
        return p.startsWith("/") ? p : "/" + p;
    }
}
