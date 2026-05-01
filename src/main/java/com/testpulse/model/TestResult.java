package com.testpulse.model;

import java.util.Objects;

/**
 * Represents a single test result from a test run.
 * Responsibility: hold the test name, status, and execution duration for one test case.
 */
public class TestResult {
    private final String testName;
    private final TestStatus status;
    private final long executionTimeMs;

    public TestResult(String testName, TestStatus status, long executionTimeMs) {
        this.testName = Objects.requireNonNull(testName, "testName cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.executionTimeMs = executionTimeMs;
    }

    public String getTestName() {
        return testName;
    }

    public TestStatus getStatus() {
        return status;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestResult that = (TestResult) o;
        return Objects.equals(testName, that.testName) &&
               status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(testName, status);
    }

    @Override
    public String toString() {
        return "TestResult{" +
               "testName='" + testName + '\'' +
               ", status=" + status +
               ", executionTimeMs=" + executionTimeMs +
               '}';
    }
}
