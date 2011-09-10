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
    private volatile boolean dirty = false;

    protected RrdBerkeleyDbBackend(String path, Database rrdDatabase) {
        super(path);
        this.rrdDatabase = rrdDatabase;
    }

    protected RrdBerkeleyDbBackend(byte[] buffer, String path, Database rrdDatabase) {
        super(path);
        this.buffer = buffer;
        this.rrdDatabase = rrdDatabase;
    }

    protected synchronized void write(long offset, byte[] bytes) throws IOException {
        super.write(offset, bytes);
        dirty = true;
    }

    public void close() throws IOException {
        if (dirty) {
            DatabaseEntry theKey = new DatabaseEntry(getPath().getBytes("UTF-8"));
            DatabaseEntry theData = new DatabaseEntry(buffer);

            try {
                // because the database was opened to support transactions, this write is performed
                // using auto commit
                rrdDatabase.put(null, theKey, theData);
            }
            catch (DatabaseException de) {
                throw new IOException(de.getMessage());
            }
        }
    }
}
