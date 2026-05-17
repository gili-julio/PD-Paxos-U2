package br.ufrn.middleware.broker;

import br.ufrn.middleware.annotation.HttpMethod;
import br.ufrn.middleware.extension.InvocationInterceptor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * Metadata for a method discovered on a Remote Object: HTTP mapping, parameter
 * bindings, and method-level interceptors.
 */
public final class RemoteMethod {
    private final HttpMethod httpMethod;
    private final PathTemplate path;
    private final Method javaMethod;
    private final List<ParamBinding> params;
    private final List<InvocationInterceptor> interceptors;

    public RemoteMethod(HttpMethod httpMethod, PathTemplate path, Method javaMethod,
                        List<ParamBinding> params, List<InvocationInterceptor> interceptors) {
        this.httpMethod = Objects.requireNonNull(httpMethod);
        this.path = Objects.requireNonNull(path);
        this.javaMethod = Objects.requireNonNull(javaMethod);
        this.params = List.copyOf(params);
        this.interceptors = List.copyOf(interceptors);
    }

    public HttpMethod httpMethod() { return httpMethod; }
    public PathTemplate path() { return path; }
    public Method javaMethod() { return javaMethod; }
    public List<ParamBinding> params() { return params; }
    public List<InvocationInterceptor> interceptors() { return interceptors; }
}
