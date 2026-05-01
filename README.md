# TestPulse: Flakiness Detector

TestPulse is a Java CLI that reads historical test results from JSON, scores each test for instability, classifies its trend, and writes both JSON and text reports to `test-output/`.

## What it detects

A flaky test is a test that changes outcome across repeated runs without corresponding code changes. In this project, flakiness is driven by:

- status changes between runs, such as `PASSED -> FAILED -> PASSED`
- intermittent `FAILED` or `ERROR` outcomes

Stable tests are still included in the overall analysis, but they are marked as `STABLE` and excluded from the flaky-only sections of the reports.

## Features

- reads one JSON file containing either a single run object or an array of runs
- calculates a flakiness score from `0.0` to `1.0`
- ignores tests seen fewer than 3 times in the default CLI flow
- classifies tests as `STABLE`, `NEWLY_FLAKY`, `CONSISTENTLY_FLAKY`, or `RECOVERING`
- generates timestamped `.json` and `.txt` reports in `test-output/`
- prints a short console summary after analysis

## Who is this for?

- Software Developers
- Quality Engineers
- DevOps Engineers

## Architecture overview

**TestPulse follows a simple layered design:**   
A compact CLI entry point parses arguments, a reader layer parses JSON into domain models, the analyser performs scoring and trend classification, and report writers generate machine- and human-readable outputs.


## Requirements

- Java 11+
- Maven 3.6+

## Example workflow

1. Export historical test results from your CI system.
2. Run TestPulse against the exported JSON.
3. Review the generated report to identify unstable tests.
4. Prioritise fixes for the highest scoring flaky tests.

## Design goals

- Keep the architecture simple and modular
- Ensure the scoring algorithm is testable in isolation
- Provide both human-readable and machine-readable outputs
- Demonstrate clean CLI application design in Java

## Build and run

Build the project, run unit tests and create the executable JAR:

```bash
mvn clean package
```

Run unit tests only:
```bash
mvn test
```

Run the analyser against the sample data in this repo. The `sample-data/` folder contains multiple JSON examples for testing. Change the JSON filename to choose a different sample dataset:

```bash
java -jar target/testpulse-jar-with-dependencies.jar sample-data/small-sample.json
```

Use the larger dataset instead:

```bash
java -jar target/testpulse-jar-with-dependencies.jar sample-data/larger-sample.json
```

Reports are written to:

- `test-output/flakiness-report-<timestamp>.json`
- `test-output/flakiness-report-<timestamp>.txt`

Artefacts:

- `target/flakiness-detector-1.0.0.jar`
- `target/testpulse-jar-with-dependencies.jar`

## Input format

The input file can contain either:

- one JSON object representing a single test run
- a JSON array of test-run objects

Example:

```json
[
  {
    "runId": "run-001",
    "timestamp": "2026-03-27T10:00:00",
    "environment": "staging",
    "tests": [
      {
        "name": "com.example.LoginTests.testValidCredentials",
        "status": "PASSED",
        "executionTimeMs": 150
      },
      {
        "name": "com.example.LoginTests.testInvalidPassword",
        "status": "FAILED",
        "executionTimeMs": 200
      }
    ]
  }
]
```

Supported fields:

- `runId`: optional string; defaults to a generated value if omitted
- `timestamp`: required ISO-8601 datetime string
- `environment`: optional string; defaults to `default`
- `tests`: optional array of test results
- `tests[].name`: required string
- `tests[].status`: optional string; supported values are `PASSED`, `FAILED`, `SKIPPED`, `ERROR`
- `tests[].executionTimeMs`: optional number; defaults to `0`

Notes:

- invalid or missing timestamps cause that run to be skipped
- unknown statuses are treated as `ERROR`
- if the same test name appears twice in one run, the last value wins

## CLI options

The application supports positional or named input arguments.

- `java -jar target/testpulse-jar-with-dependencies.jar sample-data/larger-sample.json`
- `java -jar target/testpulse-jar-with-dependencies.jar --input sample-data/larger-sample.json`

Available options:

- `--input`, `-i`: input JSON file
- `--output-dir`, `-o`: output directory for reports
- `--min-runs`, `-m`: minimum number of runs required before a test is included in analysis
- `--format`, `-f`: `json`, `text`, or `all`
- `--help`, `-h`: show usage information

## How scoring works

For each test, TestPulse calculates:

- `statusChangeRatio = statusChanges / (totalRuns - 1)`
- `failureRatio = failedOrErrorRuns / totalRuns`
- `score = (0.7 * statusChangeRatio) + (0.3 * failureRatio)`

This weights outcome instability more heavily than raw failure frequency.

Example:

- runs: 5
- failures: 2
- status changes: 3
- `statusChangeRatio = 3 / 4 = 0.75`
- `failureRatio = 2 / 5 = 0.40`
- `score = (0.7 * 0.75) + (0.3 * 0.40) = 0.645`

## Trend classification

Trend classification compares the first half of a test's history with the second half.

- `STABLE`: no status changes across the observed history
- `NEWLY_FLAKY`: historically stable, but the recent half has changes
- `CONSISTENTLY_FLAKY`: changes appear across the history, including the recent half
- `RECOVERING`: historically unstable, but the recent half is stable

