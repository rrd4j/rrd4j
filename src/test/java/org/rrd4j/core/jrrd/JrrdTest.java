package org.rrd4j.core.jrrd;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;

public class JrrdTest {

    //version 0003
    //little endian
    //64 bits
    //alignement on long
    @Test
    public void test1() throws IOException {
        testFile("/rrdtool/0003l648.rrd", "0003");
    }
    
    //version 0003
    //little endian
    //32 bits
    //alignement on long
    @Test
    public void test2() throws IOException {
        testFile("/rrdtool/0003l328.rrd", "0003");
    }
    
    //version 0003
    //little endian
    //32 bits
    //alignement on int
    @Test
    public void test3() throws IOException {
        testFile("/rrdtool/0003l324.rrd", "0003");
    }

    //version 0003
    //big endian
    //32 bits
    //alignement on long
    @Test
    public void test4() throws IOException {
        testFile("/rrdtool/0003b328.rrd", "0003");
    }

    //version 0001
    //big endian
    //64 bits
    //alignements on long
    @Test
    public void test5() throws IOException {
        testFile("/rrdtool/0001b648.rrd", "0001");
    }

    //version 0001
    //big endian
    //32 bits
    //alignement on long
    @Test
    public void test6() throws IOException {
        testFile("/rrdtool/0001b328.rrd", "0001");
    }

    private void testFile(String file, String version) throws IOException {
        URL url = getClass().getResource(file);      
        RRDatabase rrd = new RRDatabase(url.getFile());
        Assert.assertEquals("Invalid date", new Date(920808900000L), rrd.getLastUpdate());
        Assert.assertEquals("Invalid number of archives", 2, rrd.getNumArchives());
        Assert.assertEquals("Invalid version", version, rrd.header.getVersion());
    }
}
