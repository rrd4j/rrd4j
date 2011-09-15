package org.rrd4j.core.jrrd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is used to read a unival from a file
 * unival is a rrdtool type, defined in rrd_format.h
 * @author Fabrice Bacchella <fbacchella@spamcop.net>
 *
 */
public class UnivalArray {
    private ByteBuffer buffer;

    public UnivalArray(RRDFile file, int size) throws IOException {
        buffer = ByteBuffer.allocate(size * 8);
        if(file.isBigEndian())
            buffer.order(ByteOrder.BIG_ENDIAN);
        else
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        file.read(buffer);
    }
    
    public long getLong(Enum<?> e) {
        buffer.position(8 * e.ordinal());
        return buffer.getLong();
    }

    public double getDouble(Enum<?> e) {
        buffer.position(8 * e.ordinal());
        return buffer.getDouble();
    }

}
