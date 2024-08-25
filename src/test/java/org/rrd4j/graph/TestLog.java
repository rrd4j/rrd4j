package org.rrd4j.graph;

import java.util.function.DoubleUnaryOperator;

import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.GraphTester;

public class TestLog extends GraphTester {

    @Test
    public void checkResolver() {
        runTest(0, 100, new double[] {0, 1, -1, 10, 100}, new double[] {0, 0, 0, 1.0, 2.0});
        runTest(-100, 0, new double[] {0, 1, -1, 10, 100}, new double[] {0, 0, 0, 1.0, 2.0});
        runTest(1, 100, new double[] {1, -1, 10, 100}, new double[] {0, 0, 1.0, 2.0});
        runTest(-100, -1, new double[] {1, -1, -10, -100}, new double[] {0, 0, -1.0, -2.0});
        runTest(-100, 100, new double[] {1, -1, -10, -100}, new double[] {0, 0, -1.0, -2.0});
        runTest(1, 1, new double[] {1, -1, -10, -100}, new double[] {0, 0, -1.0, -2.0});
    }

    private void runTest(double min, double max, double[] in, double[] out) {
        ImageParameters im = new ImageParameters();
        im.minval = min;
        im.maxval = max;
        DoubleUnaryOperator loger = LogService.resolve(im);
        Assert.assertEquals(in.length, out.length);
        for (int i = 0; i < in.length; i++) {
            Assert.assertEquals(out[i], loger.applyAsDouble(in[i]), 1e-10);

        }
    }

}
