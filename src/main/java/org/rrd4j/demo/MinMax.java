package org.rrd4j.demo;

import static org.rrd4j.ConsolFun.*;
import org.rrd4j.core.*;
import org.rrd4j.graph.*;
import org.rrd4j.DsType;

import java.io.*;
import java.awt.*;

class MinMax {
    public static void main(String[] args) throws IOException {
        long start = Util.getTime(), end = start + 300 * 300;
        String rrdFile = Util.getRrd4jDemoPath("minmax.rrd");
        String pngFile = Util.getRrd4jDemoPath("minmax.png");
        // create
        RrdDef rrdDef = new RrdDef(rrdFile, start - 1, 300);
        rrdDef.addDatasource("a", DsType.GAUGE, 600, Double.NaN, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 300);
        rrdDef.addArchive(MIN, 0.5, 12, 300);
        rrdDef.addArchive(MAX, 0.5, 12, 300);
        RrdDb rrdDb = new RrdDb(rrdDef);
        // update
        for (long t = start; t < end; t += 300) {
            Sample sample = rrdDb.createSample(t);
            sample.setValue("a", Math.sin(t / 3000.0) * 50 + 50);
            sample.update();
        }
        // graph
        RrdGraphDef gDef = new RrdGraphDef();
        gDef.setFilename(pngFile);
        gDef.setWidth(450);
        gDef.setHeight(250);
        gDef.setImageFormat("png");
        gDef.setTimeSpan(start, start + 86400);
        gDef.setTitle("RRDTool's MINMAX.pl demo");
        gDef.datasource("a", rrdFile, "a", AVERAGE);
        gDef.datasource("b", rrdFile, "a", MIN);
        gDef.datasource("c", rrdFile, "a", MAX);
        gDef.area("a", Color.decode("0xb6e4"), "real");
        gDef.line("b", Color.decode("0x22e9"), "min");
        gDef.line("c", Color.decode("0xee22"), "max");
        new RrdGraph(gDef);
    }
}