package com.hdg.prysm.enrichment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pull Request 扩展上下文元数据。
 *
 * 包含标题、正文和少量 commit message，供 Review 输入补充上下文。
 */
public class PullRequestMetadata {

    private final String title;
    private final String body;
    private final List<String> commitMessages;
    private final String note;

    /**
     * 创建 Pull Request 元数据，并防御性复制 commit message 列表。
     */
    public PullRequestMetadata(String title, String body, List<String> commitMessages, String note) {
        if (commitMessages == null) {
            throw new IllegalArgumentException("Commit messages must not be null");
        }
        if (commitMessages.stream().anyMatch(message -> message == null)) {
            throw new IllegalArgumentException("Commit messages must not contain null values");
        }

        this.title = title;
        this.body = body;
        this.commitMessages = Collections.unmodifiableList(new ArrayList<>(commitMessages));
        this.note = note;
    }

    /**
     * 返回 Pull Request 标题。
     */
    public String getTitle() {
        return title;
    }

    /**
     * 返回 Pull Request 正文。
     */
    public String getBody() {
        return body;
    }

    /**
     * 返回少量 commit message。
     */
    public List<String> getCommitMessages() {
        return commitMessages;
    }

    /**
     * 返回元数据获取或裁剪说明。
     */
    public String getNote() {
        return note;
    }

    /**
     * 返回是否至少包含一类有效扩展上下文。
     */
    public boolean hasAnyContext() {
        return hasText(title) || hasText(body) || commitMessages.stream().anyMatch(PullRequestMetadata::hasText);
    }

    /**
     * 判断文本是否可用。
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
