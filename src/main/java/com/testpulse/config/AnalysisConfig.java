package com.testpulse.config;

import java.io.File;

import com.testpulse.exception.InvalidConfigurationException;

/**
 * Holds CLI configuration values for a TestPulse analysis run.
 * Responsibility: store parsed input file path, output directory, minimum run threshold, and report format.
 */
public class AnalysisConfig {
    private final File inputFile;
    private final File outputDir;
    private final int minRunThreshold;
    private final OutputFormat outputFormat;

    public AnalysisConfig(File inputFile,
                          File outputDir,
                          int minRunThreshold,
                          OutputFormat outputFormat) {
        this.inputFile = inputFile;
        this.outputDir = outputDir;
        this.minRunThreshold = minRunThreshold;
        this.outputFormat = outputFormat;
    }

    public File getInputFile() {
        return inputFile;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public int getMinRunThreshold() {
        return minRunThreshold;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public enum OutputFormat {
        JSON,
        TEXT,
        ALL;

        public boolean writesJson() {
            return this == JSON || this == ALL;
        }

        public boolean writesText() {
            return this == TEXT || this == ALL;
        }

        public static OutputFormat fromValue(String value) {
            if (value == null) {
                throw new InvalidConfigurationException("Unsupported format 'null'. Use json, text, or all.");
            }
            String normalised = value.trim().toUpperCase();
            for (OutputFormat format : values()) {
                if (format.name().equals(normalised)) {
                    return format;
                }
            }
            throw new InvalidConfigurationException("Unsupported format '" + value + "'. Use json, text, or all.");
        }
    }
}
