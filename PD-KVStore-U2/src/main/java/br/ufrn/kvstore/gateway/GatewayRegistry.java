package br.ufrn.kvstore.gateway;

import br.ufrn.kvstore.common.ServiceInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Registry singleton: servicos ativos por role + ultimo heartbeat. */
public final class GatewayRegistry {
    private static final GatewayRegistry INSTANCE = new GatewayRegistry();
    public static final long HEARTBEAT_TIMEOUT_MS = 10_000;

    private final ConcurrentHashMap<String, ServiceInfo> services = new ConcurrentHashMap<>();

    private GatewayRegistry() {}
    public static GatewayRegistry get() { return INSTANCE; }

    public void register(ServiceInfo info) {
        info.setLastHeartbeatMillis(System.currentTimeMillis());
        services.put(info.getId(), info);
    }

    public boolean updateHeartbeat(String id) {
        ServiceInfo s = services.get(id);
        if (s == null) return false;
        s.setLastHeartbeatMillis(System.currentTimeMillis());
        return true;
    }

    public List<ServiceInfo> activeByRole(String role) {
        long now = System.currentTimeMillis();
        var out = new ArrayList<ServiceInfo>();
        for (ServiceInfo s : services.values()) {
            if (!s.getRole().equals(role)) continue;
            if (now - s.getLastHeartbeatMillis() > HEARTBEAT_TIMEOUT_MS) continue;
            out.add(s);
        }
        return out;
    }

    public Collection<ServiceInfo> all() { return services.values(); }
}
