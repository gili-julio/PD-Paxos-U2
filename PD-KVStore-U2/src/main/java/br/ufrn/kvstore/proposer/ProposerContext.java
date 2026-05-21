package br.ufrn.kvstore.proposer;

import br.ufrn.middleware.client.ClientBroker;
import br.ufrn.middleware.identification.AbsoluteObjectReference;

/** Recursos compartilhados pelo ProposerService PER_REQUEST. */
public final class ProposerContext {
    private static volatile ProposerContext INSTANCE;

    private final ProposalNumberGenerator generator;
    private final ClientBroker clientBroker;
    private final AbsoluteObjectReference gatewayRelay;

    private ProposerContext(ProposalNumberGenerator g, ClientBroker cb, AbsoluteObjectReference relay) {
        this.generator = g;
        this.clientBroker = cb;
        this.gatewayRelay = relay;
    }

    public static synchronized void init(int proposerPort, ClientBroker cb, AbsoluteObjectReference relay) {
        if (INSTANCE != null) throw new IllegalStateException("ProposerContext already initialised");
        INSTANCE = new ProposerContext(new ProposalNumberGenerator(proposerPort), cb, relay);
    }

    public static ProposerContext get() {
        var c = INSTANCE;
        if (c == null) throw new IllegalStateException("ProposerContext not initialised");
        return c;
    }

    public ProposalNumberGenerator generator() { return generator; }
    public ClientBroker clientBroker() { return clientBroker; }
    public AbsoluteObjectReference gatewayRelay() { return gatewayRelay; }
}
