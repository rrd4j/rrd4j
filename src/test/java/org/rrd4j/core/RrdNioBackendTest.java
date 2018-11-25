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
        RrdNioBackendFactory factory = new RrdNioBackendFactory();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        factory.setSyncThreadPool(new RrdSyncThreadPool());
        File rrdfile = testFolder.newFile("testfile");
        RrdBackend be = factory.open(rrdfile.getCanonicalPath(), false);

        be.setLength(10);
        be.writeDouble(0, 0);
        be.close();
        executor.shutdown();
        DataInputStream is = new DataInputStream(new FileInputStream(rrdfile));
        Double d = is.readDouble();
        Assert.assertEquals("write to NIO failed", 0, d, 1e-10);
        is.close();
    }

    @Test
    public void testBackendFactory() throws IOException {
        RrdNioBackendFactory factory = new RrdNioBackendFactory();

        File rrdfile = testFolder.newFile("testfile");
        RrdBackend be = factory.open(rrdfile.getCanonicalPath(), false);

        be.setLength(10);
        be.writeDouble(0, 0);
        be.close();
        DataInputStream is = new DataInputStream(new FileInputStream(rrdfile));
        Double d = is.readDouble();
        Assert.assertEquals("write to NIO failed", 0, d, 1e-10);
        is.close();
    }

    @Test
    public void testBackendFactoryNoSyncing() throws IOException {
        RrdNioBackendFactory factory = new RrdNioBackendFactory(-1, 0);

        File rrdfile = testFolder.newFile("testfile");
        RrdBackend be = factory.open(rrdfile.getCanonicalPath(), false);

        be.setLength(10);
        be.writeDouble(0, 0);
        be.close();
        DataInputStream is = new DataInputStream(new FileInputStream(rrdfile));
        Double d = is.readDouble();
        Assert.assertEquals("write to NIO failed", 0, d, 1e-10);
        is.close();
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
