package org.rrd4j.data;


import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.*;

public class AggregatorTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private double testCf(ConsolFun cf) throws IOException {
        long startTime = Util.normalize(Util.getTimestamp(new Date()), 60);

        File rrd = new File(testFolder.getRoot(), "testAggregator.rrd");
        RrdDef rrdDef = new RrdDef(rrd.getAbsolutePath(), startTime, 60);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0, 1, 10);
        rrdDef.addDatasource("total", DsType.GAUGE, 60, 0, Double.NaN);

        RrdDb rrdDb = new RrdDb(rrdDef);

        Sample sample = rrdDb.createSample();
        sample.setTime(startTime+60);
        sample.setValue("total", 0);
        sample.update();

        sample = rrdDb.createSample();
        sample.setTime(startTime + 120);
        sample.setValue("total", 1);
        sample.update();

        sample = rrdDb.createSample();
        sample.setTime(startTime + 180);
        sample.setValue("total", 0);
        sample.update();

        FetchRequest fetchRequest = rrdDb.createFetchRequest(ConsolFun.AVERAGE, startTime, startTime + 240);

        return fetchRequest.fetchData().getAggregate("total", cf);

    }

    @Test
    public void testAggregatorTotal() throws IOException {
        double total = testCf(ConsolFun.TOTAL);
        assertEquals("The aggregate total should be equal to the total of the samples added", 1, total, 1e-15);
    }

    @Test
    public void testAggregatorAverage() throws IOException {
        double total = testCf(ConsolFun.AVERAGE);
        assertEquals("The aggregate average should be equal to the total of the samples added", 1.0/3, total, 1e-15);
    }

    @Test
    public void testAggregatorMin() throws IOException {
        double total = testCf(ConsolFun.MIN);
        assertEquals("The aggregate max should be equal to the total of the samples added", 0, total, 1e-15);
    }

    @Test
    public void testAggregatorMax() throws IOException {
        double total = testCf(ConsolFun.MAX);
        assertEquals("The aggregate min should be equal to the total of the samples added", 1, total, 1e-15);
    }

    @Test
    public void testAggregatorFirst() throws IOException {
        double total = testCf(ConsolFun.FIRST);
        assertEquals("The aggregate first should be equal to the total of the samples added", 0, total, 1e-15);
    }

    @Test
    public void testAggregatorLast() throws IOException {
        double total = testCf(ConsolFun.LAST);
        assertEquals("The aggregate last should be equal to the total of the samples added", 0, total, 1e-15);
    }

}

