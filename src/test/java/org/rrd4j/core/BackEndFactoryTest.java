package org.rrd4j.core;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

public abstract class BackEndFactoryTest {

    protected void checkRegistred(String name, Class<? extends RrdBackendFactory> factoryClass) {
        RrdBackendFactory factory = RrdBackendFactory.getFactory(name);
        Assert.assertEquals("Factory name mismatch", name, factory.getName());
        Assert.assertEquals("Factory class mismatch", factoryClass, factory.getClass());
    }

    protected void checkBeans(Class<? extends RrdBackendFactory> factoryClass, String... names) throws IntrospectionException {
        BeanInfo bi = Introspector.getBeanInfo(factoryClass);
        Map<String, PropertyDescriptor> beanProperties = new HashMap<String, PropertyDescriptor>();
        for(PropertyDescriptor pd: bi.getPropertyDescriptors()) {
            if(pd.getReadMethod() != null && pd.getWriteMethod() != null) {
                beanProperties.put(pd.getName(), pd);
            }
        }
        //beanProperties.remove("name");
        //beanProperties.remove("class");
        for(String beanName: names) {
            if(beanProperties.containsKey(beanName)) {
                beanProperties.remove(beanName);
            }
            else {
                Assert.fail("Unknown bean:" + beanName);
            }
        }
        if(beanProperties.size() > 0) {
            Assert.fail("Not checked beans:" + beanProperties.keySet());
        }
    }
    //    @Test
    //    public void testNIO() {
    //        checkRegistred("NIO", RrdNioBackendFactory.class);
    //    }
    
    public abstract void testName();
    public abstract void testBeans()  throws IntrospectionException ;
    

}
