package org.rrd4j.core;

import java.io.IOException;

/**
 * Factory class which creates actual {@link RrdRandomAccessFileBackend} objects. This was the default
 * backend factory in Rrd4j before 1.4.0 release.
 */
public class RrdRandomAccessFileBackendFactory extends RrdFileBackendFactory {
    /**
     * Creates RrdFileBackend object for the given file path.
     *
     * @param path     File path
     * @param readOnly True, if the file should be accessed in read/only mode.
     *                 False otherwise.
     * @return RrdFileBackend object which handles all I/O operations for the given file path
     * @throws IOException Thrown in case of I/O error.
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdRandomAccessFileBackend(path, readOnly);
    }

    /**
     * Method to determine if a file with the given path already exists.
     *
     * @param path File path
     * @return True, if such file exists, false otherwise.
     */
    protected boolean exists(String path) {
        return Util.fileExists(path);
    }

    protected boolean shouldValidateHeader(String path) throws IOException {
        return true;
    }

    public String getName() {
        return "FILE";
    }
}
