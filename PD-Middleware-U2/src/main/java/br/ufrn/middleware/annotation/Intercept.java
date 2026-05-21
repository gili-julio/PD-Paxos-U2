package br.ufrn.middleware.annotation;

import br.ufrn.middleware.extension.InvocationInterceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Anexa interceptors a classe ou metodo. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Intercept {
    Class<? extends InvocationInterceptor>[] value();
}
