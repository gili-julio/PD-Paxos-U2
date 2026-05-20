package br.ufrn.kvstore.gateway;

import br.ufrn.kvstore.common.KvResult;
import br.ufrn.kvstore.common.PutResponse;
import br.ufrn.kvstore.common.ServiceInfo;
import br.ufrn.middleware.annotation.Body;
import br.ufrn.middleware.annotation.Header;
import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.annotation.Lifecycle;
import br.ufrn.middleware.annotation.MethodMapping;
import br.ufrn.middleware.annotation.PathVar;
import br.ufrn.middleware.annotation.RemoteObject;
import br.ufrn.middleware.broker.RemotingException;
import br.ufrn.middleware.client.ClientBroker;
import br.ufrn.middleware.client.RemoteCallException;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.identification.ObjectId;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gateway: ponto unico de entrada externo. Faz registry + heartbeat e roteia
 * PUT/GET para Proposer/Acceptor (round-robin). Lifecycle STATIC.
 */
@RemoteObject(id = "gateway")
@Lifecycle(Lifecycle.Kind.STATIC)
public class GatewayService {
    private static final Logger log = LoggerFactory.getLogger(GatewayService.class);
    private static final Gson GSON = new Gson();

    private final ClientBroker clientBroker = GatewayContext.get().clientBroker();
    private final AtomicInteger proposerRr = new AtomicInteger();
    private final AtomicInteger acceptorRr = new AtomicInteger();

    @MethodMapping(method = HttpMethod.POST, path = "/register")
    public Map<String, String> register(@Body ServiceInfo info) {
        GatewayRegistry.get().register(info);
        log.info("[REGISTRY] registrado id={} role={} {}://{}:{}",
                info.getId(), info.getRole(), info.getProtocol(), info.getHost(), info.getPort());
        return Map.of("status", "registered", "id", info.getId());
    }

    @MethodMapping(method = HttpMethod.POST, path = "/heartbeat")
    public Map<String, String> heartbeat(@Header("X-Service-Id") String serviceId) {
        boolean ok = GatewayRegistry.get().updateHeartbeat(serviceId);
        if (!ok) return Map.of("status", "unknown", "id", serviceId == null ? "" : serviceId);
        return Map.of("status", "ok");
    }

    @MethodMapping(method = HttpMethod.GET, path = "/services")
    public Collection<ServiceInfo> services() {
        return GatewayRegistry.get().all();
    }

    /** Rotea PUT para um Proposer ativo (round-robin). */
    @MethodMapping(method = HttpMethod.PUT, path = "/kv/{key}")
    public PutResponse putKv(@PathVar("key") String key, @Body String rawBody) {
        ServiceInfo target = pick("proposer", proposerRr);
        var aor = new AbsoluteObjectReference(
                target.getProtocol(), target.getHost(), target.getPort(), ObjectId.of("proposer"));
        log.info("[ROUTE] PUT key={} -> {}", key, target.getId());
        try {
            String resp = clientBroker.callRaw(aor, HttpMethod.PUT, "/entries/" + key,
                    Map.of("Content-Type", "application/json"), rawBody);
            return GSON.fromJson(resp, PutResponse.class);
        } catch (RemoteCallException e) {
            throw RemotingException.internal("Falha ao rotear PUT para " + target.getId(), e);
        }
    }

    /** Rotea GET para um Acceptor ativo (round-robin). */
    @MethodMapping(method = HttpMethod.GET, path = "/kv/{key}")
    public KvResult getKv(@PathVar("key") String key) {
        ServiceInfo target = pick("acceptor", acceptorRr);
        var aor = new AbsoluteObjectReference(
                target.getProtocol(), target.getHost(), target.getPort(), ObjectId.of("kv"));
        log.info("[ROUTE] GET key={} -> {}", key, target.getId());
        try {
            String resp = clientBroker.callRaw(aor, HttpMethod.GET, "/entries/" + key, Map.of(), "");
            return GSON.fromJson(resp, KvResult.class);
        } catch (RemoteCallException e) {
            throw RemotingException.internal("Falha ao rotear GET para " + target.getId(), e);
        }
    }

    private ServiceInfo pick(String role, AtomicInteger rr) {
        List<ServiceInfo> active = GatewayRegistry.get().activeByRole(role);
        if (active.isEmpty()) throw RemotingException.internal("Nenhum " + role + " ativo", null);
        int idx = Math.floorMod(rr.getAndIncrement(), active.size());
        return active.get(idx);
    }
}
