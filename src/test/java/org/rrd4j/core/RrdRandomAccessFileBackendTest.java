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
import org.rrd4j.core.RrdBackend;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdRandomAccessFileBackendFactory;

public class RrdRandomAccessFileBackendTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testBackendFactoryWithExecutor() throws IOException {
        RrdRandomAccessFileBackendFactory factory = (RrdRandomAccessFileBackendFactory) RrdBackendFactory.getFactory("FILE");
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        File rrdfile = testFolder.newFile("testfile");
        RrdBackend be = factory.open(rrdfile.getCanonicalPath(), false);

        be.setLength(10);
        be.writeDouble(0, 0);
        be.close();
        executor.shutdown();
        DataInputStream is = new DataInputStream(new FileInputStream(rrdfile));
        Double d = is.readDouble();
        Assert.assertEquals("write to random access file failed", 0, d, 1e-10);
        is.close();
    }

    @Test
    public void testBackendFactory() throws IOException {
        RrdRandomAccessFileBackendFactory factory = (RrdRandomAccessFileBackendFactory) RrdBackendFactory.getFactory("FILE");

        File rrdfile = testFolder.newFile("testfile");
        RrdBackend be = factory.open(rrdfile.getCanonicalPath(), false);

        be.setLength(10);
        be.writeDouble(0, 0);
        be.close();
        DataInputStream is = new DataInputStream(new FileInputStream(rrdfile));
        Double d = is.readDouble();
        Assert.assertEquals("write to random access file failed", 0, d, 1e-10);
        is.close();
    }
}
