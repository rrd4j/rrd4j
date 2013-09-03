package org.rrd4j.backend.spi.binary;

import java.beans.IntrospectionException;

import org.junit.Test;
import org.rrd4j.backend.spi.binary.RrdMemoryBackendFactory;
import org.rrd4j.core.BackEndFactoryTest;


public class RrdMongoDBBackendFactoryTest extends BackEndFactoryTest {

    @Override
    @Test
    public void testName() {
        checkRegistred("MEMORY", RrdMemoryBackendFactory.class);
        
    }

    @Override
    @Test
    public void testBeans() throws IntrospectionException {
        checkBeans(RrdMemoryBackendFactory.class);
    }

}
