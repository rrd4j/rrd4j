package org.rrd4j.core;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.DsType.GAUGE;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BigStringsTest {

    static private RrdBackendFactory previousBackend;

    @BeforeClass
    public static void setBackendBefore() {
        previousBackend = RrdBackendFactory.getDefaultFactory();
        RrdBackendFactory.setActiveFactories(new RrdRandomAccessFileBackendFactory());
    }

    @AfterClass
    public static void setBackendAfter() {
        RrdBackendFactory.setActiveFactories(previousBackend);
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

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
        RrdDb.getBuilder().setRrdDef(rrdDef).build().close();

        try (RrdDb rrdDb = RrdDb.getBuilder().setPath(rrdPath).build()) {
            for(Datasource ds: rrdDb.getDatasources()) {
                Assert.assertTrue("Unexpected ds name: " + ds.getName(), dsNamesSet.contains(ds.getName()));
            }
        }
    }

}
