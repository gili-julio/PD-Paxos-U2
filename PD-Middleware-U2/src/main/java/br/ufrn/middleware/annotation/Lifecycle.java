package br.ufrn.middleware.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which lifecycle manager governs instances of a Remote Object.
 *
 * <ul>
 *   <li>{@link Kind#STATIC} - singleton (Static Instance pattern)</li>
 *   <li>{@link Kind#PER_REQUEST} - new instance per invocation (Per-Request Instance)</li>
 *   <li>{@link Kind#POOLED} - bounded pool of instances (Pooling)</li>
 *   <li>{@link Kind#LEASED} - TTL-based; reclaimed if idle (Leasing)</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Lifecycle {
    Kind value() default Kind.STATIC;

    /** Pool size when {@link Kind#POOLED}. */
    int poolSize() default 4;

    /** TTL seconds when {@link Kind#LEASED}. */
    int ttlSeconds() default 300;

    enum Kind { STATIC, PER_REQUEST, POOLED, LEASED }
}
