package br.ufrn.kvstore;

import br.ufrn.kvstore.acceptor.AcceptorContext;
import br.ufrn.kvstore.acceptor.AcceptorService;
import br.ufrn.kvstore.acceptor.KvQueryService;
import br.ufrn.kvstore.common.ServiceInfo;
import br.ufrn.kvstore.gateway.GatewayContext;
import br.ufrn.kvstore.gateway.GatewayService;
import br.ufrn.kvstore.gateway.PaxosRelayWorker;
import br.ufrn.kvstore.proposer.ProposerContext;
import br.ufrn.kvstore.proposer.ProposerService;
import br.ufrn.kvstore.support.AppConfig;
import br.ufrn.kvstore.support.GatewayLink;
import br.ufrn.middleware.broker.Broker;
import br.ufrn.middleware.broker.Marshaller;
import br.ufrn.middleware.client.ClientBroker;
import br.ufrn.middleware.extension.LoggingInterceptor;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entrypoint unico. --role=gateway|proposer|acceptor seleciona quais @RemoteObject registrar. */
public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        AppConfig cfg = AppConfig.parse(args);
        log.info("Boot {} role={} protocol={} port={}", cfg.id(), cfg.role(), cfg.protocol(), cfg.port());

        ClientBroker clientBroker = new ClientBroker(new Marshaller());

        Broker broker = Broker.builder()
                .host(cfg.host())
                .withDefaultProtocols()
                .addGlobalInterceptor(new LoggingInterceptor())
                .build();

        switch (cfg.role()) {
            case "gateway"  -> setupGateway(broker, clientBroker);
            case "proposer" -> setupProposer(broker, clientBroker, cfg);
            case "acceptor" -> setupAcceptor(broker, cfg);
            default -> throw new IllegalArgumentException("role desconhecida: " + cfg.role());
        }

        broker.start(cfg.protocol(), cfg.port());

        if (!cfg.role().equals("gateway")) {
            var gwAor = AbsoluteObjectReference.parse(cfg.gateway() + "/gateway");
            var self = new ServiceInfo(cfg.id(), cfg.role(), cfg.host(), cfg.port(), cfg.protocol());
            new GatewayLink(clientBroker, gwAor, self).registerAndStart();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(broker::stop));
        log.info("{} pronto.", cfg.id());
    }

    private static void setupGateway(Broker broker, ClientBroker cb) {
        GatewayContext.init(cb);
        broker.register(GatewayService.class, PaxosRelayWorker.class);
    }

    private static void setupProposer(Broker broker, ClientBroker cb, AppConfig cfg) {
        var relayAor = AbsoluteObjectReference.parse(cfg.gateway() + "/paxos-relay");
        ProposerContext.init(cfg.port(), cb, relayAor);
        broker.register(ProposerService.class);
    }

    private static void setupAcceptor(Broker broker, AppConfig cfg) {
        AcceptorContext.init(cfg.id());
        broker.register(AcceptorService.class, KvQueryService.class);
    }
}
