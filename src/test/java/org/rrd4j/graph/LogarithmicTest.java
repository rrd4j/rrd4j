package org.rrd4j.graph;

import java.awt.Color;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.data.Plottable;

public class LogarithmicTest {
    @BeforeClass
    public static void prepare() {
        System.getProperties().setProperty("java.awt.headless","true");
    }

    @Test
    public void test1() throws IOException {
        RrdGraphDef def = new RrdGraphDef();
        def.setStartTime(1);
        def.setEndTime(100);
        def.datasource("base1", new Plottable() {
            @Override
            public double getValue(long timestamp) {
                return Math.signum(2 * Math.PI * timestamp / 100);
            }});
        def.line("base1", Color.BLUE);
        def.setLogarithmic(true);
        new RrdGraph(def);
    }
}
