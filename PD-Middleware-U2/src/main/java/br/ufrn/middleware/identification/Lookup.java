package br.ufrn.middleware.identification;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Lookup. Registry thread-safe ObjectId -> RemoteEntry. */
public final class Lookup {
    private final Map<ObjectId, RemoteEntry> registry = new ConcurrentHashMap<>();

    public void register(RemoteEntry entry) {
        if (registry.putIfAbsent(entry.id(), entry) != null) {
            throw new IllegalStateException("Duplicate Remote Object id: " + entry.id());
        }
    }

    public Optional<RemoteEntry> find(ObjectId id) {
        return Optional.ofNullable(registry.get(id));
    }

    public Collection<RemoteEntry> all() { return registry.values(); }

    public int size() { return registry.size(); }
}
