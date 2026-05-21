package br.ufrn.kvstore.support;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser de args --chave=valor.
 *
 * <p>Args extras:
 * <ul>
 *   <li>{@code --heartbeat-port=N}: porta UDP do gateway dedicada a heartbeat (default = port+1).</li>
 *   <li>{@code --heartbeat-gateway=udp://host:port}: AOR UDP do gateway para heartbeat
 *       (default = derivado do --gateway trocando protocolo para udp e porta para heartbeat-port).</li>
 * </ul>
 */
public record AppConfig(String role, String protocol, String host, int port,
                        String gateway, String heartbeatGateway,
                        int heartbeatPort, String id) {

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
        String host = kv.getOrDefault("host", "localhost");
        int port = Integer.parseInt(required(kv, "port"));
        String gateway = kv.get("gateway");
        int heartbeatPort = Integer.parseInt(kv.getOrDefault("heartbeat-port", String.valueOf(port + 1)));
        String heartbeatGateway = kv.get("heartbeat-gateway");
        String id = kv.getOrDefault("id", role + "-" + port);

        if (!role.equals("gateway")) {
            if (gateway == null) {
                throw new IllegalArgumentException("--gateway=<protocol>://<host>:<port> requerido para role=" + role);
            }
            if (heartbeatGateway == null) {
                heartbeatGateway = deriveUdpHeartbeat(gateway);
            }
        }
        return new AppConfig(role, protocol, host, port, gateway, heartbeatGateway, heartbeatPort, id);
    }

    /** Deriva AOR UDP a partir do AOR TCP do gateway (porta = porta_tcp + 1). */
    private static String deriveUdpHeartbeat(String gatewayAor) {
        int schemeEnd = gatewayAor.indexOf("://");
        int portStart = gatewayAor.lastIndexOf(':');
        if (schemeEnd < 0 || portStart <= schemeEnd) {
            throw new IllegalArgumentException("AOR gateway invalido: " + gatewayAor);
        }
        String hostPart = gatewayAor.substring(schemeEnd + 3, portStart);
        int tcpPort = Integer.parseInt(gatewayAor.substring(portStart + 1));
        return "udp://" + hostPart + ":" + (tcpPort + 1);
    }

    private static String required(Map<String, String> kv, String key) {
        String v = kv.get(key);
        if (v == null) throw new IllegalArgumentException("Parametro --" + key + " requerido");
        return v;
    }
}
