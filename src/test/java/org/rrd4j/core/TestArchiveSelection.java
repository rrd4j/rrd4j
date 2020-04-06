package org.rrd4j.core;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;

public class TestArchiveSelection {

    private final long[] timeStamp1 = new long[] {
            920202780, //not returned by rrdtool
            920202810,
            920202840,
            920202870,
            920202900,
            920202930,
            920202960,
            920202990,
            920203020,
            920203050,
            920203080,
            920203110,
            920203140,
            920203170,
            920203200,
            920203230,
            920203260,
            920203290,
            920203320,
            920203350,
            920203380,
            920203410,
            920203440,
            920203470,
            920203500,
            920203530,
            920203560,
            920203590,
    };

    private final long[] timeStamp2 = new long[] {
            920202750, //not returned by rrdtool
            920202900,
            920203050,
            920203200,
            920203350,
            920203500,
            920203650,
    };

    private RrdDb getDb() throws IOException {

        // create RRD definition
        RrdDef rrdDef = new RrdDef("/test.rrd", 30);
        rrdDef.setStartTime(920202900);

        // create datasources
        rrdDef.addDatasource("counter", DsType.GAUGE,60, Double.NaN, Double.NaN);
        rrdDef.addDatasource("duration", DsType.GAUGE,60, Double.NaN, Double.NaN);

        // create archives
        rrdDef.addArchive(ConsolFun.AVERAGE, 0, 1, 20000);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0, 5, 200);

        // create database
        @SuppressWarnings("deprecation")
        RrdDb rrdDb = new RrdDb(rrdDef, RrdBackendFactory.getFactory("MEMORY") );

        // populate
        Sample sample = rrdDb.createSample();
        sample.setTime(920202928).setValue("counter", 2).setValue("duration", 7).update();
        sample.setTime(920202946).setValue("counter", 2).setValue("duration", 11).update();
        sample.setTime(920202985).setValue("counter", 2).setValue("duration", 15).update();
        sample.setTime(920203002).setValue("counter", 1).setValue("duration", 9).update();
        sample.setTime(920203036).setValue("counter", 3).setValue("duration", 33).update();
        sample.setTime(920203058).setValue("counter", 1).setValue("duration", 13).update();
        sample.setTime(920203103).setValue("counter", 3).setValue("duration", 45).update();
        sample.setTime(920203127).setValue("counter", 2).setValue("duration", 35).update();
        sample.setTime(920203165).setValue("counter", 3).setValue("duration", 60).update();
        sample.setTime(920203198).setValue("counter", 2).setValue("duration", 45).update();
        sample.setTime(920203216).setValue("counter", 1).setValue("duration", 24).update();
        sample.setTime(920203257).setValue("counter", 2).setValue("duration", 51).update();
        sample.setTime(920203282).setValue("counter", 2).setValue("duration", 55).update();
        sample.setTime(920203308).setValue("counter", 2).setValue("duration", 59).update();
        sample.setTime(920203337).setValue("counter", 2).setValue("duration", 63).update();
        sample.setTime(920203355).setValue("counter", 1).setValue("duration", 33).update();
        sample.setTime(920203395).setValue("counter", 3).setValue("duration", 105).update();
        sample.setTime(920203421).setValue("counter", 2).setValue("duration", 75).update();
        sample.setTime(920203454).setValue("counter", 3).setValue("duration", 120).update();
        sample.setTime(920203499).setValue("counter", 3).setValue("duration", 129).update();
        sample.setTime(920203528).setValue("counter", 3).setValue("duration", 138).update();
        //
        return rrdDb;
    }

    @Test
    public void testFetchDefault() throws Exception {
        RrdDb rrdDb = getDb();

        FetchRequest fetchRequest = rrdDb.createFetchRequest(ConsolFun.AVERAGE, 920202800, 920203565);
        FetchData fetchData = fetchRequest.fetchData();
        Assert.assertArrayEquals("timestamps don't match with no resolution", timeStamp1, fetchData.getTimestamps());

        rrdDb.close();

    }

    @Test
    public void testFetch150() throws Exception {
        RrdDb rrdDb = getDb();

        FetchRequest fetchRequest = rrdDb.createFetchRequest(ConsolFun.AVERAGE, 920202800, 920203565, 150);
        FetchData fetchData = fetchRequest.fetchData();
        Assert.assertArrayEquals("timestamps don't match with resolution 150", timeStamp2, fetchData.getTimestamps());

        rrdDb.close();

    }
}
