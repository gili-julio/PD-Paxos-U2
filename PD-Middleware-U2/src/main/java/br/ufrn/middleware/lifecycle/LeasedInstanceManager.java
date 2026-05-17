package br.ufrn.middleware.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Leasing pattern. Lazily creates a single instance and grants it a lease of
 * {@code ttlSeconds}. Every invocation renews the lease. A background sweeper
 * reclaims the instance once the lease expires; the next invocation lazily
 * re-creates it.
 */
public final class LeasedInstanceManager implements InstanceManager {
    private static final Logger log = LoggerFactory.getLogger(LeasedInstanceManager.class);

    private final Supplier<?> factory;
    private final long ttlNanos;
    private final ScheduledExecutorService sweeper;
    private final AtomicLong lastTouchNanos = new AtomicLong();
    private final String objectId;

    private volatile Object instance;

    public LeasedInstanceManager(String objectId, Supplier<?> factory, int ttlSeconds) {
        this.objectId = objectId;
        this.factory = factory;
        this.ttlNanos = TimeUnit.SECONDS.toNanos(ttlSeconds);
        this.sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "leasing-sweeper-" + objectId);
            t.setDaemon(true);
            return t;
        });
        long periodSec = Math.max(1L, ttlSeconds / 2L);
        sweeper.scheduleAtFixedRate(this::sweep, periodSec, periodSec, TimeUnit.SECONDS);
    }

    @Override public Object acquire() {
        Object local = instance;
        if (local == null) {
            synchronized (this) {
                local = instance;
                if (local == null) {
                    local = factory.get();
                    instance = local;
                    log.info("Leasing[{}]: instance created", objectId);
                }
            }
        }
        lastTouchNanos.set(System.nanoTime());
        return local;
    }

    private void sweep() {
        Object local = instance;
        if (local == null) return;
        long idleNanos = System.nanoTime() - lastTouchNanos.get();
        if (idleNanos > ttlNanos) {
            synchronized (this) {
                if (instance == local) {
                    instance = null;
                    log.info("Leasing[{}]: instance reclaimed after idle={}s",
                            objectId, TimeUnit.NANOSECONDS.toSeconds(idleNanos));
                }
            }
        }
    }

    @Override public void shutdown() { sweeper.shutdownNow(); }

    public boolean isLeased() { return instance != null; }

    @Override public String kind() {
        return "LEASED(ttl=" + TimeUnit.NANOSECONDS.toSeconds(ttlNanos) + "s)";
    }
}
