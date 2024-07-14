package org.rrd4j.core;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;

public class RrdMongoDBNewBackend extends RrdByteArrayBackend {

    private final MongoCollection<DBObject> rrdCollection;
    /**
     * <p>Constructor for RrdMongoDBBackend.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param rrdCollection a {@link MongoCollection<DBObject>} object.
     */
    public RrdMongoDBNewBackend(String path, MongoCollection<DBObject> rrdCollection) {
        super(path);
        this.rrdCollection = rrdCollection;

        BasicDBObject query = new BasicDBObject("path", path);
        DBObject rrdObject = rrdCollection.find(query).first();
        if (rrdObject != null) {
            byte[] buffer = (byte[]) rrdObject.get("rrd");
            setBuffer(buffer);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void close() {
        if (isDirty()) {
            BasicDBObject query = new BasicDBObject("path", getPath());
            byte[] rrd = getBuffer();
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
    }

}
