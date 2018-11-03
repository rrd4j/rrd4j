package org.rrd4j.data;

import org.junit.Assert;
import org.junit.Test;

public class LinearInterpolatorTest {

    @SuppressWarnings("deprecation")
    @Test
    public void testLeft() {
        LinearInterpolator li =new LinearInterpolator(new long[] {0, 100}, new double[] {100, 300});
        li.setInterpolationMethod(LinearInterpolator.INTERPOLATE_LEFT);
        Assert.assertEquals(100, li.getValue(50), 1e-5);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testLinera() {
        LinearInterpolator li =new LinearInterpolator(new long[] {0, 100}, new double[] {100, 300});
        li.setInterpolationMethod(LinearInterpolator.INTERPOLATE_LINEAR);
        Assert.assertEquals(200, li.getValue(50), 1e-5);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testRight() {
        LinearInterpolator li =new LinearInterpolator(new long[] {0, 100}, new double[] {100, 300});
        li.setInterpolationMethod(LinearInterpolator.INTERPOLATE_RIGHT);
        Assert.assertEquals(300, li.getValue(50), 1e-5);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testInterpolate() {
        LinearInterpolator li =new LinearInterpolator(new long[] {0, 100, 200}, new double[] {100, 300, 100});
        li.setInterpolationMethod(LinearInterpolator.INTERPOLATE_REGRESSION);
        Assert.assertEquals(166.66666666, li.getValue(50), 1e-5);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBadLinearDown() {
        LinearInterpolator li =new LinearInterpolator(new long[] {0, 100, 200}, new double[] {100, 300, 100});
        li.setInterpolationMethod(-1);
        Assert.assertEquals(200, li.getValue(50), 1e-5);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBadLinearUp() {
        LinearInterpolator li =new LinearInterpolator(new long[] {0, 100, 200}, new double[] {100, 300, 100});
        li.setInterpolationMethod(4);
        Assert.assertEquals(200, li.getValue(50), 1e-5);
    }

    @Test
    public void testLeftEnum() {
        LinearInterpolator li =new LinearInterpolator(new long[] {0, 100}, new double[] {100, 300});
        li.setInterpolationMethod(LinearInterpolator.Method.LEFT);
        Assert.assertEquals(100, li.getValue(50), 1e-5);
    }

    @Test
    public void testLineralEnum() {
        LinearInterpolator li =new LinearInterpolator(new long[] {0, 100}, new double[] {100, 300});
        li.setInterpolationMethod(LinearInterpolator.Method.LINEAR);
        Assert.assertEquals(200, li.getValue(50), 1e-5);
    }

    @Test
    public void testRightEnum() {
        LinearInterpolator li =new LinearInterpolator(new long[] {0, 100}, new double[] {100, 300});
        li.setInterpolationMethod(LinearInterpolator.Method.RIGHT);
        Assert.assertEquals(300, li.getValue(50), 1e-5);
    }

    @Test
    public void testInterpolateEnum() {
        LinearInterpolator li =new LinearInterpolator(new long[] {0, 100, 200}, new double[] {100, 300, 100});
        li.setInterpolationMethod(LinearInterpolator.Method.REGRESSION);
        Assert.assertEquals(166.66666666, li.getValue(50), 1e-5);
    }

    @Test
    public void testOutside() {
        LinearInterpolator li =new LinearInterpolator(new long[] {0, 100}, new double[] {100, 300});
        li.setInterpolationMethod(LinearInterpolator.Method.LEFT);
        Assert.assertTrue(Double.isNaN(li.getValue(200)));
        Assert.assertTrue(Double.isNaN(li.getValue(-100)));
    }

}
