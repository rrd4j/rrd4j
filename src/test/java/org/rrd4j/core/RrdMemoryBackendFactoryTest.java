package org.rrd4j.core;

import java.beans.IntrospectionException;

import org.junit.Test;


public class RrdMemoryBackendFactoryTest extends BackEndFactoryTest {

    @Override
    @Test
    public void testName() {
        checkRegistred("MONGODB", RrdMongoDBBackendFactory.class);
        
    }

    @Override
    @Test
    public void testBeans() throws IntrospectionException {
        checkBeans(RrdMongoDBBackendFactory.class);
    }

}
