package eu.bengreen.data.utility;

import org.rrd4j.graph.DownSampler;

/**
 * Naive implementation of down sample with simple array input Largest-Triangle-Three-Buckets, from <a href="http://skemman.is/en/item/view/1946/15343">Sveinn Steinarsson's thesis</a>, section 4.2..
 * 
 * @author Benjamin Green
 */
public abstract class DownSampleImpl implements DownSampler {

    protected final int threshold;

    protected DownSampleImpl(int threshold) {
        this.threshold = threshold;
    }

    protected void setDataSetLine(DownSampler.DataSet sampled, int rank, long timestamp, double value) {
        sampled.timestamps[rank] = timestamp;
        sampled.values[rank] = value;
    }

    @Override
    public DataSet downsize(long[] timestamps, double[] values) {
        if (timestamps == null || values == null) {
            throw new NullPointerException("Cannot cope with a null data input array.");
        }
        if (threshold <= 2) {
            throw new IllegalArgumentException("What am I supposed to do with that?");
        }
        if (timestamps.length != values.length) {
            throw new IllegalArgumentException("Unmatched size with input arrays");
        }
        int inputLength = timestamps.length;
        if (inputLength <= threshold) {
            return new DownSampler.DataSet(timestamps, values);
        } else {
            DownSampler.DataSet sampled = new DownSampler.DataSet(new long[threshold], new double[threshold]);
            return downsizeImpl(sampled, timestamps, values);
        }
    }

    protected abstract DataSet downsizeImpl(DataSet sampled, long[] timestamps, double[] values);

}
