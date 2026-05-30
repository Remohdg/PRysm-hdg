package com.hdg.prysm.trace;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Collects trace spans without changing business behavior.
 */
@Component
public class TraceRecorder {

    public TraceContext createTrace() {
        return TraceContext.create();
    }

    public TraceSpan start(TraceContext context, String spanName) {
        if (context == null) {
            throw new IllegalArgumentException("Trace context must not be null");
        }
        TraceSpan span = new TraceSpan(spanName, Instant.now());
        context.addSpan(span);
        return span;
    }

    public <T> T record(
            TraceContext context,
            String spanName,
            Supplier<T> operation,
            Consumer<TraceSpan> onSuccess
    ) {
        if (operation == null) {
            throw new IllegalArgumentException("Trace operation must not be null");
        }
        TraceSpan span = start(context, spanName);
        try {
            T result = operation.get();
            if (onSuccess != null) {
                onSuccess.accept(span);
            }
            if (span.getStatus() == null) {
                span.finish(TraceStatus.SUCCESS, Instant.now());
            }
            return result;
        } catch (RuntimeException exception) {
            span.fail(exception, Instant.now());
            throw exception;
        }
    }

    public void record(
            TraceContext context,
            String spanName,
            Runnable operation,
            Consumer<TraceSpan> onSuccess
    ) {
        if (operation == null) {
            throw new IllegalArgumentException("Trace operation must not be null");
        }
        record(context, spanName, () -> {
            operation.run();
            return null;
        }, onSuccess);
    }
}
