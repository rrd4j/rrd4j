package org.rrd4j.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

public class RrdMemoryBackendTest {

    @Test
    public void testBigString() throws IOException {
        RrdMemoryBackend backend = new RrdMemoryBackend("", new AtomicReference<ByteBuffer>());
        char c = '\ue001';
        Assert.assertTrue(c >=  '\ue000' && c <= '\uf8ff');
        StringBuilder builder = new StringBuilder();
        backend.setLength(6400 * 6400 + 10);
        int pos = 0;
        String previous = null;
        for (int i = 0 ; i < 80 ; i++) {
            previous = builder.toString();
            builder.append(i % 10);
            backend.writeString(pos, builder.toString());
            Assert.assertEquals("Not read String", builder.toString(), backend.readString(pos));
            if (!previous.isEmpty()) {
                Assert.assertEquals("Not read String", previous, backend.readString(pos - RrdPrimitive.STRING_LENGTH * 2));
            }
            pos += RrdPrimitive.STRING_LENGTH * 2;
        }
    }

}
