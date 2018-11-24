package org.rrd4j.core;

import java.io.IOException;

/**
 * Factory class which creates actual {@link org.rrd4j.core.RrdSafeFileBackend} objects.
 *
 */
@RrdBackendAnnotation(name="SAFE", shouldValidateHeader=true)
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
     * {@inheritDoc}
     *
     * Creates RrdSafeFileBackend object for the given file path.
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdSafeFileBackend(path, lockWaitTime, lockRetryPeriod);
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
