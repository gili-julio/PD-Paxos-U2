package br.ufrn.middleware.lifecycle;

import java.util.function.Supplier;

/** Static Instance. Singleton lazy. */
public final class StaticInstanceManager implements InstanceManager {
    private final Supplier<?> factory;
    private volatile Object instance;

    public StaticInstanceManager(Supplier<?> factory) {
        this.factory = factory;
    }

    @Override public Object acquire() {
        Object local = instance;
        if (local == null) {
            synchronized (this) {
                local = instance;
                if (local == null) {
                    local = factory.get();
                    instance = local;
                }
            }
        }
        return local;
    }

    @Override public String kind() { return "STATIC"; }
}
