package org.rrd4j.backends;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.core.RrdPrimitive;

public class RrdMemoryBackendTest {

    @Test
    public void testBigString() throws IOException {
        RrdMemoryBackend backend = new RrdMemoryBackend("");
        char c = '\ue001';
        Assert.assertTrue(c >=  '\ue000' && c <= '\uf8ff');
        StringBuffer builder = new StringBuffer();
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
