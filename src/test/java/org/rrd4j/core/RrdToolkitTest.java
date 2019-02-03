package org.rrd4j.core;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.DsType.GAUGE;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;

public class RrdToolkitTest {

    static private RrdBackendFactory previousBackend;

    @BeforeClass
    public static void setBackendBefore() {
        previousBackend = RrdBackendFactory.getDefaultFactory();
        RrdBackendFactory.setActiveFactories(new RrdNioBackendFactory(0));
    }

    @AfterClass
    public static void setBackendAfter() {
        RrdBackendFactory.setActiveFactories(previousBackend);
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private String createRrd(String test) throws IOException {
        String[] dsNames = new String[]{"A", "B", "C"};
        Set<String> dsNamesSet = new HashSet<>(dsNames.length);
        String rrdPath = Paths.get(testFolder.getRoot().getCanonicalPath(), test + ".rrd").toString();
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
        return rrdPath;
    }

    @Test
    public void addDatasourcesTest() throws IOException {
        String source = createRrd("adddatasources");
        String destination = Paths.get(testFolder.getRoot().getCanonicalPath(), "new.rrd").toString();
        RrdToolkit.addDatasources(source, destination, Collections.singleton(new DsDef("D", DsType.ABSOLUTE, 1, 0, 1)));
        try (RrdDb db = RrdDb.getBuilder().setPath(destination).build()) {
            Assert.assertTrue(db.containsDs("A"));
            Assert.assertTrue(db.containsDs("B"));
            Assert.assertTrue(db.containsDs("C"));
            Assert.assertTrue(db.containsDs("D"));
        }
        try (RrdDb db = RrdDb.getBuilder().setPath(source).build()) {
            Assert.assertTrue(db.containsDs("A"));
            Assert.assertTrue(db.containsDs("B"));
            Assert.assertTrue(db.containsDs("C"));
            Assert.assertFalse(db.containsDs("D"));
        }
    }

    @Test
    public void addDatasourcesWithBackupTest() throws IOException {
        String source = createRrd("adddatasourceswithbackup");
        RrdToolkit.addDatasources(source, Collections.singleton(new DsDef("D", DsType.ABSOLUTE, 1, 0, 1)), true);
        try (RrdDb db = RrdDb.getBuilder().setPath(source).build()) {
            Assert.assertTrue(db.containsDs("A"));
            Assert.assertTrue(db.containsDs("B"));
            Assert.assertTrue(db.containsDs("C"));
            Assert.assertTrue(db.containsDs("D"));
        }
        try (RrdDb db = RrdDb.getBuilder().setPath(source + ".bak").build()) {
            Assert.assertTrue(db.containsDs("A"));
            Assert.assertTrue(db.containsDs("B"));
            Assert.assertTrue(db.containsDs("C"));
            Assert.assertFalse(db.containsDs("D"));
        }
    }

    @Test
    public void removeDatasourcesTest() throws IOException {
        String source = createRrd("removedadatasources");
        String destination = Paths.get(testFolder.getRoot().getCanonicalPath(), "new.rrd").toString();
        RrdToolkit.removeDatasource(source, destination, "C");
        try (RrdDb db = RrdDb.getBuilder().setPath(destination).build()) {
            Assert.assertTrue(db.containsDs("A"));
            Assert.assertTrue(db.containsDs("B"));
            Assert.assertFalse(db.containsDs("C"));
        }
        try (RrdDb db = RrdDb.getBuilder().setPath(source).build()) {
            Assert.assertTrue(db.containsDs("A"));
            Assert.assertTrue(db.containsDs("B"));
            Assert.assertTrue(db.containsDs("C"));
        }
    }

    @Test
    public void removeDatasourcesWithBackupTest() throws IOException {
        String source = createRrd("removedatasourceswithbackup");
        RrdToolkit.addDatasources(source, Collections.singleton(new DsDef("D", DsType.ABSOLUTE, 1, 0, 1)), true);
        try (RrdDb db = RrdDb.getBuilder().setPath(source).build()) {
            Assert.assertTrue(db.containsDs("A"));
            Assert.assertTrue(db.containsDs("B"));
            Assert.assertTrue(db.containsDs("C"));
            Assert.assertTrue(db.containsDs("D"));
        }
        try (RrdDb db = RrdDb.getBuilder().setPath(source + ".bak").build()) {
            Assert.assertTrue(db.containsDs("A"));
            Assert.assertTrue(db.containsDs("B"));
            Assert.assertTrue(db.containsDs("C"));
            Assert.assertFalse(db.containsDs("D"));
        }
    }

    @Test
    public void renameDatasourceTest() throws IOException {
        String source = createRrd("renamedatasource");
        RrdToolkit.renameDatasource(source, "A", "D");
        try (RrdDb db = RrdDb.getBuilder().setPath(source).build()) {
            Assert.assertFalse(db.containsDs("A"));
            Assert.assertTrue(db.containsDs("B"));
            Assert.assertTrue(db.containsDs("C"));
            Assert.assertTrue(db.containsDs("D"));
        }
    }

}
