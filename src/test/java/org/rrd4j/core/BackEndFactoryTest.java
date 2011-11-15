package org.rrd4j.core;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;

import junit.framework.Assert;

public abstract class BackEndFactoryTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

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

    protected Map<String, Number> getStats(RrdBackendFactory factory, String path) throws IOException {        
        RrdDef rrdDef = new RrdDef(testFolder.newFile(path).getCanonicalPath(), 300);
        rrdDef.setVersion(2);
        rrdDef.addDatasource(new DsDef("A", DsType.DERIVE, 5l, 0, 10000));
        rrdDef.addDatasource(new DsDef("B", DsType.DERIVE, 5l, 0, 10000));
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 10);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 6, 20);

        RrdDb db = new RrdDb(rrdDef, factory);
        long lastUpdate = db.getLastUpdateTime() + 1000;
        db.createSample().setAndUpdate(lastUpdate + ":1:2");

        return factory.getStats();
    }

    public abstract void testName();
    public abstract void testBeans() throws IntrospectionException ;

}
