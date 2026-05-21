package br.ufrn.kvstore.gateway;

import br.ufrn.kvstore.common.PaxosAcceptRequest;
import br.ufrn.kvstore.common.PaxosPrepareRequest;
import br.ufrn.kvstore.common.PaxosRelayBatch;
import br.ufrn.kvstore.common.ServiceInfo;
import br.ufrn.middleware.annotation.Body;
import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.annotation.Lifecycle;
import br.ufrn.middleware.annotation.MethodMapping;
import br.ufrn.middleware.annotation.RemoteObject;
import br.ufrn.middleware.client.ClientBroker;
import br.ufrn.middleware.client.RemoteCallException;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.identification.ObjectId;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Fanout PREPARE/ACCEPT para acceptors. POOLED p/ backpressure. */
@RemoteObject(id = "paxos-relay")
@Lifecycle(value = Lifecycle.Kind.POOLED, poolSize = 512)
public class PaxosRelayWorker {
    private static final Logger log = LoggerFactory.getLogger(PaxosRelayWorker.class);
    private static final long FANOUT_TIMEOUT_MS = 5_000L;

    private final Gson gson = new Gson();
    private final ClientBroker clientBroker;

    public PaxosRelayWorker() {
        this.clientBroker = GatewayContext.get().clientBroker();
    }

    @MethodMapping(method = HttpMethod.POST, path = "/prepare")
    public PaxosRelayBatch prepare(@Body PaxosPrepareRequest req) {
        return fanout("PREPARE", "/prepare", gson.toJson(req));
    }

    @MethodMapping(method = HttpMethod.POST, path = "/accept")
    public PaxosRelayBatch accept(@Body PaxosAcceptRequest req) {
        return fanout("ACCEPT", "/accept", gson.toJson(req));
    }

    private PaxosRelayBatch fanout(String action, String methodPath, String body) {
        List<ServiceInfo> acceptors = GatewayRegistry.get().activeByRole("acceptor");
        if (acceptors.isEmpty()) return new PaxosRelayBatch(0, new ArrayList<>());

        log.info("[RELAY] {} -> {} acceptor(s)", action, acceptors.size());
        var futures = new ArrayList<CompletableFuture<String>>();
        for (var acceptor : acceptors) futures.add(sendAsync(acceptor, methodPath, body));

        var responses = new ArrayList<String>();
        for (var f : futures) {
            try {
                String resp = f.get(FANOUT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (resp != null) responses.add(resp);
            } catch (Exception e) {
                log.warn("[RELAY] timeout/erro: {}", e.getMessage());
            }
        }
        return new PaxosRelayBatch(acceptors.size(), responses);
    }

    private CompletableFuture<String> sendAsync(ServiceInfo target, String methodPath, String body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var aor = new AbsoluteObjectReference(
                        target.getProtocol(), target.getHost(), target.getPort(),
                        ObjectId.of("acceptor"));
                return clientBroker.callRaw(aor, HttpMethod.POST, methodPath, Map.of(), body);
            } catch (RemoteCallException e) {
                log.warn("[RELAY] falha {}: {}", target.endpoint(), e.getMessage());
                return null;
            }
        });
    }
}
