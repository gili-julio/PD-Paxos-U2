package br.ufrn.middleware.extension;

/** Invocation Interceptor. Hooks before/after/onError opcionais. */
public interface InvocationInterceptor {
    default void beforeInvocation(InvocationContext ctx) {}
    default Object afterInvocation(InvocationContext ctx, Object result) { return result; }
    default void onError(InvocationContext ctx, Throwable error) {}
}
