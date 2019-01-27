package org.rrd4j.core.jrrd;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdNioBackendFactory;

public class ExportImport {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testExport() throws Exception {
        // create xml file and write into it all rrd data
        File rrdXmlFile = testFolder.newFile("testexport.xml");

        URL url = getClass().getResource("/rrdtool/0003l648.rrd"); 

        RRDatabase rrd = new RRDatabase(url.getFile());
        rrd.toXml(new PrintStream(rrdXmlFile));
        rrd.close();

        String imported = testFolder.getRoot().getAbsolutePath() + "/testexport.rrd";
        // create rrd4j database from the xml file created previously
        RrdDb.getBuilder().setPath(imported).setExternalPath(rrdXmlFile.getCanonicalPath()).doimport();
        
        try (RrdDb db = RrdDb.getBuilder().setPath(imported).setReadOnly().setBackendFactory(new RrdNioBackendFactory()).build()) {
            Assert.assertNotNull(db.getDatasource("speed"));
            Assert.assertNotNull(db.getDatasource("weight"));
        }
    }

    @Test
    public void testImport() throws Exception {
        URL url = getClass().getResource("/rrdtool/0003l648.rrd"); 

        String imported = testFolder.getRoot().getAbsolutePath() + "/testexport.rrd";
        // create rrd4j database from the xml file created previously
        RrdDb.getBuilder().setPath(imported).setRrdToolImporter(url.getFile()).doimport();

        try (RrdDb db = RrdDb.getBuilder().setPath(imported).setReadOnly().setBackendFactory(new RrdNioBackendFactory()).build()) {
            Assert.assertNotNull(db.getDatasource("speed"));
            Assert.assertNotNull(db.getDatasource("weight"));
        }
    }

}
