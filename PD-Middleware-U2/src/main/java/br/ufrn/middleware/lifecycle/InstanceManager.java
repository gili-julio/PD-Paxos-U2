package br.ufrn.middleware.lifecycle;

/** Estrategia de Lifecycle: Static, PerRequest, Pooled, Leased. */
public interface InstanceManager {
    Object acquire() throws Exception;
    default void release(Object instance) {}
    default void shutdown() {}
    String kind();
}
