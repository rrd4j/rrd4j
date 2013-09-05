package org.rrd4j.backend.spi.binary;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.rrd4j.backend.RrdBackendFactory;
import org.rrd4j.backend.spi.binary.RrdNioBackendFactory;
import org.rrd4j.core.BackEndFactoryTest;

public class RrdNioBackendFactoryTest extends BackEndFactoryTest {

    @Test
    public void testName() {
        checkRegistred("NIO", RrdNioBackendFactory.class);
    }

    @Test
    public void testBeans() throws IntrospectionException {
        checkBeans(RrdNioBackendFactory.class, "syncPeriod", "syncThreadPool");
    }

    @Test
    public void testStat() throws IOException {
        RrdBackendFactory factory = RrdBackendFactory.getFactory("NIO");

        factory.start();
        Map<String, Number> stats = getStats(factory, "truc.rrd");
        Assert.assertEquals(0, stats.size());
    }

}
