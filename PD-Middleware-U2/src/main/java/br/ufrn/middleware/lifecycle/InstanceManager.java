package br.ufrn.middleware.lifecycle;

/**
 * Strategy that decides where/how to obtain a Remote Object instance for each
 * invocation. Implementations realize Static, Per-Request, Pooled, Leased.
 */
public interface InstanceManager {
    /** Acquire an instance to handle one invocation. */
    Object acquire() throws Exception;

    /** Release the instance once the invocation completes (pool return, etc.). */
    default void release(Object instance) {}

    /** Stop background threads and release resources. */
    default void shutdown() {}

    /** Identifies which lifecycle pattern this is, for diagnostics. */
    String kind();
}
