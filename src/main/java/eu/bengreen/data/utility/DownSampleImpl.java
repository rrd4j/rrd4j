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

}
