package com.hdg.prysm.comment;

import com.hdg.prysm.execution.ReviewFinding;
import com.hdg.prysm.result.ReviewAggregationResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将聚合后的 findings 渲染为一条 Pull Request 评论。
 */
@Component
public class ReviewCommentRenderer {

    public String render(ReviewAggregationResult result) {
        return render(result, "## PRysm 审查结果", null);
    }

    public String renderFastReview(ReviewAggregationResult result) {
        return render(
                result,
                "## PRysm 快速审查结果",
                "快速初筛完成。深度审查仍在进行，稍后会更新本评论；以下结果可能被深度审查修正。"
        );
    }

    private String render(ReviewAggregationResult result, String title, String notice) {
        if (result == null) {
            throw new IllegalArgumentException("Review aggregation result must not be null");
        }

        StringBuilder markdown = new StringBuilder();
        markdown.append(title).append("\n\n");
        if (notice != null && !notice.isBlank()) {
            markdown.append("> ")
                    .append(escapeMarkdownText(notice))
                    .append("\n\n");
        }
        markdown.append("发现 ")
                .append(result.getFindings().size())
                .append(" 个问题。");
        markdown.append("规则结果: ")
                .append(result.getRuleFindingCount())
                .append("，模型结果: ")
                .append(result.getLlmFindingCount())
                .append("，去重数量: ")
                .append(result.getDuplicateCount())
                .append("。\n\n");

        appendSummary(markdown, "规则摘要", result.getRuleSummary());
        appendSummary(markdown, "模型摘要", result.getLlmSummary());

        if (!result.hasFindings()) {
            markdown.append("未发现需要处理的明确问题。\n");
            return markdown.toString();
        }

        for (Map.Entry<String, List<ReviewFinding>> entry : groupByFile(result.getFindings()).entrySet()) {
            markdown.append("### ")
                    .append(escapeMarkdownText(displayFilePath(entry.getKey())))
                    .append("\n\n");
            for (ReviewFinding finding : entry.getValue()) {
                appendFinding(markdown, finding);
            }
        }

        return markdown.toString();
    }

    private static void appendSummary(StringBuilder markdown, String label, String summary) {
        if (summary == null || summary.isBlank()) {
            return;
        }
        markdown.append("**")
                .append(label)
                .append(":** ")
                .append(escapeMarkdownText(summary.trim()))
                .append("\n\n");
    }

    private static Map<String, List<ReviewFinding>> groupByFile(List<ReviewFinding> findings) {
        Map<String, List<ReviewFinding>> grouped = new LinkedHashMap<>();
        for (ReviewFinding finding : findings) {
            grouped.computeIfAbsent(displayFilePath(finding.getFilePath()), ignored -> new java.util.ArrayList<>())
                    .add(finding);
        }
        return grouped;
    }

    private static void appendFinding(StringBuilder markdown, ReviewFinding finding) {
        markdown.append("- **[")
                .append(escapeMarkdownText(finding.getSeverity()))
                .append("] ")
                .append(escapeMarkdownText(finding.getTitle()))
                .append("**");
        String location = location(finding);
        if (!location.isBlank()) {
            markdown.append(" (").append(location).append(")");
        }
        markdown.append("\n");
        markdown.append("  - 来源: `")
                .append(escapeCodeText(finding.getSource()))
                .append("`");
        if (finding.getRuleId() != null && !finding.getRuleId().isBlank()) {
            markdown.append(" / 规则: `")
                    .append(escapeCodeText(finding.getRuleId()))
                    .append("`");
        }
        markdown.append("\n");
        markdown.append("  - 说明: ")
                .append(escapeMarkdownText(finding.getMessage()))
                .append("\n");
        if (finding.getSuggestion() != null && !finding.getSuggestion().isBlank()) {
            markdown.append("  - 建议: ")
                    .append(escapeMarkdownText(finding.getSuggestion()))
                    .append("\n");
        }
        markdown.append("\n");
    }

    private static String location(ReviewFinding finding) {
        if (finding.getLine() != null) {
            return "第 " + finding.getLine() + " 行";
        }
        if (finding.getStartLine() != null && finding.getEndLine() != null) {
            if (finding.getStartLine().equals(finding.getEndLine())) {
                return "第 " + finding.getStartLine() + " 行";
            }
            return "第 " + finding.getStartLine() + "-" + finding.getEndLine() + " 行";
        }
        if (finding.getStartLine() != null) {
            return "第 " + finding.getStartLine() + " 行";
        }
        return "";
    }

    private static String displayFilePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "通用问题";
        }
        return filePath;
    }

    private static String escapeMarkdownText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeCodeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("`", "'");
    }
}
