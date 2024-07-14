package org.rrd4j.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.MongoNamespace;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.connection.ServerDescription;

/**
 * <p>{@link org.rrd4j.core.RrdBackendFactory} that uses <a href="http://www.mongodb.org/">MongoDB</a> for data storage. Construct a
 * MongoDB {@link com.mongodb.DBCollection} or {@link com.mongodb.client.MongoCollection} and pass it via the constructor.</p>
 * 
 * <p>A simple use case could be </p>
 * <pre>
 * MongoClient mongoClient = ...
 * MongoCollection&lt;DBObject&gt; collection = 
 * RrdBackendFactory factory = new RrdMongoDBBackendFactory(mongoClient, collection, false);
 * RrdBackendFactory.setActiveFactories(factory);
 * RrdDef def = new RrdDef(factory.getUri(...));
 * </pre>
 * 
 * <p>A mongo factory is in the form <code>mongodb://host:port/dbName/collectionName/</code></p>
 *
 * @author Mathias Bogaert
 */
@RrdBackendAnnotation(name="MONGODB", shouldValidateHeader=false)
public class RrdMongoDBBackendFactory extends RrdBackendFactory {

    interface MongoWrapper {
        void makeIndex(BasicDBObject index);
        boolean exists(BasicDBObject query);
        DBObject get(BasicDBObject query);
        void save(BasicDBObject query, byte[] rrd);
        List<ServerAddress> servers();
        void close() throws IOException;
    }

    private final URI rootUri;
    private final MongoWrapper wrapper;

    /**
     * Creates a RrdMongoDBBackendFactory. Make sure that the passed {@link com.mongodb.DBCollection} has a safe write
     * concern, is capped (if needed) and slaveOk() called if applicable. The backend will be registered as the default.
     *
     * @param rrdCollection the collection to use for storing RRD byte data
     * @deprecated create a instance instead
     */
    @Deprecated
    public RrdMongoDBBackendFactory(DBCollection rrdCollection) {
        this(rrdCollection, true);
    }

    /**
     * Creates a RrdMongoDBBackendFactory. Make sure that the passed {@link com.mongodb.DBCollection} has a safe write
     * concern, is capped (if needed) and slaveOk() called if applicable.
     * 
     * @param rrdCollection the collection to use for storing RRD byte data
     * @param registerAsDefault if true, the backend will be registered as the default
     * @deprecated create a instance instead
     */
    @Deprecated
    public RrdMongoDBBackendFactory(final DBCollection rrdCollection, boolean registerAsDefault) {

        // set the RRD backend factory
        if (registerAsDefault) {
            RrdBackendFactory.registerAndSetAsDefaultFactory(this);
        }

        wrapper = new MongoWrapper() {
            @Override
            public void makeIndex(BasicDBObject index) {
                rrdCollection.createIndex(index);
            }
            @Override
            public boolean exists(BasicDBObject query) {
                return rrdCollection.find(query).hasNext();
            }
            @Override
            public DBObject get(BasicDBObject query) {
                return rrdCollection.findOne(query);
            }
            @Override
            public void save(BasicDBObject query, byte[] rrd) {
                DBObject rrdObject = rrdCollection.findOne(query);
                if (rrdObject == null) {
                    rrdObject = new BasicDBObject();
                    rrdObject.put("path", query.get("path"));
                    rrdObject.put("rrd", rrd);
                }
                rrdCollection.save(rrdObject);
            }
            @Override
            public List<ServerAddress> servers() {
                return rrdCollection.getDB().getMongoClient().getClusterDescription().getServerDescriptions()
                        .stream()
                        .map(ServerDescription::getAddress)
                        .collect(Collectors.toList());
            }
            @Override
            public void close() throws IOException {
                // Nothing to close
            }
        };

        DB db = rrdCollection.getDB();

        List<ServerAddress> servers = db.getMongoClient().getClusterDescription().getServerDescriptions()
                .stream()
                .map(ServerDescription::getAddress)
                .collect(Collectors.toList());
        rootUri = buildRootUri(db.getName(), rrdCollection.getName(), servers);
        // make sure we have an index on the path field
        makeIndex();

    }

