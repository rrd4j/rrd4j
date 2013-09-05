package org.rrd4j.backend.spi.binary;

public interface Allocated {

    /**
     * <p>getRrdAllocator.</p>
     *
     * @return a {@link org.rrd4j.core.RrdAllocator} object.
     */
    RrdAllocator getRrdAllocator();

    /**
     * <p>getRrdBackend.</p>
     *
     * @return a {@link org.rrd4j.backend.RrdBackend} object.
     */
    RrdBinaryBackend getRrdBackend();

}
