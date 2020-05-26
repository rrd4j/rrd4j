package org.rrd4j.graph;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.data.CubicSplineInterpolator;
import org.rrd4j.data.LinearInterpolator;
import org.rrd4j.data.Plottable;
import org.rrd4j.graph.DownSampler.DataSet;

import eu.bengreen.data.utility.LargestTriangleThreeBuckets;
import eu.bengreen.data.utility.LargestTriangleThreeBucketsTime;

@SuppressWarnings("deprecation")
public class DownSamplerTest {

    private static final long[] al = new long[]{};
    private static final double[] ad = new double[]{};

    public long[] getTs(int size) {
        long[] ts = new long[size];
        for (int i = 0 ; i< size; i++) {
            ts[i] = i + 1000;
        }
        return ts;
    }

    public double[] getValues(int size, boolean withNaN) {
        double[] v = new double[size];
        for (int i = 0 ; i< size; i++) {
            if (!withNaN || (int)(i / (size / 3)) != 1) {
                v[i] = Math.cos(Math.PI * (double)i / (double)size);

            } else {
                v[i] = Double.NaN;
            }
        }
        return v;
    }

    private void run(int size, boolean withNaN, DownSampler dsi, Class<? extends Plottable> clazz, double delta) throws Exception {
        try {
            long[] ts = getTs(size);
            double[] v = getValues(size, withNaN);
            DataSet ds = dsi.downsize(ts, v);
            Constructor<? extends Plottable> cp = clazz.getConstructor(al.getClass(), ad.getClass());
            Plottable p =  cp.newInstance(ds.timestamps, ds.values);
            int countNaN = 0;
            for (int i = 0 ; i < size; i++) {
                if (!Double.isNaN(p.getValue(1000 + i)) && !Double.isNaN(v[i])){
                    Assert.assertEquals("bad interpolated value: " + Math.abs(v[i] - p.getValue(1000 + i)) , v[i], p.getValue(1000 + i), delta);
                } else {
                    countNaN++;
                }
            }
            if (countNaN != 0) {
                Assert.assertEquals("Wrong number of NaN", 1.0/3.0, (double) countNaN / size, 0.01);
            }
        } catch (InvocationTargetException e) {
            throw (Exception)e.getTargetException();
        }
    }

    @Test
    public void test1() throws Exception {
        run(1000, false, new LargestTriangleThreeBuckets(100), CubicSplineInterpolator.class, 0.0001);
        // CubicSplineInterpolator don' take NaN
        run(1000, true, new LargestTriangleThreeBuckets(100), LinearInterpolator.class, 0.0004);
    }

    @Test
    public void test2() throws Exception {
        run(1000, false, new LargestTriangleThreeBucketsTime(100), CubicSplineInterpolator.class, 0.004);
        // CubicSplineInterpolator don' take NaN
        // But anyway LargestTriangleThreeBucketsTime don't handle well NaN
        //run(1000, true, new DownSampleImpl.LargestTriangleThreeBucketsTime(100), LinearInterpolator.class, 0.01);
    }

}
