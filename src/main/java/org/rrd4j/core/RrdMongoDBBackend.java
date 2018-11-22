package org.rrd4j.core;

import java.io.IOException;

import org.rrd4j.core.RrdMongoDBBackendFactory.MongoWrapper;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * <p>RrdMongoDBBackend class.</p>
 *
 * @author Mathias Bogaert
 */
public class RrdMongoDBBackend extends RrdByteArrayBackend {
    private final MongoWrapper wrapper;

    /**
     * <p>Constructor for RrdMongoDBBackend.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @param wrapper a {@link MongoWrapper} object.
     */
    public RrdMongoDBBackend(String path, MongoWrapper wrapper) {
        super(path);
        this.wrapper = wrapper;

        BasicDBObject query = new BasicDBObject("path", path);
        DBObject rrdObject = wrapper.get(query);
        if (rrdObject != null) {
            byte[] buffer = (byte[]) rrdObject.get("rrd");
            setBuffer(buffer);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void close() throws IOException {
        if (isDirty()) {
            BasicDBObject query = new BasicDBObject("path", getPath());
            wrapper.save(query, getBuffer());
        }
    }

}
