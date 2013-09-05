package org.rrd4j.backend.spi.binary;

import java.io.IOException;

import org.rrd4j.backend.RrdBackend;
import org.rrd4j.backend.RrdBackendMeta;

/**
 * Factory class which creates actual {@link RrdNioBackend} objects. This is the default factory since
 * 1.4.0 version
 */
@RrdBackendMeta("NIO")
public class RrdNioBackendFactory extends RrdFileBackendFactory {
    /**
     * Period in seconds between consecutive synchronizations when
     * sync-mode is set to SYNC_BACKGROUND. By default in-memory cache will be
     * transferred to the disc every 300 seconds (5 minutes). Default value can be
     * changed via {@link #setSyncPeriod(int)} method.
     */
    public static final int DEFAULT_SYNC_PERIOD = 300; // seconds

    private int syncPeriod = DEFAULT_SYNC_PERIOD;

    /**
     * The core pool size for the sync executor. Defaults to 6.
     */
    public static final int DEFAULT_SYNC_CORE_POOL_SIZE = 6;

    /**
     * The thread pool to pass to newly-created RrdNioBackend instances.
     */
    private RrdSyncThreadPool syncThreadPool = null;
    
    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackendFactory#doStart()
     */
    @Override
    protected boolean startBackend() {
        if(syncThreadPool == null)
            syncThreadPool = new RrdSyncThreadPool();
        return true;
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackendFactory#doStop()
     */
    @Override
    protected boolean stopBackend() {
        syncThreadPool.shutdown();
        syncThreadPool = null;
        return true;
    }

    /**
     * Returns time between two consecutive background synchronizations. If not changed via
     * {@link #setSyncPeriod(int)} method call, defaults to {@link #DEFAULT_SYNC_PERIOD}.
     * See {@link #setSyncPeriod(int)} for more information.
     *
     * @return Time in seconds between consecutive background synchronizations.
     */
    public int getSyncPeriod() {
        return syncPeriod;
    }

    /**
     * Sets time between consecutive background synchronizations.
     *
     * @param syncPeriod Time in seconds between consecutive background synchronizations.
     */
    public void setSyncPeriod(int syncPeriod) {
        this.syncPeriod = syncPeriod;
    }

    /**
     * Creates RrdNioBackend object for the given file path.
     *
     * @param path     File path
     * @param readOnly True, if the file should be accessed in read/only mode.
     *                 False otherwise.
     * @return RrdNioBackend object which handles all I/O operations for the given file path
     * @throws IOException Thrown in case of I/O error.
     */
    protected RrdBackend doOpen(String path, boolean readOnly) throws IOException {
        return new RrdNioBackend(path, readOnly, syncThreadPool, syncPeriod);
    }

    /**
     * @param syncThreadPool the RrdSyncThreadPool to use to sync the memory-mapped files.
     */
    public void setSyncThreadPool(RrdSyncThreadPool syncThreadPool) {
        this.syncThreadPool = syncThreadPool;
    }

    /**
     * @return the syncThreadPool
     */
    public RrdSyncThreadPool getSyncThreadPool() {
        return syncThreadPool;
    }

}
