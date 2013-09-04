package org.rrd4j.backend.spi.binary;

import java.io.IOException;

/**
 * Backend to be used to store all RRD bytes in memory.
 *
 */
public class RrdMemoryBackend extends RrdByteArrayBackend {
    
    private final RrdMemoryBackendFactory.ByteBuffer bb;
    /**
     * <p>Constructor for RrdMemoryBackend.</p>
     *
     * @param path a {@link java.lang.String} object.
     */
    protected RrdMemoryBackend(String path) {
        super(path);
        bb = new RrdMemoryBackendFactory.ByteBuffer();
    }

    protected RrdMemoryBackend(String path, RrdMemoryBackendFactory.ByteBuffer bb) {
        super(path);
        this.buffer = bb.buffer;
        this.bb = bb;
    }

    /**
     * {@inheritDoc}
     *
     * Reserves a memory section as a RRD storage.
     */
    protected void setLength(long length) throws IOException {
        if (length > Integer.MAX_VALUE) {
            throw new IOException("Illegal length: " + length);
        }

        buffer = new byte[(int) length];
        bb.buffer = buffer;
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackend#getUniqId()
     */
    @Override
    public String getUniqId() throws IOException {
        return getPath();
    }

}
