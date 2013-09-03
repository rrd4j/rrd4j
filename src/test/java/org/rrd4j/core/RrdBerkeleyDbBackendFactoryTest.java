package org.rrd4j.core;

import java.beans.IntrospectionException;

import org.junit.Test;
import org.rrd4j.backend.spi.binary.RrdBerkeleyDbBackendFactory;

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
