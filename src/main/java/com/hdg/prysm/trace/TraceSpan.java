package com.hdg.prysm.trace;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One structured trace span for a runner stage.
 */
public class TraceSpan {

    private final String name;
    private final Instant startedAt;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private Instant endedAt;
    private TraceStatus status;
    private String errorType;
    private String errorMessage;

    TraceSpan(String name, Instant startedAt) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Trace span name must not be blank");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("Trace span start time must not be null");
        }

        this.name = name;
        this.startedAt = startedAt;
    }

    public String getName() {
        return name;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public TraceStatus getStatus() {
        return status;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getDurationMs() {
        Instant end = endedAt == null ? Instant.now() : endedAt;
        return Math.max(0, Duration.between(startedAt, end).toMillis());
    }

    public TraceSpan put(String key, Object value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Trace attribute key must not be blank");
        }
        if (value != null) {
            attributes.put(key, value);
        }
        return this;
    }

    public void finish(TraceStatus status, Instant endedAt) {
        if (status == null) {
            throw new IllegalArgumentException("Trace status must not be null");
        }
        if (endedAt == null) {
            throw new IllegalArgumentException("Trace span end time must not be null");
        }
        this.status = status;
        this.endedAt = endedAt;
    }

    public void fail(RuntimeException exception, Instant endedAt) {
        if (exception == null) {
            throw new IllegalArgumentException("Trace failure exception must not be null");
        }
        this.errorType = exception.getClass().getSimpleName();
        this.errorMessage = exception.getMessage();
        finish(TraceStatus.FAILED, endedAt);
    }
}
