package com.hdg.prysm.execution;

/**
 * Review 执行前的上下文充足度状态。
 */
public enum ContextStatusCode {

    FULL,
    LIMITED,
    INSUFFICIENT,
    SKIPPED
}
