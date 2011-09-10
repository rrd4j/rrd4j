package org.rrd4j.core;

import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @author Mathias Bogaert
 */
public class RrdMongoDBBackend extends RrdByteArrayBackend {
    private final DBCollection rrdCollection;
    private volatile boolean dirty = false;

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

    protected synchronized void write(long offset, byte[] bytes) throws IOException {
        super.write(offset, bytes);
        dirty = true;
    }

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
}
