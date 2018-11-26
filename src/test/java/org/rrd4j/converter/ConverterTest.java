package org.rrd4j.converter;

import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdMemoryBackendFactory;

public class ConverterTest {
 
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
    
    @Test
    public void test_3_l_64_8() throws IOException {
        testImport("/rrdtool/0003l648.rrd");
    }

    @Test
    public void test_3_l_32_8() throws IOException {
        testImport("/rrdtool/0003l328.rrd");
    }

    @Test
    public void test_3_l_32_4() throws IOException {
        testImport("/rrdtool/0003l324.rrd");
    }

    @Test
    public void test_3_b_32_8() throws IOException {
        testImport("/rrdtool/0003b328.rrd");
    }

    @Test
    public void test_1_b_64_8() throws IOException {
        testImport("/rrdtool/0001b648.rrd");
    }

    @Test
    public void test_1_b_32_8() throws IOException {
        //testImport("/rrdtool/0001b328.rrd");
    }

    @Test
    public void test_1_l_32_4() throws IOException {
        testImport("/rrdtool/0001l324.rrd");
    }

    @Test
    public void test_1_l_64_8() throws IOException {
        testImport("/rrdtool/0001l648.rrd");
    }

    private void testImport(String file) throws IOException {
        URL url = getClass().getResource(file); 
        RrdDb rrd = new RrdDb("test", "rrdtool:/" + url.getFile(), new RrdMemoryBackendFactory());
        testRrdDbXml(rrd);
    }
}
