package br.ufrn.kvstore.acceptor;

import java.util.concurrent.ConcurrentHashMap;

/** Estado Paxos por chave: maior proposta prometida, valor aceito. */
public final class AcceptorState {

    private final ConcurrentHashMap<String, KeyState> stateByKey = new ConcurrentHashMap<>();

    public KeyState getOrCreate(String key) {
        return stateByKey.computeIfAbsent(key, k -> new KeyState());
    }

    public static final class KeyState {
        long highestPromisedProposal = 0;
        long highestAcceptedProposal = 0;
        String acceptedValue = null;
    }
}
