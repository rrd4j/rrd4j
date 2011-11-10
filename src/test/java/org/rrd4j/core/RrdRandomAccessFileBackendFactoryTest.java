package org.rrd4j.core;

import java.beans.IntrospectionException;

import org.junit.Test;


public class RrdRandomAccessFileBackendFactoryTest extends BackEndFactoryTest {

    @Override
    @Test
    public void testName() {
        checkRegistred("FILE", RrdRandomAccessFileBackendFactory.class);
        
    }

    @Override
    @Test
    public void testBeans() throws IntrospectionException {
        checkBeans(RrdRandomAccessFileBackendFactory.class);
    }

}
