package com.hdg.prysm.enrichment;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PullRequestMetadataTest {

    /**
     * 元数据应防御性复制 commit message 列表。
     */
    @Test
    void shouldDefensivelyCopyCommitMessages() {
        List<String> messages = new ArrayList<>();
        messages.add("feat: add feature");

        PullRequestMetadata metadata = new PullRequestMetadata("title", "body", messages, "note");
        messages.clear();

        assertEquals(1, metadata.getCommitMessages().size());
        assertThrows(UnsupportedOperationException.class, () -> metadata.getCommitMessages().clear());
    }

    /**
     * 只要 title、body 或 commit message 任一存在，就认为有扩展上下文。
     */
    @Test
    void shouldReportAvailableContext() {
        assertTrue(new PullRequestMetadata("title", null, List.of(), null).hasAnyContext());
        assertTrue(new PullRequestMetadata(null, "body", List.of(), null).hasAnyContext());
        assertTrue(new PullRequestMetadata(null, null, List.of("commit"), null).hasAnyContext());
        assertFalse(new PullRequestMetadata(null, null, List.of(), null).hasAnyContext());
    }

    /**
     * commit message 列表不能为 null。
     */
    @Test
    void shouldRejectNullCommitMessages() {
        assertThrows(IllegalArgumentException.class, () -> new PullRequestMetadata("title", "body", null, null));
    }
}
