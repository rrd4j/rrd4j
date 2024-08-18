package org.rrd4j.graph;

import java.awt.Color;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.GraphTester;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdNioBackendFactory;

public class ImageFormatTest extends GraphTester {

    private static final String rrdpath = ImageFormatTest.class.getResource("/demo1.rrd").getFile();
    private static RrdBackendFactory previousBackend;

    @BeforeClass
    public static void setBackendBefore() {
        previousBackend = RrdBackendFactory.getDefaultFactory();
        RrdBackendFactory.setActiveFactories(new RrdNioBackendFactory(0));
    }

    @AfterClass
    public static void setBackendAfter() {
        RrdBackendFactory.setActiveFactories(previousBackend);
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private RrdGraphDef doGraph() {
        RrdGraphDef gDef = new RrdGraphDef(1, 100);
        gDef.setWidth(200);
        gDef.setHeight(200);
        gDef.datasource("sun", rrdpath, "sun", ConsolFun.AVERAGE);
        gDef.line("sun", Color.GREEN, "sun temp");
        return gDef;
    }
    
    private void doGraph(RrdGraphDef gDef) throws IOException {
        RrdGraph graph = new RrdGraph(gDef);
        Assert.assertTrue(graph.getRrdGraphInfo().getByteCount() != 0);
    }
    
    @Test
    public void testJPEG() throws IOException {
        RrdGraphDef gDef = doGraph();
        gDef.setImageFormat("jpeg");
        gDef.setImageQuality(0.99f);
        saveGraph(gDef, testFolder, "ImageFormatTest", "testJPEG", "jpeg");
        doGraph(gDef);
    }

    @Test
    public void testGIF() throws IOException {
        RrdGraphDef gDef = doGraph();
        gDef.setImageFormat("gif");
        saveGraph(gDef, testFolder, "ImageFormatTest", "testGIF", "gif");
        doGraph(gDef);
    }

    @Test
    public void testPNG() throws IOException {
        RrdGraphDef gDef = doGraph();
        gDef.setImageFormat("png");
        saveGraph(gDef, testFolder, "ImageFormatTest", "testPNG", "png");
        doGraph(gDef);
    }

    @Test
    public void testBMP() throws IOException {
        RrdGraphDef gDef = doGraph();
        gDef.setImageFormat("bmp");
        saveGraph(gDef, testFolder, "ImageFormatTest", "testBMP", "bmp");
        doGraph(gDef);
    }

    @Test(expected=RuntimeException.class)
    public void testWBMP() throws IOException {
        RrdGraphDef gDef = doGraph();
        gDef.setImageFormat("wbmp");
        doGraph(gDef);
    }

}
