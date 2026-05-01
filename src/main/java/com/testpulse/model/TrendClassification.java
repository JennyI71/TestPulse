package com.testpulse.model;

/**
 * Classification of test trend based on historical and recent behaviour.
 * Responsibility: categorise tests as stable, newly flaky, consistently flaky, or recovering.
 */
public enum TrendClassification {
    STABLE("STABLE"),
    NEWLY_FLAKY("NEWLY_FLAKY"),
    CONSISTENTLY_FLAKY("CONSISTENTLY_FLAKY"),
    RECOVERING("RECOVERING");

    private final String value;

    TrendClassification(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
