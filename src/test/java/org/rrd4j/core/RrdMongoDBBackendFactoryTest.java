package org.rrd4j.core;

import java.beans.IntrospectionException;

import org.junit.Test;


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
