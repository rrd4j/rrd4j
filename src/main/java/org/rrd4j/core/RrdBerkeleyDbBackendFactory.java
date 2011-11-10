package org.rrd4j.core;

import com.sleepycat.je.*;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * {@link RrdBackendFactory} that uses
 * <a href="http://www.oracle.com/technetwork/database/berkeleydb/overview/index.html">Oracle Berkeley DB Java Edition</a>
 * to read data. Construct a BerkeleyDB {#link Database} object and pass it via the constructor.
 *
 * @author <a href="mailto:m.bogaert@memenco.com">Mathias Bogaert</a>
 */
@RrdBackendMeta("BERKELEY")
public class RrdBerkeleyDbBackendFactory extends RrdBackendFactory {
    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackendFactory#doStart()
     */
    @Override
    protected boolean startBackend() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackendFactory#doStop()
     */
    @Override
    protected boolean stopBackend() {
        rrdDatabase.close();
        rrdDatabase = null;
        return true;
    }

    private Database rrdDatabase;

    private final Set<String> pathCache = new CopyOnWriteArraySet<String>();

    public void setDatabase(Database rrdDatabase) {
        this.rrdDatabase = rrdDatabase;
    }

    public Database getDatabase() {
        return rrdDatabase;
    }

    /**
     * Creates new RrdBerkeleyDbBackend object for the given id (path).
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        if (pathCache.contains(path)) {
            DatabaseEntry theKey = new DatabaseEntry(path.getBytes("UTF-8"));
            DatabaseEntry theData = new DatabaseEntry();

            try {
                rrdDatabase.get(null, theKey, theData, LockMode.DEFAULT);
            }
            catch (DatabaseException de) {
                throw new IOException("BerkeleyDB DatabaseException on " + path + "; " + de.getMessage());
            }

            return new RrdBerkeleyDbBackend(theData.getData(), path, rrdDatabase);
        }
        else {
            return new RrdBerkeleyDbBackend(path, rrdDatabase);
        }
    }

    public void delete(String path) {
        try {
            rrdDatabase.delete(null, new DatabaseEntry(path.getBytes("UTF-8")));
        }
        catch (DatabaseException de) {
            throw new RuntimeException(de.getMessage(), de);
        }
        catch (IOException ie) {
            throw new IllegalArgumentException(path + ": " + ie.getMessage(), ie);
        }

        pathCache.remove(path);
    }

    /**
     * Checks if the RRD with the given id (path) already exists in the database.
     */
    protected boolean exists(String path) throws IOException {
        if (pathCache.contains(path)) {
            return true;
        } else {
            DatabaseEntry theKey = new DatabaseEntry(path.getBytes("UTF-8"));
            theKey.setPartial(0, 0, true); // avoid returning rrd data since we're only checking for existence

            DatabaseEntry theData = new DatabaseEntry();

            try {
                boolean pathExists = rrdDatabase.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS;
                if (pathExists) {
                    pathCache.add(path);
                }
                return pathExists;
            }
            catch (DatabaseException de) {
                throw new IOException("BerkeleyDB DatabaseException on " + path + "; " + de.getMessage());
            }
        }
    }

    protected boolean shouldValidateHeader(String path) {
        return false;
    }

}
