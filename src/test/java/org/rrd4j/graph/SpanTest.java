package org.rrd4j.graph;

import java.awt.Color;
import java.io.IOException;


import org.junit.BeforeClass;
import org.junit.Test;

public class SpanTest {

    @BeforeClass
    public static void prepare() {
        System.getProperties().setProperty("java.awt.headless","true");
    }

    @Test
    public void test1() throws IOException {
        RrdGraphDef def = new RrdGraphDef();
        def.setStartTime(1);
        def.setEndTime(100);
        def.hspan(0, 1, Color.BLUE, "span test");
        def.vspan(0, 1, Color.BLUE, "span test");
        def.setLazy(false);
        new RrdGraph(def);
    }
}
