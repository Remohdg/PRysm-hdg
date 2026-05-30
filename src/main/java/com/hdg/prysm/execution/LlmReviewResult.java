package com.hdg.prysm.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LLM Review 执行结果。
 */
public class LlmReviewResult {

    private final List<ReviewFinding> findings;
    private final String summary;
    private final String rawResponse;

    public LlmReviewResult(List<ReviewFinding> findings, String summary, String rawResponse) {
        if (findings == null) {
            throw new IllegalArgumentException("LLM findings must not be null");
        }
        if (findings.stream().anyMatch(finding -> finding == null)) {
            throw new IllegalArgumentException("LLM findings must not contain null values");
        }

        this.findings = Collections.unmodifiableList(new ArrayList<>(findings));
        this.summary = summary;
        this.rawResponse = rawResponse;
    }

    public List<ReviewFinding> getFindings() {
        return findings;
    }

    public String getSummary() {
        return summary;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public boolean hasFindings() {
        return !findings.isEmpty();
    }
}
