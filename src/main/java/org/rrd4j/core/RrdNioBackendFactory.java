package org.rrd4j.core;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * Factory class which creates actual {@link org.rrd4j.core.RrdNioBackend} objects. This is the default factory since
 * 1.4.0 version.
 * <h3>Managing the thread pool</h3>
 * <p>Each RrdNioBackendFactory is optionally backed by a {@link org.rrd4j.core.RrdSyncThreadPool}, which it uses to sync the memory-mapped files to
 * disk. In order to avoid having these threads live longer than they should, it is recommended that clients create and
 * destroy thread pools at the appropriate time in their application's life time. Failure to manage thread pools
 * appropriately may lead to the thread pool hanging around longer than necessary, which in turn may cause memory leaks.</p>
 * <p>if sync period is negative, no sync thread will be launched</p>
 *
 */
@RrdBackendAnnotation(name="NIO", shouldValidateHeader=true)
public class RrdNioBackendFactory extends RrdFileBackendFactory {

    /**
     * Supplier that always returns "null" for a {@link RrdSyncThreadPool}.
     */
    public static final Supplier<RrdSyncThreadPool> NULLTHREADPOOL = new Supplier<RrdSyncThreadPool>() {
        @Override
        public RrdSyncThreadPool get() {
            return null;
        }
    };
    
    /**
     * Period in seconds between consecutive synchronizations when
     * sync-mode is set to SYNC_BACKGROUND. By default in-memory cache will be
     * transferred to the disc every 300 seconds (5 minutes). Default value can be
     * changed via {@link #setSyncPeriod(int)} method.
     */
    public static final int DEFAULT_SYNC_PERIOD = 300; // seconds

    private static int defaultSyncPeriod = DEFAULT_SYNC_PERIOD;

    /**
     * The core pool size for the sync executor. Defaults to 6.
     */
    public static final int DEFAULT_SYNC_CORE_POOL_SIZE = 6;

    private static int defaultSyncPoolSize = DEFAULT_SYNC_CORE_POOL_SIZE;

    /**
     * Returns time between two consecutive background synchronizations. If not changed via
     * {@link #setSyncPeriod(int)} method call, defaults to {@link #DEFAULT_SYNC_PERIOD}.
     * See {@link #setSyncPeriod(int)} for more information.
     *
     * @return Time in seconds between consecutive background synchronizations.
     */
    public static int getSyncPeriod() {
        return defaultSyncPeriod;
    }

    /**
     * Sets time between consecutive background synchronizations. If negative, it will disabling syncing for
     * all NIO backend factory.
     *
     * @param syncPeriod Time in seconds between consecutive background synchronizations.
     */
    public static void setSyncPeriod(int syncPeriod) {
        RrdNioBackendFactory.defaultSyncPeriod = syncPeriod;
    }

    /**
     * Returns the number of synchronizing threads. If not changed via
     * {@link #setSyncPoolSize(int)} method call, defaults to {@link #DEFAULT_SYNC_CORE_POOL_SIZE}.
     * See {@link #setSyncPoolSize(int)} for more information.
     *
     * @return Number of synchronizing threads.
     */
    public static int getSyncPoolSize() {
        return defaultSyncPoolSize;
    }

    /**
     * Sets the number of synchronizing threads. It must be set before the first use of this factory.
     * It will not have any effect afterward.
     *
     * @param syncPoolSize Number of synchronizing threads.
     */
    public static void setSyncPoolSize(int syncPoolSize) {
        RrdNioBackendFactory.defaultSyncPoolSize = syncPoolSize;
    }

    private final int syncPeriod;

    /**
     * The thread pool to pass to newly-created RrdNioBackend instances.
     * @see #syncThreadPoolLock
     */
    private RrdSyncThreadPool syncThreadPool;
    
    /**
     * The thread pool factory to use.
     */
    private Supplier<RrdSyncThreadPool> syncThreadPoolFactory = new Supplier<RrdSyncThreadPool>() {
        @Override
        public RrdSyncThreadPool get() {
            return new RrdSyncThreadPool(defaultSyncPoolSize);
        }
     };

