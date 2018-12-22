package org.rrd4j;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

/**
 * The code from https://github.com/rrd4j/rrd4j/wiki/Tutorial, to be sure that it compiles and run.
 * @author Fabrice Bacchella
 *
 */
public class TutorialTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    private String root;
    
    @Before
    public void set() {
        try {
            root = testFolder.getRoot().getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCode1() throws IOException {
        RrdDef rrdDef = new RrdDef(root + "/test.rrd");
        rrdDef.setStartTime(920804400L);
        rrdDef.addDatasource("speed", DsType.COUNTER, 600, Double.NaN, Double.NaN);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 24);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 6, 10);
        RrdDb rrdDb = new RrdDb(rrdDef);
        rrdDb.close();
    }

    @Test
    public void testCode2() throws IOException {
        testCode1();
        RrdDb rrdDb = new RrdDb(root + "/test.rrd");
        Sample sample = rrdDb.createSample();
        sample.setAndUpdate("920804700:12345");
        sample.setAndUpdate("920805000:12357");
        sample.setAndUpdate("920805300:12363");
        sample.setAndUpdate("920805600:12363");
        sample.setAndUpdate("920805900:12363");
        sample.setAndUpdate("920806200:12373");
        sample.setAndUpdate("920806500:12383");
        sample.setAndUpdate("920806800:12393");
        sample.setAndUpdate("920807100:12399");
        sample.setAndUpdate("920807400:12405");
        sample.setAndUpdate("920807700:12411");
        sample.setAndUpdate("920808000:12415");
        sample.setAndUpdate("920808300:12420");
        sample.setAndUpdate("920808600:12422");
        sample.setAndUpdate("920808900:12423");
        rrdDb.close();
    }

    @Test
    public void testCode3() throws IOException {
        testCode1();
        RrdDb rrdDb = new RrdDb(root + "/test.rrd");
        FetchRequest fetchRequest = rrdDb.createFetchRequest(ConsolFun.AVERAGE, 920804400L, 920809200L);
        FetchData fetchData = fetchRequest.fetchData();
        fetchData.dump();
        rrdDb.close();
    }

    @Test
    public void testCode4() throws IOException {
        testCode1();
        RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.setTimeSpan(920804400L, 920808000L);
        graphDef.datasource("myspeed", root + "/test.rrd", "speed", ConsolFun.AVERAGE);
        graphDef.line("myspeed", new Color(0xFF, 0, 0), null, 2);
        graphDef.setFilename(root + "/speed.gif");
        RrdGraph graph = new RrdGraph(graphDef);
        BufferedImage bi = new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
        graph.render(bi.getGraphics());
    }

    @Test
    public void testCode5() throws IOException {
        testCode1();
        RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.setTimeSpan(920804400L, 920808000L);
        graphDef.setVerticalLabel("m/s");
        graphDef.datasource("myspeed", root + "/test.rrd", "speed", ConsolFun.AVERAGE);
        graphDef.datasource("realspeed", "myspeed,1000,*");
        graphDef.line("realspeed", new Color(0xFF, 0, 0), null, 2);
        graphDef.setFilename(Paths.get(testFolder.getRoot().getAbsolutePath(), "speed2.gif").toString());
        RrdGraph graph = new RrdGraph(graphDef);
        BufferedImage bi = new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
        graph.render(bi.getGraphics());
    }

    @Test
    public void testCode6() throws IOException {
        testCode1();
        RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.setTimeSpan(920804400L, 920808000L);
        graphDef.setVerticalLabel("km/h");
        graphDef.datasource("myspeed", root + "/test.rrd", "speed", ConsolFun.AVERAGE);
        graphDef.datasource("kmh", "myspeed,3600,*");
        graphDef.datasource("fast", "kmh,100,GT,kmh,0,IF");
        graphDef.datasource("good", "kmh,100,GT,0,kmh,IF");
        graphDef.area("good", new Color(0, 0xFF, 0), "Good speed");
        graphDef.area("fast", new Color(0xFF, 0, 0), "Too fast");
        graphDef.hrule(100, new Color(0, 0, 0xFF), "Maximum allowed");
        graphDef.setFilename(root + "/speed3.gif");
        RrdGraph graph = new RrdGraph(graphDef);
        BufferedImage bi = new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
        graph.render(bi.getGraphics());
    }

    @Test
    public void testCode7() throws IOException {
        testCode1();
        RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.setTimeSpan(920804400L, 920808000L);
        graphDef.setVerticalLabel("km/h");
        graphDef.datasource("myspeed", root + "/test.rrd", "speed", ConsolFun.AVERAGE);
        graphDef.datasource("kmh", "myspeed,3600,*");
        graphDef.datasource("fast", "kmh,100,GT,100,0,IF");
        graphDef.datasource("over", "kmh,100,GT,kmh,100,-,0,IF");
        graphDef.datasource("good", "kmh,100,GT,0,kmh,IF");
        graphDef.area("good", new Color(0, 0xFF, 0), "Good speed");
        graphDef.area("fast", new Color(0x55, 0, 0), "Too fast");
        graphDef.stack("over", new Color(0xFF, 0, 0), "Over speed");
        graphDef.hrule(100, new Color(0, 0, 0xFF), "Maximum allowed");
        graphDef.setFilename(root + "/speed4.gif");
        RrdGraph graph = new RrdGraph(graphDef);
        BufferedImage bi = new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
        graph.render(bi.getGraphics());
    }

    @Test
    public void testCode8() throws IOException {
        RrdDef rrdDef = new RrdDef(root + "/myrouter.rrd");
        rrdDef.addDatasource("input", DsType.COUNTER, 600, Double.NaN, Double.NaN);
        rrdDef.addDatasource("output", DsType.COUNTER, 600, Double.NaN, Double.NaN);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 600);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 6, 700);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 24, 775);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 288, 797);
        rrdDef.addArchive(ConsolFun.MAX, 0.5, 1, 600);
        rrdDef.addArchive(ConsolFun.MAX, 0.5, 6, 700);
        rrdDef.addArchive(ConsolFun.MAX, 0.5, 24, 775);
        rrdDef.addArchive(ConsolFun.MAX, 0.5, 288, 797);
        RrdDb rrdDb = new RrdDb(rrdDef);
        rrdDb.close();
    }

    @Test
    public void testCode9() throws IOException {
        RrdDef rrdDef = new RrdDef(root + "/myrouter.rrd");
        rrdDef.addDatasource("DS:input:COUNTER:600:U:U");
        rrdDef.addDatasource("DS:output:COUNTER:600:U:U");
        rrdDef.addArchive("RRA:AVERAGE:0.5:1:600");
        rrdDef.addArchive("RRA:AVERAGE:0.5:6:700");
        rrdDef.addArchive("RRA:AVERAGE:0.5:24:775");
        rrdDef.addArchive("RRA:AVERAGE:0.5:288:797");
        rrdDef.addArchive("RRA:MAX:0.5:1:600");
        rrdDef.addArchive("RRA:MAX:0.5:6:700");
        rrdDef.addArchive("RRA:MAX:0.5:24:775");
        rrdDef.addArchive("RRA:MAX:0.5:288:797");
        RrdDb rrdDb = new RrdDb(rrdDef);
        rrdDb.close();
    }

    @Test
    public void testCode10() throws IOException {
        testCode9();
        RrdGraphDef graphDef = new RrdGraphDef();
        long endTime = Util.getTime();
        long startTime = endTime - (24*60*60L);
        graphDef.setTimeSpan(startTime, endTime);
        graphDef.datasource("inoctets", root + "/myrouter.rrd", "input", ConsolFun.AVERAGE);
        graphDef.datasource("outoctets", root + "/myrouter.rrd", "output", ConsolFun.AVERAGE);
        graphDef.area("inoctets", new Color(0, 0xFF, 0), "In traffic");
        graphDef.line("outoctets", new Color(0, 0, 0xFF), "Out traffic", 1);
        graphDef.setFilename(root + "myrouter-day.gif");
        BufferedImage bi = new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
        RrdGraph graph = new RrdGraph(graphDef);
        graph.render(bi.getGraphics());
    }

    @Test
    public void testCode11() throws IOException {
        RrdDef rrdDef = new RrdDef(root + "/all.rrd");
        rrdDef.setStartTime(978300900L);
        rrdDef.addDatasource("a", DsType.COUNTER, 600, Double.NaN, Double.NaN);
        rrdDef.addDatasource("b", DsType.GAUGE, 600, Double.NaN, Double.NaN);
        rrdDef.addDatasource("c", DsType.DERIVE, 600, Double.NaN, Double.NaN);
        rrdDef.addDatasource("d", DsType.ABSOLUTE, 600, Double.NaN, Double.NaN);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 10);
        RrdDb rrdDb = new RrdDb(rrdDef);
        Sample sample = rrdDb.createSample();
        sample.setAndUpdate("978301200:300:1:600:300");
        sample.setAndUpdate("978301500:600:3:1200:600");
        sample.setAndUpdate("978301800:900:5:1800:900");
        sample.setAndUpdate("978302100:1200:3:2400:1200");
        sample.setAndUpdate("978302400:1500:1:2400:1500");
        sample.setAndUpdate("978302700:1800:2:1800:1800");
        sample.setAndUpdate("978303000:2100:4:0:2100");
        sample.setAndUpdate("978303300:2400:6:600:2400");
        sample.setAndUpdate("978303600:2700:4:600:2700");
        sample.setAndUpdate("978303900:3000:2:1200:3000");
        rrdDb.close();
        RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.setTimeSpan(978300600L, 978304200L);
        graphDef.datasource("linea", root + "/all.rrd", "a", ConsolFun.AVERAGE);
        graphDef.datasource("lineb", root + "/all.rrd", "b", ConsolFun.AVERAGE);
        graphDef.datasource("linec", root + "/all.rrd", "c", ConsolFun.AVERAGE);
        graphDef.datasource("lined", root + "/all.rrd", "d", ConsolFun.AVERAGE);
        graphDef.line("linea", Color.RED, "Line A", 3);
        graphDef.line("lineb", Color.GREEN, "Line B", 3);
        graphDef.line("linec", Color.BLUE, "Line C", 3);
        graphDef.line("lined", Color.BLACK, "Line D", 3);
        graphDef.setFilename(root + "all1.gif");
        graphDef.setWidth(400);
        graphDef.setHeight(400);
        RrdGraph graph = new RrdGraph(graphDef);
        BufferedImage bim = new BufferedImage(400,400,BufferedImage.TYPE_INT_RGB);
        graph.render(bim.getGraphics());
    }

}
