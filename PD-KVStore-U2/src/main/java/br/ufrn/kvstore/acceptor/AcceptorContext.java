package br.ufrn.kvstore.acceptor;

/** Id + estado Paxos do acceptor. */
public final class AcceptorContext {
    private static volatile AcceptorContext INSTANCE;

    private final String acceptorId;
    private final AcceptorState state = new AcceptorState();

    private AcceptorContext(String acceptorId) { this.acceptorId = acceptorId; }

    public static synchronized void init(String acceptorId) {
        if (INSTANCE != null) throw new IllegalStateException("AcceptorContext ja inicializado");
        INSTANCE = new AcceptorContext(acceptorId);
    }

    public static AcceptorContext get() {
        var c = INSTANCE;
        if (c == null) throw new IllegalStateException("AcceptorContext nao inicializado");
        return c;
    }

    public String acceptorId() { return acceptorId; }
    public AcceptorState state() { return state; }
}
