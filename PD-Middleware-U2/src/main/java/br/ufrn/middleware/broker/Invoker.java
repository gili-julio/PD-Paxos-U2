package br.ufrn.middleware.broker;

import br.ufrn.middleware.extension.InterceptorChain;
import br.ufrn.middleware.extension.InvocationContext;
import br.ufrn.middleware.extension.InvocationInterceptor;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.identification.RemoteEntry;
import br.ufrn.middleware.lifecycle.InstanceManager;
import br.ufrn.middleware.protocol.Request;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Invoker (Basic Remoting Pattern). Resolves arguments from the {@link Request}
 * via the {@link RemoteMethod}'s {@link ParamBinding}s, acquires an instance
 * from the {@link InstanceManager}, runs the method through the interceptor
 * chain, and returns the result.
 */
public final class Invoker {
    private final Marshaller marshaller;

    public Invoker(Marshaller marshaller) { this.marshaller = marshaller; }

    public Object invoke(RemoteEntry entry, RemoteMethod method,
                         Map<String, String> pathVars, Request request,
                         AbsoluteObjectReference target,
                         List<InvocationInterceptor> globalInterceptors) throws Exception {
        Object[] args = resolveArgs(method, pathVars, request);

        var chained = new ArrayList<InvocationInterceptor>(globalInterceptors.size() + method.interceptors().size());
        chained.addAll(globalInterceptors);
        chained.addAll(method.interceptors());
        InterceptorChain chain = new InterceptorChain(chained);

        InvocationContext ctx = new InvocationContext(request, target, method.javaMethod(), args);
        InvocationContext.set(ctx);
        try {
            InstanceManager mgr = entry.instanceManager();
            Object instance = mgr.acquire();
            try {
                return chain.execute(ctx, () -> {
                    try {
                        return method.javaMethod().invoke(instance, args);
                    } catch (InvocationTargetException ite) {
                        Throwable cause = ite.getCause();
                        if (cause instanceof RemotingException re) throw re;
                        if (cause instanceof Exception e) throw e;
                        throw new RuntimeException(cause);
                    }
                });
            } finally {
                mgr.release(instance);
            }
        } finally {
            InvocationContext.clear();
        }
    }

    private Object[] resolveArgs(RemoteMethod method, Map<String, String> pathVars, Request req) {
        var params = method.params();
        Object[] args = new Object[params.size()];
        var bodyCache = new Object() {
            com.google.gson.JsonObject parsed;
            com.google.gson.JsonObject get() {
                if (parsed == null) parsed = marshaller.parseBodyAsObject(req.body());
                return parsed;
            }
        };

        for (int i = 0; i < params.size(); i++) {
            ParamBinding p = params.get(i);
            switch (p.source()) {
                case PATH -> {
                    String raw = pathVars.get(p.name());
                    if (raw == null && p.required()) throw RemotingException.badRequest("Missing path var: " + p.name());
                    args[i] = marshaller.convertString(raw, p.type());
                }
                case QUERY -> {
                    String raw = req.query().get(p.name());
                    if (raw == null && p.required()) throw RemotingException.badRequest("Missing query param: " + p.name());
                    args[i] = marshaller.convertString(raw, p.type());
                }
                case HEADER -> {
                    String raw = req.header(p.name());
                    if (raw == null && p.required()) throw RemotingException.badRequest("Missing header: " + p.name());
                    args[i] = marshaller.convertString(raw, p.type());
                }
                case BODY -> args[i] = (p.type() == String.class)
                        ? req.body()
                        : marshaller.fromJson(req.body(), p.type());
                case BODY_FIELD -> {
                    var obj = bodyCache.get();
                    var el = obj.get(p.name());
                    if ((el == null || el.isJsonNull()) && p.required()) {
                        throw RemotingException.badRequest("Missing body field: " + p.name());
                    }
                    args[i] = marshaller.convertJson(el, p.type());
                }
                case CONTEXT -> args[i] = InvocationContext.current();
            }
        }
        return args;
    }
}
