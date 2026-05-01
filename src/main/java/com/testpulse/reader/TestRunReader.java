package com.testpulse.reader;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.testpulse.model.TestRun;

/**
 * Interface for reading test run data from files.
 * Responsibility: define an abstraction for loading TestRun objects from storage.
 */
public interface TestRunReader {
    /**
     * Read test run data from a file.
     * @param file the input file to read
     * @return list of test runs parsed from the file
     * @throws IOException if file cannot be read
     */
    List<TestRun> read(File file) throws IOException;
}
