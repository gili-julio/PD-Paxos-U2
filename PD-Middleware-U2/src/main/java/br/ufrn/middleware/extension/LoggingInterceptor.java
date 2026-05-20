package br.ufrn.middleware.extension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/** Out-of-the-box interceptor that logs entry/exit/error for every invocation. */
public final class LoggingInterceptor implements InvocationInterceptor {
    private static final Logger log = LoggerFactory.getLogger("middleware.invocation");

    @Override public void beforeInvocation(InvocationContext ctx) {
        log.info("--> [{}] {} {} {}",
                ctx.invocationId(),
                ctx.request().method(),
                ctx.request().path(),
                ctx.method().getName());
    }

    @Override public Object afterInvocation(InvocationContext ctx, Object result) {
        log.info("<-- [{}] {} done in {} ms",
                ctx.invocationId(),
                ctx.method().getName(),
                TimeUnit.NANOSECONDS.toMillis(ctx.elapsedNanos()));
        return result;
    }

    @Override public void onError(InvocationContext ctx, Throwable error) {
        log.warn("!!! [{}] {} failed: {}",
                ctx.invocationId(), ctx.method().getName(), error.toString());
    }
}
