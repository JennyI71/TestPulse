package com.testpulse.analyser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.testpulse.model.FlakinessScore;
import com.testpulse.model.TestRun;
import com.testpulse.model.TestStatus;
import com.testpulse.model.TrendClassification;

/**
 * Analyses test runs to identify and score flaky tests.
 * Responsibility: builds per-test status timelines, calculates flakiness scores, and classifies trend behaviour.
 * Inputs: ordered test runs and a minimum-run threshold.
 * Outputs: a sorted list of FlakinessScore results.
 */
public class FlakinessAnalyser {
    private static final double STATUS_CHANGE_WEIGHT = 0.7;
    private static final double FAILURE_WEIGHT = 0.3;

    private final int minRunThreshold;

    public FlakinessAnalyser(int minRunThreshold) {
        this.minRunThreshold = Math.max(1, minRunThreshold);
    }

    /**
     * Analyse test runs and generate flakiness scores.
     * @param testRuns list of test runs to analyse
     * @return sorted list of FlakinessScore objects (descending by score)
     */
    public List<FlakinessScore> analyse(List<TestRun> testRuns) {
        if (testRuns == null || testRuns.isEmpty()) {
            return Collections.emptyList();
        }

        List<TestRun> sortedRuns = new ArrayList<>(testRuns);
        sortedRuns.sort(Comparator.comparing(TestRun::getTimestamp));

        Map<String, List<TestStatus>> testHistories = buildStatusHistory(sortedRuns);

        List<FlakinessScore> scores = new ArrayList<>();
        for (Map.Entry<String, List<TestStatus>> entry : testHistories.entrySet()) {
            if (entry.getValue().size() >= minRunThreshold) {
                scores.add(scoreTest(entry.getKey(), entry.getValue()));
            }
        }

        scores.sort(FlakinessScore::compareTo);
        return scores;
    }

    private Map<String, List<TestStatus>> buildStatusHistory(List<TestRun> sortedRuns) {
        Map<String, List<TestStatus>> history = new HashMap<>();
        for (TestRun run : sortedRuns) {
            for (var result : run.getTestResults().values()) {
                history.computeIfAbsent(result.getTestName(), key -> new ArrayList<>())
                        .add(result.getStatus());
            }
        }
        return history;
    }

    private FlakinessScore scoreTest(String testName, List<TestStatus> statuses) {
        int totalRuns = statuses.size();
        int statusChangeCount = countStatusChanges(statuses);
        int failureCount = countFailures(statuses);

        double statusChangeRatio = totalRuns <= 1 ? 0.0 : (double) statusChangeCount / (totalRuns - 1);
        double failureRatio = totalRuns == 0 ? 0.0 : (double) failureCount / totalRuns;
        double score = (statusChangeRatio * STATUS_CHANGE_WEIGHT) + (failureRatio * FAILURE_WEIGHT);
        score = Math.max(0.0, Math.min(1.0, score));

        TrendClassification trend = classifyTrend(statuses, statusChangeCount);
        return new FlakinessScore(testName, score, totalRuns, failureCount, statusChangeCount, trend);
    }

    private int countStatusChanges(List<TestStatus> statuses) {
        int changes = 0;
        for (int i = 1; i < statuses.size(); i++) {
            if (!statuses.get(i).equals(statuses.get(i - 1))) {
                changes++;
            }
        }
        return changes;
    }

    private int countFailures(List<TestStatus> statuses) {
        int failures = 0;
        for (TestStatus status : statuses) {
            if (status == TestStatus.FAILED || status == TestStatus.ERROR) {
                failures++;
            }
        }
        return failures;
    }

    private TrendClassification classifyTrend(List<TestStatus> statuses, int statusChangeCount) {
        if (statusChangeCount == 0) {
            return TrendClassification.STABLE;
        }

        int totalRuns = statuses.size();
        if (totalRuns < 2) {
            return TrendClassification.CONSISTENTLY_FLAKY;
        }

        int splitPoint = totalRuns / 2;
        List<TestStatus> historicalStatuses = statuses.subList(0, splitPoint);
        List<TestStatus> recentStatuses = statuses.subList(splitPoint, totalRuns);

        boolean historicallyStable = countStatusChanges(historicalStatuses) == 0;
        boolean recentlyFlaky = countStatusChanges(recentStatuses) > 0;

        if (historicallyStable && recentlyFlaky) {
            return TrendClassification.NEWLY_FLAKY;
        }
        if (!historicallyStable && recentlyFlaky) {
            return TrendClassification.CONSISTENTLY_FLAKY;
        }
        return TrendClassification.RECOVERING;
    }
}
