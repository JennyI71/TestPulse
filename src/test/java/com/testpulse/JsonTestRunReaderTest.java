package com.testpulse;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

import com.testpulse.model.TestRun;
import com.testpulse.reader.JsonTestRunReader;

/**
 * Unit tests for JsonTestRunReader.
 */
public class JsonTestRunReaderTest {

    @Test
    public void testReadArrayOfRuns() throws Exception {
        File file = File.createTempFile("testpulse-runs", ".json");
        file.deleteOnExit();

        String json = "[" +
                "{\"runId\":\"run-1\",\"timestamp\":\"2026-03-27T10:00:00\",\"tests\":[{\"name\":\"testA\",\"status\":\"PASSED\"}]}," +
                "{\"runId\":\"run-2\",\"timestamp\":\"2026-03-27T11:00:00\",\"tests\":[{\"name\":\"testA\",\"status\":\"FAILED\"}]}" +
                "]";
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);

        JsonTestRunReader reader = new JsonTestRunReader();
        List<TestRun> runs = reader.read(file);

        assertEquals("Should read both runs", 2, runs.size());
        assertEquals("First run id should match", "run-1", runs.get(0).getRunId());
        assertEquals("Second run id should match", "run-2", runs.get(1).getRunId());
    }

    @Test
    public void testInvalidTimestampSkipsRun() throws Exception {
        File file = File.createTempFile("testpulse-invalid", ".json");
        file.deleteOnExit();

        String json = "[" +
                "{\"runId\":\"bad-run\",\"timestamp\":\"not-a-date\",\"tests\":[{\"name\":\"testA\",\"status\":\"PASSED\"}]}," +
                "{\"runId\":\"good-run\",\"timestamp\":\"2026-03-27T11:00:00\",\"tests\":[{\"name\":\"testA\",\"status\":\"FAILED\"}]}" +
                "]";
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);

        JsonTestRunReader reader = new JsonTestRunReader();
        List<TestRun> runs = reader.read(file);

        assertEquals("Only the valid run should be loaded", 1, runs.size());
        assertEquals("Valid run should be preserved", "good-run", runs.get(0).getRunId());
    }
}
