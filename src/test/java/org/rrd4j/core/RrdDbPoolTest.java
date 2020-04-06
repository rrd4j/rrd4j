package org.rrd4j.core;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
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

    final Set<Throwable> failed = ConcurrentHashMap.newKeySet();
    final UncaughtExceptionHandler exhandler = (t, ex) -> {
        ex.printStackTrace();
        failed.add(ex);
    };
    
    @After
    public void checkTreads() {
        Assert.assertEquals(failed.toString(), 0, failed.size());
    }

    private Thread getThread(Runnable r) {
        Thread t = new Thread(r);
        t.setUncaughtExceptionHandler(exhandler);
        return t;
    }
    
    @Test(timeout=2000)
    public void testCount() throws IOException, InterruptedException {
        RrdDbPool instance = new RrdDbPool();
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
        int capacity = 100;
        RrdDbPool instance = new RrdDbPool();
        instance.setCapacity(capacity);
        Queue<RrdDb> dbs = new ConcurrentLinkedQueue<RrdDb>();
        AtomicInteger done = new AtomicInteger(0);
        AtomicInteger created = new AtomicInteger(0);
        CountDownLatch full = new CountDownLatch(capacity);
        //A thread that will count all release db
        Thread t = getThread(() -> {
            try {
                //Wait until pool was filled once
                full.await();
                while (created.get() < (capacity + 2)  || instance.getOpenFileCount() > 0) {
                    int used = instance.getOpenFileCount();
                    Assert.assertTrue("Too many open files: "+ used, used <= capacity);
                    if(dbs.size() > 0) {
                        RrdDb release = dbs.poll();
                        release.close();
                        done.incrementAndGet();
                    }
                    Thread.yield();
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        t.start();
        CountDownLatch barrier = new CountDownLatch(1);
        for (int i = 0; i < (capacity + 2); i++) {
            int locali = i;
            getThread(() -> {
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
            }).start();
        }
        Thread.yield();
        //launch all the sub-threads
        barrier.countDown();
        //Finished when pool is empty
        t.join();
        String[] files = instance.getOpenFiles();
        Assert.assertArrayEquals("Not all rrd released", new String[]{}, files);
        Assert.assertEquals("finished, but not all db seen", capacity + 2, done.get()); 
    }

    @Test(timeout=2000)
    public void testMultiOpen() throws IOException, InterruptedException {
        RrdDbPool instance = new RrdDbPool();
        instance.setCapacity(2);
        Queue<RrdDb> dbs = new ConcurrentLinkedQueue<RrdDb>();
        Set<Integer> dbid = ConcurrentHashMap.newKeySet(12);
        AtomicInteger done = new AtomicInteger(0);
        CountDownLatch full = new CountDownLatch(12);
        Thread t = getThread(() -> {
            try {
                full.await();
                while(instance.getOpenFileCount() > 0) {
                    Assert.assertTrue("too much open files", instance.getOpenFileCount() <= 1);
                    if(dbs.size() > 0) {
                        RrdDb release = dbs.poll();
                        dbid.add(Objects.hashCode(release));
                        release.close();
                        done.incrementAndGet();
                        Thread.yield();
                    }
                }
            } catch (Exception e) {
                failed.add(e);
            }
           
        });
        RrdDef def = new RrdDef(new File(testFolder.getRoot().getCanonicalFile(), "test.rrd").getCanonicalPath());
        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
        def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
        RrdDb db = RrdDb.getBuilder().usePool().setPool(instance).setRrdDef(def).build();
        dbs.add(db);

        CountDownLatch barrier = new CountDownLatch(1);
        for (int i = 0; i < 12; i++) {
            getThread(() -> {
                try {
                    barrier.await();
                    RrdDb againdb = RrdDb.getBuilder().usePool().setPool(instance).setPath(db.getCanonicalPath()).build();
                    int count = instance.getOpenCount(againdb.getUri());
                    Assert.assertTrue(count >= 0 && count <= 13);
                    dbs.add(againdb);
                    full.countDown();
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        barrier.countDown();
        full.await();
        t.start();
        t.join();
        Assert.assertEquals("failure in sub thread", 13, done.get());
        String[] files = instance.getOpenFiles();
        Assert.assertArrayEquals("not all rrd released", new String[]{}, files);
        Assert.assertEquals(failed.toString(), 0, failed.size());
        Assert.assertEquals(1, dbid.size());
    }

    @Test(timeout=2000)
    public void testWaitEmpty() throws IOException, InterruptedException {
        RrdDbPool instance = new RrdDbPool(new RrdRandomAccessFileBackendFactory());
        RrdDef def = new RrdDef(new File(testFolder.getRoot().getCanonicalFile(), "test.rrd").getCanonicalPath());
        def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
        def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
        RrdDb db = instance.requestRrdDb(def);
        long start = new Date().getTime();
        AtomicBoolean done = new AtomicBoolean(false);
        Thread t = getThread(() -> {
            try {
                try (RrdDb ldb = RrdDb.getBuilder().usePool().setPool(instance).setRrdDef(def).build()) {
                }
                done.set(true);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        t.start();
        Thread.sleep(100);
        db.close();
        t.join();
        long end = new Date().getTime();
        Assert.assertTrue("failure in sub thread", done.get()); 
        Assert.assertTrue("requestRrdDb didn't wait for available path", (end - start) > 100); 
        String[] files = instance.getOpenFiles();
        Assert.assertArrayEquals(new String[]{}, files);
    }

}
