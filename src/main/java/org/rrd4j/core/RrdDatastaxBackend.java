package org.rrd4j.core;

import com.datastax.driver.mapping.Mapper;


import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>RrdDatastaxBackend class.</p>
 *
 * @author Kasper Fock
 */
public class RrdDatastaxBackend extends RrdByteArrayBackend {
    private final Mapper<RrdDatastax> mapper;
    private volatile boolean dirty = false;

    /**
     * <p>Constructor for RrdDatastaxBackend.</p>
     *
     * @param path a {@link String} object.
     * @param mapper datastax mapper for RrdDatastax
     */
    public RrdDatastaxBackend(String path, Mapper<RrdDatastax> mapper) {
        super(path);
        this.mapper = mapper;

        RrdDatastax rrdObject = mapper.get(path);
        if (rrdObject != null) {
            buffer = rrdObject.getRrd().array();
        }
    }

    /**
     * <p>write.</p>
     *
     * @param offset a long.
     * @param bytes an array of byte.
     * @throws IOException if any.
     */
    protected synchronized void write(long offset, byte[] bytes) throws IOException {
        super.write(offset, bytes);
        dirty = true;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        if (dirty) {
            mapper.save(new RrdDatastax().setPath(getPath()).setRrd(ByteBuffer.wrap(buffer)));
        }
    }

}
