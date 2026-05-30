package com.hdg.prysm.selection;

import com.hdg.prysm.review.PrReviewContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PR6 的整体输出结果。
 *
 * 保存原始 review 上下文，以及过滤、排序后的文件选择结果。
 */
public class ReviewFileSelectionResult {

    private final PrReviewContext reviewContext;
    private final List<ReviewFileSelection> files;

    /**
     * 创建 PR6 文件选择结果，并防御性复制结果列表。
     */
    public ReviewFileSelectionResult(PrReviewContext reviewContext, List<ReviewFileSelection> files) {
        if (reviewContext == null) {
            throw new IllegalArgumentException("Review context must not be null");
        }
        if (files == null) {
            throw new IllegalArgumentException("Review file selections must not be null");
        }
        if (files.stream().anyMatch(file -> file == null)) {
            throw new IllegalArgumentException("Review file selections must not contain null values");
        }

        this.reviewContext = reviewContext;
        this.files = Collections.unmodifiableList(new ArrayList<>(files));
    }

    /**
     * 返回 PR5 产出的原始 review 上下文。
     */
    public PrReviewContext getReviewContext() {
        return reviewContext;
    }

    /**
     * 返回所有文件的选择结果，包括被过滤的文件。
     */
    public List<ReviewFileSelection> getFiles() {
        return files;
    }

    /**
     * 返回进入后续流程的文件。
     */
    public List<ReviewFileSelection> getSelectedFiles() {
        return files.stream()
                .filter(ReviewFileSelection::isSelected)
                .toList();
    }

    /**
     * 返回被过滤的文件及原因。
     */
    public List<ReviewFileSelection> getRejectedFiles() {
        return files.stream()
                .filter(file -> !file.isSelected())
                .toList();
    }
}
