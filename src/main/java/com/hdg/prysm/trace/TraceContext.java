package com.hdg.prysm.trace;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Trace context for one Prysm review run.
 */
public class TraceContext {

    private final String traceId;
    private final Instant startedAt;
    private final List<TraceSpan> spans = new ArrayList<>();

    private TraceContext(String traceId, Instant startedAt) {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("Trace id must not be blank");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("Trace start time must not be null");
        }

        this.traceId = traceId;
        this.startedAt = startedAt;
    }

    public static TraceContext create() {
        return new TraceContext(UUID.randomUUID().toString(), Instant.now());
    }

    static TraceContext create(String traceId, Instant startedAt) {
        return new TraceContext(traceId, startedAt);
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public List<TraceSpan> getSpans() {
        return Collections.unmodifiableList(spans);
    }

    public long getTotalDurationMs() {
        return Math.max(0, Duration.between(startedAt, Instant.now()).toMillis());
    }

    void addSpan(TraceSpan span) {
        if (span == null) {
            throw new IllegalArgumentException("Trace span must not be null");
        }
        spans.add(span);
    }
}
