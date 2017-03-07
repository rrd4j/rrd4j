package org.rrd4j.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.ServerAddress;

/**
 * {@link org.rrd4j.core.RrdBackendFactory} that uses <a href="http://www.mongodb.org/">MongoDB</a> for data storage. Construct a
 * MongoDB {@link com.mongodb.DBCollection} and pass it via the constructor.
 *
 * @author Mathias Bogaert
 */
public class RrdMongoDBBackendFactory extends RrdBackendFactory {

    private final DBCollection rrdCollection;
    private final URI rootUri;

    /**
     * Creates a RrdMongoDBBackendFactory. Make sure that the passed {@link com.mongodb.DBCollection} has a safe write
     * concern, is capped (if needed) and slaveOk() called if applicable.
     *
     * @param rrdCollection the collection to use for storing RRD byte data
     */
    public RrdMongoDBBackendFactory(DBCollection rrdCollection) {
        this(rrdCollection, false);
    }

    public RrdMongoDBBackendFactory(DBCollection rrdCollection, boolean registerAsDefault) {
        this.rrdCollection = rrdCollection;

        // make sure we have an index on the path field
        rrdCollection.createIndex(new BasicDBObject("path", 1));

        // set the RRD backend factory
        if (registerAsDefault) {
            RrdBackendFactory.registerAndSetAsDefaultFactory(this);
        }

        String collectionName = rrdCollection.getName();
        String dbName = rrdCollection.getDB().getName();
        List<ServerAddress> servers = rrdCollection.getDB().getMongo().getAllAddress();
        StringBuilder buffer = new StringBuilder();
        for (ServerAddress sa: servers) {
            buffer.append(sa.getHost() + ":" + sa.getPort() + ",");
        }
        buffer.deleteCharAt(buffer.length() - 1);
        try {
            rootUri = new URI("mongodb", buffer.toString(), "/" + dbName + "/" + collectionName + "/", null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }

    }

    @Override
    public URI getRootUri() {
        return rootUri;
    }

    /** {@inheritDoc} */
    @Override
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdMongoDBBackend(path, rrdCollection);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean exists(String path) throws IOException {
        BasicDBObject query = new BasicDBObject();
        query.put("path", path);
        return rrdCollection.find(query).hasNext();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean shouldValidateHeader(String path) throws IOException {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean shouldValidateHeader(URI uri) throws IOException {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "MONGODB";
    }

    /** {@inheritDoc} */
    @Override
    public String getScheme() {
        return "mongodb";
    }
 
    // Resolve for mongo needs a magic trick because of the way the hosts informations are transformed in mongo's library
    // First try a URI without authoritative informations, it will be processed later.
    @Override
    protected URI resolve(URI rootUri, URI uri, boolean relative) {
        try {
            URI tryUri = new URI(uri.getScheme(), null, uri.getPath(), uri.getQuery(), uri.getFragment());
            URI resolvedUri = super.resolve(rootUri, tryUri, relative);
            if (resolvedUri == null) {
                return null;
            }
            String rawHost = uri.getRawAuthority();
            if (rawHost == null || rawHost.length() == 0) {
                return resolvedUri;
            }
            Set<ServerAddress> tryHosts = new HashSet<>();
            for (String hostInfo: rawHost.split(",")) {
                String[] parts = hostInfo.split(":");
                if (parts.length == 1) {
                    tryHosts.add(new ServerAddress(parts[0]));
                } else if (parts.length == 2) {
                    try {
                        tryHosts.add(new ServerAddress(parts[0], Integer.parseInt(parts[1])));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("can 't parse mongodb URI " + uri.toString());
                    }
                } else {
                    throw new IllegalArgumentException("can 't parse mongodb URI " + uri.toString());
                }
                if (! Collections.disjoint(tryHosts, rrdCollection.getDB().getMongo().getAllAddress())) {
                    return resolvedUri;
                } else {
                    return null;
                }
            }
        } catch (URISyntaxException e) {
            return null;
        }
        return null;
    }

    @Override
    public boolean canStore(URI uri) {
        uri = resolve(rootUri, uri, false);
        if (uri == null) {
            return false;
        }
        // We kept the expensive check for the end, the hosts
        String rawHost = uri.getRawAuthority();
        if (rawHost == null || rawHost.length() == 0) {
            return true;
        }
        Set<ServerAddress> tryHosts = new HashSet<>();
        for (String hostInfo: rawHost.split(",")) {
            String[] parts = hostInfo.split(":");
            if (parts.length == 1) {
                tryHosts.add(new ServerAddress(parts[0]));
            } else if (parts.length == 2) {
                try {
                    tryHosts.add(new ServerAddress(parts[0], Integer.parseInt(parts[1])));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("can 't parse mongodb URI " + uri.toString());
                }
            } else {
                throw new IllegalArgumentException("can 't parse mongodb URI " + uri.toString());
            }
        }
        return ! Collections.disjoint(tryHosts, rrdCollection.getDB().getMongo().getAllAddress());
    }

}
