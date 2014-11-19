package org.rrd4j;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.ConsolFun.MAX;
import static org.rrd4j.DsType.GAUGE;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.data.Variable;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphConstants;
import org.rrd4j.graph.RrdGraphDef;
import org.rrd4j.graph.RrdGraphInfo;

public class TestDemo {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    static final long SEED = 1909752002L;
    static final Random RANDOM = new Random(SEED);
    static final String FILE = "demo";

    static final long START;
    static final long END;
    static {
        Calendar c1 = new GregorianCalendar(TimeZone.getTimeZone("CET"), Locale.US);
        c1.setTimeInMillis(0);
        c1.set(2010, 4, 1, 0, 0, 0);
        START = Util.getTimestamp(c1);

        Calendar c2 = new GregorianCalendar(TimeZone.getTimeZone("CET"), Locale.US);
        c2.setTimeInMillis(0);
        c2.set(2010, 6, 1, 0, 0, 0);
        END = Util.getTimestamp(c2);
    }

    static final int MAX_STEP = 300;

    static final int IMG_WIDTH = 500;
    static final int IMG_HEIGHT = 300;

    @Test
    public void main() throws IOException {
        System.out.println("== Starting demo");
        long startMillis = System.currentTimeMillis();
        long start = START;
        long end = END;

        String rrdPath = testFolder.getRoot().getCanonicalPath() + "FILE" + ".rrd";
        String xmlPath = testFolder.getRoot().getCanonicalPath() + "FILE" + ".xml";
        String rrdRestoredPath = testFolder.getRoot().getCanonicalPath() + "FILE" + "_restored.rrd";
        String imgPath = testFolder.getRoot().getCanonicalPath() + "FILE" + ".png";
        String logPath = testFolder.getRoot().getCanonicalPath() + "FILE" + ".log";
        PrintWriter log = new PrintWriter(new BufferedOutputStream(new FileOutputStream(logPath, false)));
        // creation
        System.out.println("== Creating RRD file " + rrdPath);
        RrdDef rrdDef = new RrdDef(rrdPath, start - 1, 300);
        rrdDef.setVersion(2);
        rrdDef.addDatasource("sun", GAUGE, 600, 0, Double.NaN);
        rrdDef.addDatasource("shade", GAUGE, 600, 0, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        rrdDef.addArchive(AVERAGE, 0.5, 6, 700);
        rrdDef.addArchive(AVERAGE, 0.5, 24, 775);
        rrdDef.addArchive(AVERAGE, 0.5, 288, 797);
        rrdDef.addArchive(MAX, 0.5, 1, 600);
        rrdDef.addArchive(MAX, 0.5, 6, 700);
        rrdDef.addArchive(MAX, 0.5, 24, 775);
        rrdDef.addArchive(MAX, 0.5, 288, 797);
        System.out.println(rrdDef.dump());
        log.println(rrdDef.dump());
        System.out.println("Estimated file size: " + rrdDef.getEstimatedSize());
        RrdDb rrdDb = new RrdDb(rrdDef);
        System.out.println("== RRD file created.");
        Assert.assertTrue(rrdDb.getRrdDef().equals(rrdDef));
        rrdDb.close();
        System.out.println("== RRD file closed.");

        // update database
        GaugeSource sunSource = new GaugeSource(1200, 20);
        GaugeSource shadeSource = new GaugeSource(300, 10);
        System.out.println("== Simulating one month of RRD file updates with step not larger than " +
                MAX_STEP + " seconds (* denotes 1000 updates)");
        long t = start;
        int n = 0;
        rrdDb = new RrdDb(rrdPath);
        Sample sample = rrdDb.createSample();

        while (t <= end + 172800L) {
            sample.setTime(t);
            sample.setValue("sun", sunSource.getValue());
            sample.setValue("shade", shadeSource.getValue());
            log.println(sample.dump());
            sample.update();
            t += RANDOM.nextDouble() * MAX_STEP + 1;
        }
        rrdDb.close();

        System.out.println("== Finished. RRD file updated " + n + " times");

        // test read-only access!
        rrdDb = new RrdDb(rrdPath, true);
        System.out.println("File reopen in read-only mode");
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("CET"), Locale.US);
        c.setTimeInMillis(rrdDb.getLastUpdateTime() * 1000);
        System.out.println("== Last update time was: " + String.format(Locale.US, "%tF %tT", c, c));
        System.out.println("== Last info was: " + rrdDb.getInfo());

        // fetch data
        System.out.println("== Fetching data for the whole month");
        FetchRequest request = rrdDb.createFetchRequest(AVERAGE, start, end);
        System.out.println(request.dump());
        log.println(request.dump());
        FetchData fetchData = request.fetchData();
        System.out.println("== Data fetched. " + fetchData.getRowCount() + " points obtained");
        System.out.println(fetchData.toString());
        System.out.println("== Dumping fetched data to XML format");
        System.out.println("== Fetch completed");

        // dump to XML file
        System.out.println("== Dumping RRD file to XML file " + xmlPath + " (can be restored with RRDTool)");
        rrdDb.exportXml(xmlPath);
        System.out.println("== Creating RRD file " + rrdRestoredPath + " from XML file " + xmlPath);
        RrdDb rrdRestoredDb = new RrdDb(rrdRestoredPath, xmlPath);

        // close files
        System.out.println("== Closing both RRD files");
        rrdDb.close();
        System.out.println("== First file closed");
        rrdRestoredDb.close();
        System.out.println("== Second file closed");

        // create graph
        System.out.println("Creating graph " + Util.getLapTime());
        System.out.println("== Creating graph from the second file");
        RrdGraphDef gDef = new RrdGraphDef();
        gDef.setLocale(Locale.US);
        gDef.setTimeZone(TimeZone.getTimeZone("CET"));
        gDef.setWidth(IMG_WIDTH);
        gDef.setHeight(IMG_HEIGHT);
        gDef.setFilename(imgPath);
        gDef.setStartTime(start);
        gDef.setEndTime(end);
        gDef.setTitle("Temperatures in May-June 2010");
        gDef.setVerticalLabel("temperature");
        gDef.setColor(RrdGraphConstants.COLOR_XAXIS, Color.BLUE);
        gDef.setColor(RrdGraphConstants.COLOR_YAXIS, new Color(0, 255, 0, 40));

        gDef.datasource("sun", rrdRestoredPath, "sun", AVERAGE);
        gDef.datasource("shade", rrdRestoredPath, "shade", AVERAGE);
        gDef.datasource("median", "sun,shade,+,2,/");
        gDef.datasource("diff", "sun,shade,-,ABS,-1,*");
        gDef.datasource("sine", "TIME," + start + ",-," + (end - start) + ",/,2,PI,*,*,SIN,1000,*");

        gDef.line("sun", Color.GREEN, "sun temp");
        gDef.line("shade", Color.BLUE, "shade temp");
        gDef.line("median", Color.MAGENTA, "median value");
        gDef.area("diff", Color.YELLOW, "difference");
        gDef.line("diff", Color.RED, null);
        gDef.line("sine", Color.CYAN, "sine fun");
        gDef.hrule(2568, Color.GREEN, "hrule");
        gDef.vrule((start + 2 * end) / 3, Color.MAGENTA, "vrule\\c");

        gDef.comment("\\r");

        Variable sunmax = new Variable.MAX();
        Variable sunaverage = new Variable.AVERAGE();
        gDef.datasource("sunmax", "sun", sunmax);
        gDef.datasource("sunaverage", "sun", sunaverage);
        gDef.gprint("sunmax", "maxSun = %.3f%s");
        gDef.gprint("sunaverage", "avgSun = %.3f%S\\c");
        gDef.print("sunmax", "maxSun = %.3f%s");
        gDef.print("sunmax", "maxSun time = %ts", true);
        gDef.print("sunaverage", "avgSun = %.3f%S\\c");

        gDef.datasource("shademax", "shade", new Variable.MAX());
        gDef.datasource("shadeverage", "shade", new Variable.AVERAGE());
        gDef.gprint("shademax", "maxShade = %.3f%S");
        gDef.gprint("shadeverage", "avgShade = %.3f%S\\c");
        gDef.print("shademax", "maxShade = %.3f%S");
        gDef.print("shadeverage", "avgShade = %.3f%S\\c");

        gDef.setImageInfo("<img src='%s' width='%d' height = '%d'>");
        gDef.setPoolUsed(false);
        gDef.setImageFormat("png");
        System.out.println("Rendering graph " + Util.getLapTime());
        // create graph finally
        RrdGraph graph = new RrdGraph(gDef);
        System.out.println(graph.getRrdGraphInfo().dump());
        System.out.println("== Graph created " + Util.getLapTime());
        // demo ends
        log.close();
        System.out.println("== Demo completed in " +
                ((System.currentTimeMillis() - startMillis) / 1000.0) + " sec");

        RrdGraphInfo graphinfo = graph.getRrdGraphInfo();
        String[] lines = graphinfo.getPrintLines();
        Assert.assertEquals("maxSun = 4.285k", lines[0]);
        Assert.assertEquals("maxSun time = 1277467200", lines[1]);
        Assert.assertEquals("avgSun = 3.000k", lines[2]);
        Assert.assertEquals("maxShade = 0.878k", lines[3]);
        Assert.assertEquals("avgShade = 0.404k", lines[4]);
        Assert.assertEquals(412, graphinfo.getHeight());
        Assert.assertEquals(591, graphinfo.getWidth());
        Assert.assertTrue(graphinfo.getFilename().endsWith(".png"));

        Assert.assertEquals(1277467200, sunmax.getValue().timestamp);
        Assert.assertEquals(4284.9218056, sunmax.getValue().value, 1e-15);
    }

    static class GaugeSource {
        private double value;
        private double step;

        GaugeSource(double value, double step) {
            this.value = value;
            this.step = step;
        }

        long getValue() {
            double oldValue = value;
            double increment = RANDOM.nextDouble() * step;
            if (RANDOM.nextDouble() > 0.5) {
                increment *= -1;
            }
            value += increment;
            if (value <= 0) {
                value = 0;
            }
            return Math.round(oldValue);
        }
    }

}
