package org.rrd4j.backend.spi.binary;

import java.io.File;
import java.io.IOException;

import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.Util;

/**
 * An abstract backend factory which is used to store RRD data to ordinary files on the disk.
 * <p>
 * Every backend factory storing RRD data as ordinary files should inherit from it, some check are done
 * in the code for instanceof.
 *
 */
public abstract class RrdFileBackendFactory extends RrdBackendFactory {
    /**
     * {@inheritDoc}
     *
     * Method to determine if a file with the given path already exists.
     */
    protected boolean exists(String path) {
        return Util.fileExists(path);
    }

    /** {@inheritDoc} */
    protected boolean shouldValidateHeader(String path) throws IOException {
        return true;
    }
    
    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackend#resolveUniqId(java.lang.Object)
     */
    @Override
    public String resolveUniqId(Object id) throws IOException {
        if(id instanceof String) {
            return (new File((String) id)).getCanonicalPath();
        }
        else if(id instanceof File) {
            return ((File) id).getCanonicalPath();
        }
        else {
            throw new IllegalArgumentException("can't resolve canonical path to " + id);
        }
    }

}
