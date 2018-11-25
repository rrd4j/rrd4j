package org.rrd4j.core;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        try (RrdNioBackendFactory factory = new RrdNioBackendFactory(RrdNioBackendFactory.DEFAULT_SYNC_PERIOD, executor)) {
            File rrdfile = testFolder.newFile("testfile");
            RrdBackend be = factory.open(rrdfile.getCanonicalPath(), false);

            be.setLength(10);
            be.writeDouble(0, 0);
            be.close();
            executor.shutdown();
            try (DataInputStream is = new DataInputStream(new FileInputStream(rrdfile))) {
                Double d = is.readDouble();
                Assert.assertEquals("write to NIO failed", 0, d, 1e-10);
            }
        }
    }

    @Test
    public void testBackendFactoryDefaults() throws IOException {
        @SuppressWarnings("resource")
        // Don't close a default NIO, it will close the background sync threads executor
        RrdNioBackendFactory factory = new RrdNioBackendFactory();

        File rrdfile = testFolder.newFile("testfile");
        RrdBackend be = factory.open(rrdfile.getCanonicalPath(), false);

        be.setLength(10);
        be.writeDouble(0, 0);
        be.close();
        try (DataInputStream is = new DataInputStream(new FileInputStream(rrdfile))) {
            Double d = is.readDouble();
            Assert.assertEquals("write to NIO failed", 0, d, 1e-10);
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
            try (DataInputStream is = new DataInputStream(new FileInputStream(rrdfile))) {
                Double d = is.readDouble();
                Assert.assertEquals("write to NIO failed", 0, d, 1e-10);
            };
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
