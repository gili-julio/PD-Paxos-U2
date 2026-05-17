package br.ufrn.middleware.lifecycle;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Pooling pattern. Maintains a bounded pool of {@code size} pre-created
 * instances. {@link #acquire()} blocks (up to {@link #ACQUIRE_TIMEOUT_MS})
 * until one is available, and {@link #release(Object)} returns it to the pool.
 */
public final class PooledInstanceManager implements InstanceManager {
    private static final long ACQUIRE_TIMEOUT_MS = 5_000L;

    private final BlockingQueue<Object> pool;
    private final int size;

    public PooledInstanceManager(Supplier<?> factory, int size) {
        if (size <= 0) throw new IllegalArgumentException("Pool size must be > 0");
        this.size = size;
        this.pool = new ArrayBlockingQueue<>(size);
        for (int i = 0; i < size; i++) pool.add(factory.get());
    }

    @Override public Object acquire() throws Exception {
        Object o = pool.poll(ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (o == null) throw new IllegalStateException(
                "Pool exhausted (size=" + size + ") after " + ACQUIRE_TIMEOUT_MS + "ms");
        return o;
    }

    @Override public void release(Object instance) { pool.offer(instance); }

    public int available() { return pool.size(); }
    public int size() { return size; }

    @Override public String kind() { return "POOLED(size=" + size + ")"; }
}
