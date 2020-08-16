package org.rrd4j.core;

import static org.junit.Assert.fail;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.Variable;

public abstract class BackendTester {

    public void testBackendFactory(RrdBackendFactory factory, String path) throws IOException {
        RrdBackend be = factory.open(path, false);

        be.setLength(10);
        be.writeDouble(0, 0);
        be.close();
        try (DataInputStream is = new DataInputStream(new FileInputStream(path));) {
            Double d = is.readDouble();
            Assert.assertEquals("write to random access file failed", 0, d, 1e-10);
        };
    }

    public void testRrdDb(RrdDb db) throws IOException {

        Assert.assertNotNull(db.getBytes());

        Assert.assertEquals("Invalid step", 300L, db.getHeader().getStep());

        Assert.assertEquals("Invalid number of datasources", 2, db.getDsCount());

        Assert.assertEquals("Invalid name for first data source", "sun", db.getDatasource(0).getName());
        Assert.assertEquals("Invalid dsType for first data source", DsType.GAUGE, db.getDatasource(0).getType());
        Assert.assertEquals("Invalid hearthbeat for first data source", 600, db.getDatasource(0).getHeartbeat());
        Assert.assertTrue("Invalid max value for first data source", Double.isNaN(db.getDatasource(0).getMaxValue()));
        Assert.assertEquals("Invalid min value for first data source", 0, db.getDatasource(0).getMinValue(), 1e-7);

        Assert.assertEquals("Invalid name for second data source", "shade", db.getDatasource(1).getName());
        Assert.assertEquals("Invalid dsType for second data source", DsType.GAUGE, db.getDatasource(1).getType());
        Assert.assertEquals("Invalid hearthbeat for second data source", 600, db.getDatasource(1).getHeartbeat());
        Assert.assertTrue("Invalid max value for second data source", Double.isNaN(db.getDatasource(1).getMaxValue()));
        Assert.assertEquals("Invalid min value for second data source",  0, db.getDatasource(0).getMinValue(), 1e-7);

        Assert.assertEquals("Invalid number of archives", 12, db.getArcCount());

        Assert.assertEquals("Invalid consolidation function for first archive", ConsolFun.AVERAGE, db.getArchive(0).getConsolFun());
        Assert.assertEquals("Invalid number of steps for first archive", 1, db.getArchive(0).getSteps());
        Assert.assertEquals("Invalid number of row for first archive", 600, db.getArchive(0).getRows());
        Assert.assertEquals("Invalid XFF for first archive", 0.5, db.getArchive(0).getXff(), 1e-7);

        Assert.assertEquals("Invalid consolidation function for second archive", ConsolFun.AVERAGE, db.getArchive(1).getConsolFun());
        Assert.assertEquals("Invalid number of steps for second archive", 6, db.getArchive(1).getSteps());
        Assert.assertEquals("Invalid number of row for seconde archive", 700, db.getArchive(1).getRows());
        Assert.assertEquals("Invalid XFF for second archive", 0.5, db.getArchive(1).getXff(), 1e-7);
    }

