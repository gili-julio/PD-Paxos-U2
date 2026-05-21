package br.ufrn.middleware.extension;

import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.protocol.Request;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Invocation Context. Estado da chamada atual via ThreadLocal. */
public final class InvocationContext {
    private static final ThreadLocal<InvocationContext> CURRENT = new ThreadLocal<>();

    private final String invocationId;
    private final long startNanos;
    private final Request request;
    private final AbsoluteObjectReference target;
    private final Method method;
    private final Object[] args;
    private final Map<String, Object> attributes = new HashMap<>();

    public InvocationContext(Request request, AbsoluteObjectReference target, Method method, Object[] args) {
        this.invocationId = UUID.randomUUID().toString();
        this.startNanos = System.nanoTime();
        this.request = request;
        this.target = target;
        this.method = method;
        this.args = args;
    }

    public static InvocationContext current() { return CURRENT.get(); }
    public static void set(InvocationContext ctx) { CURRENT.set(ctx); }
    public static void clear() { CURRENT.remove(); }

    public String invocationId() { return invocationId; }
    public long startNanos() { return startNanos; }
    public long elapsedNanos() { return System.nanoTime() - startNanos; }
    public Request request() { return request; }
    public AbsoluteObjectReference target() { return target; }
    public Method method() { return method; }
    public Object[] args() { return args; }

    public <T> T attribute(String key) {
        @SuppressWarnings("unchecked") T v = (T) attributes.get(key);
        return v;
    }
    public void attribute(String key, Object value) { attributes.put(key, value); }
}
