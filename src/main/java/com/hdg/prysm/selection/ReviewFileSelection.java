package com.hdg.prysm.selection;

import com.hdg.prysm.review.PrReviewFileContext;

/**
 * PR6 产出的单文件过滤与优先级结果。
 *
 * selected 表示是否进入后续预算分配；priority 数值越小越优先。
 */
public class ReviewFileSelection {

    private final PrReviewFileContext fileContext;
    private final boolean selected;
    private final int priority;
    private final String reason;

    /**
     * 创建单文件选择结果。
     */
    public ReviewFileSelection(
            PrReviewFileContext fileContext,
            boolean selected,
            int priority,
            String reason
    ) {
        if (fileContext == null) {
            throw new IllegalArgumentException("Review file context must not be null");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("Priority must not be negative");
        }
        if (!selected && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Rejected file reason must not be blank");
        }

        this.fileContext = fileContext;
        this.selected = selected;
        this.priority = priority;
        this.reason = reason;
    }

    /**
     * 返回 PR5 产出的文件级 review 上下文。
     */
    public PrReviewFileContext getFileContext() {
        return fileContext;
    }

    /**
     * 返回该文件是否进入后续预算分配和输入组装。
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * 返回文件优先级，数值越小越靠前。
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 返回过滤原因；文件被选中时通常为空。
     */
    public String getReason() {
        return reason;
    }
}
