package org.rrd4j;

import static org.rrd4j.ConsolFun.AVERAGE;

import java.awt.Color;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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

public class TestLSL {
    static final long START = getTimestamp(2010, 3, 1, 0, 0);
    static final long LASTWEEK = getTimestamp(2010, 3, 25, 23, 59);
    static final long END = getTimestamp(2010, 3, 30, 23, 59);
    static final long SEED = 1909752002L;
    static final Random RANDOM = new Random(SEED);
    static final int MAX_STEP = 300;

    static private final class GaugeSource {
        private double value;
        private double step;

        GaugeSource(double value, double step) {
            this.value = value;
            this.step = step;
        }

        long getValue() {
            double oldValue = value;
            double increment = RANDOM.nextDouble() * step;
            if (RANDOM.nextDouble() > 0.50) {
                increment *= -1;
            }
            value += increment;
            if (value <= 0) {
                value = 0;
            }
            return Math.round(oldValue);
        }
    }

    private static long getTimestamp(int year, int month, int day, int hour, int min){
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("CET"), Locale.US);
        c.set(year, month, day, hour, min);
        return Util.getTimestamp(c);
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void test1() throws IOException {
        String rrdpath = testFolder.newFile("trend.rrd").getCanonicalPath();
        RrdDef rrdDef = new RrdDef(rrdpath, START - 1, 300);

        rrdDef.addDatasource("ns-dskPercent", DsType.GAUGE, 300, 0, 100);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        rrdDef.addArchive(AVERAGE, 0.5, 6, 700);
        rrdDef.addArchive(AVERAGE, 0.5, 24, 775);
        RrdDb rrdDb = new RrdDb(rrdDef);

        long t = START;
        Sample sample = rrdDb.createSample();
        GaugeSource dskPercent = new GaugeSource(0, 10);

        for(t = START ; t <= END; t += RANDOM.nextInt(MAX_STEP) + 1) {
            sample.setTime(t);
            double value = Math.min((dskPercent.getValue() % 50) + (1 - Math.cos(( Math.PI) /  2 * (t - START ) / (END - START))) * 80, 100);
            sample.setValue("ns-dskPercent",  (int) value);
            sample.update();
        }

        FetchRequest week = rrdDb.createFetchRequest(ConsolFun.AVERAGE, LASTWEEK, END);
        FetchRequest month = rrdDb.createFetchRequest(ConsolFun.AVERAGE, START, END);

        RrdGraphDef gdef = new RrdGraphDef();
        gdef.setLocale(Locale.US);
        gdef.setTimeZone(TimeZone.getTimeZone("CET"));
        gdef.setFilename(testFolder.newFile("trend.png").getCanonicalPath());
        gdef.setStartTime(LASTWEEK);
        gdef.setEndTime(END);
        gdef.setImageFormat("png");
        gdef.setTitle("Disk Usage Prediction: /");                              //--title="Disk Usage Prediction: {ns-dskPath}"
        gdef.setWidth(620);                                                     //--width 620
        gdef.setHeight(200);                                                    //--height 200
        gdef.setInterlaced(true);                                               //--interlace
        gdef.setVerticalLabel("Disk used (%)");                                 //--vertical-label="Disk used (%)"
        gdef.setMinValue(0);                                                    //--lower-limit=0
        gdef.setMaxValue(100);                                                  //--upper-limit=100
        gdef.setRigid(true);                                                    //--rigid
        gdef.datasource("pused1", rrdDb.getCanonicalPath(), "ns-dskPercent", ConsolFun.AVERAGE);   //DEF:pused1={rrd1}:ns-dskPercent:AVERAGE
        gdef.datasource("pused2", "ns-dskPercent", week.fetchData());           //DEF:pused2={rrd1}:ns-dskPercent:AVERAGE:start=-1w
        gdef.datasource("pused3", "ns-dskPercent", month.fetchData());          //DEF:pused3={rrd1}:ns-dskPercent:AVERAGE:start=-1m
        gdef.datasource("D2", "pused2", new Variable.LSLSLOPE());               //VDEF:D2=pused2,LSLSLOPE
        gdef.datasource("H2", "pused2", new Variable.LSLINT());                 //VDEF:H2=pused2,LSLINT
        gdef.datasource("avg2", "pused2,POP,D2,COUNT,*,H2,+");                  //CDEF:avg2=pused2,POP,D2,COUNT,*,H2,+
        gdef.datasource("abc2", "avg2,90,100,LIMIT");                           //CDEF:abc2=avg2,90,100,LIMIT
        gdef.line(90.0, RrdGraphConstants.BLIND_COLOR, 1.0f, false);            //LINE1:90
        gdef.area(5.0, new Color(0xff,0,0,0x22), true);                         //AREA:5#FF000022::STACK
        gdef.area(5.0, new Color(0xff,0,0,0x44), true);                         //AREA:5#FF000044::STACK
        gdef.comment("                       Now          Min              Avg             Max\\l");    //COMMENT:"                       Now          Min              Avg             Max\\n"
        gdef.area("pused1", new Color(0,0x88,0,0x77), "Disk Used", false);      //AREA:pused1#00880077:"Disk Used"
        gdef.datasource("pused1last", "pused1", new Variable.LAST()); gdef.gprint("pused1last", "%12.0lf%%");           //GPRINT:pused1:LAST:"%12.0lf%%"
        gdef.datasource("pused1min", "pused1", new Variable.MIN()); gdef.gprint("pused1min", "%12.0lf%%");              //GPRINT:pused1:MIN:"%10.0lf%%"
        gdef.datasource("pused1average", "pused1", new Variable.AVERAGE()); gdef.gprint("pused1average", "%12.0lf%%");  //GPRINT:pused1:AVERAGE:"%13.0lf%%"
        gdef.datasource("pused1max", "pused1", new Variable.MAX()); gdef.gprint("pused1max", "%12.0lf%%\\l");           //GPRINT:pused1:MAX:"%13.0lf%%\\n"
        gdef.comment(" \\l");                                                   //COMMENT:" \\n"
        gdef.datasource("D3", "pused3", new Variable.LSLSLOPE());               //VDEF:D3=pused3,LSLSLOPE
        gdef.datasource("H3", "pused3", new Variable.LSLINT());                 //VDEF:H3=pused3,LSLINT
        gdef.datasource("avg3", "pused3,POP,D3,COUNT,*,H3,+");                  //CDEF:avg3=pused3,POP,D3,COUNT,*,H3,+
        gdef.datasource("abc3", "avg3,90,100,LIMIT");                           //CDEF:abc3=avg3,90,100,LIMIT
        gdef.area("abc2", new Color(0xff, 0xBB, 0, 0x77), null, false);         //AREA:abc2#FFBB0077
        gdef.area("abc3", new Color(0, 0x77, 0xff, 0x77), null, false);         //AREA:abc3#0077FF77
        gdef.line("abc2", new Color(0xff, 0xbb, 00), null, 2.0f, false);        //LINE2:abc2#FFBB00
        gdef.line("abc3", new Color(0x00, 0x77, 0xff), null, 2.0f, false);      //LINE2:abc3#0077FF
        gdef.line("avg2", new Color(0xff, 0xbb, 00), "Trend since 1 week                           ", 2.0f, false);    //LINE2:avg2#FFBB00:"Trend since 1 week                           :dashes=10"
        gdef.line("avg3", new Color(0x00, 0x77, 0xff), "Trend since 1 month\\l", 2.0f, false);  //LINE2:avg3#0077FF:"Trend since 1 month\\n:dashes=10"
        gdef.datasource("minabc2", "abc2", new Variable.FIRST());               //VDEF:minabc2=abc2,FIRST
        gdef.datasource("maxabc2", "abc2", new Variable.LAST());                //VDEF:maxabc2=abc2,LAST
        gdef.datasource("minabc3", "abc3", new Variable.FIRST());               //VDEF:minabc3=abc3,FIRST
        gdef.datasource("maxabc3", "abc3", new Variable.LAST());                //VDEF:maxabc3=abc3,LAST

        gdef.gprint("minabc2", "  Reach   90%% at %tc ", true);                 //GPRINT:minabc2:"  Reach  90% at %c ":strftime
        gdef.gprint("minabc3", "  Reach   90%% at %tc\\l", true);               //GPRINT:minabc3:"  Reach  90% at %c "\\n:strftime
        gdef.gprint("maxabc2", "  Reach  100%% at %tc ", true);                 //GPRINT:maxabc2:"  Reach 100% at %c ":strftime
        gdef.gprint("maxabc3", "  Reach  100%% at %tc\\l", true);               //GPRINT:maxabc3:"  Reach 100% at %c "\\n:strftime

        gdef.print("minabc2", "  Reach   90%% at %tc ", true);                 //GPRINT:minabc2:"  Reach  90% at %c ":strftime
        gdef.print("minabc3", "  Reach   90%% at %tc\\l", true);               //GPRINT:minabc3:"  Reach  90% at %c "\\n:strftime
        gdef.print("maxabc2", "  Reach  100%% at %tc ", true);                 //GPRINT:maxabc2:"  Reach 100% at %c ":strftime
        gdef.print("maxabc3", "  Reach  100%% at %tc\\l", true);               //GPRINT:maxabc3:"  Reach 100% at %c "\\n:strftime

        RrdGraph graph = new RrdGraph(gdef);
        RrdGraphInfo graphinfo = graph.getRrdGraphInfo();
        System.out.println(graphinfo.dump());
        String[] lines = graphinfo.getPrintLines();
        Assert.assertEquals("  Reach   90% at Wed Apr 28 10:30:00 CEST 2010 ", lines[0]);
        Assert.assertEquals("  Reach   90% at Wed Apr 28 11:00:00 CEST 2010", lines[1]);
        Assert.assertEquals("  Reach  100% at Sat May 01 00:00:00 CEST 2010 ", lines[2]);
        Assert.assertEquals("  Reach  100% at Sat May 01 00:00:00 CEST 2010", lines[3]);

    }
}















