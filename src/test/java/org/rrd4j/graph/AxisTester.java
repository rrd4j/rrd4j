package org.rrd4j.graph;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdRandomAccessFileBackendFactory;
import org.rrd4j.core.Util;

public abstract class AxisTester<T extends Axis> {

    static private RrdBackendFactory previousBackend;

    protected ImageWorker imageWorker;
    ImageParameters imageParameters;
    protected RrdGraphDef graphDef;

    @BeforeClass
    public static void setBackendBefore() {
        previousBackend = RrdBackendFactory.getDefaultFactory();
        RrdBackendFactory.setActiveFactories(new RrdRandomAccessFileBackendFactory());
    }

    @AfterClass
    public static void setBackendAfter() {
        RrdBackendFactory.setActiveFactories(previousBackend);
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    String jrbFileName;
    final long startTime = 1;
    private T valueAxis;

    @Before 
    public void setup() throws IOException {
        jrbFileName = testFolder.newFile("test-value-axis.rrd").getCanonicalPath();
    }

    void createGaugeRrd(int rowCount) throws IOException {
        RrdDef def = new RrdDef(jrbFileName);
        def.setStartTime(startTime);
        def.setStep(60);
        def.addDatasource("testvalue", DsType.GAUGE, 120, Double.NaN, Double.NaN);
        def.addArchive("RRA:AVERAGE:0.5:1:" + rowCount);

        //Create the empty rrd.  Other code may open and append data
        try (RrdDb rrd = RrdDb.getBuilder().setRrdDef(def).build()) {
        }

    }

    //Cannot be called until the RRD has been populated; wait
    void prepareGraph() throws IOException {

        graphDef = new RrdGraphDef(startTime, startTime + (60*60*24));
        graphDef.datasource("testvalue", jrbFileName, "testvalue", ConsolFun.AVERAGE);
        graphDef.area("testvalue", Util.parseColor("#FF0000"), "TestValue");
        graphDef.setLocale(Locale.US);

        setupGraphDef();

        RrdGraph graph = new RrdGraph(graphDef);

        imageParameters = graph.im;
        //There's only a couple of methods of ImageWorker that we actually care about in this test.
        // More to the point, we want the rest to work as normal (like getFontHeight, getFontAscent etc)
        imageWorker = createMockBuilder(BufferedImageWorker.class)
                .addMockedMethod("drawLine")
                .addMockedMethod("drawString")
                .withConstructor(Integer.TYPE, Integer.TYPE)
                .withArgs(imageParameters.xgif, imageParameters.ygif)
                .createStrictMock(); //Order is important!

        valueAxis = makeAxis(graph);

    }

    void run() {
        replay(imageWorker);

        Assert.assertTrue(valueAxis.draw());

        //Validate the calls to the imageWorker
        verify(imageWorker);
    }

    void setupGraphDef() {

    }

    abstract T makeAxis(RrdGraph graph);

}
