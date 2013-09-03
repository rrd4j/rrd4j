package org.rrd4j.core;

import java.io.IOException;

public interface RrdUpdater {
    /**
     * <p>getRrdBackend.</p>
     *
     * @return a {@link org.rrd4j.core.RrdBackend} object.
     */
    RrdBackend getRrdBackend();

    /**
     * <p>copyStateTo.</p>
     *
     * @param updater a {@link org.rrd4j.core.RrdUpdater} object.
     * @throws java.io.IOException if any.
     * @throws BackendException 
     */
    void copyStateTo(RrdUpdater updater) throws IOException;

}
