package org.rrd4j.core;

import java.beans.IntrospectionException;

import org.junit.Test;

public class RrdBerkeleyDbBackendFactoryTest extends BackEndFactoryTest {

    @Test
    public void testName() {
        checkRegistred("BERKELEY", RrdBerkeleyDbBackendFactory.class);
    }

    @Test
    public void testBeans() throws IntrospectionException {
        checkBeans(RrdBerkeleyDbBackendFactory.class, "database");
    }

}
