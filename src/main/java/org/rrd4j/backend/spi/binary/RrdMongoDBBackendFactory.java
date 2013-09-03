package org.rrd4j.backend.spi.binary;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.rrd4j.core.RrdBackend;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdBackendMeta;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.mongodb.MongoException;

/**
 * {@link RrdBackendFactory} that uses <a href="http://www.mongodb.org/">MongoDB</a> for data storage. Construct a
 * MongoDB {@link com.mongodb.DBCollection} and pass it via the constructor.
 *
 * @author Mathias Bogaert
 */
@RrdBackendMeta("MONGODB")
public class RrdMongoDBBackendFactory extends RrdBackendFactory {
    private DBCollection rrdCollection;

    /**
	 * Set the mongodb collection to use.
     *
     * Make sure that the passed {@link com.mongodb.DBCollection} has a safe write
     * concern, is capped (if needed) and slaveOk() called if applicable.
     *
     * @param rrdCollection the collection to use for storing RRD byte data
     */
    public void setDBCollection(DBCollection rrdCollection) {
        this.rrdCollection = rrdCollection;
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackendFactory#doStart()
     */
    @Override
    protected boolean startBackend() {
        // make sure we have an index on the path field
        try {
            rrdCollection.ensureIndex(new BasicDBObject("path", 1));
        } catch (MongoException e) {
            throw new IllegalStateException("Failed to start the backend " + getName(), e);
        }
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
    protected RrdBackend doOpen(String path, boolean readOnly) throws IOException {
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

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdBackendFactory#getStats()
     */
    @Override
    public Map<String, Number> getStats() {
        CommandResult stats = rrdCollection.getStats();
        Map<String, Number> statsMap = new HashMap<String, Number>(stats.size());
        for(Map.Entry<String, Object> e: stats.entrySet()) {
            if(e.getValue() instanceof Number) {
                statsMap.put(e.getKey(), (Number)e.getValue());
            }
        }
        return statsMap;
    }

}
