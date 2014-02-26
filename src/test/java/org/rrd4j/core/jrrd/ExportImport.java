package org.rrd4j.core.jrrd;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.core.RrdDb;

public class ExportImport {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testExport() throws Exception {
        // create xml file and write into it all rrd data
        //File rrdXmlFile = testFolder.newFile("testexport.xml");
        File rrdXmlFile = new File("/tmp/testexport.xml");

        URL url = getClass().getResource("/rrdtool/0003l648.rrd"); 

        RRDatabase rrd = new RRDatabase(url.getFile());
        rrd.toXml(new PrintStream(rrdXmlFile));
        rrd.close();
        
        // create rrd4j database from the xml file created previously
        RrdDb rrdDb = new RrdDb(testFolder.newFile("testexport.rrd").getCanonicalPath(), rrdXmlFile.getCanonicalPath());
        rrdDb.close();
        
    }

}
