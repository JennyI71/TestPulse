package com.testpulse.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.testpulse.model.FlakinessScore;
import com.testpulse.model.TrendClassification;

/**
 * Writes flakiness analysis results to a human-readable text report.
 * Responsibility: generate summary statistics and a readable flaky-test section for human review.
 */
public class TextReportWriter {
    private boolean isFlaky(FlakinessScore score) {
        return score.getTrend() != TrendClassification.STABLE;
    }

    public void write(List<FlakinessScore> scores, File outputFile) throws IOException {
        StringBuilder report = new StringBuilder();

        // Header
        report.append("================================================================================\n");
        report.append("TestPulse: Flakiness Detector Report\n");
        report.append("================================================================================\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)).append("\n\n");

        // Summary Statistics
        report.append("SUMMARY STATISTICS\n");
        report.append("-------------------\n");
        report.append("Total tests analysed: ").append(scores.size()).append("\n");

        double avgScore = scores.isEmpty() ? 0.0 :
                scores.stream().mapToDouble(FlakinessScore::getScore).average().orElse(0.0);
        report.append("Average flakiness score: ").append(String.format("%.2f", avgScore)).append("\n");

        long newlyFlaky = scores.stream()
                .filter(s -> s.getTrend() == TrendClassification.NEWLY_FLAKY).count();
        long consistentlyFlaky = scores.stream()
                .filter(s -> s.getTrend() == TrendClassification.CONSISTENTLY_FLAKY).count();
        long recovering = scores.stream()
                .filter(s -> s.getTrend() == TrendClassification.RECOVERING).count();
        long stable = scores.stream()
                .filter(s -> s.getTrend() == TrendClassification.STABLE).count();

        report.append("\nTrend Breakdown:\n");
        report.append("  - Newly Flaky: ").append(newlyFlaky).append("\n");
        report.append("  - Consistently Flaky: ").append(consistentlyFlaky).append("\n");
        report.append("  - Recovering: ").append(recovering).append("\n");
        report.append("  - Stable / Not Flaky: ").append(stable).append("\n\n");

        // Detailed Flaky Tests
        List<FlakinessScore> flakyScores = scores.stream()
                .filter(this::isFlaky)
                .collect(Collectors.toList());

        if (!flakyScores.isEmpty()) {
            report.append("FLAKY TESTS (sorted by flakiness score)\n");
            report.append("=======================================\n\n");

            for (FlakinessScore score : flakyScores) {
                report.append("Test: ").append(score.getTestName()).append("\n");
                report.append("  Flakiness Score: ").append(String.format("%.2f", score.getScore())).append(" (");
                report.append(String.format("%.1f%%", score.getScore() * 100)).append(")\n");
                report.append("  Trend: ").append(score.getTrend()).append("\n");
                report.append("  Total Runs: ").append(score.getTotalRuns()).append("\n");
                report.append("  Failed Runs: ").append(score.getFailureCount()).append("\n");
                report.append("  Status Changes: ").append(score.getStatusChangeCount()).append("\n");
                report.append("\n");
            }
        } else {
            report.append("FLAKY TESTS (sorted by flakiness score)\n");
            report.append("=======================================\n\n");
            report.append("No flaky tests detected.\n\n");
        }

        // Footer
        report.append("================================================================================\n");
        report.append("End of Report\n");
        report.append("================================================================================\n");

        // Write to file
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(report.toString());
        }
    }
}
