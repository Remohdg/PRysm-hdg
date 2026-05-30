package com.hdg.prysm.execution;

import com.hdg.prysm.context.PrContext;
import com.hdg.prysm.diff.PrChangedFile;
import com.hdg.prysm.diff.PrChangedFileStatus;
import com.hdg.prysm.diff.PrDiff;
import com.hdg.prysm.review.PrReviewFileContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewExecutionInputTest {

    /**
     * 执行输入应保留 PR 上下文、diff、目标文件和 prompt 上下文。
     */
    @Test
    void shouldCreateReviewExecutionInput() {
        PrContext prContext = new PrContext("chinensdkcsdck", "PRysm", 5);
        PrChangedFile changedFile = newChangedFile();
        PrDiff diff = new PrDiff(prContext, List.of(changedFile));
        ReviewTargetFile targetFile = newTargetFile(changedFile);

        ReviewExecutionInput input = new ReviewExecutionInput(
                prContext,
                diff,
                List.of(targetFile),
                new ContextStatus(ContextStatusCode.FULL, "context ready"),
                new PromptPayload("system", "user", "{}")
        );

        assertEquals(prContext, input.getPrContext());
        assertEquals(diff, input.getDiff());
        assertEquals(1, input.getFiles().size());
        assertTrue(input.getContextStatus().canRunLlmReview());
        assertEquals("context ready", input.getContextStatus().getReason());
        assertEquals("system", input.getPromptPayload().getSystemPrompt());
    }

    /**
     * 文件列表应防御性复制，避免上游构造后继续修改影响下游执行。
     */
    @Test
    void shouldDefensivelyCopyTargetFiles() {
        PrContext prContext = new PrContext("chinensdkcsdck", "PRysm", 5);
        PrChangedFile changedFile = newChangedFile();
        PrDiff diff = new PrDiff(prContext, List.of(changedFile));
        List<ReviewTargetFile> files = new ArrayList<>();
        files.add(newTargetFile(changedFile));

        ReviewExecutionInput input = new ReviewExecutionInput(
                prContext,
                diff,
                files,
                new ContextStatus(ContextStatusCode.FULL, null),
                new PromptPayload("system", "user", "{}")
        );

        files.clear();

        assertEquals(1, input.getFiles().size());
        assertThrows(UnsupportedOperationException.class, () -> input.getFiles().clear());
    }

    /**
     * prompt 载荷是 B 线执行 LLM Review 的直接输入，不能为空。
     */
    @Test
    void shouldRejectMissingPromptPayload() {
        PrContext prContext = new PrContext("chinensdkcsdck", "PRysm", 5);
        PrChangedFile changedFile = newChangedFile();
        PrDiff diff = new PrDiff(prContext, List.of(changedFile));
        ReviewTargetFile targetFile = newTargetFile(changedFile);

        assertThrows(IllegalArgumentException.class, () -> new ReviewExecutionInput(
                prContext,
                diff,
                List.of(targetFile),
                new ContextStatus(ContextStatusCode.FULL, null),
                null
        ));
    }

    /**
     * finding 结构需要同时支持 summary comment 和 GitHub inline comment 定位。
     */
    @Test
    void shouldCreateReviewFindingWithInlineCommentLocation() {
        ReviewFinding finding = new ReviewFinding(
                "llm",
                "warning",
                "src/App.java",
                10,
                12,
                "RIGHT",
                12,
                "RIGHT",
                "Potential null pointer",
                "The value may be null.",
                "Add a null check.",
                "LLM_NULL_CHECK"
        );

        assertEquals("llm", finding.getSource());
        assertEquals("src/App.java", finding.getFilePath());
        assertEquals(10, finding.getStartLine());
        assertEquals(12, finding.getLine());
        assertEquals("RIGHT", finding.getSide());
    }

    private ReviewTargetFile newTargetFile(PrChangedFile changedFile) {
        PrReviewFileContext.Snippet snippet = new PrReviewFileContext.Snippet(1, 1, "content");
        return new ReviewTargetFile(
                changedFile,
                List.of(snippet),
                0,
                true,
                "selected"
        );
    }

    private PrChangedFile newChangedFile() {
        return new PrChangedFile(
                "README.md",
                PrChangedFileStatus.MODIFIED,
                1,
                0,
                "@@ -1,1 +1,1 @@"
        );
    }
}
