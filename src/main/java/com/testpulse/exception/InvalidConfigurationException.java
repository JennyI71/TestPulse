package com.testpulse.exception;

/**
 * Exception thrown when CLI arguments or configuration values are invalid.
 * Responsibility: signal configuration parsing failures to the CLI entry point.
 */
public class InvalidConfigurationException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public InvalidConfigurationException(String message) {
        super(message);
    }

    public InvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
