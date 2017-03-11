package org.rrd4j.core;


import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.DsType.GAUGE;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class BigStringsTest {
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testBackend() throws IOException {
        RrdMemoryBackend backend = new RrdMemoryBackend("");
        char c = '\ue001';
        Assert.assertTrue(c >=  '\ue000' && c <= '\uf8ff');
        StringBuffer builder = new StringBuffer();
        backend.setLength(6400 * 6400 + 10);
        int pos = 0;
        String previous = null;
        for (int i = 0 ; i < 80 ; i++) {
            previous = builder.toString();
            builder.append(i % 10);
            backend.writeString(pos, builder.toString());
            Assert.assertEquals("Not read String", builder.toString(), backend.readString(pos));
            if (!previous.isEmpty()) {
                Assert.assertEquals("Not read String", previous, backend.readString(pos - RrdPrimitive.STRING_LENGTH * 2));
            }
            pos += RrdPrimitive.STRING_LENGTH * 2;
        }
    }
    
    @Test
    public void testBigRrd() throws IOException {
        String[] dsNames = new String[]{"012345678901234567890123456789", "01234567890123456789", "0123456789012345678901234567890123456789"};
        Set<String> dsNamesSet = new HashSet<>(dsNames.length);
        String rrdPath = testFolder.getRoot().getCanonicalPath() + "FILE.rrd";
        RrdDef rrdDef = new RrdDef(rrdPath, 0, 300);
        rrdDef.setVersion(2);
        for (String dsName: dsNames) {
            rrdDef.addDatasource(dsName, GAUGE, 600, 0, Double.NaN);
            dsNamesSet.add(dsName);
        }
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        RrdDb rrdDb = new RrdDb(rrdDef);
        rrdDb.close();

        rrdDb = new RrdDb(rrdPath);
        for(Datasource ds: rrdDb.getDatasources()) {
            Assert.assertTrue("Unexpected ds name: " + ds.getName(), dsNamesSet.contains(ds.getName()));
        }
        rrdDb.close();
    }

}
