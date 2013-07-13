package org.rrd4j.core;

import java.io.IOException;

class RrdLong extends RrdPrimitive {
    private long cache;
    private boolean cached = false;

    RrdLong(RrdUpdater updater, boolean isConstant) throws IOException {
        super(updater, RrdPrimitive.RRD_LONG, isConstant);
    }

    RrdLong(RrdUpdater updater) throws IOException {
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
