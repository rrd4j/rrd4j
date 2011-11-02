package org.rrd4j.core;

import java.io.File;
import java.io.IOException;

/**
 * An abstract backend which is used to store RRD data to ordinary files on the disk.
 * <p>
 * Every backend storing RRD data as ordinary files should inherit from it, some check are done
 * in the code for instanceof.
 */
public abstract class RrdFileBackend extends RrdBackend {
    /**
     * Read/write file status.
     */
    protected final boolean readOnly;
    
    protected final File file;

    protected RrdFileBackend(String path, boolean readOnly) {
        super(path);
        this.readOnly = readOnly;
        this.file = new File(path);
    }

    /**
     * Returns canonical path to the file on the disk.
     *
     * @param path File path
     * @return Canonical file path
     * @throws IOException Thrown in case of I/O error
     */
    public static String getCanonicalPath(String path) throws IOException {
        return Util.getCanonicalPath(path);
    }

    /**
     * Returns canonical path to the file on the disk.
     *
     * @return Canonical file path
     * @throws IOException Thrown in case of I/O error
     */
    public String getCanonicalPath() throws IOException {
        return RrdRandomAccessFileBackend.getCanonicalPath(getPath());
    }

    /**
     * Closes the underlying RRD file.
     *
     * @throws IOException Thrown in case of I/O error
     */
    abstract public void close() throws IOException;

        /**
     * Returns RRD file length.
     *
     * @return File length.
     * @throws IOException Thrown in case of I/O error.
     */
    @Override
    public long getLength() throws IOException {
        return file.length();
    }
}