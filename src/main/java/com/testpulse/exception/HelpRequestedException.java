package com.testpulse.exception;

/**
 * Indicates the user requested command-line help.
 * Responsibility: signal the application to print usage information and exit normally.
 */
public class HelpRequestedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
}
