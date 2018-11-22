package org.rrd4j.data;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Util;

public class VariableTest {
    static final private String backend = "MEMORY";
    static final private String fileName = "variabletest.rrd";
    static final double[] vals = {
            39.0, 94.0, 95.0, 101.0, 155.0, 262.0, 274.0, 302.0, 319.0, 402.0, 466.0, 468.0, 494.0, 549.0, 550.0, 575.0, 600.0, 615.0, 625.0, 703.0, 729.0, 824.0, 976.0, 1018.0, 1036.0, 1138.0, 1195.0, 1265.0, 1287.0, 1323.0, 1410.0, 1443.0, 1516.0, 1538.0, 1664.0, 1686.0, 1801.0, 1912.0, 1921.0, 1929.0, 1936.0, 1941.0, 1985.0, 2003.0, 2010.0, 2013.0, 2082.0, 2106.0, 2213.0, 2358.0, 2394.0, 2572.0, 2616.0, 2627.0, 2676.0, 2694.0, 2736.0, 2740.0, 2966.0, 3005.0, 3037.0, 3041.0, 3146.0, 3194.0, 3228.0, 3235.0, 3243.0, 3339.0, 3365.0, 3414.0, 3440.0, 3454.0, 3567.0, 3570.0, 3615.0, 3619.0, 3802.0, 3831.0, 3864.0, 4061.0, 4084.0, 4106.0, 4233.0, 4328.0, 4362.0, 4372.0, 4376.0, 4388.0, 4413.0, 4527.0, 4612.0, 4643.0, 4684.0, 4750.0, 4799.0, 4810.0, 4824.0, 4825.0, 4871.0, 4932.0, 5028.0, 5112.0, 5118.0, 5163.0, 5198.0, 5256.0, 5296.0, 5413.0, 5471.0, 5568.0, 5628.0, 5645.0, 5733.0, 5790.0, 5851.0, 5886.0, 5927.0, 5937.0, 6018.0, 6027.0, 6046.0, 6145.0, 6147.0, 6289.0, 6371.0, 6384.0, 6393.0, 6431.0, 6469.0, 6543.0, 6649.0, 6772.0, 6864.0, 6943.0, 7009.0, 7014.0, 7037.0, 7258.0, 7356.0, 7364.0, 7386.0, 7387.0, 7399.0, 7450.0, 7519.0, 7527.0, 7578.0, 7632.0, 7709.0, 7849.0, 7896.0, 7952.0, 7980.0, 8050.0, 8126.0, 8152.0, 8165.0, 8332.0, 8347.0, 8520.0, 8522.0, 8542.0, 8587.0, 8621.0, 8678.0, 8721.0, 8739.0, 8765.0, 8889.0, 8951.0, 8962.0, 9082.0, 9149.0, 9199.0, 9278.0, 9334.0, 9339.0, 9345.0, 9365.0, 9383.0, 9402.0, 9471.0, 9483.0, 9492.0, 9496.0, 9532.0, 9553.0, 9563.0, 9571.0, 9574.0, 100000.0, 120000.0, 150000.0, 200000.0, 500000.0, 1000000.0, 2000000.0, 4000000.0, 8000000.0, 16000000.0
    };

    private static final long start = Util.getTimestamp(2010, 4, 1);
    private static final long step = 300;
    private static long startTime;
    private static long endTime;
    private static final boolean dorrdtool = false;

    @BeforeClass
    public static void createRrd() throws IOException {
        startTime = start - start % step; 
        endTime = startTime +  200 * step;

        RrdDef def = new RrdDef(RrdBackendFactory.getFactory(backend).getUri(fileName), startTime - 3 * step, step);

        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
        def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);

