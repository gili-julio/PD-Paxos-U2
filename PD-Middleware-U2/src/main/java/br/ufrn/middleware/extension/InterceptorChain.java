package br.ufrn.middleware.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/** Ordered chain of {@link InvocationInterceptor}s applied around a target call. */
public final class InterceptorChain {
    private final List<InvocationInterceptor> interceptors;

    public InterceptorChain(List<InvocationInterceptor> interceptors) {
        this.interceptors = Collections.unmodifiableList(new ArrayList<>(interceptors));
    }

    public static InterceptorChain empty() { return new InterceptorChain(List.of()); }

    public Object execute(InvocationContext ctx, Callable<Object> target) throws Exception {
        for (var i : interceptors) i.beforeInvocation(ctx);
        try {
            Object result = target.call();
            for (int i = interceptors.size() - 1; i >= 0; i--) {
                result = interceptors.get(i).afterInvocation(ctx, result);
            }
            return result;
        } catch (Throwable t) {
            for (int i = interceptors.size() - 1; i >= 0; i--) {
                try { interceptors.get(i).onError(ctx, t); } catch (Throwable ignored) {}
            }
            if (t instanceof Exception e) throw e;
            throw new RuntimeException(t);
        }
    }
}
