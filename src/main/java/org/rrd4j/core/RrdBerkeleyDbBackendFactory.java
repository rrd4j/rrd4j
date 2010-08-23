package org.rrd4j.core;

import com.sleepycat.je.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * {@link RrdBackendFactory} that uses <a href="http://www.oracle.com/technology/products/berkeley-db/je/index.html">Oracle Berkeley DB Java Edition</a>
 * to read data. Call {@link #init()} after instantiation and {@link #destroy()} when tearing down
 * (or when using Spring use init-method and destroy-method).
 *
 * <p>NOTE: you can set the used Berkeley DB name using {@link #setRrdDatabaseName(String)}</p>
 *
 * @author <a href="mailto:m.bogaert@memenco.com">Mathias Bogaert</a>
 */
public class RrdBerkeleyDbBackendFactory extends RrdBackendFactory {
    private String homeDirectory = ".";

    private Environment environment;
    private Database rrdDatabase;
    private String rrdDatabaseName = "rrd4j";

    private final Set<String> knownPaths = new CopyOnWriteArraySet<String>();

    public void init() throws Exception {
        // set the RRD backend factory
        RrdBackendFactory.registerAndSetAsDefaultFactory(this);

        final EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        environment = new Environment(new File(homeDirectory), envConfig);

        final DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);
        rrdDatabase = environment.openDatabase(null, rrdDatabaseName, dbConfig);
    }

    public void destroy() throws Exception {
        if (rrdDatabase != null) {
            rrdDatabase.close();
        }
        if (environment != null) {
            environment.close();
        }
    }

    /**
     * Creates new RrdBerkeleyDbBackend object for the given id (path).
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        if (knownPaths.contains(path)) {
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

        knownPaths.remove(path);
    }

    /**
     * Checks if the RRD with the given id (path) already exists in the database.
     */
    protected boolean exists(String path) throws IOException {
        if (!knownPaths.contains(path)) {
            DatabaseEntry theKey = new DatabaseEntry(path.getBytes("UTF-8"));
            theKey.setPartial(0, 0, true); // avoid returning rrd data since we're only checking for existence

            DatabaseEntry theData = new DatabaseEntry();

            try {
                boolean pathExists = rrdDatabase.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS;
                if (pathExists) {
                    knownPaths.add(path);
                }
                return pathExists;
            }
            catch (DatabaseException de) {
                throw new IOException("BerkeleyDB DatabaseException on " + path + "; " + de.getMessage());
            }
        }
        else {
            return true;
        }
    }

    protected boolean shouldValidateHeader(String path) {
        return false;
    }

    public String getName() {
        return "BERKELEY";
    }

    public void setHomeDirectory(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    public void setRrdDatabaseName(String rrdDatabaseName) {
        this.rrdDatabaseName = rrdDatabaseName;
    }
}
