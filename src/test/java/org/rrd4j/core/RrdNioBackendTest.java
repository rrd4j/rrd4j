package org.rrd4j.core;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RrdNioBackendTest extends BackendTester {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testBackendFactoryWithExecutor() throws IOException {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        File rrdfile = testFolder.newFile("testfile");
        try (RrdNioBackendFactory factory = new RrdNioBackendFactory(RrdNioBackendFactory.DEFAULT_SYNC_PERIOD, executor)) {
            super.testBackendFactory(factory,rrdfile.getCanonicalPath());
        }
    }

    @Test
    public void testBackendFactoryDefaults() throws IOException {
        try (RrdNioBackendFactory factory = new RrdNioBackendFactory(0)) {
            File rrdfile = testFolder.newFile("testfile");
            super.testBackendFactory(factory,rrdfile.getCanonicalPath());
        }
    }

    @Test
    public void testBackendFactoryNoSyncing() throws IOException {
        try (RrdNioBackendFactory factory = new RrdNioBackendFactory(-1, 0)) {
            File rrdfile = testFolder.newFile("testfile");
            RrdBackend be = factory.open(rrdfile.getCanonicalPath(), false);

            be.setLength(10);
            be.writeDouble(0, 0);
            be.close();
            try (DataInputStream is = new DataInputStream(Files.newInputStream(rrdfile.toPath()))) {
                double d = is.readDouble();
                Assert.assertEquals("write to NIO failed", 0, d, 1e-10);
            }
            Assert.assertNull(factory.getSyncThreadPool());
        }
    }

    @Test
    public void testRead1() throws IOException {
        super.testRead1(new RrdNioBackendFactory());
    }

    @Test
    public void testRead2() throws IOException {
        super.testRead2(new RrdNioBackendFactory());
    }

    @Test(expected=InvalidRrdException.class)
    public void testReadCorruptSignature() throws Exception {
        super.testReadCorruptSignature(new RrdNioBackendFactory());
    }

    @Test(expected=InvalidRrdException.class)
    public void testReadEmpty() throws Exception {
        super.testReadEmpty(new RrdNioBackendFactory());
    }

}
