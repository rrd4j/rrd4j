package org.rrd4j.core;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory class which creates actual {@link RrdNioBackend} objects. This is the default factory since
 * 1.4.0 version
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
     * The {@link java.util.concurrent.ScheduledExecutorService} used to periodically sync the mapped file to disk with.
     */
    private volatile ScheduledExecutorService syncExecutor;

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if(syncExecutor != null) {
            syncExecutor.shutdown();
        }
    }

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
     * Creates RrdNioBackend object for the given file path.
     *
     * @param path     File path
     * @param readOnly True, if the file should be accessed in read/only mode.
     *                 False otherwise.
     * @return RrdNioBackend object which handles all I/O operations for the given file path
     * @throws IOException Thrown in case of I/O error.
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        if(syncExecutor == null) {
            synchronized(this) {
                if(syncExecutor == null)
                    syncExecutor = Executors.newScheduledThreadPool(syncPoolSize, new DaemonThreadFactory("RRD4J Sync"));
            }
        }
        return new RrdNioBackend(path, readOnly, syncExecutor, syncPeriod);
    }

    public String getName() {
        return "NIO";
    }

    /**
     * Daemon thread factory used by the monitor executors.
     * <p>
     * This factory creates all new threads used by an Executor in the same ThreadGroup.
     * If there is a SecurityManager, it uses the group of System.getSecurityManager(), else the group
     * of the thread instantiating this DaemonThreadFactory. Each new thread is created as a daemon thread
     * with priority Thread.NORM_PRIORITY. New threads have names accessible via Thread.getName()
     * of "<pool-name> Pool [Thread-M]", where M is the sequence number of the thread created by this factory.
     */
    static class DaemonThreadFactory implements ThreadFactory {
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;
        final String nameSuffix = "]";

        DaemonThreadFactory(String poolName) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = poolName + " Pool [Thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement() + nameSuffix);
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
