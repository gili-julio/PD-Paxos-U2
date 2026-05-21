package br.ufrn.kvstore.gateway;

import br.ufrn.middleware.client.ClientBroker;

/** ClientBroker compartilhado pelo pool de workers. */
public final class GatewayContext {
    private static volatile GatewayContext INSTANCE;

    private final ClientBroker clientBroker;

    private GatewayContext(ClientBroker cb) { this.clientBroker = cb; }

    public static synchronized void init(ClientBroker cb) {
        if (INSTANCE != null) throw new IllegalStateException("GatewayContext ja inicializado");
        INSTANCE = new GatewayContext(cb);
    }

    public static GatewayContext get() {
        var c = INSTANCE;
        if (c == null) throw new IllegalStateException("GatewayContext nao inicializado");
        return c;
    }

    public ClientBroker clientBroker() { return clientBroker; }
}
