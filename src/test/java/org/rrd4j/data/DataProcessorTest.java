package org.rrd4j.data;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.DsType.GAUGE;

import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdMemoryBackendFactory;

public class DataProcessorTest {

    private static final RrdBackendFactory backendFactory = new RrdMemoryBackendFactory();
    private static final int STEP = 5;
    private static final String PATH = "testBuild.rrd";
    
    @BeforeClass
    static public void prepare() throws IOException {
        RrdDef rrdDef = new RrdDef(PATH);
        rrdDef.setStep(5);
        rrdDef.addDatasource("ds0", GAUGE, STEP * 2, 0, Double.NaN);
        rrdDef.addDatasource("ds1", GAUGE, STEP * 2, 0, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 200);
        rrdDef.addArchive(AVERAGE, 0.5, 4, 200);
        rrdDef.setStartTime(0);
        try (RrdDb rrdDb = RrdDb.getBuilder().setRrdDef(rrdDef).setBackendFactory(backendFactory).build()) {
            for (int ts = 95; ts <= 201; ts += STEP) {
                rrdDb.createSample(ts).setValue("ds0", ts).setValue("ds1", ts).update();
            }
        }
    }
    
    private RrdDb getRrdDb() throws IOException {
        return RrdDb.getBuilder().setPath(PATH).setBackendFactory(backendFactory).build();
    }

    @Test
    public void testFetchSimple() throws IOException {
        try (RrdDb rrdDb = getRrdDb()) {
            DataProcessor dp = new DataProcessor(100, 200);
            dp.datasource("ds0", rrdDb.getPath(), "ds0", AVERAGE, backendFactory);
            dp.setFetchRequestResolution(STEP);
            dp.processData();
            Assert.assertEquals(STEP, dp.getStep());
            Assert.assertEquals(STEP, dp.getFetchRequestResolution());
            long[] tss = dp.getTimestamps();
            double[] values = dp.getValues("ds0");
            Assert.assertArrayEquals(new long[] {100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150, 155, 160, 165, 170, 175, 180, 185, 190, 195, 200}, tss);
            double[] refValues = new double[] {100.0, 105.0, 110.0, 115.0, 120.0, 125.0, 130.0, 135.0, 140.0, 145.0, 150.0, 155.0, 160.0, 165.0, 170.0, 175.0, 180.0, 185.0, 190.0, 195.0, 200.0};
            for (int i = 0 ; i < refValues.length; i++) {
                Assert.assertEquals(refValues[i], values[i], 1e-1);
            }
        }
    }

    @Test
    public void testFetchData() throws IOException {
        try (RrdDb rrdDb = getRrdDb()) {
            DataProcessor dp = new DataProcessor(100, 200);
            FetchRequest fr = rrdDb.createFetchRequest(ConsolFun.AVERAGE, 100, 200, STEP);
            FetchData fd = fr.fetchData();
            dp.datasource("ds0", fd);
            dp.setFetchRequestResolution(STEP * 2);
            dp.processData();
            Assert.assertEquals(STEP, dp.getStep());
            Assert.assertEquals(STEP * 2, dp.getFetchRequestResolution());
            long[] tss = dp.getTimestamps();
            double[] values = dp.getValues("ds0");
            Assert.assertArrayEquals(new long[] {100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150, 155, 160, 165, 170, 175, 180, 185, 190, 195, 200}, tss);
            double[] refValues = new double[] {100.0, 105.0, 110.0, 115.0, 120.0, 125.0, 130.0, 135.0, 140.0, 145.0, 150.0, 155.0, 160.0, 165.0, 170.0, 175.0, 180.0, 185.0, 190.0, 195.0, 200.0};
            for (int i = 0 ; i < refValues.length; i++) {
                Assert.assertEquals(refValues[i], values[i], 1e-1);
            }
        }
    }

    @Test
    public void testFetchDataBigStep() throws IOException {
        try (RrdDb rrdDb = getRrdDb()) {
            DataProcessor dp = new DataProcessor(100, 200);
            FetchRequest fr = rrdDb.createFetchRequest(ConsolFun.AVERAGE, 100, 200, STEP);
            FetchData fd = fr.fetchData();
            dp.datasource("ds0", fd);
            dp.setStep(STEP * 2);
            dp.processData();
            Assert.assertEquals(STEP * 2, dp.getStep());
            Assert.assertEquals(1, dp.getFetchRequestResolution());
            long[] tss = dp.getTimestamps();
            double[] values = dp.getValues("ds0");
            Assert.assertArrayEquals(new long[] {100, 110, 120, 130, 140, 150, 160, 170, 180,  190, 200}, tss);
            double[] refValues = new double[] {100.0, 107.5, 117.5, 127.5, 137.5, 147.5, 157.5, 167.5, 177.5, 187.5, 197.50};
            for (int i = 0 ; i < refValues.length; i++) {
                Assert.assertEquals(refValues[i], values[i], 1e-1);
            }
        }
    }

    @Test
    public void testMixedResolution() throws IOException {
        try (RrdDb rrdDb = getRrdDb()) {
            DataProcessor dp = new DataProcessor(100, 200);
            dp.datasource("ds0", rrdDb.createFetchRequest(ConsolFun.AVERAGE, 100, 200, STEP).fetchData());
            dp.datasource("ds1", rrdDb.createFetchRequest(ConsolFun.AVERAGE, 100, 200, STEP * 2).fetchData());
            dp.setFetchRequestResolution(STEP * 3);
            dp.processData();
            Assert.assertEquals(STEP, dp.getStep());
            Assert.assertEquals(STEP * 3, dp.getFetchRequestResolution());
            long[] tss = dp.getTimestamps();
            double[] values = dp.getValues("ds0");
            Assert.assertArrayEquals(new long[] {100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150, 155, 160, 165, 170, 175, 180, 185, 190, 195, 200}, tss);
            double[] refValues = new double[] {100.0, 105.0, 110.0, 115.0, 120.0, 125.0, 130.0, 135.0, 140.0, 145.0, 150.0, 155.0, 160.0, 165.0, 170.0, 175.0, 180.0, 185.0, 190.0, 195.0, 200.0};
            for (int i = 0 ; i < refValues.length; i++) {
                Assert.assertEquals(refValues[i], values[i], 1e-1);
            }
        }
    }

    @Test
    public void testPlotable() throws IOException {
        try (RrdDb rrdDb = getRrdDb()) {
            DataProcessor dp = new DataProcessor(100, 200);
            dp.datasource("p", ts -> ts * 2.0);
            dp.setFetchRequestResolution(STEP * 2);
            dp.processData();
            Assert.assertEquals(1, dp.getStep());
            Assert.assertEquals(STEP * 2, dp.getFetchRequestResolution());
            long[] tss = dp.getTimestamps();
            double[] values = dp.getValues("p");
            Assert.assertEquals(100, tss[0]);
            Assert.assertEquals(200, tss[100]);
            for (int i = 0; i < values.length; i++) {
                Assert.assertEquals(200 + i * 2, values[i], 1e-1);
            }
        }
    }
}
