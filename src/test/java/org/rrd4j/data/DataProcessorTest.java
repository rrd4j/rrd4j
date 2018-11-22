package org.rrd4j.data;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.DsType.GAUGE;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;

public class DataProcessorTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testMemoryDataprocess() throws IOException {
        RrdDef rrdDef = new RrdDef(testFolder.newFile("testBuild.rrd").getCanonicalPath());
        rrdDef.addDatasource("sun", GAUGE, 600, 0, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        try (RrdDb rrdDb = new RrdDb(rrdDef, RrdBackendFactory.getFactory("MEMORY"))){
            FetchRequest fr = rrdDb.createFetchRequest(ConsolFun.AVERAGE, 10000, 20000);
            DataProcessor dp = new DataProcessor(10000, 20000);
            dp.addDatasource("sun", fr.fetchData());
            dp.processData();
        }
    }

}
