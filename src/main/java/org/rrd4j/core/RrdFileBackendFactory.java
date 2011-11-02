package org.rrd4j.core;

import java.io.IOException;

/**
 * An abstract backend factory which is used to store RRD data to ordinary files on the disk.
 * <p>
 * Every backend factory storing RRD data as ordinary files should inherit from it, some check are done
 * in the code for instanceof.
 */
public abstract class RrdFileBackendFactory extends RrdBackendFactory {
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
}
