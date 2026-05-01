package com.testpulse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.testpulse.analyser.FlakinessAnalyser;
import com.testpulse.model.FlakinessScore;
import com.testpulse.model.TestResult;
import com.testpulse.model.TestRun;
import com.testpulse.model.TestStatus;
import com.testpulse.model.TrendClassification;

/**
 * Unit tests for FlakinessAnalyser.
 */
public class FlakinessAnalyserTest {
    private FlakinessAnalyser analyser;

    @Before
    public void setUp() {
        analyser = new FlakinessAnalyser(2);
    }

    @Test
    public void testEmptyTestRuns() {
        List<FlakinessScore> scores = analyser.analyse(new ArrayList<>());
        assertTrue("Empty input should result in empty scores", scores.isEmpty());
    }

    @Test
    public void testSingleTestRun() {
        List<TestRun> runs = new ArrayList<>();
        TestRun run1 = new TestRun("run1", LocalDateTime.now(), "staging");
        run1.addTestResult(new TestResult("test1", TestStatus.PASSED, 100));
        runs.add(run1);

        List<FlakinessScore> scores = analyser.analyse(runs);
        assertTrue("Single run should not meet minimum threshold", scores.isEmpty());
    }

    @Test
    public void testConsistentlyPassing() {
        List<TestRun> runs = createTestRuns(
                new int[][]{
                        {1, 1, 1}  // All PASSED
                }
        );

        List<FlakinessScore> scores = analyser.analyse(runs);
        assertEquals("Consistently passing test should be analysed", 1, scores.size());
        assertEquals("Consistently passing test should be stable",
                TrendClassification.STABLE, scores.get(0).getTrend());
        assertEquals("Consistently passing test should have zero flakiness",
                0.0, scores.get(0).getScore(), 0.0001);
    }

    @Test
    public void testFlakyTest() {
        List<TestRun> runs = createTestRuns(
                new int[][]{
                        {1, 0, 1, 0, 1}  // test1: PASSED, FAILED, PASSED, FAILED, PASSED
                }
        );

        List<FlakinessScore> scores = analyser.analyse(runs);
        assertEquals("Should identify flaky test", 1, scores.size());

        FlakinessScore score = scores.get(0);
        assertEquals("Flaky test should be first", "test1", score.getTestName());
        assertTrue("Score should be non-zero for flaky test", score.getScore() > 0.0);
        assertEquals("Should have 4 status changes", 4, score.getStatusChangeCount());
    }

    @Test
    public void testMinRunThreshold() {
        FlakinessAnalyser analyser3 = new FlakinessAnalyser(3);
        List<TestRun> runs = new ArrayList<>();

        // Create 2 runs (below threshold)
        for (int i = 1; i <= 2; i++) {
            TestRun run = new TestRun("run" + i, LocalDateTime.now().plusDays(i), "staging");
            run.addTestResult(new TestResult("test1", i % 2 == 0 ? TestStatus.FAILED : TestStatus.PASSED, 100));
            runs.add(run);
        }

        List<FlakinessScore> scores = analyser3.analyse(runs);
        assertTrue("Tests below minimum threshold should be excluded", scores.isEmpty());
    }

    @Test
    public void testPrioritisation() {
        List<TestRun> runs = new ArrayList<>();

        // Test 1: Very flaky (4 changes)
        List<TestStatus> test1Statuses = List.of(
                TestStatus.PASSED, TestStatus.FAILED, TestStatus.PASSED,
                TestStatus.FAILED, TestStatus.PASSED
        );

        // Test 2: Moderately flaky (2 changes)
        List<TestStatus> test2Statuses = List.of(
                TestStatus.PASSED, TestStatus.PASSED, TestStatus.FAILED,
                TestStatus.FAILED, TestStatus.FAILED
        );

        for (int i = 0; i < 5; i++) {
            TestRun run = new TestRun("run" + i, LocalDateTime.now().plusDays(i), "staging");
            run.addTestResult(new TestResult("veryFlaky", test1Statuses.get(i), 100));
            run.addTestResult(new TestResult("moderatelyFlaky", test2Statuses.get(i), 100));
            runs.add(run);
        }

        List<FlakinessScore> scores = analyser.analyse(runs);
        assertEquals("Should have 2 flaky tests", 2, scores.size());

        // Very flaky should be first (higher priority)
        assertEquals("Very flaky test should be first", "veryFlaky", scores.get(0).getTestName());
        assertTrue("Very flaky should have higher score",
                scores.get(0).getScore() > scores.get(1).getScore());
    }

    @Test
    public void testStableAndFlakyClassification() {
        List<TestRun> runs = new ArrayList<>();
        List<TestStatus> flakyStatuses = List.of(
                TestStatus.FAILED, TestStatus.PASSED, TestStatus.FAILED, TestStatus.PASSED, TestStatus.FAILED
        );

        for (int i = 0; i < 5; i++) {
            TestRun run = new TestRun("run" + i, LocalDateTime.now().plusDays(i), "staging");
            run.addTestResult(new TestResult("stableTest", TestStatus.PASSED, 100));
            run.addTestResult(new TestResult("flakyTest", flakyStatuses.get(i), 100));
            runs.add(run);
        }

        List<FlakinessScore> scores = analyser.analyse(runs);
        assertEquals("Should analyse both tests", 2, scores.size());

        FlakinessScore stableScore = scores.stream()
                .filter(score -> score.getTestName().equals("stableTest"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing stable test"));
        FlakinessScore flakyScore = scores.stream()
                .filter(score -> score.getTestName().equals("flakyTest"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing flaky test"));

        assertEquals("Stable test should be classified as stable",
                TrendClassification.STABLE, stableScore.getTrend());
        assertEquals("Flaky test should remain consistently flaky",
                TrendClassification.CONSISTENTLY_FLAKY, flakyScore.getTrend());
    }

    /**
     * Helper method to create test runs with given status patterns.
     * 1 = PASSED, 0 = FAILED
     */
    private List<TestRun> createTestRuns(int[][] statusPatterns) {
        List<TestRun> runs = new ArrayList<>();

        for (int i = 0; i < statusPatterns[0].length; i++) {
            TestRun run = new TestRun("run" + (i + 1), LocalDateTime.now().plusDays(i), "staging");
            for (int j = 0; j < statusPatterns.length; j++) {
                TestStatus status = statusPatterns[j][i] == 1 ? TestStatus.PASSED : TestStatus.FAILED;
                run.addTestResult(new TestResult("test" + (j + 1), status, 100));
            }
            runs.add(run);
        }

        return runs;
    }
}
