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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Liga o servico ao gateway. Single-thread scheduler dispara um tick a cada
 * {@link #HEARTBEAT_PERIOD_SEC} segundos:
 * <ul>
 *   <li>Se ainda nao registrado (boot ou {@code unknown} recebido), tenta registrar.</li>
 *   <li>Se registrado, envia heartbeat. Resposta {@code "unknown"} marca como
 *       nao-registrado para o proximo tick re-registrar.</li>
 * </ul>
 * Falhas de rede sao logadas e o tick segue tentando — nao bloqueia o boot.
 */
public final class GatewayLink {
    private static final Logger log = LoggerFactory.getLogger(GatewayLink.class);
    private static final long HEARTBEAT_PERIOD_SEC = 2;

    private final ClientBroker clientBroker;
    private final AbsoluteObjectReference gatewayAor;
    private final AbsoluteObjectReference heartbeatAor;
    private final ServiceInfo self;
    private final Gson gson = new Gson();
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    public GatewayLink(ClientBroker clientBroker,
                       AbsoluteObjectReference gatewayAor,
                       AbsoluteObjectReference heartbeatAor,
                       ServiceInfo self) {
        this.clientBroker = clientBroker;
        this.gatewayAor = gatewayAor;
        this.heartbeatAor = heartbeatAor == null ? gatewayAor : heartbeatAor;
        this.self = self;
    }

    public void registerAndStart() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gateway-link-" + self.getId());
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::tick,
                0, HEARTBEAT_PERIOD_SEC, TimeUnit.SECONDS);
    }

    private void tick() {
        try {
            if (!registered.get()) {
                register();
            } else {
                sendHeartbeat();
            }
        } catch (Throwable t) {
            log.warn("gateway-link tick erro: {}", t.toString());
        }
    }

    private void register() {
        try {
            String resp = clientBroker.callRaw(gatewayAor, HttpMethod.POST, "/register",
                    Map.of(), gson.toJson(self));
            log.info("Registrado no gateway (heartbeat via {}): {}",
                    heartbeatAor.protocol(), resp);
            registered.set(true);
        } catch (RemoteCallException e) {
            log.warn("Registro falhou: {} - retry em {}s", e.getMessage(), HEARTBEAT_PERIOD_SEC);
            registered.set(false);
        }
    }

    private void sendHeartbeat() {
        try {
            String resp = clientBroker.callRaw(heartbeatAor, HttpMethod.POST, "/heartbeat",
                    Map.of("X-Service-Id", self.getId()), "");
            if (resp != null && resp.contains("\"unknown\"")) {
                log.warn("Gateway nao reconhece {} - vai re-registrar no proximo tick", self.getId());
                registered.set(false);
            }
        } catch (RemoteCallException e) {
            log.warn("Heartbeat falhou ({}): {}", heartbeatAor.protocol(), e.getMessage());
            // mantem registered=true: falha transitoria de rede, nao significa que gateway esqueceu
        }
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }
}
