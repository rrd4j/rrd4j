package org.rrd4j.graph;

import java.awt.Color;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.GraphTester;

public class SpanTest extends GraphTester {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void test1() throws IOException {
        RrdGraphDef def = new RrdGraphDef(1, 100);
        def.hspan(0, 1, Color.BLUE, "span test");
        def.vspan(0, 1, Color.BLUE, "span test");
        def.setLazy(false);
        saveGraph(def, testFolder, "SpanTest", "test1", "png");
        new RrdGraph(def);
    }
}
