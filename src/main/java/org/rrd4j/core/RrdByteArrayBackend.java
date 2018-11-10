package org.rrd4j.core;

import java.io.IOException;

/**
 * Abstract byte array based backend.
 *
 */
public abstract class RrdByteArrayBackend extends RrdBackend {
    protected byte[] buffer;

    /**
     * <p>Constructor for RrdByteArrayBackend.</p>
     *
     * @param path a {@link java.lang.String} object.
     */
    protected RrdByteArrayBackend(String path) {
        super(path);
    }

    /**
     * <p>write.</p>
     *
     * @param offset a long.
     * @param bytes an array of byte.
     * @throws java.io.IOException if any.
     * @throws java.lang.IllegalArgumentException if offset is bigger that the possible length.
     */
    protected synchronized void write(long offset, byte[] bytes) throws IOException {
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Illegal offset: " + offset);
        }

        int pos = (int) offset;
        System.arraycopy(bytes, 0, buffer, pos, bytes.length);
    }

    /**
     * <p>read.</p>
     *
     * @param offset a long.
     * @param bytes an array of byte.
     * @throws java.io.IOException if any.
     * @throws java.lang.IllegalArgumentException if offset is bigger that the possible length.
     */
    protected synchronized void read(long offset, byte[] bytes) throws IOException {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Illegal offset: " + offset);
        }

        if (offset + bytes.length <= buffer.length) {
            System.arraycopy(buffer, (int) offset, bytes, 0, bytes.length);
        }
        else {
            throw new RrdBackendException("Not enough bytes available in RRD buffer; RRD " + getPath());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return Number of RRD bytes held in memory.
     */
    public long getLength() {
        return buffer.length;
    }

    /**
     * {@inheritDoc}
     *
     * <p>It will reserves a memory section as a RRD storage.</p>
     * 
     * @throws java.lang.IllegalArgumentException if length is bigger that the possible length.
     */
    protected void setLength(long length) throws IOException {
        if (length < 0 || length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Illegal length: " + length);
        }

        buffer = new byte[(int) length];
    }

    /**
     * This method is required by the base class definition, but it does not
     * releases any memory resources at all.
     *
     * @throws java.io.IOException if any.
     */
    public void close() throws IOException {
        // NOP
    }

    /**
     * This method is overridden to disable high-level caching in frontend RRD4J classes.
     *
     * @return Always returns <code>false</code>. There is no need to cache anything in high-level classes
     *         since all RRD bytes are already in memory.
     */
    protected boolean isCachingAllowed() {
        return false;
    }
}
