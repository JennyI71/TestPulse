package com.testpulse.model;

import java.util.Objects;

/**
 * Represents the flakiness analysis result for a single test.
 * Responsibility: store score, run counts, failure count, status changes, and trend classification.
 */
public class FlakinessScore implements Comparable<FlakinessScore> {
    private final String testName;
    private final double score; // 0.0 to 1.0
    private final int totalRuns;
    private final int failureCount;
    private final int statusChangeCount;
    private final TrendClassification trend;

    public FlakinessScore(String testName, double score, int totalRuns,
                         int failureCount, int statusChangeCount,
                         TrendClassification trend) {
        this.testName = Objects.requireNonNull(testName, "testName cannot be null");
        this.score = Math.max(0.0, Math.min(1.0, score));
        this.totalRuns = totalRuns;
        this.failureCount = failureCount;
        this.statusChangeCount = statusChangeCount;
        this.trend = Objects.requireNonNull(trend, "trend cannot be null");
    }

    public String getTestName() {
        return testName;
    }

    public double getScore() {
        return score;
    }

    public int getTotalRuns() {
        return totalRuns;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public int getStatusChangeCount() {
        return statusChangeCount;
    }

    public TrendClassification getTrend() {
        return trend;
    }

    /**
     * Compare by flakiness score in descending order.
     */
    @Override
    public int compareTo(FlakinessScore other) {
        return Double.compare(other.score, this.score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlakinessScore that = (FlakinessScore) o;
        return Objects.equals(testName, that.testName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testName);
    }

    @Override
    public String toString() {
        return "FlakinessScore{" +
               "testName='" + testName + '\'' +
               ", score=" + String.format("%.4f", score) +
               ", totalRuns=" + totalRuns +
               ", failureCount=" + failureCount +
               ", statusChangeCount=" + statusChangeCount +
               ", trend=" + trend +
               '}';
    }
}
