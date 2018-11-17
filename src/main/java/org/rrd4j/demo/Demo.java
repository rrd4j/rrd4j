package org.rrd4j.demo;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.ConsolFun.MAX;
import static org.rrd4j.ConsolFun.TOTAL;
import static org.rrd4j.DsType.GAUGE;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import org.rrd4j.backends.RrdSafeFileBackend;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.data.Variable;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;
import org.rrd4j.graph.TimeLabelFormat;

/**
 * Simple demo just to check that everything is OK with this library. Creates two files in your
 * $HOME/rrd4j-demo directory: demo.rrd and demo.png.
 */
public class Demo {
    static final long SEED = 1909752002L;
    static final Random RANDOM = new Random(SEED);
    static final String FILE = "demo";

    static final long START = Util.getTimestamp(2010, 4, 1);
    static final long END = Util.getTimestamp(2010, 6, 1);
    static final int MAX_STEP = 300;

    static final int IMG_WIDTH = 500;
    static final int IMG_HEIGHT = 300;
    private static final String SHADE = "shade";
    private static final String SUNMAX = "sunmax";
    private static final String SUNAVERAGE = "sunaverage";
    private static final String SHADEMAX = "shademax";
    private static final String SHADEVERAGE = "shadeverage";

    private Demo() {}

    /**
     * <p>To start the demo, use the following command:</p>
     * <pre>
     * java -cp rrd4j-{version}.jar org.rrd4j.demo.Demo
     * </pre>
     *
     * @param args the name of the backend factory to use (optional)
     * @throws java.io.IOException Thrown
     */
    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless","true");

        println("== Starting demo");
        long startMillis = System.currentTimeMillis();
        if (args.length > 0) {
            println("Setting default backend factory to " + args[0]);
            RrdDb.setDefaultFactory(args[0]);
        }
        long start = START;
        long end = END;
        String rrdPath = Util.getRrd4jDemoPath(FILE + ".rrd");
        String xmlPath = Util.getRrd4jDemoPath(FILE + ".xml");
        String rrdRestoredPath = Util.getRrd4jDemoPath(FILE + "_restored.rrd");
        String imgPath = Util.getRrd4jDemoPath(FILE + ".png");
        String logPath = Util.getRrd4jDemoPath(FILE + ".log");
        PrintWriter log = new PrintWriter(new BufferedOutputStream(new FileOutputStream(logPath, false)));
        // creation
        println("== Creating RRD file " + rrdPath);
        RrdDef rrdDef = new RrdDef(rrdPath, start - 1, 300);
        rrdDef.setVersion(2);
        rrdDef.addDatasource("sun", GAUGE, 600, 0, Double.NaN);
        rrdDef.addDatasource(SHADE, GAUGE, 600, 0, Double.NaN);
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
        println(rrdDef.dump());
        log.println(rrdDef.dump());
        println("Estimated file size: " + rrdDef.getEstimatedSize());
        try (RrdDb rrdDb = new RrdDb(rrdDef)){
            println("== RRD file created.");
            if (rrdDb.getRrdDef().equals(rrdDef)) {
                println("Checking RRD file structure... OK");
            } else {
                println("Invalid RRD file created. This is a serious bug, bailing out");
                log.close();
                return;
            }
        }
        println("== RRD file closed.");

        // update database
        GaugeSource sunSource = new GaugeSource(1200, 20);
        GaugeSource shadeSource = new GaugeSource(300, 10);
        println("== Simulating one month of RRD file updates with step not larger than " +
                MAX_STEP + " seconds (* denotes 1000 updates)");
        long t = start;
        int n = 0;
        try (RrdDb rrdDb = new RrdDb(rrdPath)){
            Sample sample = rrdDb.createSample();
            while (t <= end + 172800L) {
                sample.setTime(t);
                sample.setValue("sun", sunSource.getValue());
                sample.setValue(SHADE, shadeSource.getValue());
                log.println(sample.dump());
                sample.update();
                t += RANDOM.nextDouble() * MAX_STEP + 1;
                if (((++n) % 1000) == 0) {
                    System.out.print("*");
                }
            } 
        }

        println("");
        println("== Finished. RRD file updated " + n + " times");

        // test read-only access!
        RrdDb rrdDb = new RrdDb(rrdPath, true);
        println("File reopen in read-only mode");
        println("== Last update time was: " + rrdDb.getLastUpdateTime());
        println("== Last info was: " + rrdDb.getInfo());

