package br.ufrn.kvstore.support;

import br.ufrn.kvstore.common.ServiceInfo;
import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.client.ClientBroker;
import br.ufrn.middleware.client.RemoteCallException;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Registra o servico no gateway e mantem heartbeat em background. */
public final class GatewayLink {
    private static final Logger log = LoggerFactory.getLogger(GatewayLink.class);
    private static final long HEARTBEAT_PERIOD_SEC = 3;

    private final ClientBroker clientBroker;
    private final AbsoluteObjectReference gatewayAor;
    private final ServiceInfo self;
    private final Gson gson = new Gson();
    private ScheduledExecutorService scheduler;

    public GatewayLink(ClientBroker clientBroker, AbsoluteObjectReference gatewayAor, ServiceInfo self) {
        this.clientBroker = clientBroker;
        this.gatewayAor = gatewayAor;
        this.self = self;
    }

    public void registerAndStart() {
        register();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-" + self.getId());
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::sendHeartbeat,
                HEARTBEAT_PERIOD_SEC, HEARTBEAT_PERIOD_SEC, TimeUnit.SECONDS);
    }

    private void register() {
        try {
            String resp = clientBroker.callRaw(gatewayAor, HttpMethod.POST, "/register",
                    Map.of(), gson.toJson(self));
            log.info("Registrado no gateway: {}", resp);
        } catch (RemoteCallException e) {
            log.error("Falha ao registrar no gateway: {}", e.getMessage());
        }
    }

    private void sendHeartbeat() {
        try {
            clientBroker.callRaw(gatewayAor, HttpMethod.POST, "/heartbeat",
                    Map.of("X-Service-Id", self.getId()), "");
        } catch (RemoteCallException e) {
            log.warn("Heartbeat falhou: {}", e.getMessage());
        }
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }
}
