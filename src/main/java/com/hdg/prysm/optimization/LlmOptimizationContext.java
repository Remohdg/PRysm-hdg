package com.hdg.prysm.optimization;

import org.springframework.stereotype.Component;

/**
 * Holds per-review LLM optimization decisions for downstream components and trace reporting.
 */
@Component
public class LlmOptimizationContext {

    private LlmOptimizationDecision currentDecision = LlmOptimizationDecision.baseline(null);

    public LlmOptimizationDecision getCurrentDecision() {
        return currentDecision;
    }

    public void setCurrentDecision(LlmOptimizationDecision currentDecision) {
        if (currentDecision == null) {
            throw new IllegalArgumentException("Optimization decision must not be null");
        }
        this.currentDecision = currentDecision;
    }
}
