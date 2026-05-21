package br.ufrn.middleware.broker;

import java.lang.reflect.Type;
import java.util.Objects;

/** Como um parametro Java vem do Request. */
public final class ParamBinding {
    public enum Source { PATH, QUERY, HEADER, BODY, BODY_FIELD, CONTEXT }

    private final Source source;
    private final String name;
    private final Type type;
    private final boolean required;

    public ParamBinding(Source source, String name, Type type, boolean required) {
        this.source = Objects.requireNonNull(source);
        this.name = name;
        this.type = Objects.requireNonNull(type);
        this.required = required;
    }

    public Source source() { return source; }
    public String name() { return name; }
    public Type type() { return type; }
    public boolean required() { return required; }
}
