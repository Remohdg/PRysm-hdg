package com.hdg.prysm.enrichment;

import com.hdg.prysm.execution.ContextStatus;
import com.hdg.prysm.execution.ContextStatusCode;
import com.hdg.prysm.execution.PromptPayload;
import com.hdg.prysm.execution.ReviewExecutionInput;
import com.hdg.prysm.execution.ReviewTargetFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 扩展上下文注入与上下文充足度门禁。
 *
 * 该类只调整 ReviewExecutionInput，不调用规则引擎、LLM 或 GitHub 回写。
 */
@Component
public class ReviewContextEnrichmentService {

    private final PullRequestMetadataProvider metadataProvider;
    private final int minReviewableFiles;
    private final int minPromptCharacters;

    /**
     * 注入 Pull Request 元数据提供者和门禁阈值。
     */
    @Autowired
    public ReviewContextEnrichmentService(
            PullRequestMetadataProvider metadataProvider,
            @Value("${prysm.review.gate.min-reviewable-files:1}") int minReviewableFiles,
            @Value("${prysm.review.gate.min-prompt-chars:200}") int minPromptCharacters
    ) {
        if (metadataProvider == null) {
            throw new IllegalArgumentException("Pull request metadata provider must not be null");
        }
        if (minReviewableFiles < 0) {
            throw new IllegalArgumentException("Minimum reviewable files must not be negative");
        }
        if (minPromptCharacters < 0) {
            throw new IllegalArgumentException("Minimum prompt characters must not be negative");
        }

        this.metadataProvider = metadataProvider;
        this.minReviewableFiles = minReviewableFiles;
        this.minPromptCharacters = minPromptCharacters;
    }

    /**
     * 注入扩展上下文，并生成最终交给 B 线消费的 ReviewExecutionInput。
     */
    public ReviewExecutionInput enrich(ReviewExecutionInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Review execution input must not be null");
        }
        if (input.getContextStatus().getCode() == ContextStatusCode.SKIPPED) {
            return input;
        }

        PullRequestMetadata metadata = pullRequestMetadata(input);
        PromptPayload promptPayload = enrichPromptPayload(input.getPromptPayload(), metadata);
        ContextStatus contextStatus = contextStatus(input, metadata, promptPayload);
        return new ReviewExecutionInput(
                input.getPrContext(),
                input.getDiff(),
                input.getFiles(),
                contextStatus,
                promptPayload
        );
    }

    /**
     * 获取 PR 扩展上下文，获取失败时降级为空元数据。
     */
    private PullRequestMetadata pullRequestMetadata(ReviewExecutionInput input) {
        try {
            return metadataProvider.fetch(input.getPrContext());
        } catch (RuntimeException exception) {
            return new PullRequestMetadata(
                    null,
                    null,
                    List.of(),
                    "PR 扩展上下文获取失败: " + exception.getMessage()
            );
        }
    }

    /**
     * 将 PR title/body/commit message 注入 user prompt。
     */
    private PromptPayload enrichPromptPayload(PromptPayload promptPayload, PullRequestMetadata metadata) {
        return new PromptPayload(
                promptPayload.getSystemPrompt(),
                extendedContext(metadata) + "\n" + promptPayload.getUserPrompt(),
                promptPayload.getOutputSchema()
        );
    }

    /**
     * 生成扩展上下文 prompt 片段。
     */
    private String extendedContext(PullRequestMetadata metadata) {
        StringBuilder context = new StringBuilder();
        context.append("扩展上下文\n");
        appendOptionalLine(context, "PR 标题", metadata.getTitle());
        appendOptionalBlock(context, "PR 正文", metadata.getBody());
        appendCommitMessages(context, metadata.getCommitMessages());
        appendOptionalLine(context, "扩展上下文说明", metadata.getNote());
        if (!metadata.hasAnyContext()) {
            context.append("未获取到可用的 PR 标题、正文或 commit message。\n");
        }
        return context.toString();
    }

    /**
     * 写入可选单行上下文。
     */
    private void appendOptionalLine(StringBuilder context, String name, String value) {
        if (value != null && !value.isBlank()) {
            context.append("- ").append(name).append(": ").append(value).append('\n');
        }
    }

    /**
     * 写入可选块状上下文。
     */
    private void appendOptionalBlock(StringBuilder context, String name, String value) {
        if (value != null && !value.isBlank()) {
            context.append(name).append('\n');
            context.append(value.replace("```", "'''")).append('\n');
        }
    }

    /**
     * 写入 commit message 列表。
     */
    private void appendCommitMessages(StringBuilder context, List<String> commitMessages) {
        if (commitMessages.isEmpty()) {
            return;
        }
        context.append("Commit messages\n");
        for (String message : commitMessages) {
            if (message != null && !message.isBlank()) {
                context.append("- ").append(message.replace("```", "'''")).append('\n');
            }
        }
    }

    /**
     * 根据基础输入、扩展上下文和 prompt 大小生成最终上下文状态。
     */
    private ContextStatus contextStatus(
            ReviewExecutionInput input,
            PullRequestMetadata metadata,
            PromptPayload promptPayload
    ) {
        if (reviewableFiles(input) < minReviewableFiles) {
            return new ContextStatus(ContextStatusCode.INSUFFICIENT, "可审查文件数量不足");
        }
        if (promptPayload.getUserPrompt().length() < minPromptCharacters) {
            return new ContextStatus(ContextStatusCode.INSUFFICIENT, "Review prompt 上下文过少");
        }
        if (!metadata.hasAnyContext() || input.getContextStatus().getCode() == ContextStatusCode.LIMITED) {
            return new ContextStatus(ContextStatusCode.LIMITED, limitedReason(input, metadata));
        }
        return new ContextStatus(ContextStatusCode.FULL, "扩展上下文已注入，Review 上下文充足");
    }

    /**
     * 返回可审查文件数量。
     */
    private int reviewableFiles(ReviewExecutionInput input) {
        return (int) input.getFiles().stream()
                .filter(ReviewTargetFile::isSelected)
                .count();
    }

    /**
     * 生成 LIMITED 状态说明。
     */
    private String limitedReason(ReviewExecutionInput input, PullRequestMetadata metadata) {
        if (!metadata.hasAnyContext()) {
            return "未获取到 PR 扩展上下文，使用 patch 和 snippet 降级审查";
        }
        return input.getContextStatus().getReason();
    }
}