        // fetch data
        println("== Fetching data for the whole month");
        FetchRequest request = rrdDb.createFetchRequest(AVERAGE, start, end);
        println(request.dump());
        log.println(request.dump());
        FetchData fetchData = request.fetchData();
        println("== Data fetched. " + fetchData.getRowCount() + " points obtained");
        println(fetchData.toString());
        println("== Dumping fetched data to XML format");
        println(fetchData.exportXml());
        println("== Fetch completed");

        // dump to XML file
        println("== Dumping RRD file to XML file " + xmlPath + " (can be restored with RRDTool)");
        rrdDb.exportXml(xmlPath);
        println("== Creating RRD file " + rrdRestoredPath + " from XML file " + xmlPath);
        RrdDb rrdRestoredDb = new RrdDb(rrdRestoredPath, xmlPath);

        // close files
        println("== Closing both RRD files");
        rrdDb.close();
        println("== First file closed");
        rrdRestoredDb.close();
        println("== Second file closed");

        // create graph
        println("Creating graph " + Util.getLapTime());
        println("== Creating graph from the second file");
        RrdGraphDef gDef = new RrdGraphDef();
        gDef.setTimeLabelFormat(new CustomTimeLabelFormat());
        gDef.setLocale(Locale.US);
        gDef.setWidth(IMG_WIDTH);
        gDef.setHeight(IMG_HEIGHT);

        // To use rrdtool font set or not
        //gDef.setFontSet(true);

        gDef.setFilename(imgPath);
        gDef.setStartTime(start);
        gDef.setEndTime(end);
        gDef.setTitle("Temperatures in May-June 2010");
        gDef.setVerticalLabel("temperature");

        gDef.datasource("sun", rrdRestoredPath, "sun", AVERAGE);
        gDef.datasource(SHADE, rrdRestoredPath, SHADE, AVERAGE);
        gDef.datasource("median", "sun,shade,+,2,/");
        gDef.datasource("diff", "sun,shade,-,ABS,-1,*");
        gDef.datasource("sine", "TIME," + start + ",-," + (end - start) + ",/,2,PI,*,*,SIN,1000,*");

        gDef.line("sun", Color.GREEN, "sun temp");
        gDef.line(SHADE, Color.BLUE, "shade temp");
        gDef.line("median", Color.MAGENTA, "median value");
        gDef.area("diff", Color.YELLOW, "difference");
        gDef.line("diff", Color.RED, null);
        gDef.line("sine", Color.CYAN, "sine fun");
        gDef.hrule(2568, Color.GREEN, "hrule");
        gDef.vrule((start + 2 * end) / 3, Color.MAGENTA, "vrule\\c");

        gDef.comment("\\r");

        Variable sunmax = new Variable.MAX();
        Variable sunaverage = new Variable.AVERAGE();
        gDef.datasource(SUNMAX, "sun", sunmax);
        gDef.datasource(SUNAVERAGE, "sun", sunaverage);
        gDef.gprint(SUNMAX, "maxSun = %.3f%s");
        gDef.gprint(SUNAVERAGE, "avgSun = %.3f%S\\c");
        gDef.print(SUNMAX, "maxSun = %.3f%s");
        gDef.print(SUNMAX, "maxSun time = %ts", true);
        gDef.print(SUNAVERAGE, "avgSun = %.3f%S\\c");

        gDef.datasource(SHADEMAX, SHADE, new Variable.MAX());
        gDef.datasource(SHADEVERAGE, SHADE, new Variable.AVERAGE());
        gDef.gprint(SHADEMAX, "maxShade = %.3f%S");
        gDef.gprint(SHADEVERAGE, "avgShade = %.3f%S\\c");
        gDef.print(SHADEMAX, "maxShade = %.3f%S");
        gDef.print(SHADEVERAGE, "avgShade = %.3f%S\\c");

        gDef.setImageInfo("<img src='%s' width='%d' height = '%d'>");
        gDef.setPoolUsed(false);
        gDef.setImageFormat("png");
        gDef.setDownsampler(new eu.bengreen.data.utility.LargestTriangleThreeBuckets((int)(IMG_WIDTH * 1)));
        println("Rendering graph " + Util.getLapTime());
        // create graph finally
        RrdGraph graph = new RrdGraph(gDef);
        println(graph.getRrdGraphInfo().dump());
        println("== Graph created " + Util.getLapTime());
        // locks info
        println("== Locks info ==");
        println(RrdSafeFileBackend.getLockInfo());
        // demo ends
        log.close();
        println("== Demo completed in " +
                ((System.currentTimeMillis() - startMillis) / 1000.0) + " sec");
    }

    static void println(String msg) {
        //System.out.println(msg + " " + Util.getLapTime());
        System.out.println(msg);
    }

    static void print(String msg) {
        System.out.print(msg);
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

