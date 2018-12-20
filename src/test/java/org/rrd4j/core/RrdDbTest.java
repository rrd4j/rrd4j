package org.rrd4j.core;

import static org.junit.Assert.*;
import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.ConsolFun.MAX;
import static org.rrd4j.ConsolFun.TOTAL;
import static org.rrd4j.DsType.GAUGE;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Random;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.data.Aggregates;

@SuppressWarnings("deprecation")
public class RrdDbTest {
    static final long SEED = 1909752002L;
    static final Random RANDOM = new Random(SEED);
    static final long START = Util.getTimestamp(2010, 4, 1);
    static final long END = Util.getTimestamp(2010, 6, 1);
    static final int MAX_STEP = 300;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    public void testRrdDb(RrdDb db) throws IOException {

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

    public void testRrdDbXml(RrdDb db) throws IOException {
        double value;

        Assert.assertEquals("Invalid date", 920808900L, db.getLastUpdateTime());
        Assert.assertEquals("Invalid step", 300L, db.getHeader().getStep());

        Assert.assertEquals("Invalid number of datasources", 2, db.getDsCount());

        Assert.assertEquals("Invalid name for first data source", "speed", db.getDatasource(0).getName());
        Assert.assertEquals("Invalid dsType for first data source", DsType.COUNTER, db.getDatasource(0).getType());
        Assert.assertEquals("Invalid hearthbeat for first data source", 600, db.getDatasource(0).getHeartbeat());
        Assert.assertTrue("Invalid max value for first data source", Double.isNaN(db.getDatasource(0).getMaxValue()));
        Assert.assertTrue("Invalid min value for first data source", Double.isNaN(db.getDatasource(0).getMinValue()));
        Assert.assertEquals("Invalid last value for first data source", 12405, db.getDatasource(0).getLastValue(), 1e-7);
        Assert.assertEquals("Invalid nan secondes for first data source", 0, db.getDatasource(0).getNanSeconds(), 1e-7);

        Assert.assertEquals("Invalid name for second data source", "weight", db.getDatasource(1).getName());
        Assert.assertEquals("Invalid dsType for second data source", DsType.GAUGE, db.getDatasource(1).getType());
        Assert.assertEquals("Invalid hearthbeat for second data source", 600, db.getDatasource(1).getHeartbeat());
        Assert.assertTrue("Invalid max value for second data source", Double.isNaN(db.getDatasource(1).getMaxValue()));
        Assert.assertTrue("Invalid min value for second data source", Double.isNaN(db.getDatasource(1).getMinValue()));
        value = db.getDatasource(1).getLastValue();
        Assert.assertTrue("Invalid last value for second data source", value == 3.0 || Double.isNaN(value));
        Assert.assertEquals("Invalid nan secondes for second data source", 0, db.getDatasource(1).getNanSeconds(), 1e-7);

        Assert.assertEquals("Invalid number of archives", 2, db.getArcCount());

        Assert.assertEquals("Invalid consolidation function for first archive", ConsolFun.AVERAGE, db.getArchive(0).getConsolFun());
        Assert.assertEquals("Invalid number of steps for first archive", 1, db.getArchive(0).getSteps());
        Assert.assertEquals("Invalid number of row for first archive", 24, db.getArchive(0).getRows());
        Assert.assertEquals("Invalid XFF for first archive", 0.5, db.getArchive(0).getXff(), 1e-7);
        Assert.assertEquals("Invalid start time for first archive", 920802000, db.getArchive(0).getStartTime());
        Assert.assertEquals("Invalid end time for first archive", 920808900, db.getArchive(0).getEndTime());
        value = db.getArchive(0).getArcState(0).getAccumValue();
        Assert.assertTrue("Invalid value for first ds in first archive: " + value, Double.isNaN(value));
        value = db.getArchive(0).getArcState(1).getAccumValue();
        Assert.assertTrue("Invalid value for second ds in first archive: " + value, Double.isNaN(value));

        Assert.assertEquals("Invalid consolidation function for second archive", ConsolFun.AVERAGE, db.getArchive(1).getConsolFun());
        Assert.assertEquals("Invalid number of steps for second archive", 6, db.getArchive(1).getSteps());
        Assert.assertEquals("Invalid number of row for seconde archive", 10, db.getArchive(1).getRows());
        Assert.assertEquals("Invalid XFF for second archive", 0.5, db.getArchive(1).getXff(), 1e-7);
        Assert.assertEquals("Invalid start time for second archive", 920791800, db.getArchive(1).getStartTime());
        Assert.assertEquals("Invalid end time for second archive", 920808000, db.getArchive(1).getEndTime());
        value = db.getArchive(1).getArcState(0).getAccumValue();
        Assert.assertEquals("Invalid value for first ds in second archive: " + value, 1.4316557620e+07, value, 1e-5);
        value = db.getArchive(1).getArcState(1).getAccumValue();
        Assert.assertEquals("Invalid value for second ds in second archive: " + value, 6.0, value, 1e-5);
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

        Aggregates speedAggr = fd.getAggregates("sun");
        Assert.assertEquals("Invalid average for sun", 1.1985168039e1, speedAggr.getAverage(), 1e-7);
        Assert.assertEquals("Invalid first for sun", 3.5834466667e3, speedAggr.getFirst(), 1e-7);
        Assert.assertEquals("Invalid last for sun", 3.9572500000e3, speedAggr.getLast(), 1e-7);
        Assert.assertEquals("Invalid min for sun", 3.3940000000e3, speedAggr.getMin(), 1e-7);
        Assert.assertEquals("Invalid max for sun", 3.9840533333e3, speedAggr.getMax(), 1e-7);
        Assert.assertEquals("Invalid total for sun", 2153734.6966666686, speedAggr.getTotal(), 1e-7);

        Aggregates weightAggr = fd.getAggregates("shade");
        Assert.assertEquals("Invalid average for shade", 2.0128491560, weightAggr.getAverage(), 1e-7);
        Assert.assertEquals("Invalid first for shade", 5.9251000000E02, weightAggr.getFirst(), 1e-7);
        Assert.assertEquals("Invalid last for shade", 5.0486666667E02, weightAggr.getLast(), 1e-7);
        Assert.assertEquals("Invalid min for shade", 4.4615000000E02, weightAggr.getMin(), 1e-7);
        Assert.assertEquals("Invalid max for shade", 7.3451666667E02, weightAggr.getMax(), 1e-7);
        Assert.assertEquals("Invalid total for shade", 361708.99333333364, weightAggr.getTotal(), 1e-7);
    }

    @Test
    public void testBuild1() throws IOException {
        long start = START;

        RrdDef rrdDef = new RrdDef(testFolder.newFile("testBuild.rrd").getCanonicalPath(), start - 1, 300);
        rrdDef.setVersion(1);
        rrdDef.addDatasource("sun", GAUGE, 600, 0, Double.NaN);
        rrdDef.addDatasource("shade", GAUGE, 600, 0, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        rrdDef.addArchive(AVERAGE, 0.5, 6, 700);
        rrdDef.addArchive(AVERAGE, 0.5, 24, 775);
        rrdDef.addArchive(AVERAGE, 0.5, 288, 797);
        rrdDef.addArchive(TOTAL, 0.5, 1, 600);
        rrdDef.addArchive(TOTAL, 0.5, 6, 700);
        rrdDef.addArchive(TOTAL, 0.5, 24, 775);
        rrdDef.addArchive(TOTAL, 0.5, 288, 797);
        rrdDef.addArchive(MAX, 0.5, 1, 600);
        rrdDef.addArchive(MAX, 0.5, 6, 700);
        rrdDef.addArchive(MAX, 0.5, 24, 775);
        rrdDef.addArchive(MAX, 0.5, 288, 797);
        RrdDb rrdDb = new RrdDb(rrdDef);
        testRrdDb(rrdDb);
        Assert.assertEquals("not expected version", 1, rrdDb.getRrdDef().getVersion());
    }

    @Test
    public void testBuild2() throws IOException {
        long start = START;

        RrdDef rrdDef = new RrdDef(testFolder.newFile("testBuild.rrd").getCanonicalPath(), start - 1, 300);
        rrdDef.setVersion(2);
        rrdDef.addDatasource("sun", GAUGE, 600, 0, Double.NaN);
        rrdDef.addDatasource("shade", GAUGE, 600, 0, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        rrdDef.addArchive(AVERAGE, 0.5, 6, 700);
        rrdDef.addArchive(AVERAGE, 0.5, 24, 775);
        rrdDef.addArchive(AVERAGE, 0.5, 288, 797);
        rrdDef.addArchive(TOTAL, 0.5, 1, 600);
        rrdDef.addArchive(TOTAL, 0.5, 6, 700);
        rrdDef.addArchive(TOTAL, 0.5, 24, 775);
        rrdDef.addArchive(TOTAL, 0.5, 288, 797);
        rrdDef.addArchive(MAX, 0.5, 1, 600);
        rrdDef.addArchive(MAX, 0.5, 6, 700);
        rrdDef.addArchive(MAX, 0.5, 24, 775);
        rrdDef.addArchive(MAX, 0.5, 288, 797);
        RrdDb rrdDb = new RrdDb(rrdDef);
        testRrdDb(rrdDb);

        Assert.assertEquals("not expected version", 2, rrdDb.getRrdDef().getVersion());
    }

    @Test
    public void testRead1() throws IOException {
        URL url = getClass().getResource("/demo1.rrd"); 
        RrdDb rrd = new RrdDb(url.getFile(), RrdBackendFactory.getFactory("FILE"));
        testRrdDb(rrd);
        checkValues(rrd);
        Assert.assertEquals("not expected version", 1, rrd.getRrdDef().getVersion());
    }

    @Test
    public void testRead2() throws IOException {
        URL url = getClass().getResource("/demo2.rrd"); 
        RrdDb rrd = new RrdDb(url.getFile(), RrdBackendFactory.getFactory("FILE"));
        testRrdDb(rrd);
        Assert.assertEquals("not expected version", 2, rrd.getRrdDef().getVersion());
        checkValues(rrd);
    }

    @Test(expected=InvalidRrdException.class)
    public void testReadCorruptSignature() throws Exception {
        URL url = getClass().getResource("/corrupt.rrd"); 
        RrdBackendFactory backendFactory = RrdBackendFactory.getFactory("FILE");

        try (RrdDb rdb = new RrdDb(url.getFile(), backendFactory)) {
        }
    }

    @Test(expected=InvalidRrdException.class)
    public void testReadEmpty() throws Exception {
        URL url = getClass().getResource("/empty.rrd"); 
        RrdBackendFactory backendFactory = RrdBackendFactory.getFactory("FILE");

        try (RrdDb rdb = new RrdDb(url.getFile(), backendFactory)){
            fail();
        }
    }

    @Test
    public void testXml1Import() throws IOException {
        URL url = getClass().getResource("/rrdtool/rrdtool1.xml"); 
        RrdDb rrd = new RrdDb(testFolder.newFile("testxml1import.rrd").getCanonicalPath(), "xml:/" + url.getFile(), RrdBackendFactory.getFactory("FILE"));
        testRrdDbXml(rrd);
    }

    @Test
    public void testXml3Import() throws IOException {
        URL url = getClass().getResource("/rrdtool/rrdtool3.xml"); 
        RrdDb rrd = new RrdDb(testFolder.newFile("testxml3import.rrd").getCanonicalPath(), "xml:/" + url.getFile(), RrdBackendFactory.getFactory("FILE"));
        testRrdDbXml(rrd);
    }

    @Test
    public void testSpike() throws IOException {
        RrdDef rrdDef = new RrdDef(testFolder.newFile("testSpike.rrd").getCanonicalPath(), 0, 60);
        rrdDef.setVersion(2);
        rrdDef.addDatasource("ds", GAUGE, 3600, -5, 30);
        rrdDef.addArchive(AVERAGE, 0.5, 60, 999);
        try (RrdDb rrdDb = new RrdDb(rrdDef)) {
            Calendar testTime = Calendar.getInstance();
            testTime.set(Calendar.MINUTE, 0);
            testTime.set(Calendar.SECOND, 0);
            testTime.set(Calendar.MILLISECOND, 0);
            //testTime.add(Calendar.HOUR, -1);
            long start =  Util.getTimestamp(testTime);
            long timeStamp = start;

            for(int i = 0; i < 180; i++) {
                long  sampleTime = timeStamp;
                if(i == 117) {
                    sampleTime += -1;
                }
                rrdDb.createSample(sampleTime).setValue("ds", 30).update();
                timeStamp += 60;
            }
            long end = timeStamp;
            FetchData f = rrdDb.createFetchRequest(AVERAGE, start, end).fetchData();
            double[] values = f.getValues("ds");
            Assert.assertEquals("Data before first entry", Double.NaN, values[0], 0.0);
            Assert.assertEquals("Bad average in point 1", 30, values[1], 1e-3);
            Assert.assertEquals("Bad average in point 2", 30, values[2], 1e-3);
            Assert.assertEquals("Data after last entry", Double.NaN, values[3], 0.0);
        }
    }

    @Test
    public void testDefDump() throws IOException {
        long start = START;

        RrdDef rrdDef = new RrdDef(testFolder.newFile("testBuild.rrd").getCanonicalPath(), start - 1, 300);
        rrdDef.setVersion(2);
        rrdDef.addDatasource("short", GAUGE, 600, 0, Double.NaN);
        rrdDef.addDatasource("veryverylongnamebiggerthatoldlimit", GAUGE, 600, 0, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        rrdDef.addArchive(TOTAL, 0.5, 1, 600);
        rrdDef.addArchive(MAX, 0.5, 1, 600);
        String[] dsNames1 = new String[2];
        try (RrdDb rrdDb = new RrdDb(rrdDef)) {
            int dsCount = rrdDb.getHeader().getDsCount();
            for(int i = 0; i < dsCount; i++) {
                Datasource srcDs = rrdDb.getDatasource(i);
                String dsName = srcDs.getName();
                int j = rrdDb.getDsIndex(dsName);
                dsNames1[j] = dsName;
            }
        }
        String[] dsNames2 = new String[2];
        try (RrdDb rrdDb = new RrdDb(rrdDef.getPath(), true, new RrdNioBackendFactory())) {
            int dsCount = rrdDb.getHeader().getDsCount();
            for(int i = 0; i < dsCount; i++) {
                Datasource srcDs = rrdDb.getDatasource(i);
                String dsName = srcDs.getName();
                int j = rrdDb.getDsIndex(dsName);
                dsNames2[j] = dsName;
            }
        }
        String[] dsNames3 = new String[2];
        try (RrdDb rrdDb = new RrdDb(rrdDef.getPath(), true, new RrdRandomAccessFileBackendFactory())) {
            int dsCount = rrdDb.getHeader().getDsCount();
            for(int i = 0; i < dsCount; i++) {
                Datasource srcDs = rrdDb.getDatasource(i);
                String dsName = srcDs.getName();
                int j = rrdDb.getDsIndex(dsName);
                dsNames3[j] = dsName;
            }
        }
        Assert.assertArrayEquals(dsNames1, dsNames2);
        Assert.assertArrayEquals(dsNames1, dsNames3);
        Assert.assertEquals("short", dsNames1[0]);
        Assert.assertEquals("veryverylongnamebiggerthatoldlimit", dsNames1[1]);
    }

}
