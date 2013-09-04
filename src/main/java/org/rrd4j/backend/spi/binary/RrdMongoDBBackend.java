package org.rrd4j.backend.spi.binary;

import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * <p>RrdMongoDBBackend class.</p>
 *
 * @author Mathias Bogaert
 */
public class RrdMongoDBBackend extends RrdByteArrayBackend {
    private final DBCollection rrdCollection;
    private volatile boolean dirty = false;

    /**
     * <p>Constructor for RrdMongoDBBackend.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param rrdCollection a {@link com.mongodb.DBCollection} object.
     */
    public RrdMongoDBBackend(String path, DBCollection rrdCollection) {
        super(path);
        this.rrdCollection = rrdCollection;

        BasicDBObject query = new BasicDBObject();
        query.put("path", path);
        DBObject rrdObject = rrdCollection.findOne(query);
        if (rrdObject != null) {
            this.buffer = (byte[]) rrdObject.get("rrd");
        }
    }

    /**
     * <p>write.</p>
     *
     * @param offset a long.
     * @param bytes an array of byte.
     * @throws java.io.IOException if any.
     */
    protected synchronized void write(long offset, byte[] bytes) throws IOException {
        super.write(offset, bytes);
        dirty = true;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        if (dirty) {
            BasicDBObject query = new BasicDBObject();
            query.put("path", getPath());

            DBObject rrdObject = rrdCollection.findOne(query);
            if (rrdObject == null) {
                rrdObject = new BasicDBObject();
                rrdObject.put("path", getPath());
            }
            rrdObject.put("rrd", buffer);
            rrdCollection.save(rrdObject);
        }
    }

    @Override
    public String getUniqId() throws IOException {
        return rrdCollection.getFullName() + "/" + getPath();
    }

}
