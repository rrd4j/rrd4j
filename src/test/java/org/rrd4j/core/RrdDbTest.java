package org.rrd4j.core;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

public class RrdDbTest {
    @Test
    public void testXml1Import() throws IOException {
        URL url = getClass().getResource("/rrdtool/rrdtool1.xml"); 
        RrdDb rrd = new RrdDb("test", "xml:/" + url.getFile(), RrdBackendFactory.getFactory("MEMORY"));
        org.rrd4j.TestsUtils.testRrdDb(rrd);
    }
    @Test
    public void testXml3Import() throws IOException {
        URL url = getClass().getResource("/rrdtool/rrdtool3.xml"); 
        RrdDb rrd = new RrdDb("test", "xml:/" + url.getFile(), RrdBackendFactory.getFactory("MEMORY"));
        org.rrd4j.TestsUtils.testRrdDb(rrd);
    }
}
