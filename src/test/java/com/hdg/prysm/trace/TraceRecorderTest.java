package com.hdg.prysm.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceRecorderTest {

    @Test
    void shouldRecordSuccessfulSpan() {
        TraceRecorder recorder = new TraceRecorder();
        TraceContext context = recorder.createTrace();

        String result = recorder.record(
                context,
                "test_span",
                () -> "ok",
                span -> span.put("count", 3)
        );

        assertEquals("ok", result);
        assertEquals(1, context.getSpans().size());
        TraceSpan span = context.getSpans().get(0);
        assertEquals("test_span", span.getName());
        assertEquals(TraceStatus.SUCCESS, span.getStatus());
        assertEquals(3, span.getAttributes().get("count"));
        assertTrue(span.getDurationMs() >= 0);
    }

    @Test
    void shouldRecordFailedSpanAndRethrow() {
        TraceRecorder recorder = new TraceRecorder();
        TraceContext context = recorder.createTrace();

        assertThrows(IllegalStateException.class, () -> recorder.record(
                context,
                "failed_span",
                () -> {
                    throw new IllegalStateException("boom");
                },
                null
        ));

        TraceSpan span = context.getSpans().get(0);
        assertEquals(TraceStatus.FAILED, span.getStatus());
        assertEquals("IllegalStateException", span.getErrorType());
        assertEquals("boom", span.getErrorMessage());
    }
}
