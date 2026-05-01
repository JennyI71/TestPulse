package com.testpulse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

import com.testpulse.model.FlakinessScore;
import com.testpulse.model.TrendClassification;
import com.testpulse.report.JsonReportWriter;
import com.testpulse.report.TextReportWriter;

/**
 * Unit tests for report writers.
 */
public class ReportWriterTest {

    @Test
    public void testTextReportOmitsStableTestsFromFlakySection() throws Exception {
        File file = File.createTempFile("testpulse-report", ".txt");
        file.deleteOnExit();

        new TextReportWriter().write(createScores(), file);
        String report = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        assertTrue("Trend summary should include stable count", report.contains("Stable / Not Flaky: 1"));
        assertTrue("Flaky test should appear in flaky section", report.contains("Test: flakyTest"));
        assertFalse("Stable test should not appear in flaky section", report.contains("Test: stableTest"));
    }

    @Test
    public void testJsonReportIncludesStableSummaryAndFlakySubset() throws Exception {
        File file = File.createTempFile("testpulse-report", ".json");
        file.deleteOnExit();

        new JsonReportWriter().write(createScores(), file);
        String report = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        assertTrue("JSON report should include stable summary", report.contains("\"stable\": 1"));
        assertTrue("JSON report should include full score list", report.contains("\"flakinessScores\""));
        assertTrue("JSON report should include flaky subset", report.contains("\"flakyTests\""));
        assertTrue("Stable test should remain in the full score list", report.contains("\"testName\": \"stableTest\""));
    }

    private List<FlakinessScore> createScores() {
        return List.of(
                new FlakinessScore("flakyTest", 0.64, 5, 2, 3, TrendClassification.CONSISTENTLY_FLAKY),
                new FlakinessScore("stableTest", 0.0, 5, 0, 0, TrendClassification.STABLE)
        );
    }
}
