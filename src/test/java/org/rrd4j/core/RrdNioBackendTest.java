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

public class RrdNioBackendTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testBackendFactoryWithExecutor() throws IOException {
        RrdNioBackendFactory factory = (RrdNioBackendFactory) RrdBackendFactory.getFactory("NIO");
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
        RrdNioBackendFactory factory = (RrdNioBackendFactory) RrdBackendFactory.getFactory("NIO");

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
}