    /**
     * Creates a RrdMongoDBBackendFactory. Make sure that the passed {@link com.mongodb.client.MongoClient} has a safe write
     * concern, is capped (if needed) and slaveOk() called if applicable.
     * 
     * @param client the client connection
     * @param rrdCollection the collection to use for storing RRD byte data
     * @param registerAsDefault if true, the backend will be registered as the default
     */
    @SuppressWarnings("deprecation")
    public RrdMongoDBBackendFactory(final MongoClient client, final MongoCollection<DBObject> rrdCollection, boolean registerAsDefault) {

        wrapper = new MongoWrapper() {
            @Override
            public void makeIndex(BasicDBObject index) {
                rrdCollection.createIndex(index);
            }
            @Override
            public boolean exists(BasicDBObject query) {
                return rrdCollection.countDocuments(query) != 0;
            }
            @Override
            public DBObject get(BasicDBObject query) {
                return rrdCollection.find(query).first();
            }
            @Override
            public void save(BasicDBObject query, byte[] rrd) {
                String path =  (String) query.get("path");
                DBObject rrdObject = rrdCollection.find(query).first();
                if (rrdObject == null) {
                    rrdObject = new BasicDBObject();
                    rrdObject.put("path", path);
                    rrdObject.put("rrd", rrd);
                    rrdCollection.insertOne(rrdObject);
                } else {
                    rrdObject.put("rrd", rrd);
                    rrdCollection.replaceOne(query, rrdObject);
                }
            }
            @Override
            public List<ServerAddress> servers() {
                return client.getClusterDescription().getServerDescriptions()
                        .stream()
                        .map(ServerDescription::getAddress)
                        .collect(Collectors.toList());
            }
            @Override
            public void close() throws IOException {
                client.close();
            }
        };

        MongoNamespace ns = rrdCollection.getNamespace();
        List<ServerAddress> servers = client.getClusterDescription().getServerDescriptions()
                .stream()
                .map(ServerDescription::getAddress)
                .collect(Collectors.toList());

        rootUri = buildRootUri(ns.getDatabaseName(), ns.getCollectionName(), servers);
        // make sure we have an index on the path field
        makeIndex();

    }

    private URI buildRootUri(String dbName, String collectionName, List<ServerAddress> servers) {
        StringBuilder buffer = new StringBuilder();
        for (ServerAddress sa: servers) {
            buffer.append(sa.getHost()).append(":").append(sa.getPort()).append(",");
        }
        buffer.deleteCharAt(buffer.length() - 1);
        try {
            return new URI("mongodb", buffer.toString(), "/" + dbName + "/" + collectionName + "/", null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private void makeIndex() {
        wrapper.makeIndex(new BasicDBObject("path", 1));
    }

    @Override
    public URI getRootUri() {
        return rootUri;
    }

    /** {@inheritDoc} */
    @Override
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdMongoDBBackend(path, wrapper);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean exists(String path) throws IOException {
        BasicDBObject query = new BasicDBObject();
        query.put("path", path);
        return wrapper.exists(query);
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
                        throw new IllegalArgumentException("can 't parse mongodb URI " + uri);
                    }
                } else {
                    throw new IllegalArgumentException("can 't parse mongodb URI " + uri);
                }
                if (! Collections.disjoint(tryHosts, wrapper.servers())) {
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
                    throw new IllegalArgumentException("can 't parse mongodb URI " + uri);
                }
            } else {
                throw new IllegalArgumentException("can 't parse mongodb URI " + uri);
            }
        }
        return ! Collections.disjoint(tryHosts, wrapper.servers());
    }

    @Override
    public void close() throws IOException {
        wrapper.close();
    }

}