## Output

The JSON report includes:

- metadata such as generation time and average flakiness
- `trendSummary` counts
- `flakinessScores` for every analysed test that met the minimum run threshold
- `flakyTests`, which excludes `STABLE` tests

Example shape:

```json
{
  "generatedAt": "2026-03-27T14:24:25.2817839",
  "totalTests": 5,
  "averageFlakinessScore": 0.235,
  "trendSummary": {
    "newlyFlaky": 0,
    "consistentlyFlaky": 2,
    "recovering": 0,
    "stable": 3
  },
  "flakinessScores": [
    {
      "testName": "com.example.LoginTests.testInvalidPassword",
      "flakinessScore": 0.645,
      "totalRuns": 5,
      "failureCount": 2,
      "statusChangeCount": 3,
      "trend": "CONSISTENTLY_FLAKY"
    }
  ],
  "flakyTests": [
    {
      "testName": "com.example.LoginTests.testInvalidPassword",
      "flakinessScore": 0.645,
      "totalRuns": 5,
      "failureCount": 2,
      "statusChangeCount": 3,
      "trend": "CONSISTENTLY_FLAKY"
    }
  ]
}
```

The text report contains the same information in a human-readable summary and lists only non-stable tests in the `FLAKY TESTS` section.

## Console output

A typical run prints:

```text
[INFO] TestPulse: Flakiness Detector
[INFO] Reading test runs from: sample-data/larger-sample-.json
[INFO] Minimum run threshold: 3
[INFO] Output format: all
[INFO] JSON report: C:\path\to\test-output\flakiness-report-123.json
[INFO] Text report: C:\path\to\test-output\flakiness-report-123.txt

[RESULTS]================================
Total tests analysed: 5
Tests with 3+ runs: 5
Flaky tests detected: 2
Average flakiness: 0.23
Trends: 0 newly flaky, 2 consistently flaky, 0 recovering, 3 stable

Top flaky tests:
1. com.example.LoginTests.testInvalidPassword (0.645)
2. com.example.CheckoutTests.testPaymentRetry (0.602)
====================================
```

## Project structure

Main components:

- `TestPulseApp`: CLI entry point
- `JsonTestRunReader`: JSON parsing and run construction
- `FlakinessAnalyser`: scoring and trend classification
- `JsonReportWriter`: machine-readable report output
- `TextReportWriter`: human-readable report output
- model classes under `com.testpulse.model`

Tests:

- `FlakinessAnalyserTest`: scoring, filtering, ordering, and trend behaviour
- `JsonTestRunReaderTest`: reader behaviour and invalid timestamp handling
- `ReportWriterTest`: JSON/text report structure and flaky-only filtering
- `TestPulseAppTest`: CLI argument parsing and validation

## Architecture rationale

The code is intentionally split into reader, analyser, model, report-writer, and CLI layers.

- the reader is responsible for converting raw JSON into validated domain objects
- the analyser owns scoring and trend classification logic
- the report writers transform analysis results into human-readable and machine-readable outputs
- the CLI entry point handles configuration, orchestration, and process-level error reporting

This separation keeps the flakiness algorithm testable in isolation and makes the application easier to evolve without coupling parsing, business rules, and output formatting together.

## Review response and improvements made

The project now includes explicit changes made in response to review feedback:

- stable tests are classified as `STABLE` rather than incorrectly reported as consistently flaky
- flaky-only sections now exclude stable tests while overall averages still include them
- invalid timestamps are rejected instead of being silently replaced with the current time, which improves determinism
- the CLI is configurable for input path, output directory, run threshold, and output format
- report and reader behaviour now have dedicated automated tests

These changes strengthen correctness, make the tool more release-ready, and provide clearer evidence of iterative problem-solving.

## Future improvements

- the minimum run threshold is hardcoded to `3` in the CLI entry point
- reader coverage is lighter than analyser coverage
- the input parser accepts ISO-style datetimes only

## Limitations

* Designed for historical data analysis (real-time CI integration is a future enhancement)
* Requires JSON input in a specific schema
* Does not automatically detect code changes between runs
* Uses heuristic scoring rather than machine learning

## Performance Notes

* Analysis runs in near-linear time due to pre-indexing test histories.
* Designed to handle thousands of test results efficiently.

## Extensibility

The modular architecture supports easy extensions:

* Support CSV or XML readers by implementing new reader classes
* Add a web dashboard by integrating report writers with a web framework
* Integrate into CI pipelines via API wrappers or plugins
* Add database storage by replacing file-based readers/writers with ORM layers

## Architecture Diagram

```
CLI (TestPulseApp)
    ↓
Config (AnalysisConfig)
    ↓
Reader (JsonTestRunReader)
    ↓
Indexer (TestHistoryIndexer)
    ↓
Scorer (FlakinessScorer)
    ↓
Analyser (FlakinessAnalyser)
    ↓
Report Writers (JsonReportWriter, TextReportWriter)
```

---

**Author:** BP0296795  
**Version:** v1.0.0
