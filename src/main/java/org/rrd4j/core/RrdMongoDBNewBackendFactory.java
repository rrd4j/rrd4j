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
 */
@RrdBackendAnnotation(name="MONGODBNEW", shouldValidateHeader=false)
public class RrdMongoDBNewBackendFactory extends RrdBackendFactory {

    private final URI rootUri;
    private final MongoClient client;
    private final MongoCollection<DBObject> rrdCollection;

    /**
     * Creates a RrdMongoDBBackendFactory. Make sure that the passed {@link com.mongodb.client.MongoClient} has a safe write
     * concern, is capped (if needed) and slaveOk() called if applicable.
     * 
     * @param client the client connection
     * @param rrdCollection the collection to use for storing RRD byte data
     */
    public RrdMongoDBNewBackendFactory(MongoClient client, MongoCollection<DBObject> rrdCollection) {
        this.client = client;
        this.rrdCollection = rrdCollection;

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
        rrdCollection.createIndex(new BasicDBObject("path", 1));
    }

    @Override
    public URI getRootUri() {
        return rootUri;
    }

    /** {@inheritDoc} */
    @Override
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdMongoDBNewBackend(path, rrdCollection);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean exists(String path) throws IOException {
        BasicDBObject query = new BasicDBObject();
        query.put("path", path);
        return rrdCollection.countDocuments(query) != 0;
    }

    // Resolve for mongo needs a magic trick because of the way the hosts information are transformed in mongo's library
    // First try a URI without authoritative information, it will be processed later.
    @Override
    protected URI resolve(URI rootUri, URI uri, boolean relative) {
        try {
            URI tryUri = new URI(uri.getScheme(), null, uri.getPath(), uri.getQuery(), uri.getFragment());
            URI resolvedUri = super.resolve(rootUri, tryUri, relative);
            if (resolvedUri == null) {
                return null;
            }
            String rawHost = uri.getRawAuthority();
            if (rawHost == null || rawHost.isEmpty()) {
                return resolvedUri;
            }
            Set<ServerAddress> tryHosts = new HashSet<>();
            for (String hostInfo: rawHost.split(",")) {
                tryHosts.add(resolveServerAddress(hostInfo));
                if (! Collections.disjoint(tryHosts, servers())) {
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

    private ServerAddress resolveServerAddress(String hostInfo) {
        String[] parts = hostInfo.split(":");
        if (parts.length == 1) {
            return new ServerAddress(parts[0]);
        } else if (parts.length == 2) {
            try {
                return new ServerAddress(parts[0], Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("can't parse mongodb URI " + hostInfo);
            }
        } else {
            throw new IllegalArgumentException("can't parse mongodb URI " + hostInfo);
        }
    }

    public List<ServerAddress> servers() {
        return client.getClusterDescription().getServerDescriptions()
                     .stream()
                     .map(ServerDescription::getAddress)
                     .collect(Collectors.toList());
    }

    @Override
    public boolean canStore(URI uri) {
        uri = resolve(rootUri, uri, false);
        if (uri == null) {
            return false;
        }
        // We kept the expensive check for the end, the hosts
        String rawHost = uri.getRawAuthority();
        if (rawHost == null || rawHost.isEmpty()) {
            return true;
        }
        Set<ServerAddress> tryHosts = new HashSet<>();
        for (String hostInfo: rawHost.split(",")) {
            tryHosts.add(resolveServerAddress(hostInfo));
        }
        return ! Collections.disjoint(tryHosts, servers());
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

}
