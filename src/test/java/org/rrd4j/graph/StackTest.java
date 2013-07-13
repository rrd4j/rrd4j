package org.rrd4j.graph;

import java.awt.Color;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.data.Plottable;

public class StackTest {

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
                return (timestamp % 2 == 0) ? Double.NaN : 1;
            }});
        def.datasource("base2", new Plottable() {
            @Override
            public double getValue(long timestamp) {
                return (timestamp % 2 == 0) ? 1: Double.NaN;
            }});
        def.datasource("base3", new Plottable() {
            @Override
            public double getValue(long timestamp) {
                return 1;
            }});
        def.line("base1", Color.BLUE);
        def.stack("base2", Color.RED);
        def.stack("base3", Color.RED);
        new RrdGraph(def);

        SourcedPlotElement base2 = (SourcedPlotElement) def.plotElements.get(1);
        SourcedPlotElement base3 = (SourcedPlotElement) def.plotElements.get(2);
        for(int i=1; i < 100; i++) {
            Assert.assertEquals("base2 value failed", 1.0, base2.getValues()[i], 0.0000001);
            Assert.assertEquals("base3 value failed", 2.0, base3.getValues()[i], 0.0000001);
        }
    }
}
