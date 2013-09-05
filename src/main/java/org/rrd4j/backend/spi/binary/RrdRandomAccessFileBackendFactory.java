package org.rrd4j.backend.spi.binary;

import java.io.IOException;

import org.rrd4j.backend.RrdBackend;
import org.rrd4j.backend.RrdBackendMeta;
import org.rrd4j.core.Util;

/**
 * Factory class which creates actual {@link RrdRandomAccessFileBackend} objects. This was the default
 * backend factory in Rrd4j before 1.4.0 release.
 */
@RrdBackendMeta("FILE")
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
    protected RrdBackend doOpen(String path, boolean readOnly) throws IOException {
        return new RrdRandomAccessFileBackend(path, readOnly);
    }

    /**
     * Method to determine if a file with the given path already exists.
     *
     * @param path File path
     * @return True, if such file exists, false otherwise.
     */
    public boolean exists(String path) {
        return Util.fileExists(path);
    }

    public boolean shouldValidateHeader(String path) throws IOException {
        return true;
    }

    @Override
    protected boolean  startBackend() {
        return true;
    }

    @Override
    protected boolean stopBackend() {
        return true;
    }

}
