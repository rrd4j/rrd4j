package org.rrd4j.demo;

import static org.rrd4j.ConsolFun.AVERAGE;

import java.awt.Color;
import java.io.IOException;

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
 * This class implements the example from the RRD tutorial using RRD4j:
 * http://oss.oetiker.ch/rrdtool/tut/rrdtutorial.en.html
 * 
 * This source file may be freely redistributed under the same terms as RRD4j.
 * 
 * @author Chris Lott
 */
public class RrdTutorial {

    // Arbitrary start point: 7th of March, 1999 at noon in MET,
    // expressed in seconds since the unix Epoch.
    // I'm in a different timezone so this isn't right:
    // Util.getTimestamp(1999, 3, 7) + 12 * 60 * 60;
    static final long START = 920804400;
    // End is 80 minutes later
    static final long END = START + 80 * 60;
    // For graphs
    static final String FILE = "speed";
    static final int IMG_WIDTH = 500;
    static final int IMG_HEIGHT = 300;
    static final String FILE_FORMAT = "png";

    /**
     * <p>
     * To run the tutorial code, use the following command:
     * </p>
     * 
     * <pre>
     * java -cp rrd4j-{version}.jar org.rrd4j.demo.RrdTutorial
     * </pre>
     * 
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        println("== Starting tutorial code");
        final String rrdPath = Util.getRrd4jDemoPath(FILE + ".rrd");
        final String speedSource = "speed";

        // rrdtool create test.rrd \
        // --start 920804400 \
        // DS:speed:COUNTER:600:U:U \
        // RRA:AVERAGE:0.5:1:24 \
        // RRA:AVERAGE:0.5:6:10
        println("== Creating RRD file " + rrdPath + " with initial time "
                + START);
        // Expect new data every 300 seconds
        RrdDef rrdDef = new RrdDef(rrdPath, START, 300);
        rrdDef.setVersion(2);
        // A counter for km: heartbeat is 600 sec; min val 0; no max val.
        // Heartbeat: after no update for the interval, declare value UNKNOWN
        rrdDef.addDatasource(speedSource, DsType.COUNTER, 600, 0, Double.NaN);
        // Add archive using fn AVERAGE, known:unkown ratio of 0.5,
        // use 1 data point, keep 24 rows. I.e., this archive keeps the latest
        // value, it's not actually an average.
        rrdDef.addArchive(AVERAGE, 0.5, 1, 24);
        // Add archive using fn AVERAGE, known:unkown ratio of 0.5,
        // use 6 data points, keep 10 rows. I.e., this keeps an average over
        // a 30 minute interval, and 10 * 30 = 300 minutes (5hrs) are kept.
        rrdDef.addArchive(AVERAGE, 0.5, 6, 10);

        // Create and check the database
        println(rrdDef.dump());
        println("Estimated file size: " + rrdDef.getEstimatedSize());
        RrdDb rrdDb = new RrdDb(rrdDef);
        println("== RRD file created.");
        if (rrdDb.getRrdDef().equals(rrdDef)) {
            println("Checking RRD file structure... OK");
        } else {
            println("Invalid RRD file created. This is a serious bug, bailing out");
            return;
        }
        rrdDb.close();

        // Reopen and get ready to update
        rrdDb = new RrdDb(rrdPath);
        Sample sample = rrdDb.createSample();

        // Add these data points:
        // 12:05 12345 km
        // 12:10 12357 km
        // 12:15 12363 km
        // 12:20 12363 km
        // 12:25 12363 km
        // 12:30 12373 km
        // 12:35 12383 km
        // 12:40 12393 km
        // 12:45 12399 km
        // 12:50 12405 km
        // 12:55 12411 km
        // 13:00 12415 km
        // 13:05 12420 km
        // 13:10 12422 km
        // 13:15 12423 km
        final int[] odoData = { 12345, 12357, 12363, 12363, 12363, 12373,
                12383, 12393, 12399, 12405, 12411, 12415, 12420, 12422, 12423 };
        println("== Adding odometer data");
        final int fiveMinutes = 5 * 60;
        for (int i = 0; i < odoData.length; ++i) {
            // First sample is 12:05, so use i + 1
            sample.setTime(START + (i + 1) * fiveMinutes);
            sample.setValue(speedSource, odoData[i]);
            // Store the value
            sample.update();
        }
        rrdDb.close();
        println("== Finished. RRD file updated " + odoData.length + " times");

        // test read-only access!
        rrdDb = new RrdDb(rrdPath, true);
        println("File reopen in read-only mode");
        println("== Last update time was: " + rrdDb.getLastUpdateTime());
        println("== Last info was: " + rrdDb.getInfo());

        // rrdtool fetch test.rrd AVERAGE --start 920804400 --end 920809200
        // Fetch data at finest resolution (300 sec), which is the default.
        println("== Fetch request for the interval at default resolution:");
        FetchRequest request = rrdDb.createFetchRequest(AVERAGE, START, END);
        println(request.dump());
        println("== Fetching data at default resolution");
        FetchData fetchData = request.fetchData();
        println("== Data fetched, " + fetchData.getRowCount()
                + " points obtained");
        // This output is different from the tutorial output from rrdtool;
        // it includes 920804400:nan but *not* 920809500:nan
        println(fetchData.toString());

        long coarseRes = 1800;
        println("== Fetch request for the interval at coarse resolution:");
        // START is before the first data point; END is after the last data
        // point; ensure start and end are even multiples of the resolution
        FetchRequest request2 = rrdDb.createFetchRequest(AVERAGE, START
                / coarseRes * coarseRes, END / coarseRes * coarseRes, 1800);
        println(request2.dump());
        println("== Fetching data at coarse resolution");
        FetchData fetchData2 = request2.fetchData();
        println("== Data fetched, " + fetchData2.getRowCount()
                + " points obtained");
        println(fetchData2.toString());

        // Done with direct access
        rrdDb.close();

        // Graph 1 has units in millis
        // rrdtool graph speed.png \
        // --start 920804400 --end 920808000 \
        // DEF:myspeed=test.rrd:speed:AVERAGE \
        // LINE2:myspeed#FF0000
        println("== Creating graph 1");
        RrdGraphDef gDef1 = new RrdGraphDef();
        gDef1.setWidth(IMG_WIDTH);
        gDef1.setHeight(IMG_HEIGHT);
        String img1Path = Util.getRrd4jDemoPath(FILE + "." + FILE_FORMAT);
        gDef1.setFilename(img1Path);
        gDef1.setStartTime(920804400);
        gDef1.setEndTime(920808000);
        gDef1.datasource("myspeed", rrdPath, "speed", AVERAGE);
        gDef1.line("myspeed", new Color(0xFF, 0x00, 0x00), "speed");
        gDef1.comment("Graph 1\\r");
        gDef1.setImageInfo("<img src='%s' width='%d' height = '%d'>");
        gDef1.setImageFormat(FILE_FORMAT);
        println("Rendering graph 1");
        RrdGraph graph1 = new RrdGraph(gDef1);
        println(graph1.getRrdGraphInfo().dump());
        println("== Graph 1 created");

        // Graph 2 adjusts the units etc.
        // rrdtool graph speed2.png \
        // --start 920804400 --end 920808000 \
        // --vertical-label m/s \
        // DEF:myspeed=test.rrd:speed:AVERAGE \
        // CDEF:realspeed=myspeed,1000,\* \
        // LINE2:realspeed#FF0000
        println("== Creating graph 2");
        RrdGraphDef gDef2 = new RrdGraphDef();
        gDef2.setWidth(IMG_WIDTH);
        gDef2.setHeight(IMG_HEIGHT);
        String img2Path = Util.getRrd4jDemoPath(FILE + "2" + "." + FILE_FORMAT);
        gDef2.setFilename(img2Path);
        gDef2.setStartTime(920804400);
        gDef2.setEndTime(920808000);
        gDef2.setVerticalLabel("m/s");
        gDef2.datasource("myspeed", rrdPath, "speed", AVERAGE);
        gDef2.datasource("realspeed", "myspeed,1000,*");
        gDef2.line("realspeed", new Color(0xFF, 0x00, 0x00), "realspeed");
        gDef2.comment("Graph 2\\r");
        gDef2.setImageInfo("<img src='%s' width='%d' height = '%d'>");
        gDef2.setImageFormat(FILE_FORMAT);
        println("Rendering graph 2");
        RrdGraph graph2 = new RrdGraph(gDef2);
        println(graph2.getRrdGraphInfo().dump());
        println("== Graph 2 created");

        // Graph 3 is fancier yet
        // rrdtool graph speed3.png \
        // --start 920804400 --end 920808000 \
        // --vertical-label km/h \
        // DEF:myspeed=test.rrd:speed:AVERAGE \
        // "CDEF:kmh=myspeed,3600,*" \
        // CDEF:fast=kmh,100,GT,kmh,0,IF \
        // CDEF:good=kmh,100,GT,0,kmh,IF \
        // HRULE:100#0000FF:"Maximum allowed" \
        // AREA:good#00FF00:"Good speed" \
        // AREA:fast#FF0000:"Too fast"
        println("== Creating graph 3");
        RrdGraphDef gDef3 = new RrdGraphDef();
        gDef3.setWidth(IMG_WIDTH);
        gDef3.setHeight(IMG_HEIGHT);
        String img3Path = Util.getRrd4jDemoPath(FILE + "3" + "." + FILE_FORMAT);
        gDef3.setFilename(img3Path);
        gDef3.setStartTime(920804400);
        gDef3.setEndTime(920808000);
        gDef3.setVerticalLabel("km/h");
        gDef3.datasource("myspeed", rrdPath, "speed", AVERAGE);
        gDef3.datasource("kmh", "myspeed,3600,*");
        gDef3.datasource("fast", "kmh,100,GT,kmh,0,IF");
        gDef3.datasource("good", "kmh,100,GT,0,kmh,IF");
        gDef3.hrule(100, new Color(0x00, 0x00, 0xFF), "Maximum allowed");
        gDef3.area("good", new Color(0x00, 0xFF, 0x00), "Good Speed");
        gDef3.area("fast", new Color(0xFF, 0x00, 0x00), "Too fast");
        gDef3.comment("Graph 3\\r");
        gDef3.setImageInfo("<img src='%s' width='%d' height = '%d'>");
        gDef3.setImageFormat(FILE_FORMAT);
        println("Rendering graph 3");
        RrdGraph graph3 = new RrdGraph(gDef3);
        println(graph3.getRrdGraphInfo().dump());
        println("== Graph 3 created");

        // Graph 4 is the fanciest
        // rrdtool graph speed4.png \
        // --start 920804400 --end 920808000 \
        // --vertical-label km/h \
        // DEF:myspeed=test.rrd:speed:AVERAGE \
        // CDEF:nonans=myspeed,UN,0,myspeed,IF \
        // CDEF:kmh=nonans,3600,* \
        // CDEF:fast=kmh,100,GT,100,0,IF \
        // CDEF:over=kmh,100,GT,kmh,100,-,0,IF \
        // CDEF:good=kmh,100,GT,0,kmh,IF \
        // HRULE:100#0000FF:"Maximum allowed" \
        // AREA:good#00FF00:"Good speed" \
        // AREA:fast#550000:"Too fast" \
        // STACK:over#FF0000:"Over speed"
        println("== Creating graph 4");
        RrdGraphDef gDef4 = new RrdGraphDef();
        gDef4.setWidth(IMG_WIDTH);
        gDef4.setHeight(IMG_HEIGHT);
        String img4Path = Util.getRrd4jDemoPath(FILE + "4" + "." + FILE_FORMAT);
        gDef4.setFilename(img4Path);
        gDef4.setStartTime(920804400);
        gDef4.setEndTime(920808000);
        gDef4.setVerticalLabel("km/h");
        gDef4.datasource("myspeed", rrdPath, "speed", AVERAGE);
        gDef4.datasource("nonans", "myspeed,UN,0,myspeed,IF");
        gDef4.datasource("kmh", "nonans,3600,*");
        gDef4.datasource("fast", "kmh,100,GT,kmh,0,IF");
        gDef4.datasource("over", "kmh,100,GT,kmh,100,-,0,IF");
        gDef4.datasource("good", "kmh,100,GT,0,kmh,IF");
        gDef4.hrule(100, new Color(0x00, 0x00, 0xFF), "Maximum allowed");
        gDef4.area("good", new Color(0x00, 0xFF, 0x00), "Good Speed");
        gDef4.area("fast", new Color(0x55, 00, 00), "Too fast");
        gDef4.stack("over", new Color(0xff, 0x00, 0x00), "Over speed");
        gDef4.comment("Graph 4\\r");
        gDef4.setImageInfo("<img src='%s' width='%d' height = '%d'>");
        gDef4.setImageFormat(FILE_FORMAT);
        println("Rendering graph 4");
        RrdGraph graph4 = new RrdGraph(gDef4);
        println(graph4.getRrdGraphInfo().dump());
        println("== Graph 4 created");
    }

    static void println(String msg) {
        System.out.println(msg);
    }

    static void print(String msg) {
        System.out.print(msg);
    }
}
