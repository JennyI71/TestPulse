package com.testpulse.reader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.testpulse.exception.DataReadException;
import com.testpulse.model.TestResult;
import com.testpulse.model.TestRun;
import com.testpulse.model.TestStatus;

/**
 * Reads JSON test-run data from a file and converts it into domain TestRun objects.
 * Responsibility: parse array or object-based JSON and skip invalid runs while preserving valid entries.
 */
public class JsonTestRunReader implements TestRunReader {
    private static final DateTimeFormatter DEFAULT_FORMATTER =
            DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public List<TestRun> read(File file) throws IOException {
        List<TestRun> runs = new ArrayList<>();

        try (FileReader reader = new FileReader(file)) {
            JsonElement element = JsonParser.parseReader(reader);

            if (element.isJsonObject()) {
                JsonObject jsonObj = element.getAsJsonObject();
                TestRun run = parseTestRun(jsonObj);
                if (run != null) {
                    runs.add(run);
                }
            } else if (element.isJsonArray()) {
                JsonArray jsonArray = element.getAsJsonArray();
                for (JsonElement item : jsonArray) {
                    if (item.isJsonObject()) {
                        TestRun run = parseTestRun(item.getAsJsonObject());
                        if (run != null) {
                            runs.add(run);
                        }
                    }
                }
            } else {
                throw new DataReadException("Input JSON must be an object or an array of objects.");
            }
        } catch (RuntimeException e) {
            throw new DataReadException("Unable to parse JSON input.", e);
        }

        return runs;
    }

    private TestRun parseTestRun(JsonObject jsonObj) {
        String runId = getStringField(jsonObj, "runId", "run-" + System.currentTimeMillis());
        try {
            String timestamp = getStringField(jsonObj, "timestamp", null);
            String environment = getStringField(jsonObj, "environment", "default");

            if (timestamp == null || timestamp.trim().isEmpty()) {
                throw new IllegalArgumentException("Missing required timestamp");
            }

            LocalDateTime parsedTime = parseTimestamp(timestamp);
            TestRun run = new TestRun(runId, parsedTime, environment);

            // Parse tests array
            if (jsonObj.has("tests") && jsonObj.get("tests").isJsonArray()) {
                JsonArray tests = jsonObj.getAsJsonArray("tests");
                for (JsonElement testElement : tests) {
                    if (testElement.isJsonObject()) {
                        JsonObject testObj = testElement.getAsJsonObject();
                        TestResult result = parseTestResult(testObj);
                        if (result != null) {
                            run.addTestResult(result);
                        }
                    }
                }
            }

            return run;
        } catch (Exception e) {
            System.err.println("Error parsing test run '" + runId + "': " + e.getMessage());
            return null;
        }
    }

    private TestResult parseTestResult(JsonObject testObj) {
        try {
            String testName = getStringField(testObj, "name", null);
            if (testName == null) {
                return null;
            }

            String statusStr = getStringField(testObj, "status", "PASSED");
            TestStatus status = TestStatus.fromString(statusStr);

            long executionTime = getLongField(testObj, "executionTimeMs", 0);

            return new TestResult(testName, status, executionTime);
        } catch (Exception e) {
            System.err.println("Error parsing test result: " + e.getMessage());
            return null;
        }
    }

    private String getStringField(JsonObject obj, String field, String defaultValue) {
        if (obj.has(field)) {
            JsonElement element = obj.get(field);
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }
        }
        return defaultValue;
    }

    private long getLongField(JsonObject obj, String field, long defaultValue) {
        if (obj.has(field)) {
            JsonElement element = obj.get(field);
            if (element.isJsonPrimitive()) {
                try {
                    return element.getAsLong();
                } catch (Exception e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            return LocalDateTime.parse(timestamp, DEFAULT_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timestamp: " + timestamp, e);
        }
    }
}
