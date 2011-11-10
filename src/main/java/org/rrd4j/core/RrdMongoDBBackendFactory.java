package org.rrd4j.core;

import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

/**
 * {@link RrdBackendFactory} that uses <a href="http://www.mongodb.org/">MongoDB</a> for data storage. Construct a
 * MongoDB {#link DBCollection} and pass it via the constructor.
 *
 * @author Mathias Bogaert
 */
@RrdBackendMeta("MONGODB")
public class RrdMongoDBBackendFactory extends RrdBackendFactory {
    private DBCollection rrdCollection;

    /**
     * Creates a RrdMongoDBBackendFactory. Make sure that the passed {@link DBCollection} has a safe write
     * concern, is capped (if needed) and slaveOk() called if applicable.
     *
     * @param rrdCollection the collection to use for storing RRD byte data
     */
    public RrdMongoDBBackendFactory() {
    }
    
    public void setDBCollection(DBCollection rrdCollection) {
        this.rrdCollection = rrdCollection;
    }
    
    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackendFactory#doStart()
     */
    @Override
    protected boolean startBackend() {
        // make sure we have an index on the path field
        rrdCollection.ensureIndex(new BasicDBObject("path", 1));
        return true;
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackendFactory#doStop()
     */
    @Override
    protected boolean stopBackend() {
        rrdCollection = null;
        return true;
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

}
