package org.rrd4j.converter;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;

public class ConverterTest {
    @Test
    public void test_3_l_64_8() throws IOException {
        testImport("/rrdtool/0003l648.rrd");
    }

    @Test
    public void test_3_l_32_8() throws IOException {
        testImport("/rrdtool/0003l328.rrd");
    }

    @Test
    public void test_3_l_32_4() throws IOException {
        testImport("/rrdtool/0003l324.rrd");
    }

    @Test
    public void test_3_b_32_8() throws IOException {
        testImport("/rrdtool/0003b328.rrd");
    }

    @Test
    public void test_1_b_64_8() throws IOException {
        testImport("/rrdtool/0001b648.rrd");
    }

    @Test
    public void test_1_b_32_8() throws IOException {
        //testImport("/rrdtool/0001b328.rrd");
    }

    @Test
    public void test_1_l_32_4() throws IOException {
        testImport("/rrdtool/0001l324.rrd");
    }

    @Test
    public void test_1_l_64_8() throws IOException {
        testImport("/rrdtool/0001l648.rrd");
    }

    private void testImport(String file) throws IOException {
        URL url = getClass().getResource(file); 
        RrdDb rrd = new RrdDb("test", "rrdtool:/" + url.getFile(), RrdBackendFactory.getFactory("MEMORY"));
        org.rrd4j.TestsUtils.testRrdDb(rrd);
    }
}
