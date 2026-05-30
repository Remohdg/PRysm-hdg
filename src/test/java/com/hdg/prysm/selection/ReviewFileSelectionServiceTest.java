package com.hdg.prysm.selection;

import com.hdg.prysm.context.PrContext;
import com.hdg.prysm.diff.PrChangedFile;
import com.hdg.prysm.diff.PrChangedFileStatus;
import com.hdg.prysm.diff.PrDiff;
import com.hdg.prysm.review.PrReviewContext;
import com.hdg.prysm.review.PrReviewFileContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewFileSelectionServiceTest {

    private final ReviewFileSelectionService selectionService = new ReviewFileSelectionService();

    /**
     * 源码文件应优先于配置和文档文件进入后续输入。
     */
    @Test
    void shouldPrioritizeSourceFilesBeforeConfigAndDocs() {
        PrReviewFileContext readme = file("README.md", PrChangedFileStatus.MODIFIED, true, null);
        PrReviewFileContext config = file("src/main/resources/application.yml", PrChangedFileStatus.MODIFIED, true, null);
        PrReviewFileContext source = file("src/main/java/com/hdg/prysm/App.java", PrChangedFileStatus.MODIFIED, true, null);

        ReviewFileSelectionResult result = selectionService.select(reviewContext(List.of(readme, config, source)));

        List<ReviewFileSelection> selectedFiles = result.getSelectedFiles();
        assertEquals(3, selectedFiles.size());
        assertEquals("src/main/java/com/hdg/prysm/App.java", selectedFiles.get(0).getFileContext().getChangedFile().getFilename());
        assertEquals("src/main/resources/application.yml", selectedFiles.get(1).getFileContext().getChangedFile().getFilename());
        assertEquals("README.md", selectedFiles.get(2).getFileContext().getChangedFile().getFilename());
    }

    /**
     * 明显低价值文件应被过滤，但仍保留过滤原因供后续报告使用。
     */
    @Test
    void shouldRejectLowValueFiles() {
        PrReviewFileContext asset = file("src/main/resources/logo.png", PrChangedFileStatus.ADDED, true, null);
        PrReviewFileContext lock = file("package-lock.json", PrChangedFileStatus.MODIFIED, true, null);
        PrReviewFileContext buildOutput = file("target/classes/App.class", PrChangedFileStatus.MODIFIED, true, null);

        ReviewFileSelectionResult result = selectionService.select(reviewContext(List.of(asset, lock, buildOutput)));

        assertTrue(result.getSelectedFiles().isEmpty());
        assertEquals(3, result.getRejectedFiles().size());
        assertRejectReason(result, "src/main/resources/logo.png", "binary or asset file");
        assertRejectReason(result, "package-lock.json", "lock file");
        assertRejectReason(result, "target/classes/App.class", "generated or build output");
    }

    /**
     * 没有 snippet 的文件不能进入后续 AI 输入，但应保留 PR5 给出的说明。
     */
    @Test
    void shouldRejectFileWithoutSnippets() {
        PrReviewFileContext fileContext = file("src/main/java/App.java", PrChangedFileStatus.MODIFIED, false, "patch unavailable");

        ReviewFileSelectionResult result = selectionService.select(reviewContext(List.of(fileContext)));

        assertTrue(result.getSelectedFiles().isEmpty());
        assertEquals("patch unavailable", result.getRejectedFiles().get(0).getReason());
    }

    /**
     * 删除文件由 diff patch 表达即可，不进入 snippet 后续输入。
     */
    @Test
    void shouldRejectRemovedFile() {
        PrReviewFileContext fileContext = file("src/main/java/OldApp.java", PrChangedFileStatus.REMOVED, true, null);

        ReviewFileSelectionResult result = selectionService.select(reviewContext(List.of(fileContext)));

        assertFalse(result.getFiles().get(0).isSelected());
        assertEquals("removed file", result.getFiles().get(0).getReason());
    }

    /**
     * 选择结果列表应不可变，避免后续阶段意外修改 PR6 输出。
     */
    @Test
    void shouldReturnImmutableSelectionLists() {
        PrReviewFileContext fileContext = file("src/main/java/App.java", PrChangedFileStatus.MODIFIED, true, null);
        ReviewFileSelectionResult result = selectionService.select(reviewContext(List.of(fileContext)));

        assertThrows(UnsupportedOperationException.class, () -> result.getFiles().clear());
    }

    private PrReviewContext reviewContext(List<PrReviewFileContext> files) {
        List<PrChangedFile> changedFiles = files.stream()
                .map(PrReviewFileContext::getChangedFile)
                .toList();
        PrDiff diff = new PrDiff(new PrContext("chinensdkcsdck", "PRysm", 6), changedFiles);
        return new PrReviewContext(diff, files);
    }

    private void assertRejectReason(ReviewFileSelectionResult result, String filename, String reason) {
        ReviewFileSelection selection = result.getRejectedFiles().stream()
                .filter(file -> file.getFileContext().getChangedFile().getFilename().equals(filename))
                .findFirst()
                .orElseThrow();
        assertEquals(reason, selection.getReason());
    }

    private PrReviewFileContext file(
            String filename,
            PrChangedFileStatus status,
            boolean hasSnippet,
            String note
    ) {
        PrChangedFile changedFile = new PrChangedFile(filename, status, 1, 0, "@@ -1,1 +1,1 @@");
        List<PrReviewFileContext.Snippet> snippets = hasSnippet
                ? List.of(new PrReviewFileContext.Snippet(1, 1, "content"))
                : List.of();
        return new PrReviewFileContext(changedFile, snippets, false, note);
    }
}
