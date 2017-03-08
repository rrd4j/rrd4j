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
    private volatile boolean dirty = false;

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
            buffer = (byte[]) rrdObject.get("rrd");
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
            BasicDBObject query = new BasicDBObject("path", getPath());
            wrapper.save(query, buffer);
        }
    }

}
