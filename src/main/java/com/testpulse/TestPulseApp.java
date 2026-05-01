package com.testpulse;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.testpulse.analyser.FlakinessAnalyser;
import com.testpulse.config.AnalysisConfig;
import com.testpulse.exception.HelpRequestedException;
import com.testpulse.model.FlakinessScore;
import com.testpulse.model.TestRun;
import com.testpulse.model.TrendClassification;
import com.testpulse.reader.JsonTestRunReader;
import com.testpulse.report.JsonReportWriter;
import com.testpulse.report.TextReportWriter;

/**
 * Main CLI application for TestPulse flakiness analysis.
 * Responsibility: parse arguments, load JSON test history, run analysis, and generate JSON/text reports.
 * Inputs: input file path, output directory, minimum run threshold, and output format.
 * Outputs: console summary and timestamped report files.
 */
public class TestPulseApp {
    static final int DEFAULT_MIN_RUN_THRESHOLD = 3;
    private static final int TOP_FLAKY_TESTS = 5;

    public static void main(String[] args) {
        try {
            AnalysisConfig config = parseArguments(args);

            System.out.println("\n[INFO] TestPulse: Flakiness Detector");
            System.out.println("[INFO] Reading test runs from: " + config.getInputFile().getPath());
            System.out.println("[INFO] Minimum run threshold: " + config.getMinRunThreshold());
            System.out.println("[INFO] Output format: " + config.getOutputFormat().name().toLowerCase());

            // Read test results
            File inputFile = config.getInputFile();
            if (!inputFile.exists()) {
                System.err.println("[ERROR] File not found: " + inputFile.getAbsolutePath());
                System.exit(1);
            }

            JsonTestRunReader reader = new JsonTestRunReader();
            List<TestRun> runs = reader.read(inputFile);
            if (runs.isEmpty()) {
                System.err.println("[ERROR] No valid test runs were found in the input file.");
                System.exit(1);
            }
            System.out.println("[INFO] Loaded " + runs.size() + " test runs");

            // Analyse flakiness
            System.out.println("[INFO] Analysing flakiness...");
            FlakinessAnalyser analyser = new FlakinessAnalyser(config.getMinRunThreshold());
            List<FlakinessScore> scores = analyser.analyse(runs);

            // Create versioned output
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            File outputDir = config.getOutputDir();
            if (outputDir.exists() && !outputDir.isDirectory()) {
                System.err.println("[ERROR] Output path exists and is not a directory: " + outputDir.getAbsolutePath());
                System.exit(1);
            }
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                System.err.println("[ERROR] Failed to create output directory: " + outputDir.getAbsolutePath());
                System.exit(1);
            }

            if (config.getOutputFormat().writesJson()) {
                File jsonReportFile = new File(outputDir, "flakiness-report-" + timestamp + ".json");
                JsonReportWriter jsonWriter = new JsonReportWriter();
                jsonWriter.write(scores, jsonReportFile);
                System.out.println("[INFO] JSON report: " + jsonReportFile.getAbsolutePath());
            }

            if (config.getOutputFormat().writesText()) {
                File textReportFile = new File(outputDir, "flakiness-report-" + timestamp + ".txt");
                TextReportWriter textWriter = new TextReportWriter();
                textWriter.write(scores, textReportFile);
                System.out.println("[INFO] Text report: " + textReportFile.getAbsolutePath());
            }

            // Console summary
            printConsoleSummary(runs, scores);
            System.out.println();

        } catch (HelpRequestedException e) {
            printUsage();
            System.exit(0);
        } catch (IllegalArgumentException e) {
            System.err.println("[ERROR] " + e.getMessage());
            printUsage();
            System.exit(1);
        } catch (IOException e) {
            System.err.println("[ERROR] I/O error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printConsoleSummary(List<TestRun> runs, List<FlakinessScore> scores) {
        List<FlakinessScore> flakyScores = scores.stream()
                .filter(s -> s.getTrend() != TrendClassification.STABLE)
                .collect(Collectors.toList());

        System.out.println("\n[RESULTS]================================");
        System.out.println("Total tests analysed: " + countTests(runs));
        System.out.println("Tests with 3+ runs: " + scores.size());
        System.out.println("Flaky tests detected: " + flakyScores.size());

        if (flakyScores.isEmpty()) {
            System.out.println("No flaky tests detected!");
            System.out.println("====================================");
            return;
        }

        double avgScore = scores.stream()
                .mapToDouble(FlakinessScore::getScore)
                .average()
                .orElse(0.0);
        System.out.println("Average flakiness: " + String.format("%.2f", avgScore));

        long newlyFlaky = scores.stream()
                .filter(s -> s.getTrend() == TrendClassification.NEWLY_FLAKY).count();
        long consistentlyFlaky = scores.stream()
                .filter(s -> s.getTrend() == TrendClassification.CONSISTENTLY_FLAKY).count();
        long recovering = scores.stream()
                .filter(s -> s.getTrend() == TrendClassification.RECOVERING).count();
        long stable = scores.stream()
                .filter(s -> s.getTrend() == TrendClassification.STABLE).count();

        System.out.println("Trends: " + newlyFlaky + " newly flaky, " +
                consistentlyFlaky + " consistently flaky, " + recovering + " recovering, " +
                stable + " stable");

        List<FlakinessScore> topFlaky = flakyScores.stream()
                .sorted(getComparator())
                .limit(TOP_FLAKY_TESTS)
                .collect(Collectors.toList());

        System.out.println("\nTop flaky tests:");
        for (int i = 0; i < topFlaky.size(); i++) {
            FlakinessScore score = topFlaky.get(i);
            System.out.println("" + (i + 1) + ". " + score.getTestName() + " (" +
                    String.format("%.3f", score.getScore()) + ")");
        }

        System.out.println("====================================\n");
    }

    private static java.util.Comparator<FlakinessScore> getComparator() {
        return java.util.Comparator.comparingDouble(FlakinessScore::getScore).reversed();
    }

    private static int countTests(List<TestRun> runs) {
        return (int) runs.stream()
                .flatMap(r -> r.getTestResults().keySet().stream())
                .distinct()
                .count();
    }

    public static AnalysisConfig parseArguments(String[] args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Missing input file.");
        }

        File inputFile = null;
        File outputDir = new File("test-output");
        int minRunThreshold = DEFAULT_MIN_RUN_THRESHOLD;
        AnalysisConfig.OutputFormat outputFormat = AnalysisConfig.OutputFormat.ALL;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help":
                case "-h":
                    throw new HelpRequestedException();
                case "--input":
                case "-i":
                    inputFile = new File(requireValue(args, ++i, arg));
                    break;
                case "--output-dir":
                case "-o":
                    outputDir = new File(requireValue(args, ++i, arg));
                    break;
                case "--min-runs":
                case "-m":
                    minRunThreshold = parsePositiveInteger(requireValue(args, ++i, arg), arg);
                    break;
                case "--format":
                case "-f":
                    outputFormat = AnalysisConfig.OutputFormat.fromValue(requireValue(args, ++i, arg));
                    break;
                default:
                    if (arg.startsWith("--")) {
                        throw new IllegalArgumentException("Unknown argument: " + arg);
                    }
                    if (inputFile != null) {
                        throw new IllegalArgumentException("Multiple input files provided.");
                    }
                    inputFile = new File(arg);
                    break;
            }
        }

        if (inputFile == null) {
            throw new IllegalArgumentException("Missing input file.");
        }

        return new AnalysisConfig(
                inputFile,
                outputDir,
                minRunThreshold,
                outputFormat
        );
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }

    private static int parsePositiveInteger(String value, String option) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw new IllegalArgumentException("Value for " + option + " must be >= 1");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value for " + option + " must be a whole number", e);
        }
    }

    private static void printUsage() {
        System.out.println("TestPulse: Flakiness Detector");
        System.out.println("Usage: java -jar target/testpulse-jar-with-dependencies.jar [options] <input-file.json>");
        System.out.println("Options:");
        System.out.println("  --input, -i <file>           Input JSON file");
        System.out.println("  --output-dir, -o <dir>       Output directory (default: test-output)");
        System.out.println("  --min-runs, -m <number>      Minimum runs required per test (default: " +
                DEFAULT_MIN_RUN_THRESHOLD + ")");
        System.out.println("  --format, -f <value>         Report format: json, text, all (default: all)");
        System.out.println("  --help, -h                   Show usage");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  Quick start: java -jar app.jar sample-data/larger-sample.json");
        System.out.println("  Advanced: java -jar app.jar --input sample-data/larger-sample.json --output-dir reports --min-runs 5 --format json");
    }
}
