package eu.bengreen.data.utility;

import org.rrd4j.graph.DownSampler;

/**
 * Naive implementation of down sample with simple array input Largest-Triangle-Three-Buckets, from <a href="http://skemman.is/en/item/view/1946/15343">Sveinn Steinarsson's thesis</a>, section 4.2..
 * 
 * @author Benjamin Green
 */
public class DownSampleImpl {

    private static void setDataSetLine(DownSampler.DataSet sampled, int rank, long timestamp, double value) {
        sampled.timestamps[rank] = timestamp;
        sampled.values[rank] = value;
    }

    /**
     * First implementation of Largest-Triangle-Three-Buckets.
     * <p>
     * Written by dcrane (https://github.com/drcrane/downsample).
     * @author Fabrice Bacchella
     *
     */
    public static class LargestTriangleThreeBuckets implements DownSampler {
        private final int threshold;
        public LargestTriangleThreeBuckets(int threshold) {
            this.threshold = threshold;
        }

        @Override
        public DataSet downsize(long[] timestamps, double[] values) {
            return DownSampleImpl.largestTriangleThreeBuckets(timestamps, values, threshold);
        };
    }

    /**
     * A variation of the Largest-Triangle-Three-Buckets.
     * <p>
     * Written by dcrane (https://github.com/drcrane/downsample).
     * @author Fabrice Bacchella
     *
     */
    public static class LargestTriangleThreeBucketsTime implements DownSampler {
        private final int threshold;
        public LargestTriangleThreeBucketsTime(int threshold) {
            this.threshold = threshold;
        }

        @Override
        public DataSet downsize(long[] timestamps, double[] values) {
            return DownSampleImpl.largestTriangleThreeBucketsTime(timestamps, values, threshold);
        };
    }

    private static DownSampler.DataSet largestTriangleThreeBuckets(long[] timestamps, double[] values, int threshold) {
        DownSampler.DataSet sampled = new DownSampler.DataSet(new long[threshold], new double[threshold]);
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
        }
        int sampled_index = 0;
        double every = (double)(inputLength - 2) / (double)(threshold - 2);
        int a = 0, next_a = 0;
        long max_area_point_timestamp = -1;
        double max_area_point_value = Double.NaN;
        double max_area, area;

        //Skip initials NaN
        while( Double.isNaN(values[a])) {
            a++;
        }
        setDataSetLine(sampled, sampled_index++, timestamps[a], values[a]);

        for (int i = 0; i < threshold - 2; i++) {
            long avg_x = 0L;
            double avg_y = 0.0D;
            int avg_range_start = (int)Math.floor((i+1)*every) + 1;
            int avg_range_end = (int)Math.floor((i+2)*every) + 1;
            avg_range_end = avg_range_end < inputLength ? avg_range_end : inputLength;
            int avg_range_length = (int)(avg_range_end - avg_range_start);
            while (avg_range_start < avg_range_end) {
                avg_x = avg_x + timestamps[avg_range_start];
                avg_y += values[avg_range_start];
                avg_range_start ++;
            }
            avg_x /= avg_range_length;
            avg_y /= avg_range_length;

            int range_offs = (int)Math.floor((i + 0) * every) + 1;
            int range_to = (int)Math.floor((i + 1) * every) + 1;

            long point_a_x = timestamps[a];
            double point_a_y = values[a];

            max_area = area = -1;

            while (range_offs < range_to) {
                area = Math.abs(
                        0.5D * 
                        (point_a_x - avg_x) * (values[range_offs] - point_a_y) -
                        (point_a_x - timestamps[range_offs]) * (avg_y - point_a_y)
                        );
                if (area > max_area) {
                    max_area = area;
                    max_area_point_timestamp =  timestamps[range_offs];
                    max_area_point_value = values[range_offs];
                    next_a = range_offs;
                }
                range_offs ++;
            }
            setDataSetLine(sampled, sampled_index++, max_area_point_timestamp, max_area_point_value);
            a = next_a;
        }

        setDataSetLine(sampled, sampled_index++, timestamps[inputLength - 1], values[inputLength - 1]);
        return sampled;
    }

    private static DownSampler.DataSet largestTriangleThreeBucketsTime(long[] timestamps, double[] values, int threshold) {
        DownSampler.DataSet sampled = new DownSampler.DataSet(new long[threshold], new double[threshold]);
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
        }
        int bucket_interval = (int)((timestamps[inputLength - 1] - timestamps[0]) / threshold);
        int sampled_index = 0;
        double every = (double)(inputLength - 2) / (double)(threshold - 2);
        int a = 0, next_a = 0;
        long max_area_point_timestamp = -1;
        double max_area_point_value = Double.NaN;
        double max_area = Double.MIN_VALUE;

        //Skip initials NaN
        while( Double.isNaN(values[a])) {
            a++;
        }
        setDataSetLine(sampled, sampled_index++, timestamps[a], values[a]);

        for (int i = 0; i < threshold - 2; i++) {
            long avg_x = 0L;
            double avg_y = 0.0D;
            int avg_range_start = (int)Math.floor((i+1)*every) + 1;
            int avg_range_end = (int)Math.floor((i+2)*every) + 1;
            avg_range_end = avg_range_end < inputLength ? avg_range_end : inputLength;
            int avg_range_length = (int)(avg_range_end - avg_range_start);
            while (avg_range_start < avg_range_end) {
                avg_x = avg_x + timestamps[avg_range_start];
                avg_y += values[avg_range_start];
                avg_range_start ++;
            }
            avg_x /= avg_range_length;
            avg_y /= avg_range_length;

            int range_offs = (int)Math.floor((i + 0) * every) + 1;
            int range_to = (int)Math.floor((i + 1) * every) + 1;

            double point_a_x = timestamps[a];
            double point_a_y = values[a];

            max_area = -1;

            long ending_time = timestamps[range_offs] + bucket_interval;

            while (range_offs < range_to) {
                double area = Math.abs(
                        0.5D *
                        (point_a_x - avg_x) * (values[range_offs] - point_a_y) -
                        (point_a_x - timestamps[range_offs]) * (avg_y - point_a_y)
                        );
                if (area > max_area) {
                    max_area = area;
                    max_area_point_timestamp =  ending_time;
                    max_area_point_value = values[range_offs];
                    next_a = range_offs;
                }
                range_offs ++;
            }
            setDataSetLine(sampled, sampled_index++, max_area_point_timestamp, max_area_point_value);
            a = next_a;
        }

        setDataSetLine(sampled, sampled_index++, timestamps[inputLength - 1], values[inputLength - 1]);
        return sampled;
    }

}