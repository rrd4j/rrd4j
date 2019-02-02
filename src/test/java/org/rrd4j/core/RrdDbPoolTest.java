package org.rrd4j.core;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;

/**
 * @author Fabrice Bacchella
 *
 */
public class RrdDbPoolTest {

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

    @Test(timeout=2000)
    public void testCount() throws IOException, InterruptedException {
        final RrdDbPool instance = new RrdDbPool();
        instance.setCapacity(10);
        RrdDef def = new RrdDef(new File(testFolder.getRoot().getCanonicalFile(), "test.rrd").getCanonicalPath());
        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
        def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
        RrdDb[] openned = new RrdDb[10];
        openned[0] = RrdDb.getBuilder().usePool().setPool(instance).setRrdDef(def).build();
        for(int i = 1 ; i < 10 ; i++ ) {
            openned[i] = RrdDb.getBuilder().usePool().setPool(instance).setPath(openned[0].getPath()).build();
        }
        Assert.assertEquals(1, instance.getOpenFileCount());
        for(int i=0; i < 10; i++ ) {
            openned[i].close();
        }
        String[] files = instance.getOpenFiles();
        Assert.assertArrayEquals("not all rrd released", new String[]{}, files);
    }


    @Test(timeout=2000)
    public void testPoolFull() throws IOException, InterruptedException {
        final RrdDbPool instance = new RrdDbPool();
        instance.setCapacity(10);
        final Queue<RrdDb> dbs = new ConcurrentLinkedQueue<RrdDb>();
        final AtomicInteger done = new AtomicInteger(0);
        final AtomicInteger created = new AtomicInteger(0);
        final CountDownLatch full = new CountDownLatch(10);
        //A thread that will count all release db
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    //Wait until pool was filled once
                    full.await();
                    while(created.get() < 12 || instance.getOpenFileCount() > 0) {
                        Assert.assertTrue("too much open files", instance.getOpenFileCount() <= 10);
                        if(dbs.size() > 0) {
                            RrdDb release = dbs.poll();
                            release.close();
                            done.incrementAndGet();
                        }
                        Thread.yield();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        t.start();
        final CountDownLatch barrier = new CountDownLatch(1);
        for(int i=0; i < 12; i++) {
            final int locali = i;
            new Thread() {
                @Override
                public void run() {
                    try {
                        RrdDef def = new RrdDef(new File(testFolder.getRoot().getCanonicalFile(), "test" + locali + ".rrd").getCanonicalPath());
                        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
                        def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
                        //All thread are synchronized and will try to create a db at the same time
                        barrier.await();
                        RrdDb db = RrdDb.getBuilder().usePool().setPool(instance).setRrdDef(def).build();
                        dbs.add(db);
                        created.incrementAndGet();
                        full.countDown();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
        Thread.yield();
        //launch all the sub-threads
        barrier.countDown();
        //Finished when pool is empty
        t.join();
        String[] files = instance.getOpenFiles();
        Assert.assertArrayEquals("not all rrd released", new String[]{}, files);
        Assert.assertEquals("finished, but not all db seen", 12, done.get()); 
    }

    @Test(timeout=2000)
    public void testMultiOpen() throws IOException, InterruptedException {
        final RrdDbPool instance = new RrdDbPool();
        instance.setCapacity(2);
        final Queue<RrdDb> dbs = new ConcurrentLinkedQueue<RrdDb>();
        final AtomicInteger done = new AtomicInteger(0);
        final CountDownLatch full = new CountDownLatch(12);
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    full.await();
                    while(instance.getOpenFileCount() > 0) {
                        Assert.assertTrue("too much open files", instance.getOpenFileCount() <= 1);
                        if(dbs.size() > 0) {
                            RrdDb release = dbs.poll();
                            release.close();
                            done.incrementAndGet();
                            Thread.yield();
                        }
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        RrdDef def = new RrdDef(new File(testFolder.getRoot().getCanonicalFile(), "test.rrd").getCanonicalPath());
        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
        def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
        final RrdDb db = RrdDb.getBuilder().usePool().setPool(instance).setRrdDef(def).build();
        dbs.add(db);

        final CountDownLatch barrier = new CountDownLatch(1);
        for(int i=0; i < 12; i++) {
            new Thread() {

                @Override
                public void run() {
                    try {
                        barrier.await();
                        RrdDb againdb = RrdDb.getBuilder().usePool().setPool(instance).setPath(db.getCanonicalPath()).build();
                        dbs.add(againdb);
                        full.countDown();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
        barrier.countDown();
        full.await();
        t.start();
        t.join();
        Assert.assertEquals("failure in sub thread", 13, done.get()); 
        String[] files = instance.getOpenFiles();
        Assert.assertArrayEquals("not all rrd released", new String[]{}, files);
    }

    @Test(timeout=2000)
    public void testWaitEmpty() throws IOException, InterruptedException {
        final RrdDbPool instance = new RrdDbPool();
        final RrdDef def = new RrdDef(new File(testFolder.getRoot().getCanonicalFile(), "test.rrd").getCanonicalPath());
        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
        def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
        RrdDb db = RrdDb.getBuilder().usePool().setPool(instance).setRrdDef(def).build();
        final long start = new Date().getTime();
        final AtomicInteger done = new AtomicInteger(0);
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    try (RrdDb db = RrdDb.getBuilder().usePool().setPool(instance).setRrdDef(def).build()) {
                        
                    }
                    done.set(1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        };
        t.start();
        Thread.sleep(100);
        db.close();
        t.join();
        long end = new Date().getTime();
        Assert.assertEquals("failure in sub thread", 1, done.get()); 
        Assert.assertTrue("requestRrdDb didn't wait for available path", (end - start) > 100); 
        String[] files = instance.getOpenFiles();
        Assert.assertArrayEquals(new String[]{}, files);
    }
}
