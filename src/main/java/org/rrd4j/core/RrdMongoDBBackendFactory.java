package org.rrd4j.core;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import java.io.IOException;

/**
 * @author Mathias Bogaert
 */
public class RrdMongoDBBackendFactory extends RrdBackendFactory {
    private DBCollection rrdCollection;

    public RrdMongoDBBackendFactory(DBCollection rrdCollection) {
        this.rrdCollection = rrdCollection;
    }

    @Override
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdMongoDBBackend(path, rrdCollection);
    }

    @Override
    protected boolean exists(String path) throws IOException {
        BasicDBObject query = new BasicDBObject();
        query.put("path", path);
        return rrdCollection.findOne(query) != null;
    }

    @Override
    protected boolean shouldValidateHeader(String path) throws IOException {
        return false;
    }

    @Override
    public String getName() {
        return "MongoDB";
    }
}
