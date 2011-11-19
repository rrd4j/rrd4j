package org.rrd4j.core;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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

    /**
     * The {@link java.util.concurrent.ScheduledExecutorService} used to periodically sync the mapped file to disk with.
     */
    private final ScheduledExecutorService syncExecutor;

    public RrdNioBackendFactory() {
        syncExecutor = Executors.newScheduledThreadPool(DEFAULT_SYNC_CORE_POOL_SIZE, new DaemonThreadFactory("RRD4J Sync"));

        // Add a shutdown hook to stop the thread pool gracefully when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread("RRD4J Sync-ThreadPool-Shutdown") {
            @Override
            public void run() {
                try {
                    // Progress and failure logging arising from the following code cannot be logged, since the
                    // behavior of logging is undefined in shutdown hooks.
                    syncExecutor.shutdown();
                    syncExecutor.awaitTermination(120, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    // Shutting down...so ignore.
                }
            }
        });
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
     * Creates RrdNioBackend object for the given file path.
     *
     * @param path     File path
     * @param readOnly True, if the file should be accessed in read/only mode.
     *                 False otherwise.
     * @return RrdNioBackend object which handles all I/O operations for the given file path
     * @throws IOException Thrown in case of I/O error.
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
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
