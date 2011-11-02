package org.rrd4j.core;

import java.io.IOException;

/**
 * Factory class which creates actual {@link RrdSafeFileBackend} objects.
 */
public class RrdSafeFileBackendFactory extends RrdRandomAccessFileBackendFactory {
    /**
     * Default time (in milliseconds) this backend will wait for a file lock.
     */
    public static final long LOCK_WAIT_TIME = 3000L;
    private static long lockWaitTime = LOCK_WAIT_TIME;

    /**
     * Default time between two consecutive file locking attempts.
     */
    public static final long LOCK_RETRY_PERIOD = 50L;
    private static long lockRetryPeriod = LOCK_RETRY_PERIOD;

    /**
     * Creates RrdSafeFileBackend object for the given file path.
     *
     * @param path     File path
     * @param readOnly This parameter is ignored
     * @return RrdSafeFileBackend object which handles all I/O operations for the given file path
     * @throws IOException Thrown in case of I/O error.
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdSafeFileBackend(path, lockWaitTime, lockRetryPeriod);
    }

    public String getName() {
        return "SAFE";
    }

    /**
     * Returns time this backend will wait for a file lock.
     *
     * @return Time (in milliseconds) this backend will wait for a file lock.
     */
    public static long getLockWaitTime() {
        return lockWaitTime;
    }

    /**
     * Sets time this backend will wait for a file lock.
     *
     * @param lockWaitTime Maximum lock wait time (in milliseconds)
     */
    public static void setLockWaitTime(long lockWaitTime) {
        RrdSafeFileBackendFactory.lockWaitTime = lockWaitTime;
    }

    /**
     * Returns time between two consecutive file locking attempts.
     *
     * @return Time (im milliseconds) between two consecutive file locking attempts.
     */
    public static long getLockRetryPeriod() {
        return lockRetryPeriod;
    }

    /**
     * Sets time between two consecutive file locking attempts.
     *
     * @param lockRetryPeriod time (in milliseconds) between two consecutive file locking attempts.
     */
    public static void setLockRetryPeriod(long lockRetryPeriod) {
		RrdSafeFileBackendFactory.lockRetryPeriod = lockRetryPeriod;
	}
}
