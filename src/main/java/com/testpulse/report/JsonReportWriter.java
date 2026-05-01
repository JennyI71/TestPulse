package com.testpulse.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.testpulse.model.FlakinessScore;
import com.testpulse.model.TrendClassification;

/**
 * Writes flakiness analysis results to JSON format.
 * Responsibility: serialise scores, trend summaries, and metadata into a machine-readable report.
 */
public class JsonReportWriter {
    private boolean isFlaky(FlakinessScore score) {
        return score.getTrend() != TrendClassification.STABLE;
    }

    public void write(List<FlakinessScore> scores, File outputFile) throws IOException {
        JsonObject report = new JsonObject();

        // Add metadata
        report.addProperty("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        report.addProperty("totalTests", scores.size());

        // Calculate summary statistics
        double avgScore = scores.isEmpty() ? 0.0 :
                scores.stream().mapToDouble(FlakinessScore::getScore).average().orElse(0.0);
        report.addProperty("averageFlakinessScore", Math.round(avgScore * 10000.0) / 10000.0);

        // Count trend classifications
        long newlyFlaky = scores.stream()
                .filter(s -> s.getTrend().getValue().equals("NEWLY_FLAKY"))
                .count();
        long consistentlyFlaky = scores.stream()
                .filter(s -> s.getTrend().getValue().equals("CONSISTENTLY_FLAKY"))
                .count();
        long recovering = scores.stream()
                .filter(s -> s.getTrend().getValue().equals("RECOVERING"))
                .count();
        long stable = scores.stream()
                .filter(s -> s.getTrend() == TrendClassification.STABLE)
                .count();

        JsonObject trendSummary = new JsonObject();
        trendSummary.addProperty("newlyFlaky", newlyFlaky);
        trendSummary.addProperty("consistentlyFlaky", consistentlyFlaky);
        trendSummary.addProperty("recovering", recovering);
        trendSummary.addProperty("stable", stable);
        report.add("trendSummary", trendSummary);

        // Add detailed scores
        JsonArray scoresArray = new JsonArray();
        for (FlakinessScore score : scores) {
            JsonObject scoreObj = new JsonObject();
            scoreObj.addProperty("testName", score.getTestName());
            scoreObj.addProperty("flakinessScore", Math.round(score.getScore() * 10000.0) / 10000.0);
            scoreObj.addProperty("totalRuns", score.getTotalRuns());
            scoreObj.addProperty("failureCount", score.getFailureCount());
            scoreObj.addProperty("statusChangeCount", score.getStatusChangeCount());
            scoreObj.addProperty("trend", score.getTrend().getValue());
            scoresArray.add(scoreObj);
        }
        report.add("flakinessScores", scoresArray);

        JsonArray flakyTestsArray = new JsonArray();
        for (FlakinessScore score : scores) {
            if (!isFlaky(score)) {
                continue;
            }

            JsonObject scoreObj = new JsonObject();
            scoreObj.addProperty("testName", score.getTestName());
            scoreObj.addProperty("flakinessScore", Math.round(score.getScore() * 10000.0) / 10000.0);
            scoreObj.addProperty("totalRuns", score.getTotalRuns());
            scoreObj.addProperty("failureCount", score.getFailureCount());
            scoreObj.addProperty("statusChangeCount", score.getStatusChangeCount());
            scoreObj.addProperty("trend", score.getTrend().getValue());
            flakyTestsArray.add(scoreObj);
        }
        report.add("flakyTests", flakyTestsArray);

        // Write to file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(report, writer);
        }
    }
}
