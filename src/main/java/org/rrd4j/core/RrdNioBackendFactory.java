package org.rrd4j.core;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Factory class which creates actual {@link RrdNioBackend} objects. This is the default factory since
 * 1.4.0 version.
 * <h3>Managing the thread pool</h3>
 * Each RrdNioBackendFactory is backed by a {@link RrdSyncThreadPool}, which it uses to sync the memory-mapped files to
 * disk. In order to avoid having these threads live longer than they should, it is recommended that clients create and
 * destroy thread pools at the appropriate time in their application's life time. Failure to manage thread pools
 * appropriately may lead to the thread pool hanging around longer than necessary, which in turn may cause memory leaks.
 */
public class RrdNioBackendFactory extends RrdFileBackendFactory {
    /**
     * Period in seconds between consecutive synchronizations when
     * sync-mode is set to SYNC_BACKGROUND. By default in-memory cache will be
     * transferred to the disc every 300 seconds (5 minutes). Default value can be
     * changed via {@link #setSyncPeriod(int)} method.
     */
    public static final int DEFAULT_SYNC_PERIOD = 300; // seconds

    private static int syncPeriod = DEFAULT_SYNC_PERIOD;

    /**
     * The core pool size for the sync executor. Defaults to 6.
     */
    public static final int DEFAULT_SYNC_CORE_POOL_SIZE = 6;

    private static int syncPoolSize = DEFAULT_SYNC_CORE_POOL_SIZE;

    /**
     * The thread pool to pass to newly-created RrdNioBackend instances.
     */
    private RrdSyncThreadPool syncThreadPool;

    /**
     * Returns time between two consecutive background synchronizations. If not changed via
     * {@link #setSyncPeriod(int)} method call, defaults to {@link #DEFAULT_SYNC_PERIOD}.
     * See {@link #setSyncPeriod(int)} for more information.
     *
     * @return Time in seconds between consecutive background synchronizations.
     */
    public static int getSyncPeriod() {
        return syncPeriod;
    }

    /**
     * Sets time between consecutive background synchronizations.
     *
     * @param syncPeriod Time in seconds between consecutive background synchronizations.
     */
    public static void setSyncPeriod(int syncPeriod) {
        RrdNioBackendFactory.syncPeriod = syncPeriod;
    }

    /**
     * Returns the number of synchronizing threads. If not changed via
     * {@link #setSyncPoolSize(int)} method call, defaults to {@link #DEFAULT_SYNC_CORE_POOL_SIZE}.
     * See {@link #setSyncPoolSize(int)} for more information.
     *
     * @return Number of synchronizing threads.
     */
    public static int getSyncPoolSize() {
        return syncPoolSize;
    }

    /**
     * Sets the number of synchronizing threads. It must be set before the first use of this factory.
     * It will not have any effect afterward.
     *
     * @param syncPoolSize Number of synchronizing threads.
     */
    public static void setSyncPoolSize(int syncPoolSize) {
        RrdNioBackendFactory.syncPoolSize = syncPoolSize;
    }

    /**
     * Creates a new RrdNioBackendFactory. One should call {@link #setSyncThreadPool(RrdSyncThreadPool syncThreadPool)}
     * or {@link #setSyncThreadPool(RrdSyncThreadPool syncThreadPool)} before the first call to 
     * {@link #open(String path, boolean readOnly)}.
     * Failure to do so will lead to memory leaks in anything but the simplest applications because the underlying thread pool will not
     * be shut down cleanly. Read the Javadoc for this class to understand why using this constructor is discouraged.
     * <p/>
     */
    public RrdNioBackendFactory() {
        super();
    }

    /**
     * @param syncThreadPool the RrdSyncThreadPool to use to sync the memory-mapped files.
     */
    public void setSyncThreadPool(RrdSyncThreadPool syncThreadPool) {
        this.syncThreadPool = syncThreadPool;
    }

    /**
     * @param syncThreadPool the ScheduledExecutorService that will back the RrdSyncThreadPool  used to sync the memory-mapped files.
     */
    public void setSyncThreadPool(ScheduledExecutorService syncThreadPool) {
        this.syncThreadPool = new RrdSyncThreadPool(syncThreadPool);
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
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        // Instantiate a thread pool if none was provided
        if(syncThreadPool == null)
            syncThreadPool = DefaultSyncThreadPool.INSTANCE;

        return new RrdNioBackend(path, readOnly, syncThreadPool, syncPeriod);
    }

    public String getName() {
        return "NIO";
    }

    /**
     * This is a holder class as per the "initialisation on demand" Java idiom. The only purpose of this holder class is
     * to ensure that the thread pool is created lazily the first time that it is needed, and not before.
     * <p/>
     * In practice this thread pool will be used if clients rely on the factory returned by {@link
     * org.rrd4j.core.RrdBackendFactory#getDefaultFactory()}, but not if clients provide their own backend instance when
     * creating {@code RrdDb} instances.
     */
    private static class DefaultSyncThreadPool
    {
        /**
         * The default thread pool used to periodically sync the mapped file to disk with.
         */
        static RrdSyncThreadPool INSTANCE = new RrdSyncThreadPool(syncPoolSize);
    }
}
