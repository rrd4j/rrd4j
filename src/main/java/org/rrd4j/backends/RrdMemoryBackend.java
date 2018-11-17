package org.rrd4j.backends;

/**
 * Backend to be used to store all RRD bytes in memory.
 *
 */
public class RrdMemoryBackend extends RrdByteArrayBackend {
    /**
     * <p>Constructor for RrdMemoryBackend.</p>
     *
     * @param path a {@link java.lang.String} object.
     */
    protected RrdMemoryBackend(String path) {
        super(path);
    }
}
