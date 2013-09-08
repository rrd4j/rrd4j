package org.rrd4j.graph;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.backend.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.data.Variable;

public class PercentileTest {
    static final private String backend = "MEMORY";
    static final private String fileName = "foobar.rrd";
    static final double[] vals = {
            39.0, 94.0, 95.0, 101.0, 155.0, 262.0, 274.0, 302.0, 319.0, 402.0, 466.0, 468.0, 494.0, 549.0, 550.0, 575.0, 600.0, 615.0, 625.0, 703.0, 729.0, 824.0, 976.0, 1018.0, 1036.0, 1138.0, 1195.0, 1265.0, 1287.0, 1323.0, 1410.0, 1443.0, 1516.0, 1538.0, 1664.0, 1686.0, 1801.0, 1912.0, 1921.0, 1929.0, 1936.0, 1941.0, 1985.0, 2003.0, 2010.0, 2013.0, 2082.0, 2106.0, 2213.0, 2358.0, 2394.0, 2572.0, 2616.0, 2627.0, 2676.0, 2694.0, 2736.0, 2740.0, 2966.0, 3005.0, 3037.0, 3041.0, 3146.0, 3194.0, 3228.0, 3235.0, 3243.0, 3339.0, 3365.0, 3414.0, 3440.0, 3454.0, 3567.0, 3570.0, 3615.0, 3619.0, 3802.0, 3831.0, 3864.0, 4061.0, 4084.0, 4106.0, 4233.0, 4328.0, 4362.0, 4372.0, 4376.0, 4388.0, 4413.0, 4527.0, 4612.0, 4643.0, 4684.0, 4750.0, 4799.0, 4810.0, 4824.0, 4825.0, 4871.0, 4932.0, 5028.0, 5112.0, 5118.0, 5163.0, 5198.0, 5256.0, 5296.0, 5413.0, 5471.0, 5568.0, 5628.0, 5645.0, 5733.0, 5790.0, 5851.0, 5886.0, 5927.0, 5937.0, 6018.0, 6027.0, 6046.0, 6145.0, 6147.0, 6289.0, 6371.0, 6384.0, 6393.0, 6431.0, 6469.0, 6543.0, 6649.0, 6772.0, 6864.0, 6943.0, 7009.0, 7014.0, 7037.0, 7258.0, 7356.0, 7364.0, 7386.0, 7387.0, 7399.0, 7450.0, 7519.0, 7527.0, 7578.0, 7632.0, 7709.0, 7849.0, 7896.0, 7952.0, 7980.0, 8050.0, 8126.0, 8152.0, 8165.0, 8332.0, 8347.0, 8520.0, 8522.0, 8542.0, 8587.0, 8621.0, 8678.0, 8721.0, 8739.0, 8765.0, 8889.0, 8951.0, 8962.0, 9082.0, 9149.0, 9199.0, 9278.0, 9334.0, 9339.0, 9345.0, 9365.0, 9383.0, 9402.0, 9471.0, 9483.0, 9492.0, 9496.0, 9532.0, 9553.0, 9563.0, 9571.0, 9574.0, 100000.0, 120000.0, 150000.0, 200000.0, 500000.0, 1000000.0, 2000000.0, 4000000.0, 8000000.0, 16000000.0
    };
    
    static private final long now = System.currentTimeMillis();

    @Test
    public void testSampleVDEFPercentile1() throws Exception {
        RrdBackendFactory factory = RrdBackendFactory.getFactory(backend);
        factory.start();

        long startTime = (int)( now / 1000);
        startTime -= (startTime % 300); 
        long endTime = startTime +  200 * 300;

        createRrdFile(fileName, startTime);
        System.out.println(String.format("rrdtool graph /dev/null -s %d -e %d 'DEF:baz=/tmp/rrd1.rrd:bar:AVERAGE' 'VDEF:nfp=baz,95,PERCENT' 'PRINT:nfp:%%le'", startTime - 300, endTime + 300));

        RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.setStartTime(startTime - 300);
        graphDef.setEndTime(endTime + 300);
        graphDef.datasource("baz", fileName, "bar", ConsolFun.AVERAGE, backend);
        graphDef.datasource("nfp", "baz", new Variable.PERCENTILE(95));
        graphDef.print("nfp", "%le", false);
        graphDef.setLocale(Locale.ENGLISH);
        RrdGraph graph = new RrdGraph(graphDef);
        Assert.assertNotNull("graph object", graph);

        RrdGraphInfo info = graph.getRrdGraphInfo();
        Assert.assertNotNull("graph info object", info);

        String[] printLines = info.getPrintLines();
        Assert.assertNotNull("graph printLines", printLines);
        Assert.assertEquals("graph printLines size", 1, printLines.length);
        // rrdtools says 9.574000e+03
        Assert.assertEquals("graph printLines item 0", "9.574000e+03", printLines[0]);

        factory.stop();
    }

    @Test
    public void testSampleVDEFPercentile2() throws Exception {

        long startTime = (int)( now / 1000);
        startTime -= (startTime % 300); 
        long endTime = startTime +  100 * 300;

        createRrdFile(fileName, startTime);
        System.out.println(String.format("rrdtool graph /dev/null -s %d -e %d 'DEF:baz=/tmp/rrd2.rrd:bar:AVERAGE' 'VDEF:nfp=baz,95,PERCENT' 'PRINT:nfp:%%le'", startTime - 300, endTime + 300));

        RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.setStartTime(startTime - 300);
        graphDef.setEndTime(endTime + 300);
        graphDef.datasource("baz", fileName, "bar", ConsolFun.AVERAGE, backend);
        graphDef.datasource("nfp", "baz", new Variable.PERCENTILE(95));
        graphDef.print("nfp", "%le", false);
        graphDef.setLocale(Locale.ENGLISH);
        RrdGraph graph = new RrdGraph(graphDef);
        Assert.assertNotNull("graph object", graph);

        RrdGraphInfo info = graph.getRrdGraphInfo();
        Assert.assertNotNull("graph info object", info);

        String[] printLines = info.getPrintLines();
        Assert.assertNotNull("graph printLines", printLines);
        Assert.assertEquals("graph printLines size", 1, printLines.length);
        // rrdtools says 4.810000e+03
        Assert.assertEquals("graph printLines item 0", "4.799000e+03", printLines[0]);
    }

    public RrdDb createRrdFile(String fileName, long startTime) throws Exception {
        RrdDef def = new RrdDef(fileName, 300);

        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
        def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);

        RrdDb db = new RrdDb(def, RrdBackendFactory.getFactory(backend));

        db.createSample((startTime + 300 )).setValue(0, 0.0).update();
        long sampleTime = startTime + 600;
        for (double val : vals) {
            db.createSample(sampleTime).setValue(0, val).update();
            sampleTime += 300;
        }

        return db;
    }

}
