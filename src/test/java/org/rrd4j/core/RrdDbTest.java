package org.rrd4j.core;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.ConsolFun.MAX;
import static org.rrd4j.ConsolFun.TOTAL;
import static org.rrd4j.DsType.GAUGE;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RrdDbTest {
    static final long SEED = 1909752002L;
    static final Random RANDOM = new Random(SEED);
    static final long START = Util.getTimestamp(2010, 4, 1);
    static final long END = Util.getTimestamp(2010, 6, 1);
    static final int MAX_STEP = 300;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

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
        org.rrd4j.TestsUtils.testRrdDb(rrdDb);       
        Assert.assertEquals("not expected version", 1, rrdDb.getRrdDef().getVersion());
    }

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
        org.rrd4j.TestsUtils.testRrdDb(rrdDb);       
        Assert.assertEquals("not expected version", 2, rrdDb.getRrdDef().getVersion());
    }

    public void testRead1() throws IOException {
        URL url = getClass().getResource("/demo1.rrd"); 
        RrdDb rrd = new RrdDb(url.getFile(), RrdBackendFactory.getFactory("MEMORY"));
        org.rrd4j.TestsUtils.testRrdDb(rrd);     
        Assert.assertEquals("not expected version", 1, rrd.getRrdDef().getVersion());
    }

    public void testRead2() throws IOException {
        URL url = getClass().getResource("/demo2.rrd"); 
        RrdDb rrd = new RrdDb(url.getFile(), RrdBackendFactory.getFactory("MEMORY"));
        org.rrd4j.TestsUtils.testRrdDb(rrd);       
        Assert.assertEquals("not expected version", 2, rrd.getRrdDef().getVersion());
    }

    @Test
    public void testXml1Import() throws IOException {
        URL url = getClass().getResource("/rrdtool/rrdtool1.xml"); 
        RrdDb rrd = new RrdDb("test", "xml:/" + url.getFile(), RrdBackendFactory.getFactory("MEMORY"));
        org.rrd4j.TestsUtils.testRrdDb(rrd);
    }

    @Test
    public void testXml3Import() throws IOException {
        URL url = getClass().getResource("/rrdtool/rrdtool3.xml"); 
        RrdDb rrd = new RrdDb("test", "xml:/" + url.getFile(), RrdBackendFactory.getFactory("MEMORY"));
        org.rrd4j.TestsUtils.testRrdDb(rrd);
    }
    
    @Test
    public void testSpike() throws IOException {
        RrdDef rrdDef = new RrdDef(testFolder.newFile("testSpike.rrd").getCanonicalPath(), 0, 60);
        rrdDef.setVersion(2);
        rrdDef.addDatasource("ds", GAUGE, 3600, -5, 30);
        rrdDef.addArchive(AVERAGE, 0.5, 60, 999);
        RrdDb rrdDb = new RrdDb(rrdDef);
        
        Calendar testTime = Calendar.getInstance();
        testTime.set(Calendar.MINUTE, 0);
        testTime.set(Calendar.SECOND, 0);
        testTime.set(Calendar.MILLISECOND, 0);
        System.out.println(testTime);
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
        System.out.println(f.dump());
        double[] values = f.getValues("ds");
        Assert.assertEquals("Data before first entry", Double.NaN, values[0]);
        Assert.assertEquals("Bad average in point 1", 30, values[1], 1e-3);
        Assert.assertEquals("Bad average in point 2", 30, values[2], 1e-3);
        Assert.assertEquals("Data after last entry", Double.NaN, values[3]);
    }
}
