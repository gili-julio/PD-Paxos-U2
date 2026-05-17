package br.ufrn.middleware.broker;

import br.ufrn.middleware.annotation.Body;
import br.ufrn.middleware.annotation.Header;
import br.ufrn.middleware.annotation.Intercept;
import br.ufrn.middleware.annotation.MethodMapping;
import br.ufrn.middleware.annotation.Param;
import br.ufrn.middleware.annotation.PathVar;
import br.ufrn.middleware.annotation.RemoteObject;
import br.ufrn.middleware.extension.InvocationContext;
import br.ufrn.middleware.extension.InvocationInterceptor;
import br.ufrn.middleware.identification.ObjectId;
import br.ufrn.middleware.identification.RemoteEntry;
import br.ufrn.middleware.lifecycle.InstanceManagerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflects over a {@link RemoteObject}-annotated class to build the
 * {@link RemoteEntry} (object id, lifecycle, route table, param bindings).
 */
public final class RemoteObjectScanner {

    private RemoteObjectScanner() {}

    public static RemoteEntry scan(Class<?> clazz) {
        RemoteObject ann = clazz.getAnnotation(RemoteObject.class);
        if (ann == null) throw new IllegalArgumentException(clazz.getName() + " is not @RemoteObject");

        String idStr = ann.id().isBlank() ? defaultId(clazz) : ann.id();
        ObjectId objectId = ObjectId.of(idStr);
        String basePath = normalize(ann.basePath());

        List<RemoteMethod> methods = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            MethodMapping mm = m.getAnnotation(MethodMapping.class);
            if (mm == null) continue;
            m.setAccessible(true);
            String full = basePath + normalize(mm.path());
            PathTemplate tpl = new PathTemplate(full);
            List<ParamBinding> bindings = bindings(m, tpl);
            List<InvocationInterceptor> ints = collectInterceptors(clazz, m);
            methods.add(new RemoteMethod(mm.method(), tpl, m, bindings, ints));
        }
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(clazz.getName() + " has no @MethodMapping methods");
        }

        var manager = InstanceManagerFactory.create(clazz, idStr);
        return new RemoteEntry(objectId, clazz, manager, methods);
    }

    private static String defaultId(Class<?> clazz) {
        String n = clazz.getSimpleName();
        return Character.toLowerCase(n.charAt(0)) + n.substring(1);
    }

    private static String normalize(String p) {
        if (p == null || p.isBlank()) return "";
        return p.startsWith("/") ? p : "/" + p;
    }

    private static List<ParamBinding> bindings(Method m, PathTemplate tpl) {
        Parameter[] params = m.getParameters();
        List<ParamBinding> out = new ArrayList<>(params.length);
        for (Parameter p : params) {
            ParamBinding b = bindingFor(p, tpl);
            out.add(b);
        }
        return out;
    }

    private static ParamBinding bindingFor(Parameter p, PathTemplate tpl) {
        if (p.getType().equals(InvocationContext.class)) {
            return new ParamBinding(ParamBinding.Source.CONTEXT, null, p.getParameterizedType(), false);
        }
        PathVar pv = p.getAnnotation(PathVar.class);
        if (pv != null) {
            if (!tpl.varNames().contains(pv.value()))
                throw new IllegalArgumentException("@PathVar '" + pv.value() + "' not in path template " + tpl.template());
            return new ParamBinding(ParamBinding.Source.PATH, pv.value(), p.getParameterizedType(), true);
        }
        Header h = p.getAnnotation(Header.class);
        if (h != null) {
            return new ParamBinding(ParamBinding.Source.HEADER, h.value(), p.getParameterizedType(), h.required());
        }
        Body b = p.getAnnotation(Body.class);
        if (b != null) {
            return new ParamBinding(ParamBinding.Source.BODY, null, p.getParameterizedType(), false);
        }
        Param par = p.getAnnotation(Param.class);
        if (par != null) {
            // BODY_FIELD by default; if name matches a path var, treat as path.
            if (tpl.varNames().contains(par.name())) {
                return new ParamBinding(ParamBinding.Source.PATH, par.name(), p.getParameterizedType(), par.required());
            }
            return new ParamBinding(ParamBinding.Source.BODY_FIELD, par.name(), p.getParameterizedType(), par.required());
        }
        throw new IllegalArgumentException(
                "Parameter '" + p.getName() + "' of " + p.getDeclaringExecutable()
                        + " requires one of @Param/@PathVar/@Body/@Header or type InvocationContext");
    }

    private static List<InvocationInterceptor> collectInterceptors(Class<?> clazz, Method m) {
        List<InvocationInterceptor> all = new ArrayList<>();
        addFromAnnotation(clazz.getAnnotation(Intercept.class), all);
        addFromAnnotation(m.getAnnotation(Intercept.class), all);
        return all;
    }

    private static void addFromAnnotation(Annotation a, List<InvocationInterceptor> sink) {
        if (a == null) return;
        for (Class<? extends InvocationInterceptor> c : ((Intercept) a).value()) {
            try { sink.add(c.getDeclaredConstructor().newInstance()); }
            catch (Exception e) { throw new RuntimeException("Cannot instantiate interceptor " + c.getName(), e); }
        }
    }
}
