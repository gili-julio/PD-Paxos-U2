package br.ufrn.middleware.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Remote Object (Remoting Patterns).
 * Methods annotated with {@link MethodMapping} become remotely invocable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RemoteObject {
    /** Logical object id used by Lookup. Empty -> derived from simple class name. */
    String id() default "";

    /** Optional base path prefix prepended to each method mapping. */
    String basePath() default "";
}
