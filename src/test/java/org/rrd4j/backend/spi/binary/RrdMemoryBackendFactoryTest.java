package org.rrd4j.backend.spi.binary;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.rrd4j.backend.RrdBackendFactory;
import org.rrd4j.backend.spi.binary.RrdMemoryBackendFactory;
import org.rrd4j.backend.spi.binary.RrdMongoDBBackendFactory;
import org.rrd4j.core.BackEndFactoryTest;

public class RrdMemoryBackendFactoryTest extends BackEndFactoryTest {

    @Override
    @Test
    public void testName() {
        checkRegistred("MEMORY", RrdMemoryBackendFactory.class);
    }

    @Override
    @Test
    public void testBeans() throws IntrospectionException {
        checkBeans(RrdMongoDBBackendFactory.class);
    }

    @Test
    public void testStat() throws IOException {
        RrdBackendFactory factory = RrdBackendFactory.getFactory("MEMORY");
        
        boolean started = factory.start();
        
        Assert.assertTrue("Failed to start the backend", started);
        
        Map<String, Number> stats = getStats(factory, "dummy");
        Assert.assertTrue(stats.containsKey("memory usage"));
        Assert.assertEquals(1, stats.size());
    }
}
