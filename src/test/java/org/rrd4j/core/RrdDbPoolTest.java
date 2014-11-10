/**
 * 
 */
package org.rrd4j.core;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
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

    @Test(timeout=200)
    public void test1() throws IOException, InterruptedException {
        final RrdDbPool instance = new RrdDbPool();
        instance.setCapacity(10);
        final LinkedList<RrdDb> dbs = new LinkedList<RrdDb>();
        final AtomicInteger done = new AtomicInteger(1);
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    while(dbs.size() > 0) {
                        instance.release(dbs.pop());                        
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    done.set(0);
                }
            }
        };
        t.start();
        long start = new Date().getTime();
        for(int i=0; i < 11; i++) {
            RrdDef def = new RrdDef(new File(testFolder.getRoot().getCanonicalFile(), "test" + i + ".rrd").getCanonicalPath());
            def.addArchive(ConsolFun.AVERAGE, 0.5, 1, 215);
            def.addDatasource("bar", DsType.GAUGE, 3000, Double.NaN, Double.NaN);
            RrdDb db = instance.requestRrdDb(def);
            dbs.add(db);
        }
        long end = new Date().getTime();
        t.join();
        Assert.assertTrue("pool didn't wait for capacity", (end - start) > 100); 
        Assert.assertEquals("failure in sub thread", 1, done.get()); 
        String[] files = instance.getOpenFiles();
        Assert.assertArrayEquals("not all rrd released", new String[]{}, files);
    }

    @Test(timeout=200)
    public void test2() throws IOException, InterruptedException {
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
                    e.printStackTrace();
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
