package br.ufrn.middleware.lifecycle;

import java.util.function.Supplier;

/**
 * Per-Request Instance pattern. A brand new instance is created for every
 * invocation, then discarded. Maximum isolation, no concurrency concerns.
 */
public final class PerRequestInstanceManager implements InstanceManager {
    private final Supplier<?> factory;

    public PerRequestInstanceManager(Supplier<?> factory) { this.factory = factory; }

    @Override public Object acquire() { return factory.get(); }
    @Override public String kind() { return "PER_REQUEST"; }
}
