package com.hdg.prysm.comment;

import com.hdg.prysm.execution.ReviewFinding;
import com.hdg.prysm.result.ReviewAggregationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewCommentRendererTest {

    @Test
    void shouldRenderGroupedReviewComment() {
        ReviewAggregationResult result = new ReviewAggregationResult(
                List.of(new ReviewFinding(
                        "builtin",
                        "HIGH",
                        "src/App.java",
                        12,
                        12,
                        "RIGHT",
                        12,
                        "RIGHT",
                        "Merge conflict marker found",
                        "The changed code contains a merge conflict marker.",
                        "Resolve the marker.",
                        "BUILTIN_CONFLICT_MARKER"
                )),
                1,
                0,
                0,
                "Built-in rules found 1 issue.",
                "LLM review skipped."
        );

        String markdown = new ReviewCommentRenderer().render(result);

        assertTrue(markdown.contains("## PRysm 审查结果"));
        assertTrue(markdown.contains("发现 1 个问题。规则结果: 1，模型结果: 0，去重数量: 0。"));
        assertTrue(markdown.contains("### src/App.java"));
        assertTrue(markdown.contains("**[HIGH] Merge conflict marker found** (第 12 行)"));
        assertTrue(markdown.contains("来源: `builtin` / 规则: `BUILTIN_CONFLICT_MARKER`"));
        assertTrue(markdown.contains("建议: Resolve the marker."));
    }

    @Test
    void shouldRenderEmptyReviewComment() {
        ReviewAggregationResult result = new ReviewAggregationResult(
                List.of(),
                0,
                0,
                0,
                "No rule findings.",
                "No LLM findings."
        );

        String markdown = new ReviewCommentRenderer().render(result);

        assertTrue(markdown.contains("发现 0 个问题。"));
        assertTrue(markdown.contains("未发现需要处理的明确问题。"));
    }
}
