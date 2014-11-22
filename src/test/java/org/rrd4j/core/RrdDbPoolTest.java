package org.rrd4j.core;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
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

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test(timeout=500)
    public void testCount() throws IOException, InterruptedException {
        final RrdDbPool instance = new RrdDbPool();
        instance.setCapacity(10);
        RrdDef def = new RrdDef(new File(testFolder.getRoot().getCanonicalFile(), "test.rrd").getCanonicalPath());
        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
        def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
        RrdDb db = instance.requestRrdDb(def);
        for(int i=0; i < 9; i++ ) {
            instance.requestRrdDb(db.getPath());
        }
        Assert.assertEquals(1, instance.getOpenFileCount());
        for(int i=0; i < 10; i++ ) {
            instance.release(db);
        }
        String[] files = instance.getOpenFiles();
        Assert.assertArrayEquals("not all rrd released", new String[]{}, files);
    }


    @Test(timeout=100)
    public void testPoolFull() throws IOException, InterruptedException {
        final RrdDbPool instance = new RrdDbPool();
        instance.setCapacity(10);
        final Queue<RrdDb> dbs = new ConcurrentLinkedQueue<RrdDb>();
        final AtomicInteger done = new AtomicInteger(0);
        final CountDownLatch full = new CountDownLatch(10);
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    full.await();
                    while(instance.getOpenFileCount() > 0) {
                        Assert.assertTrue("too much open files", instance.getOpenFileCount() <= 10);
                        if(dbs.size() > 0) {
                            RrdDb release = dbs.poll();
                            instance.release(release);
                            done.incrementAndGet();
                            Thread.yield();
                        }
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
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
                        barrier.await();
                        RrdDb db = instance.requestRrdDb(def);
                        dbs.add(db);                        
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
        Assert.assertEquals("failure in sub thread", 12, done.get()); 
        String[] files = instance.getOpenFiles();
        Assert.assertArrayEquals("not all rrd released", new String[]{}, files);
    }

    @Test(timeout=100)
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
                            instance.release(release);
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
        final RrdDb db = instance.requestRrdDb(def);
        dbs.add(db);                        

        final CountDownLatch barrier = new CountDownLatch(1);
        for(int i=0; i < 12; i++) {
            new Thread() {

                @Override
                public void run() {
                    try {
                        barrier.await();
                        RrdDb againdb = instance.requestRrdDb(db.getCanonicalPath());
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

    @Test(timeout=500)
    public void testWaitEmpty() throws IOException, InterruptedException {
        final RrdDbPool instance = new RrdDbPool();
        final RrdDef def = new RrdDef(new File(testFolder.getRoot().getCanonicalFile(), "test.rrd").getCanonicalPath());
        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
        def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
        RrdDb db = instance.requestRrdDb(def);
        final long start = new Date().getTime();
        final AtomicInteger done = new AtomicInteger(0);
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    RrdDb db = instance.requestRrdDb(def);
                    instance.release(db);
                    done.set(1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        };
        t.start();
        Thread.sleep(100);
        instance.release(db);
        t.join();
        long end = new Date().getTime();
        Assert.assertEquals("failure in sub thread", 1, done.get()); 
        Assert.assertTrue("requestRrdDb didn't wait for available path", (end - start) > 100); 
        String[] files = instance.getOpenFiles();
        Assert.assertArrayEquals(new String[]{}, files);
    }
}
