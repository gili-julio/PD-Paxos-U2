package br.ufrn.middleware.identification;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lookup (Identification Pattern). Thread-safe registry mapping {@link ObjectId}
 * -> {@link RemoteEntry}. The {@link br.ufrn.middleware.broker.ServerRequestHandler}
 * consults it to resolve incoming requests.
 */
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
