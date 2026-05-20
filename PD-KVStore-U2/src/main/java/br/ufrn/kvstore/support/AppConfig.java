package br.ufrn.kvstore.support;

import java.util.HashMap;
import java.util.Map;

/** Parser de args de linha de comando no formato --chave=valor. */
public record AppConfig(String role, String protocol, String host, int port,
                        String gateway, String id) {

    public static AppConfig parse(String[] args) {
        Map<String, String> kv = new HashMap<>();
        for (String a : args) {
            if (a.startsWith("--")) {
                int eq = a.indexOf('=');
                if (eq > 0) kv.put(a.substring(2, eq), a.substring(eq + 1));
                else kv.put(a.substring(2), "true");
            }
        }
        String role = required(kv, "role");
        String protocol = kv.getOrDefault("protocol", "tcp");
        String host = kv.getOrDefault("host", "127.0.0.1");
        int port = Integer.parseInt(required(kv, "port"));
        String gateway = kv.get("gateway");
        String id = kv.getOrDefault("id", role + "-" + port);
        if (!role.equals("gateway") && gateway == null) {
            throw new IllegalArgumentException("--gateway=<protocol>://<host>:<port> requerido para role=" + role);
        }
        return new AppConfig(role, protocol, host, port, gateway, id);
    }

    private static String required(Map<String, String> kv, String key) {
        String v = kv.get(key);
        if (v == null) throw new IllegalArgumentException("Parametro --" + key + " requerido");
        return v;
    }
}
