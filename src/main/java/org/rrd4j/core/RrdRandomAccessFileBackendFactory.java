package org.rrd4j.core;

import java.io.IOException;

/**
 * Factory class which creates actual {@link org.rrd4j.core.RrdRandomAccessFileBackend} objects. This was the default
 * backend factory in Rrd4j before 1.4.0 release.
 *
 */
public class RrdRandomAccessFileBackendFactory extends RrdFileBackendFactory {
    /**
     * {@inheritDoc}
     *
     * Creates RrdFileBackend object for the given file path.
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdRandomAccessFileBackend(path, readOnly);
    }

    /** {@inheritDoc} */
    protected boolean shouldValidateHeader(String path) throws IOException {
        return true;
    }

    /**
     * <p>getName.</p>
     *
     * @return The {@link java.lang.String} "FILE".
     */
    public String getName() {
        return "FILE";
    }
}
