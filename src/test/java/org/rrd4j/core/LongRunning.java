package org.rrd4j.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;

public class LongRunning {
    private static final int RRD_STEP = 60;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private static final String TEST_FILE = "longrunning.rrd";

    private List<Long> startTimes;

    private long now;

    @Before
    public void setup() throws Exception {
        now = System.currentTimeMillis();

        startTimes = new ArrayList<Long>();
        startTimes.add(now - (TimeUnit.HOURS).toMillis(1));
        startTimes.add(now - (TimeUnit.DAYS).toMillis(1));
        startTimes.add(now - (TimeUnit.DAYS).toMillis(7));
        startTimes.add(now - (TimeUnit.DAYS).toMillis(30));
        startTimes.add(now - (TimeUnit.DAYS).toMillis(365));
    }

    public void run(int version) throws IOException {
        int min = 200;
        int max = 2000;
        Random random = new Random();

        for (Long startTime : startTimes) {
            Long sampleTime = startTime;
            int sampleSize = (int) ((now - startTime) / (RRD_STEP * 1000));

            RrdDb rrdDb = buildRrdDb(version, startTime, sampleSize);
            try {
                Sample sample = rrdDb.createSample();
                for (int i = 1; i <= sampleSize; i++) {
                    int randomQueryResponseTime = random.nextInt(max - min + 1) + min; // in ms
                    sample.setTime(sampleTime);
                    sample.setValue("data", randomQueryResponseTime);
                    sample.update();

                    // Increment by RRD step
                    sampleTime += RRD_STEP;
                }
            } finally {
                rrdDb.close();
            }
        }
    }
    
    @Test
    public void testVersion1() throws IOException {
        run(1);
    }

    @Test
    public void testVersion2() throws IOException {
        run(2);
    }

    private RrdDb buildRrdDb(int version, long startTime, int sampleSize) throws IOException {
        String rrdFileName = new File(testFolder.getRoot(), TEST_FILE).getCanonicalPath();
        RrdDef rrdDef = new RrdDef(rrdFileName, RRD_STEP);

        // This works
        rrdDef.setVersion(version);

        rrdDef.setStartTime(startTime - 1);
        rrdDef.addDatasource("data", DsType.GAUGE, 90, 0, Double.NaN);

        // 1 step, 60 seconds per step, for 5 minutes
        rrdDef.addArchive(ConsolFun.TOTAL, 0.5, 1, sampleSize);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, sampleSize);
        rrdDef.addArchive(ConsolFun.MAX, 0.5, 1, sampleSize);
        rrdDef.addArchive(ConsolFun.MIN, 0.5, 1, sampleSize);

        // 5 steps, 60 seconds per step, for 30 minutes
        rrdDef.addArchive(ConsolFun.TOTAL, 0.5, 5, sampleSize + 1);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 5, sampleSize + 1);
        rrdDef.addArchive(ConsolFun.MAX, 0.5, 5, sampleSize + 1);
        rrdDef.addArchive(ConsolFun.MIN, 0.5, 5, sampleSize + 1);

        return new RrdDb(rrdDef);
    }
}
