package com.testpulse.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single test suite run with its metadata and contained test results.
 * Responsibility: provide chronological run data and manage test results by name.
 */
public class TestRun {
    private final String runId;
    private final LocalDateTime timestamp;
    private final String environment;
    private final Map<String, TestResult> testResults;

    public TestRun(String runId, LocalDateTime timestamp, String environment) {
        this.runId = Objects.requireNonNull(runId, "runId cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.environment = Objects.requireNonNull(environment, "environment cannot be null");
        this.testResults = new HashMap<>();
    }

    public String getRunId() {
        return runId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getEnvironment() {
        return environment;
    }

    public void addTestResult(TestResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        testResults.put(result.getTestName(), result);
    }

    public TestResult getTestResult(String testName) {
        return testResults.get(testName);
    }

    public Map<String, TestResult> getTestResults() {
        return Collections.unmodifiableMap(testResults);
    }

    public int getTotalTests() {
        return testResults.size();
    }

    @Override
    public String toString() {
        return "TestRun{" +
               "runId='" + runId + '\'' +
               ", timestamp=" + timestamp +
               ", environment='" + environment + '\'' +
               ", testCount=" + testResults.size() +
               '}';
    }
}
