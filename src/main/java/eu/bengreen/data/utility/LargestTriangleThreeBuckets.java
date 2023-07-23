package eu.bengreen.data.utility;

import org.rrd4j.graph.DownSampler;

/**
 * First implementation of Largest-Triangle-Three-Buckets.
 * <p>
 * Modified by Fabrice Bacchella for NaN support.
 * @author Benjamin Green
 *
 */
public class LargestTriangleThreeBuckets extends DownSampleImpl {

    public LargestTriangleThreeBuckets(int threshold) {
        super(threshold);
    }

    @Override
    public DataSet downsizeImpl(DownSampler.DataSet sampled, long[] timestamps, double[] values) {
        int inputLength = timestamps.length;
        int sampled_index = 0;
        double every = (double)(inputLength - 2) / (double)(threshold - 2);
        int a = 0, next_a = 0;
        long max_area_point_timestamp = -1;
        double max_area_point_value;
        double max_area;

        setDataSetLine(sampled, sampled_index++, timestamps[a], values[a]);

        for (int i = 0; i < threshold - 2; i++) {
            long avg_x = 0L;
            double avg_y = Double.NaN;
            int avg_range_start = (int)Math.floor((i+0)*every) + 0;
            int avg_range_end = (int)Math.floor((i+1)*every) + 1;
            avg_range_end = avg_range_end < inputLength ? avg_range_end : inputLength;
            int avg_range_length = avg_range_end - avg_range_start;
            while (avg_range_start < avg_range_end) {
                avg_x = avg_x + timestamps[avg_range_start];
                if ( ! Double.isNaN(values[avg_range_start])) {
                    avg_y = Double.isNaN(avg_y) ? values[avg_range_start] : avg_y + values[avg_range_start];
                }
                avg_range_start++;
            }
            if(Double.isNaN(avg_y)) {
                a = avg_range_end;
                setDataSetLine(sampled, sampled_index++, timestamps[avg_range_end - 1], Double.NaN);
                continue;
            }
            avg_x /= avg_range_length;
            avg_y /= avg_range_length;

            int range_offs = (int)Math.floor((i + 0) * every) + 1;
            int range_to = (int)Math.floor((i + 1) * every) + 1;

            long point_a_x = timestamps[a];
            double point_a_y = Double.isNaN(values[a]) ? 0 : values[a];
            max_area = -1;
            max_area_point_value = Double.NaN;

            while (range_offs < range_to) {
                double offs_value = Double.isNaN(values[range_offs]) ? 0 : values[range_offs];
                double area = Math.abs(
                        0.5D * 
                        (point_a_x - avg_x) * (offs_value - point_a_y) -
                        (point_a_x - timestamps[range_offs]) * (avg_y - point_a_y)
                        );
                if (area > max_area) {
                    max_area = area;
                    max_area_point_timestamp = timestamps[range_offs];
                    max_area_point_value = offs_value;
                    next_a = range_offs;
                }
                range_offs++;
            }
            setDataSetLine(sampled, sampled_index++, max_area_point_timestamp, max_area_point_value);
            a = next_a;
        }

        setDataSetLine(sampled, sampled_index++, timestamps[inputLength - 1], values[inputLength - 1]);
        return sampled;
    }

}
