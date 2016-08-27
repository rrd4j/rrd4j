package org.rrd4j.graph;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.awt.Font;
import java.io.IOException;
import java.util.Locale;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.graph.RrdGraphConstants.FontConstructor;

public class TimeAxisText extends DummyGraph {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private TimeAxis timeAxis;
    private String jrbFileName;
    private final long startTime = 1;

    @Before 
    public void setup() throws IOException {
        jrbFileName = testFolder.newFile("test-value-axis.rrd").getCanonicalPath();
    }

    private void createGaugeRrd(int rowCount) throws IOException {
        RrdDef def = new RrdDef(jrbFileName);
        def.setStartTime(startTime);
        def.setStep(60);
        def.addDatasource("testvalue", DsType.GAUGE, 120, Double.NaN, Double.NaN);
        def.addArchive("RRA:AVERAGE:0.5:1:"+rowCount);

        //Create the empty rrd.  Other code may open and append data
        RrdDb rrd = new RrdDb(def);
        rrd.close();
    }

    private void prepareGraph() throws IOException {

        graphDef = new RrdGraphDef();
        graphDef.datasource("testvalue", jrbFileName, "testvalue", ConsolFun.AVERAGE);
        graphDef.setStartTime(startTime);
        graphDef.setEndTime(startTime + (60*60*24));
        graphDef.setLocale(Locale.US);


        //There's only a couple of methods of ImageWorker that we actually care about in this test.
        // More to the point, we want the rest to work as normal (like getFontHeight, getFontAscent etc)
        imageWorker = createMockBuilder(ImageWorker.class)
                .addMockedMethod("drawString")
                .createStrictMock(); //Order is important!

        buildGraph();

        timeAxis = new TimeAxis(imageParameters, imageWorker, graphDef, graphMapper);
    }

    @Test
    public void firstTest() throws IOException {
        createGaugeRrd(100);
        prepareGraph();

        imageWorker.drawString("06:00", 132, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);
        imageWorker.drawString("12:00", 232, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);
        imageWorker.drawString("18:00", 332, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);
        imageWorker.drawString("00:00", 432, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);
        replay(imageWorker);
        timeAxis.draw();
        //Validate the calls to the imageWorker
        verify(imageWorker);
    }

}
