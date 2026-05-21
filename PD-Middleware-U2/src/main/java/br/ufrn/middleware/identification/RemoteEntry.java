package br.ufrn.middleware.identification;

import br.ufrn.middleware.broker.RemoteMethod;
import br.ufrn.middleware.lifecycle.InstanceManager;

import java.util.List;
import java.util.Objects;

/** Entry do Lookup: id + classe + InstanceManager + rotas. */
public final class RemoteEntry {
    private final ObjectId id;
    private final Class<?> remoteClass;
    private final InstanceManager instanceManager;
    private final List<RemoteMethod> methods;

    public RemoteEntry(ObjectId id, Class<?> remoteClass,
                       InstanceManager instanceManager, List<RemoteMethod> methods) {
        this.id = Objects.requireNonNull(id);
        this.remoteClass = Objects.requireNonNull(remoteClass);
        this.instanceManager = Objects.requireNonNull(instanceManager);
        this.methods = List.copyOf(methods);
    }

    public ObjectId id() { return id; }
    public Class<?> remoteClass() { return remoteClass; }
    public InstanceManager instanceManager() { return instanceManager; }
    public List<RemoteMethod> methods() { return methods; }
}
