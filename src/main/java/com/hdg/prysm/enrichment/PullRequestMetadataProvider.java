package com.hdg.prysm.enrichment;

import com.hdg.prysm.context.PrContext;

/**
 * Pull Request 扩展上下文获取边界。
 */
public interface PullRequestMetadataProvider {

    /**
     * 获取当前 Pull Request 的标题、正文和少量 commit message。
     */
    PullRequestMetadata fetch(PrContext context);
}
