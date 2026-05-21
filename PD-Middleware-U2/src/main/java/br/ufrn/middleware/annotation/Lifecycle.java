package br.ufrn.middleware.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Lifecycle do Remote Object: STATIC, PER_REQUEST, POOLED, LEASED. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Lifecycle {
    Kind value() default Kind.STATIC;
    int poolSize() default 4;
    int ttlSeconds() default 300;

    enum Kind { STATIC, PER_REQUEST, POOLED, LEASED }
}
