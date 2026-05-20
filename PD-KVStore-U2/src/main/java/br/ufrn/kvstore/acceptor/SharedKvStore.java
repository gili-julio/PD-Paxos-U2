package br.ufrn.kvstore.acceptor;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Store KV compartilhado pelo Acceptor e pelo KvQueryService (leased). */
public final class SharedKvStore {
    private static final SharedKvStore INSTANCE = new SharedKvStore();
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    private SharedKvStore() {}

    public static SharedKvStore get() { return INSTANCE; }

    public void put(String key, String value) { store.put(key, value); }
    public Optional<String> get(String key) { return Optional.ofNullable(store.get(key)); }
    public boolean containsKey(String key) { return store.containsKey(key); }
    public int size() { return store.size(); }
}
