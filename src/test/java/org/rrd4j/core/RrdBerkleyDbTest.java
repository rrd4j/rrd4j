package org.rrd4j.core;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assume.assumeThat;
import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.DsType.GAUGE;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class RrdBerkleyDbTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void test1() throws IOException, URISyntaxException {
        String javaVersion = System.getProperty("java.version");
        assumeThat(javaVersion, not(startsWith((("1.7")))));

        EnvironmentConfig cfg = new EnvironmentConfig();
        cfg.setAllowCreate(true);
        cfg.setTransactional(false);

        try (Environment env = new Environment(testFolder.getRoot(), cfg)) {
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setTransactional(false);
            dbConfig.setAllowCreate(true);
            dbConfig.setSortedDuplicates(false);
            try (Database myDatabase = env.openDatabase(null, "rrddb/path#1", dbConfig)) {
                RrdBerkeleyDbBackendFactory factory = new RrdBerkeleyDbBackendFactory(myDatabase);
                RrdBackendFactory.setActiveFactories(factory);
                URI dbUri = new URI("berkeley", "", "", "", "testrrd");
                long now = Util.normalize(Util.getTimestamp(new Date()), 300);
                try (RrdDb db = new RrdDb(getDef(dbUri))) {
                    db.createSample(now).setValue("short", 1.0).update();
                }
                try (RrdDb db = new RrdDb(factory.getUri("testrrd"))) {
                    Assert.assertEquals(now, db.getLastArchiveUpdateTime());
                }
            }
        }
    }

    private RrdDef getDef(URI uri) {
        RrdDef rrdDef = new RrdDef(uri, Util.getTimestamp(2010, 4, 1) - 1, 300);
        rrdDef.setVersion(2);
        rrdDef.addDatasource("short", GAUGE, 600, 0, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        return rrdDef;

    }
}
