package br.ufrn.middleware.extension;

/**
 * Invocation Interceptor (Extension Pattern). Implementations can observe or
 * mutate invocations before/after the target method runs.
 *
 * <p>Default methods make all hooks optional.
 */
public interface InvocationInterceptor {
    default void beforeInvocation(InvocationContext ctx) {}
    default Object afterInvocation(InvocationContext ctx, Object result) { return result; }
    default void onError(InvocationContext ctx, Throwable error) {}
}
