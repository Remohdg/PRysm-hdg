package com.hdg.prysm.selection;

import com.hdg.prysm.diff.PrChangedFile;
import com.hdg.prysm.diff.PrChangedFileStatus;
import com.hdg.prysm.review.PrReviewContext;
import com.hdg.prysm.review.PrReviewFileContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 基于 PR5 的 review 上下文做文件过滤和优先级排序。
 *
 * 该类不做字符预算分配，也不组装 prompt；这些属于后续 PR。
 */
@Component
public class ReviewFileSelectionService {

    private static final int SOURCE_PRIORITY = 10;
    private static final int TEST_PRIORITY = 20;
    private static final int CONFIG_PRIORITY = 30;
    private static final int DOCUMENT_PRIORITY = 60;
    private static final int DEFAULT_PRIORITY = 50;
    private static final int REJECTED_PRIORITY = 1000;

    /**
     * 对 PR5 的文件上下文执行过滤和排序。
     */
    public ReviewFileSelectionResult select(PrReviewContext reviewContext) {
        if (reviewContext == null) {
            throw new IllegalArgumentException("Review context must not be null");
        }

        List<ReviewFileSelection> selections = new ArrayList<>();
        for (PrReviewFileContext fileContext : reviewContext.getFiles()) {
            selections.add(selectFile(fileContext));
        }

        selections.sort(Comparator
                .comparing(ReviewFileSelection::isSelected).reversed()
                .thenComparingInt(ReviewFileSelection::getPriority)
                .thenComparing(selection -> selection.getFileContext().getChangedFile().getFilename()));
        return new ReviewFileSelectionResult(reviewContext, selections);
    }

    /**
     * 判断单个文件是否值得进入后续输入，并给出基础优先级。
     */
    private ReviewFileSelection selectFile(PrReviewFileContext fileContext) {
        PrChangedFile changedFile = fileContext.getChangedFile();
        String filename = changedFile.getFilename();
        String normalizedFilename = normalize(filename);

        if (changedFile.getStatus() == PrChangedFileStatus.REMOVED) {
            return rejected(fileContext, "removed file");
        }
        if (isGeneratedOrBuildPath(normalizedFilename)) {
            return rejected(fileContext, "generated or build output");
        }
        if (isLockFile(normalizedFilename)) {
            return rejected(fileContext, "lock file");
        }
        if (isBinaryOrAssetFile(normalizedFilename)) {
            return rejected(fileContext, "binary or asset file");
        }
        if (!fileContext.hasSnippets()) {
            return rejected(fileContext, fileContext.getNote() == null ? "no review snippets" : fileContext.getNote());
        }

        return new ReviewFileSelection(fileContext, true, priorityOf(normalizedFilename), null);
    }

    /**
     * 创建被过滤的文件结果，统一使用最低优先级。
     */
    private ReviewFileSelection rejected(PrReviewFileContext fileContext, String reason) {
        return new ReviewFileSelection(fileContext, false, REJECTED_PRIORITY, reason);
    }

    /**
     * 根据文件类型给基础优先级；更细的预算控制留给 PR7。
     */
    private int priorityOf(String filename) {
        if (isTestFile(filename)) {
            return TEST_PRIORITY;
        }
        if (isSourceFile(filename)) {
            return SOURCE_PRIORITY;
        }
        if (isConfigFile(filename)) {
            return CONFIG_PRIORITY;
        }
        if (isDocumentFile(filename)) {
            return DOCUMENT_PRIORITY;
        }
        return DEFAULT_PRIORITY;
    }

    /**
     * 业务源码优先级最高，是 AI Review 的主要对象。
     */
    private boolean isSourceFile(String filename) {
        return filename.endsWith(".java");
    }

    /**
     * 测试文件有审查价值，但通常排在业务源码之后。
     */
    private boolean isTestFile(String filename) {
        return filename.contains("/src/test/") || filename.endsWith("test.java") || filename.endsWith("tests.java");
    }

    /**
     * 配置文件可能影响运行行为，应保留但低于源码和测试。
     */
    private boolean isConfigFile(String filename) {
        return filename.endsWith(".yml")
                || filename.endsWith(".yaml")
                || filename.endsWith(".properties")
                || filename.endsWith(".xml")
                || filename.endsWith(".json");
    }

    /**
     * 文档默认保留，但优先级较低。
     */
    private boolean isDocumentFile(String filename) {
        return filename.endsWith(".md") || filename.endsWith(".txt");
    }

    /**
     * 构建产物和生成目录不进入 AI 输入，避免浪费上下文。
     */
    private boolean isGeneratedOrBuildPath(String filename) {
        return filename.startsWith("target/")
                || filename.startsWith("build/")
                || filename.startsWith("dist/")
                || filename.contains("/target/")
                || filename.contains("/build/")
                || filename.contains("/dist/")
                || filename.contains("/generated/");
    }

    /**
     * 锁文件通常很大且机器生成，默认不进入审查输入。
     */
    private boolean isLockFile(String filename) {
        return filename.endsWith("package-lock.json")
                || filename.endsWith("pnpm-lock.yaml")
                || filename.endsWith("yarn.lock")
                || filename.endsWith("pom.lock");
    }

    /**
     * 二进制和资源文件不适合进入文本 Review 上下文。
     */
    private boolean isBinaryOrAssetFile(String filename) {
        return filename.endsWith(".png")
                || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".gif")
                || filename.endsWith(".webp")
                || filename.endsWith(".pdf")
                || filename.endsWith(".zip")
                || filename.endsWith(".jar")
                || filename.endsWith(".class");
    }

    /**
     * 统一路径分隔符和大小写，避免平台差异影响过滤结果。
     */
    private String normalize(String filename) {
        return filename.replace('\\', '/').toLowerCase(Locale.ROOT);
    }
}
