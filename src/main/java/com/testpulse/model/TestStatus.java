package com.testpulse.model;

/**
 * Enumeration of possible test statuses.
 * Responsibility: represent normalised test execution outcomes used by the analyser.
 */
public enum TestStatus {
    PASSED("PASSED"),
    FAILED("FAILED"),
    SKIPPED("SKIPPED"),
    ERROR("ERROR");

    private final String value;

    TestStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parse a string to TestStatus, ignoring case.
     */
    public static TestStatus fromString(String status) {
        if (status == null) {
            return ERROR;
        }
        String normalised = status.toUpperCase().trim();
        for (TestStatus ts : TestStatus.values()) {
            if (ts.value.equals(normalised)) {
                return ts;
            }
        }
        return ERROR;
    }
}
