package org.rrd4j.core;

import com.sleepycat.je.*;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * {@link RrdBackendFactory} that uses
 * <a href="http://www.oracle.com/technetwork/database/berkeleydb/overview/index.html">Oracle Berkeley DB Java Edition</a>
 * to read data. Construct a BerkeleyDB {@link com.sleepycat.je.Database} object and pass it via the constructor.
 *
 * @author <a href="mailto:m.bogaert@memenco.com">Mathias Bogaert</a>
 */
public class RrdBerkeleyDbBackendFactory extends RrdBackendFactory {
    private final Database rrdDatabase;

    private final Set<String> pathCache = new CopyOnWriteArraySet<String>();

    public RrdBerkeleyDbBackendFactory(Database rrdDatabase) {
        this.rrdDatabase = rrdDatabase;
        RrdBackendFactory.registerAndSetAsDefaultFactory(this);
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

    public String getName() {
        return "BERKELEY";
    }
}