    /**
     * Creates a new RrdNioBackendFactory with default settings.
     */
    public RrdNioBackendFactory() {
        this(RrdNioBackendFactory.defaultSyncPeriod);
    }

    /**
     * Creates a new RrdNioBackendFactory.
     *
     * @param syncPeriod If syncPeriod is negative or 0, sync threads are disabled.
     */
    public RrdNioBackendFactory(int syncPeriod) {
        this(syncPeriod, syncPeriod > 0 ? defaultSyncPoolSize : -1);
    }

    /**
     * Creates a new RrdNioBackendFactory.
     *
     * @param syncPeriod
     * @param syncPoolSize The number of threads to use to sync the mapped file to disk, if inferior to 0, sync threads are disabled.
     */
    public RrdNioBackendFactory(int syncPeriod, final int syncPoolSize) {
        this(syncPeriod, syncPoolSize > 0 ? new Supplier<RrdSyncThreadPool>() {
            @Override
            public RrdSyncThreadPool get() {
                return new RrdSyncThreadPool(syncPoolSize);
            }
        } : NULLTHREADPOOL);
    }

    /**
     * Creates a new RrdNioBackendFactory.
     *
     * @param syncPeriod
     * @param syncThreadPool If null, disable background sync threads
     */
    public RrdNioBackendFactory(int syncPeriod, final ScheduledExecutorService syncThreadPool) {
        this(syncPeriod, syncThreadPool != null ? new Supplier<RrdSyncThreadPool>() {
            @Override
            public RrdSyncThreadPool get() {
                return new RrdSyncThreadPool(syncThreadPool);
            }
        } : NULLTHREADPOOL);
    }

    /**
     * Creates a new RrdNioBackendFactory.
     *
     * @param syncPeriod
     * @param syncThreadPoolFactory If null, disable background sync threads
     */
    public RrdNioBackendFactory(int syncPeriod, Supplier<RrdSyncThreadPool> syncThreadPoolFactory) {
        if (syncThreadPool != null && syncPeriod < 0) {
            throw new IllegalArgumentException("Both thread pool defined and negative sync period");
        }
        this.syncPeriod = syncPeriod;
        this.syncThreadPoolFactory = syncThreadPoolFactory;
    }

    /**
     * <p>Setter for the field <code>syncThreadPool</code>.</p>
     *
     * @param syncThreadPool the RrdSyncThreadPool to use to sync the memory-mapped files.
     * @deprecated Create a custom instance instead
     */
    @Deprecated
    public void setSyncThreadPool(RrdSyncThreadPool syncThreadPool) {
        this.syncThreadPool = syncThreadPool;
    }

    /**
     * <p>Setter for the field <code>syncThreadPool</code>.</p>
     *
     * @param syncThreadPool the ScheduledExecutorService that will back the RrdSyncThreadPool used to sync the memory-mapped files.
     * @deprecated Create a custom instance instead
     */
    @Deprecated
    public void setSyncThreadPool(ScheduledExecutorService syncThreadPool) {
        this.syncThreadPool = new RrdSyncThreadPool(syncThreadPool);
    }

    /**
     * {@inheritDoc}
     *
     * Creates RrdNioBackend object for the given file path, using the currently set thread pool factory to create a thread pool.
     * @see #syncThreadPoolFactory
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdNioBackend(path, readOnly, getSyncThreadPool(), syncPeriod);
    }

    /**
     * @return The {@link RrdSyncThreadPool} or null if syncing is disabled
     */
    public RrdSyncThreadPool getSyncThreadPool() {
        if (syncThreadPool == null) {
            syncThreadPool = syncThreadPoolFactory.get();
        }
        return syncThreadPool;
    }

    @Override
    public void close() throws IOException {
        if (syncThreadPool != null) {
            syncThreadPool.shutdown();
            syncThreadPool = null;
        }
    }

}
