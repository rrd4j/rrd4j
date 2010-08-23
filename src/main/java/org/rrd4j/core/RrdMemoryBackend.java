package org.rrd4j.core;

import java.io.IOException;

/**
 * Backend to be used to store all RRD bytes in memory.
 */
public class RrdMemoryBackend extends RrdByteArrayBackend {
    protected RrdMemoryBackend(String path) {
        super(path);
    }
}
