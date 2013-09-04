package org.rrd4j.backend.spi.binary;

import java.io.IOException;

class RrdLong extends RrdPrimitive {
    private long cache;
    private boolean cached = false;

    RrdLong(Allocated updater, boolean isConstant) throws IOException {
        super(updater, RrdType.LONG, isConstant);
    }

    RrdLong(Allocated updater) throws IOException {
        this(updater, false);
    }

    void set(long value) throws IOException {
        if (!isCachingAllowed()) {
            writeLong(value);
        }
        // caching allowed
        else if (!cached || cache != value) {
            // update cache
            writeLong(cache = value);
            cached = true;
        }
    }

    long get() throws IOException {
        if (!isCachingAllowed()) {
            return readLong();
        }
        else {
            if (!cached) {
                cache = readLong();
                cached = true;
            }
            return cache;
        }
    }
}
