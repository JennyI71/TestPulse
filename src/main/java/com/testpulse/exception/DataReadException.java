package com.testpulse.exception;

import java.io.IOException;

/**
 * Exception thrown when test data cannot be read or parsed correctly.
 * Responsibility: wrap parsing and I/O failures from reading test run input.
 */
public class DataReadException extends IOException {
    private static final long serialVersionUID = 1L;

    public DataReadException(String message) {
        super(message);
    }

    public DataReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
