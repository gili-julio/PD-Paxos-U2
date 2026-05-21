package br.ufrn.middleware.lifecycle;

import br.ufrn.middleware.annotation.Lifecycle;
import br.ufrn.middleware.broker.RemotingException;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

/** Cria InstanceManager conforme @Lifecycle da classe. */
public final class InstanceManagerFactory {
    private InstanceManagerFactory() {}

    public static InstanceManager create(Class<?> clazz, String objectId) {
        Supplier<Object> factory = defaultFactory(clazz);
        Lifecycle lc = clazz.getAnnotation(Lifecycle.class);
        Lifecycle.Kind kind = lc == null ? Lifecycle.Kind.STATIC : lc.value();
        return switch (kind) {
            case STATIC      -> new StaticInstanceManager(factory);
            case PER_REQUEST -> new PerRequestInstanceManager(factory);
            case POOLED      -> new PooledInstanceManager(factory, lc.poolSize());
            case LEASED      -> new LeasedInstanceManager(objectId, factory, lc.ttlSeconds());
        };
    }

    private static Supplier<Object> defaultFactory(Class<?> clazz) {
        return () -> {
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            } catch (NoSuchMethodException e) {
                throw RemotingException.internal(
                        "Remote class " + clazz.getName() + " requires a no-arg constructor", e);
            } catch (Exception e) {
                throw RemotingException.internal(
                        "Failed to instantiate " + clazz.getName(), e);
            }
        };
    }
}
