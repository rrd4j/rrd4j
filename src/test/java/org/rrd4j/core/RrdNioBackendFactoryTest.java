package org.rrd4j.core;

import java.beans.IntrospectionException;

import org.junit.Test;

public class RrdNioBackendFactoryTest extends BackEndFactoryTest {

    @Test
    public void testName() {
        checkRegistred("NIO", RrdNioBackendFactory.class);
    }

    @Test
    public void testBeans() throws IntrospectionException {
        checkBeans(RrdNioBackendFactory.class, "syncPeriod");
    }

}
