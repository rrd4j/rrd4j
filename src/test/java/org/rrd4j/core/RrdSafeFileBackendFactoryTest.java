package org.rrd4j.core;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;


public class RrdSafeFileBackendFactoryTest extends BackEndFactoryTest {

    @Override
    @Test
    public void testName() {
        checkRegistred("SAFE", RrdSafeFileBackendFactory.class);
        
    }

    @Override
    @Test
    public void testBeans() throws IntrospectionException {
        checkBeans(RrdSafeFileBackendFactory.class, "lockRetryPeriod", "lockWaitTime");
    }

    @Test
    public void testStat() throws IOException {
        RrdBackendFactory factory = RrdBackendFactory.getFactory("SAFE");
        
        factory.startAndWait();
        Map<String, Number> stats = getStats(factory, "truc.rrd");
        Assert.assertEquals(0, stats.size());
    }

}
