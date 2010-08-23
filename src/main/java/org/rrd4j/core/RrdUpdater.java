package org.rrd4j.core;

import java.io.IOException;

interface RrdUpdater {
    RrdBackend getRrdBackend();

    void copyStateTo(RrdUpdater updater) throws IOException;

    RrdAllocator getRrdAllocator();
}
