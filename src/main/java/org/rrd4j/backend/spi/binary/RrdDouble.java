package org.rrd4j.backend.spi.binary;

import java.io.IOException;

import org.rrd4j.core.Util;

class RrdDouble extends RrdPrimitive {
    private double cache;
    private boolean cached = false;

    RrdDouble(Allocated updater, boolean isConstant) throws IOException {
        super(updater, RrdType.DOUBLE, isConstant);
    }

    RrdDouble(Allocated updater) throws IOException {
        super(updater, RrdType.DOUBLE, false);
    }

    void set(double value) throws IOException {
        if (!isCachingAllowed()) {
            writeDouble(value);
        }
        // caching allowed
        else if (!cached || !Util.equal(cache, value)) {
            // update cache
            writeDouble(cache = value);
            cached = true;
        }
    }

    double get() throws IOException {
        if (!isCachingAllowed()) {
            return readDouble();
        }
        else {
            if (!cached) {
                cache = readDouble();
                cached = true;
            }
            return cache;
        }
    }
}
