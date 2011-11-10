package org.rrd4j.core;

import java.beans.IntrospectionException;

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

}
