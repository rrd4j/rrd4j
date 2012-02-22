package org.rrd4j.core;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Backend which is used to store RRD data to ordinary files on the disk. This was the
 * default factory before 1.4.0 version. This backend is based on the RandomAccessFile class (java.io.* package).
 */
public class RrdRandomAccessFileBackend extends  RrdFileBackend {
    /**
     * Random access file handle.
     */
    protected final RandomAccessFile rafile;

    /**
     * Creates RrdFileBackend object for the given file path, backed by RandomAccessFile object.
     *
     * @param path     Path to a file
     * @param readOnly True, if file should be open in a read-only mode. False otherwise
     * @throws IOException Thrown in case of I/O error
     */
    protected RrdRandomAccessFileBackend(String path, boolean readOnly) throws IOException {
        super(path, readOnly);
        this.rafile = new RandomAccessFile(path, readOnly ? "r" : "rw");
    }

    /**
     * Closes the underlying RRD file.
     *
     * @throws IOException Thrown in case of I/O error
     */
    public void close() throws IOException {
        rafile.close();
    }

    /**
     * Writes bytes to the underlying RRD file on the disk
     *
     * @param offset Starting file offset
     * @param b      Bytes to be written.
     * @throws IOException Thrown in case of I/O error
     */
    protected void write(long offset, byte[] b) throws IOException {
        rafile.seek(offset);
        rafile.write(b);
    }

    /**
     * Reads a number of bytes from the RRD file on the disk
     *
     * @param offset Starting file offset
     * @param b      Buffer which receives bytes read from the file.
     * @throws IOException Thrown in case of I/O error.
     */
    protected void read(long offset, byte[] b) throws IOException {
        rafile.seek(offset);
        if (rafile.read(b) != b.length) {
            throw new IOException("Not enough bytes available in file " + getPath());
        }
    }

    /**
     * Sets length of the underlying RRD file. This method is called only once, immediately
     * after a new RRD file gets created.
     *
     * @param length Length of the RRD file
     * @throws IOException Thrown in case of I/O error.
     */
    protected void setLength(long length) throws IOException {
        rafile.setLength(length);
	}
}