    public void checkValues(RrdDb db) throws IOException {
        double value;

        Assert.assertEquals("Invalid date", 1278107831, db.getLastUpdateTime());
        Assert.assertEquals("Invalid last value for first data source", 3947, db.getDatasource(0).getLastValue(), 1e-7);
        Assert.assertEquals("Invalid nan secondes for first data source", 0, db.getDatasource(0).getNanSeconds(), 1e-7);
        Assert.assertEquals("Invalid nan secondes for second data source", 0, db.getDatasource(1).getNanSeconds(), 1e-7);
        Assert.assertEquals("Invalid start time for first archive", 1277928000, db.getArchive(0).getStartTime());
        Assert.assertEquals("Invalid end time for first archive", 1278107700, db.getArchive(0).getEndTime());
        Assert.assertEquals("Invalid start time for second archive", 1276848000, db.getArchive(1).getStartTime());
        Assert.assertEquals("Invalid end time for second archive", 1278106200, db.getArchive(1).getEndTime());

        value = db.getDatasource(1).getLastValue();
        Assert.assertEquals("Invalid last value for second data source", 497, value, 1e-7);
        value = db.getArchive(0).getArcState(0).getAccumValue();
        Assert.assertTrue("Invalid value for first ds in first archive: " + value, Double.isNaN(value));
        value = db.getArchive(0).getArcState(1).getAccumValue();
        Assert.assertTrue("Invalid value for second ds in first archive: " + value, Double.isNaN(value));
        value = db.getArchive(1).getArcState(0).getAccumValue();
        Assert.assertEquals("Invalid value for first ds in second archive: " + value, 19757.199999999997, value, 1e-5);
        value = db.getArchive(1).getArcState(1).getAccumValue();
        Assert.assertEquals("Invalid value for second ds in second archive: " + value, 2591.2066666666665, value, 1e-5);

        FetchData fd = db.createFetchRequest(ConsolFun.AVERAGE, 1277928000, 1278107700).fetchData();
        DataProcessor dp = new DataProcessor(1277928000, 1278107700);
        dp.datasource("sun", fd);
        dp.datasource("shade", fd);
        dp.processData();
        testVariable("Invalid average for sun", 3595.5504117974388, dp, new Variable.AVERAGE(), "sun");
        testVariable("Invalid last for sun", 3583.4466666666667, dp, new Variable.FIRST(), "sun");
        testVariable("Invalid min for sun", 3394.0, dp, new Variable.MIN(), "sun");
        testVariable("Invalid max for sun", 3984.0533333333333, dp, new Variable.MAX(), "sun");
        testVariable("Invalid total for sun", 6.461204090000006E8, dp, new Variable.TOTAL(), "sun");

        testVariable("Invalid average for shade", 603.8547468002228, dp, new Variable.AVERAGE(), "shade");
        testVariable("Invalid last for shade", 592.51, dp, new Variable.FIRST(), "shade");
        testVariable("Invalid min for shade", 446.15, dp, new Variable.MIN(), "shade");
        testVariable("Invalid max for shade", 734.5166666666667, dp, new Variable.MAX(), "shade");
        testVariable("Invalid total for shade", 1.0851269800000009E8, dp, new Variable.TOTAL(), "shade");
    }

    private void testVariable(String message, double expected, DataProcessor dp, Variable v, String ds) {
        Assert.assertEquals(message, expected, dp.getVariable(ds, v).value, 1e-7);

    }

    protected void testRead1(RrdBackendFactory factory) throws IOException {
        URL url = getClass().getResource("/demo1.rrd"); 
        RrdDb rrd = RrdDb.getBuilder().setPath(url.getFile()).setBackendFactory(factory).build();
        testRrdDb(rrd);
        checkValues(rrd);
        Assert.assertEquals("not expected version", 1, rrd.getRrdDef().getVersion());
    }

    public void testRead2(RrdBackendFactory factory) throws IOException {
        URL url = getClass().getResource("/demo2.rrd"); 
        RrdDb rrd = RrdDb.getBuilder().setPath(url.getFile()).setBackendFactory(factory).build();
        testRrdDb(rrd);
        Assert.assertEquals("not expected version", 2, rrd.getRrdDef().getVersion());
        checkValues(rrd);
    }

    public void testReadCorruptSignature(RrdBackendFactory factory) throws Exception {
        URL url = getClass().getResource("/corrupt.rrd"); 
        try (RrdDb rdb = RrdDb.getBuilder().setPath(url.getFile()).setBackendFactory(factory).build()) {
        }
    }

    public void testReadEmpty(RrdBackendFactory factory) throws Exception {
        URL url = getClass().getResource("/empty.rrd"); 

        try (RrdDb rdb = RrdDb.getBuilder().setPath(url.getFile()).setBackendFactory(factory).build()){
            fail();
        }
    }

}