        try (RrdDb db = new RrdDb(def, RrdBackendFactory.getFactory(backend))) {
            db.createSample((startTime - step )).setValue(0, 0.0).update();

            long sampleTime = startTime;
            for (double val : vals) {
                db.createSample(sampleTime).setValue(0, val).update();
                sampleTime += step;
            }

            if(dorrdtool) {
                String xmlfile = System.getProperty("java.io.tmpdir") + File.separator + "variabletest.xml";
                String rrdfile = System.getProperty("java.io.tmpdir") + File.separator + "variabletest.rrd";
                db.dumpXml(xmlfile);
                System.out.println("rrdtool restore " + xmlfile + " " + rrdfile);
                String cmd = "rrdtool graph /dev/null " +
                        String.format("--start=%d ", startTime ) +
                        String.format("--end=%d ", endTime) +
                        String.format("DEF:baz=%s:bar:AVERAGE ", rrdfile) +
                        "VDEF:min=baz,MINIMUM " + "PRINT:min:\"mininum %1.15le\" " +
                        "VDEF:max=baz,MAXIMUM " + "PRINT:max:\"maximum %1.15le\" " +
                        "VDEF:avg=baz,AVERAGE " + "PRINT:avg:\"average %1.15le\" " +
                        "VDEF:stdev=baz,STDEV " + "PRINT:stdev:\"stdev %1.15le\" " +
                        "VDEF:first=baz,FIRST " + "PRINT:first:\"first %1.15le\" " +
                        "VDEF:last=baz,LAST " + "PRINT:last:\"last %1.15le\" " +
                        "VDEF:total=baz,TOTAL " + "PRINT:total:\"total %1.15le\" " +
                        "VDEF:percent=baz,95,PERCENT " + "PRINT:percent:\"95-th percent %1.15le\" " +
                        "VDEF:percentnan=baz,95,PERCENTNAN " + "PRINT:percentnan:\"95-th percentnan %1.15le\" " +
                        "VDEF:lslope=baz,LSLSLOPE " + "PRINT:lslope:\"lslope %1.15le\" " +
                        "VDEF:lslint=baz,LSLINT " + "PRINT:lslint:\"lslintn %1.15le\" " +
                        "VDEF:lslcorrel=baz,LSLCORREL " + "PRINT:lslcorrel:\"lslcorrel %1.15le\" ";
                System.out.println(cmd);
                long interval = (endTime - startTime) / 3;
                String cmd2 = "rrdtool graph /dev/null " +
                        String.format("--start=%d ", startTime  + interval) +
                        String.format("--end=%d ", endTime - interval) +
                        String.format("DEF:baz=%s:bar:AVERAGE ", rrdfile) +
                        "VDEF:min=baz,MINIMUM " + "PRINT:min:\"mininum %1.15le\" " +
                        "VDEF:max=baz,MAXIMUM " + "PRINT:max:\"maximum %1.15le\" " +
                        "VDEF:avg=baz,AVERAGE " + "PRINT:avg:\"average %1.15le\" " +
                        "VDEF:stdev=baz,STDEV " + "PRINT:stdev:\"stdev %1.15le\" " +
                        "VDEF:first=baz,FIRST " + "PRINT:first:\"first %1.15le\" " +
                        "VDEF:last=baz,LAST " + "PRINT:last:\"last %1.15le\" " +
                        "VDEF:total=baz,TOTAL " + "PRINT:total:\"total %1.15le\" " +
                        "VDEF:percent=baz,95,PERCENT " + "PRINT:percent:\"95-th percent %1.15le\" " +
                        "VDEF:percentnan=baz,95,PERCENTNAN " + "PRINT:percentnan:\"95-th percentnan %1.15le\" " +
                        "VDEF:lslope=baz,LSLSLOPE " + "PRINT:lslope:\"lslope %1.15le\" " +
                        "VDEF:lslint=baz,LSLINT " + "PRINT:lslint:\"lslintn %1.15le\" " +
                        "VDEF:lslcorrel=baz,LSLCORREL " + "PRINT:lslcorrel:\"lslcorrel %1.15le\" ";
                System.out.println(cmd2);

                String cmd3 = "rrdtool graph /dev/null " +
                        String.format("--start=%d ", startTime - 10 * step) +
                        String.format("--end=%d ", endTime + 2 * step) +
                        String.format("DEF:baz=%s:bar:AVERAGE ", rrdfile) +
                        "VDEF:min=baz,MINIMUM " + "PRINT:min:\"mininum %1.15le\" " +
                        "VDEF:max=baz,MAXIMUM " + "PRINT:max:\"maximum %1.15le\" " +
                        "VDEF:avg=baz,AVERAGE " + "PRINT:avg:\"average %1.15le\" " +
                        "VDEF:stdev=baz,STDEV " + "PRINT:stdev:\"stdev %1.15le\" " +
                        "VDEF:first=baz,FIRST " + "PRINT:first:\"first %1.15le\" " +
                        "VDEF:last=baz,LAST " + "PRINT:last:\"last %1.15le\" " +
                        "VDEF:total=baz,TOTAL " + "PRINT:total:\"total %1.15le\" " +
                        "VDEF:percent=baz,95,PERCENT " + "PRINT:percent:\"95-th percent %1.15le\" " +
                        "VDEF:percentnan=baz,95,PERCENTNAN " + "PRINT:percentnan:\"95-th percentnan %1.15le\" " +
                        "VDEF:lslope=baz,LSLSLOPE " + "PRINT:lslope:\"lslope %1.15le\" " +
                        "VDEF:lslint=baz,LSLINT " + "PRINT:lslint:\"lslintn %1.15le\" " +
                        "VDEF:lslcorrel=baz,LSLCORREL " + "PRINT:lslcorrel:\"lslcorrel %1.15le\" ";
                System.out.println(cmd3);
            }
        };


    }

    private DataProcessor getDp(Variable v) throws IOException {
        DataProcessor dp = new DataProcessor(startTime, endTime);
        dp.addDatasource("baz", fileName, "bar", ConsolFun.AVERAGE, backend);
        dp.addDatasource("value", "baz", v);
        dp.processData();
        return dp;
    }

    // A range that does not fit steps
    private DataProcessor getDp2(Variable v) throws IOException {
        long interval = (endTime - startTime) / 3;
        DataProcessor dp = new DataProcessor(startTime + interval, endTime - interval);
        dp.addDatasource("baz", fileName, "bar", ConsolFun.AVERAGE, backend);
        dp.addDatasource("value", "baz", v);
        dp.processData();
        return dp;
    }

    //A range that includes NaN
    private DataProcessor getDp3(Variable v) throws IOException {
        DataProcessor dp = new DataProcessor(startTime - 10 * step , endTime + 2 * step);
        dp.addDatasource("baz", fileName, "bar", ConsolFun.AVERAGE, backend);
        dp.addDatasource("value", "baz", v);
        dp.processData();
        return dp;
    }

    @Test
    public void test95Percentile() throws Exception {        
        DataProcessor dp = getDp(new Variable.PERCENTILE(95));
        // rrdtools says 9.574000e+03
        Assert.assertEquals("Wrong percentile", 9.574000e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void test95Percentile2() throws Exception {        
        DataProcessor dp = getDp2(new Variable.PERCENTILE(95));
        // rrdtools says 6.772000000000000e+03
        Assert.assertEquals("Wrong percentile", 6.772000000000000e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void test95Percentile3() throws Exception {        
        DataProcessor dp = getDp3(new Variable.PERCENTILE(95));
        // rrdtools says 9.571000000000000e+03
        Assert.assertEquals("Wrong percentile", 9.571000000000000e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void test95PercentileNaN() throws Exception {        
        DataProcessor dp = getDp(new Variable.PERCENTILENAN(95));
        // rrdtools says 9.574000e+03
        Assert.assertEquals("Wrong percentile", 9.574000e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void test95PercentileNaN2() throws Exception {        
        DataProcessor dp = getDp2(new Variable.PERCENTILENAN(95));
        // rrdtools says 6.772000000000000e+03
        Assert.assertEquals("Wrong percentile", 6.772000000000000e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void test95PercentileNan3() throws Exception {        
        DataProcessor dp = getDp3(new Variable.PERCENTILENAN(95));
        // rrdtools says 9.574000000000000e+03
        Assert.assertEquals("Wrong percentile", 9.574000000000000e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testFirst() throws Exception {
        DataProcessor dp = getDp(new Variable.FIRST());
        // rrdtools says 9.400000e+01
        Assert.assertEquals("Wrong first", 9.400000e+01, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testFirst2() throws Exception {
        DataProcessor dp = getDp2(new Variable.FIRST());
        // rrdtools says 3.339000000000000e+03
        Assert.assertEquals("Wrong first", 3.339000000000000e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testFirst3() throws Exception {
        DataProcessor dp = getDp3(new Variable.FIRST());
        // rrdtools says 0.000000000000000e+00
        Assert.assertEquals("Wrong first", 0.000000000000000e+00, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testLast() throws Exception {
        DataProcessor dp = getDp(new Variable.LAST());
        // rrdtools says 1.600000e+07
        Assert.assertEquals("Wrong last", 1.600000e+07, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testLast2() throws Exception {
        DataProcessor dp = getDp2(new Variable.LAST());
        // rrdtools says 7.009000000000000e+03
        Assert.assertEquals("Wrong last", 7.009000000000000e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testLast3() throws Exception {
        DataProcessor dp = getDp3(new Variable.LAST());
        // rrdtools says 1.600000000000000e+07
        Assert.assertEquals("Wrong last", 1.600000000000000e+07, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testMin() throws Exception {
        DataProcessor dp = getDp(new Variable.MIN());
        // rrdtools says 9.400000e+01
        Assert.assertEquals("Wrong minimum", 9.400000e+01, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testMin2() throws Exception {
        DataProcessor dp = getDp2(new Variable.MIN());
        // rrdtools says 3.339000000000000e+03
        Assert.assertEquals("Wrong minimum", 3.339000000000000e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testMin3() throws Exception {
        DataProcessor dp = getDp3(new Variable.MIN());
        // rrdtools says 0.000000000000000e+00
        Assert.assertEquals("Wrong minimum", 0.000000000000000e+00, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testMax() throws Exception {
        DataProcessor dp = getDp(new Variable.MAX());
        // rrdtools says 1.600000e+07
        Assert.assertEquals("Wrong maximum", 1.600000e+07, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testMax2() throws Exception {
        DataProcessor dp = getDp2(new Variable.MAX());
        // rrdtools says 7.009000000000000e+03
        Assert.assertEquals("Wrong maximum", 7.009000000000000e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testMax3() throws Exception {
        DataProcessor dp = getDp3(new Variable.MAX());
        // rrdtools says 1.600000000000000e+07
        Assert.assertEquals("Wrong maximum", 1.600000000000000e+07, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testTotal() throws Exception {
        DataProcessor dp = getDp(new Variable.TOTAL());
        // rrdtools says 9.896709e+09
        Assert.assertEquals("Wrong total", 9.896709e+09, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testTotal2() throws Exception {
        DataProcessor dp = getDp2(new Variable.TOTAL());
        // rrdtools says 1.039200000000000e+08
        Assert.assertEquals("Wrong total", 1.039200000000000e+08, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testTotal3() throws Exception {
        DataProcessor dp = getDp3(new Variable.TOTAL());
        // rrdtools says 9.896720700000000e+09
        Assert.assertEquals("Wrong total", 9.896720700000000e+09, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testAverage() throws Exception {
        DataProcessor dp = getDp(new Variable.AVERAGE());
        // rrdtools says 1.657740201e+05
        Assert.assertEquals("Wrong average", 1.657740201e+05, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testAverage2() throws Exception {
        DataProcessor dp = getDp2(new Variable.AVERAGE());
        // rrdtools says 5.094117647058823e+03
        Assert.assertEquals("Wrong average", 5.094117647058823e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testAverage3() throws Exception {
        DataProcessor dp = getDp3(new Variable.AVERAGE());
        // rrdtools says 3.295591240875912e+03
        Assert.assertEquals("Wrong average", 1.633122227722772e+05, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testStdDev() throws Exception {
        DataProcessor dp = getDp(new Variable.STDDEV());
        // rrdtools says 1.299157546152125e+06, but it might be wrong
        Assert.assertEquals("Wrong standard deviation", 1302434.1151546114, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testStdDev2() throws Exception {
        DataProcessor dp = getDp2(new Variable.STDDEV());
        // rrdtools says 1.040348817767271e+03, but it might be wrong
        Assert.assertEquals("Wrong standard deviation", 1048.0838597160848, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testStdDev3() throws Exception {
        DataProcessor dp = getDp3(new Variable.STDDEV());
        // rrdtools says 1.289630121109904e+06, but it might be wrong
        Assert.assertEquals("Wrong standard deviation", 1292834.1760384508, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testLslSlope() throws Exception {
        DataProcessor dp = getDp(new Variable.LSLSLOPE());
        // rrdtools says 4.823830423328765e+03
        Assert.assertEquals("Wrong LSL slope", 4.823830423328765e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testLslSlope2() throws Exception {
        DataProcessor dp = getDp2(new Variable.LSLSLOPE());
        // rrdtools says 5.289899606825209e+01
        Assert.assertEquals("Wrong LSL slope", 5.289899606825209e+01, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testLslSlope3() throws Exception {
        DataProcessor dp = getDp3(new Variable.LSLSLOPE());
        // rrdtools says 4.684118512689442e+03
        Assert.assertEquals("Wrong LSL slope", 4.684118512689442e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testLslInt() throws Exception {
        DataProcessor dp = getDp(new Variable.LSLINT());
        // rrdtools says -3.117851918090452e+05
        Assert.assertEquals("Wrong LSL y-intercept", -3.117851918090452e+05, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testLslInt2() throws Exception {
        DataProcessor dp = getDp2(new Variable.LSLINT());
        // rrdtools says 3.322001278772379e+03
        Assert.assertEquals("Wrong LSL y-intercept", 3.322001278772379e+03, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testLslInt3() throws Exception {
        DataProcessor dp = getDp3(new Variable.LSLINT());
        // rrdtools says -3.121258062657012e+05
        Assert.assertEquals("Wrong LSL y-intercept", -3.402305173418378e+05, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testlslCorrel() throws Exception {
        DataProcessor dp = getDp(new Variable.LSLCORREL());
        // rrdtools says 2.132982e-01
        Assert.assertEquals("Wrong LSL Correlation Coefficient", 2.132982e-01, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testlslCorrel2() throws Exception {
        DataProcessor dp = getDp2(new Variable.LSLCORREL());
        // rrdtools says 9.980212206387034e-01
        Assert.assertEquals("Wrong LSL Correlation Coefficient", 9.980212206387034e-01, dp.getVariable("value").value, 1e-6);        
    }

    @Test
    public void testlslCorrel3() throws Exception {
        DataProcessor dp = getDp3(new Variable.LSLCORREL());
        // rrdtools says 2.117961840477416e-01
        Assert.assertEquals("Wrong LSL Correlation Coefficient", 2.117961840477416e-01, dp.getVariable("value").value, 1e-6);        
    }

}
