package org.rrd4j.core;

import java.beans.IntrospectionException;
import java.io.IOException;
import com.google.common.util.concurrent.Service.State;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

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
        
        State started = factory.startAndWait();
        
        Assert.assertEquals(State.RUNNING, started);
        
        Map<String, Number> stats = getStats(factory, "dummy");
        Assert.assertTrue(stats.containsKey("memory usage"));
        Assert.assertEquals(1, stats.size());
    }
}
