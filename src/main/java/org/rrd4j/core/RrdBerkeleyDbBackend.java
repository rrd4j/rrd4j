package org.rrd4j.core;

import java.io.IOException;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;

/**
 * Backend which is used to store RRD data to ordinary disk files
 * using <a href="http://www.oracle.com/technology/products/berkeley-db/je/index.html">Oracle Berkeley DB Java Edition</a>.
 *
 * @author <a href="mailto:m.bogaert@memenco.com">Mathias Bogaert</a>
 */
public class RrdBerkeleyDbBackend extends RrdByteArrayBackend {

    private final Database rrdDatabase;

    /**
     * <p>Constructor for RrdBerkeleyDbBackend.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param rrdDatabase a {@link com.sleepycat.je.Database} object.
     */
    protected RrdBerkeleyDbBackend(String path, Database rrdDatabase) {
        super(path);
        this.rrdDatabase = rrdDatabase;
    }

    /**
     * <p>Constructor for RrdBerkeleyDbBackend.</p>
     *
     * @param buffer an array of byte.
     * @param path a {@link java.lang.String} object.
     * @param rrdDatabase a {@link com.sleepycat.je.Database} object.
     */
    protected RrdBerkeleyDbBackend(byte[] buffer, String path, Database rrdDatabase) {
        super(path);
        setBuffer(buffer);
        this.rrdDatabase = rrdDatabase;
    }

    /**
     * <p>close.</p>
     *
     * @throws java.io.IOException if any.
     */
    protected void close() throws IOException {
        if (isDirty()) {
            DatabaseEntry theKey = new DatabaseEntry(getPath().getBytes("UTF-8"));
            DatabaseEntry theData = new DatabaseEntry(getBuffer());

            try {
                // because the database was opened to support transactions, this write is performed
                // using auto commit
                rrdDatabase.put(null, theKey, theData);
            }
            catch (DatabaseException de) {
                throw new RrdBackendException("failed to close the backend", de);
            }
        }
    }
}
