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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdRandomAccessFileBackendFactory;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.data.Variable;
import org.rrd4j.graph.ElementsNames;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;
import org.rrd4j.graph.RrdGraphInfo;
import org.rrd4j.graph.TimeLabelFormat;

import eu.bengreen.data.utility.LargestTriangleThreeBuckets;

public class TestDemo {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    static private RrdBackendFactory previousBackend;

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

    @BeforeClass
    public static void setBackendBefore() {
        previousBackend = RrdBackendFactory.getDefaultFactory();
        RrdBackendFactory.setActiveFactories(new RrdRandomAccessFileBackendFactory());
    }

    @AfterClass
    public static void setBackendAfter() {
        RrdBackendFactory.setActiveFactories(previousBackend);
    }

    @Test
    public void main() throws IOException {
        long start = START;
        long end = END;

        String rrdPath = testFolder.getRoot().getCanonicalPath() + "FILE" + ".rrd";
        String xmlPath = testFolder.getRoot().getCanonicalPath() + "FILE" + ".xml";
        String rrdRestoredPath = testFolder.getRoot().getCanonicalPath() + "FILE" + "_restored.rrd";
        String imgPath = testFolder.getRoot().getCanonicalPath() + "FILE" + ".png";
        String logPath = testFolder.getRoot().getCanonicalPath() + "FILE" + ".log";
        PrintWriter log = new PrintWriter(new BufferedOutputStream(new FileOutputStream(logPath, false)));
        // creation

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

        log.println(rrdDef.dump());

        RrdDb rrdDb = new RrdDb(rrdDef);

        Assert.assertTrue(rrdDb.getRrdDef().equals(rrdDef));
        rrdDb.close();


        // update database
        GaugeSource sunSource = new GaugeSource(1200, 20);
        GaugeSource shadeSource = new GaugeSource(300, 10);

        long t = start;
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

        // test read-only access!
        rrdDb = new RrdDb(rrdPath, true);
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("CET"), Locale.US);
        c.setTimeInMillis(rrdDb.getLastUpdateTime() * 1000);

        // fetch data
        FetchRequest request = rrdDb.createFetchRequest(AVERAGE, start, end);
        log.println(request.dump());

        // dump to XML file
        rrdDb.exportXml(xmlPath);
        RrdDb rrdRestoredDb = new RrdDb(rrdRestoredPath, xmlPath);

        // close files
        rrdDb.close();
        rrdRestoredDb.close();

        // create graph
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
        gDef.setColor(ElementsNames.xaxis, Color.BLUE);
        gDef.setColor(ElementsNames.yaxis, new Color(0, 255, 0, 40));
        gDef.setTimeLabelFormat(new CustomTimeLabelFormat());
        gDef.setDownsampler(new LargestTriangleThreeBuckets(IMG_WIDTH));

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
        // create graph finally
        RrdGraph graph = new RrdGraph(gDef);
        // demo ends
        log.close();

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

    static class CustomTimeLabelFormat implements TimeLabelFormat {
        public String format(Calendar c, Locale locale) {
            if (c.get(Calendar.MILLISECOND) != 0) {
                return String.format(locale, "%1$tH:%1$tM:%1$tS.%1$tL", c);
            } else if (c.get(Calendar.SECOND) != 0) {
                return String.format(locale, "%1$tH:%1$tM:%1$tS", c);
            } else if (c.get(Calendar.MINUTE) != 0) {
                return String.format(locale, "%1$tH:%1$tM", c);
            } else if (c.get(Calendar.HOUR_OF_DAY) != 0) {
                return String.format(locale, "%1$tH:%1$tM", c);
            } else if (c.get(Calendar.DAY_OF_MONTH) != 1) {
                return String.format(locale, "%1$td %1$tb", c);
            } else if (c.get(Calendar.DAY_OF_YEAR) != 1) {
                return String.format(locale, "%1$td %1$tb", c);
            } else {
                return String.format(locale, "%1$tY", c);
            }
        }
    }

}
