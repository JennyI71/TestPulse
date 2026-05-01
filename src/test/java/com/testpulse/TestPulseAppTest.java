package com.testpulse;

import java.io.File;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.testpulse.config.AnalysisConfig;

/**
 * Unit tests for CLI argument parsing.
 */
public class TestPulseAppTest {

    @Test
    public void testParseArgumentsWithDefaults() {
        AnalysisConfig config = TestPulseApp.parseArguments(new String[]{"historical-runs.json"});

        assertEquals("Input file should match", new File("historical-runs.json"), config.getInputFile());
        assertEquals("Default output directory should be test-output",
                new File("test-output"), config.getOutputDir());
        assertEquals("Default threshold should be used",
                3, config.getMinRunThreshold());
        assertEquals("Default format should be all",
                AnalysisConfig.OutputFormat.ALL, config.getOutputFormat());
    }

    @Test
    public void testParseArgumentsWithOptions() {
        AnalysisConfig config = TestPulseApp.parseArguments(new String[]{
                "--input", "historical-runs.json",
                "--output-dir", "custom-output",
                "--min-runs", "5",
                "--format", "json"
        });

        assertEquals("Configured input file should match", new File("historical-runs.json"), config.getInputFile());
        assertEquals("Configured output directory should match", new File("custom-output"), config.getOutputDir());
        assertEquals("Configured threshold should match", 5, config.getMinRunThreshold());
        assertEquals("Configured format should match", AnalysisConfig.OutputFormat.JSON, config.getOutputFormat());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseArgumentsRejectsInvalidThreshold() {
        TestPulseApp.parseArguments(new String[]{"--min-runs", "0", "historical-runs.json"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseArgumentsRejectsUnknownFormat() {
        TestPulseApp.parseArguments(new String[]{"--format", "xml", "historical-runs.json"});
    }

}
