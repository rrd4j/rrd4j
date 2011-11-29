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
    public void test_3_l_64_8() throws IOException {
        testFile("/rrdtool/0003l648.rrd", "0003");
    }

    //version 0003
    //little endian
    //32 bits
    //alignement on long
    @Test
    public void test_3_l_32_8() throws IOException {
        testFile("/rrdtool/0003l328.rrd", "0003");
    }

    //version 0003
    //little endian
    //32 bits
    //alignement on int
    @Test
    public void test_3_l_32_4() throws IOException {
        testFile("/rrdtool/0003l324.rrd", "0003");
    }

    //version 0003
    //big endian
    //32 bits
    //alignement on long
    @Test
    public void test_3_b_32_8() throws IOException {
        testFile("/rrdtool/0003b328.rrd", "0003");
    }

    //version 0001
    //big endian
    //64 bits
    //alignements on long
    @Test
    public void test_1_b_64_8() throws IOException {
        testFile("/rrdtool/0001b648.rrd", "0001");
    }

    //version 0001
    //big endian
    //32 bits
    //alignement on long
    @Test
    public void test_1_b_32_8() throws IOException {
        testFile("/rrdtool/0001b328.rrd", "0001");
    }

    private void testFile(String file, String version) throws IOException {
        System.out.println("");
        System.out.println("***************");
        System.out.println(file);
        URL url = getClass().getResource(file); 
        RRDatabase rrd = new RRDatabase(url.getFile());
        System.out.println(rrd.rrdFile.ras.length() - rrd.rrdFile.getFilePointer());
        System.out.println(rrd);
        Assert.assertEquals("Invalid date", new Date(920808900000L), rrd.getLastUpdate());
        Assert.assertEquals("Invalid number of archives", 2, rrd.getNumArchives());
        Assert.assertEquals("Invalid number of archives", 2, rrd.getDataSourcesName().size());
        Assert.assertEquals("Invalid version", version, rrd.header.getVersion());
        Assert.assertEquals("Invalid number of row", 24, rrd.getArchive(0).getRowCount());
        Assert.assertEquals("Invalid number of row", 10, rrd.getArchive(1).getRowCount());
        boolean b0 = "12405".equals(rrd.getDataSource(0).getPDPStatusBlock().lastReading);
        boolean b1 = "UNKN".equals(rrd.getDataSource(1).getPDPStatusBlock().lastReading);
        boolean b2 = "3".equals(rrd.getDataSource(1).getPDPStatusBlock().lastReading);
        Assert.assertTrue("Failed getting last reading", b0 && (b1 || b2));
        if("0003".equals(version) ) {
            Assert.assertEquals("bad primary value", 1.43161853E7, rrd.getArchive(0).getCDPStatusBlock(0).primary_value, 1);
        }
        DataChunk data = rrd.getData(ConsolidationFunctionType.AVERAGE, 920802300, 920808900, 300);
        System.out.println(data.toPlottable("speed").getValue(920802300));
        //Assert.assertEquals(0.02, data.toPlottable("speed").getValue(920802300), 1e-7);
        Assert.assertEquals(1.0, data.toPlottable("weight").getValue(920802300), 1e-7);
    }

}
