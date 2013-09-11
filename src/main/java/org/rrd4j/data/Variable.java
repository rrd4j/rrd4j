package org.rrd4j.data;

import java.util.Arrays;

import org.rrd4j.backend.spi.RobinIterator;
import org.rrd4j.backend.spi.RobinTimeSet;
import org.rrd4j.core.RrdValue;

/**
 *  An abstract class to help extract single value from a set of value (VDEF in rrdtool)
 *  
 *  It can be used to add new fancy statistical calculation with rrd values 
 *
 */
public abstract class Variable {

    /**
     * This class store both the value and the time stamp
     * It will be used by graph rendering legend
     */
    public static final class Value {
        final public double value;
        final public long timestamp;
        Value(long timestamp, double value) {
            this.value = value;
            this.timestamp = timestamp;
        }
    };

    static public final Value INVALIDVALUE = new Value(0, Double.NaN);

    private Value val = null;

    /**
     * Used to calculate the needed value from a source, this method call fill.
     * @param s
     * @param start
     * @param end
     */
    void calculate(Source s, long start, long end) {
        //        long step = s.timestamps[1] - s.timestamps[0];
        //        int first = -1;
        //        int last = -1;
        //        // Iterate over array, stop then end cursor reach start or when both start and end has been found
        //        for(int i = 0, j = s.timestamps.length - 1; j > first && first == -1 || start == -1; i++, j--) {
        //            if(first == -1) {
        //                long leftdown = Math.max(s.timestamps[i] - step, start);
        //                long rightdown = Math.min(s.timestamps[i], end);
        //                if(rightdown > leftdown) {
        //                    first = i;
        //                }                
        //            }
        //            
        //            if(last == -1) {
        //                long leftup = Math.max(s.timestamps[j] - step, start);
        //                long rightup = Math.min(s.timestamps[j], end);
        //                if(rightup - leftup > 0) {
        //                    last = j;
        //                }                
        //            }
        //        }
        //        if( first == -1 || last == -1) {
        //            throw new RuntimeException("Invalid range");
        //        }
        if(s instanceof VDef) {
            // Already a variable, just check if it fits
            Value v = ((VDef) s).getValue();
            // No time stamp, or not time stamped value, keep it
            if(v.timestamp == 0) {
                val = v;
            }
            else {
                if(v.timestamp < end && v.timestamp > start) {
                    val = v;
                }
                else {
                    val = new Value(0, Double.NaN);
                }
            }
        }
        else {
            val = fill(s.getIterator());            
        }
    }

    public Value getValue() {
        assert val != null : "Used before calculation";
        return val;
    }

    /**
     * This method is call with the needed values, extracted from the datasource to do the calculation.
     * 
     * Value is to be filled with both the double value and a possible timestamp, when it's used to find
     * a specific point
     * 
     * @param timestamps the timestamps for the value
     * @param values the actual values
     * @param start the start of the period
     * @param end the end of the period
     * @return a filled Value object
     */
    abstract protected Value fill(RobinTimeSet iter);

    /**
     * Find the first valid data point and it's timestamp
     *
     */
    public static class FIRST extends Variable {
        @Override
        protected Value fill(RobinTimeSet iter) {
            long start = iter.getStart();
            long end = iter.getEnd();
            for(RobinIterator.RobinPoint i: iter ) {
                long ts = i.timestamp;
                if( ts > start && ts < end) {
                    return new Value(ts, i.value);
                }
            }
            return new Value(0, Double.NaN);
        }
    }

    /**
     * Find the first last valid point and it's timestamp
     *
     */
    public static class LAST extends Variable {
        @Override
        protected Value fill(RobinTimeSet iter) {
            long start = iter.getStart();
            long end = iter.getEnd();
            double last = Double.NaN;
            long lastTime =  0;
            for(RobinIterator.RobinPoint i: iter) {
                long ts = i.timestamp;
                double value = i.value;
                if( ts > start && ts < end && !Double.isNaN(ts)) {
                    last = value;
                    lastTime = ts;
                }
            }
            return new Value(lastTime, last);            
        }
    }

    /**
     * The smallest of the data points and it's time stamp (the first one) is stored.
     *
     */
    public static class MIN extends Variable {
        @Override
        protected Value fill(RobinTimeSet iter) {
            long timestamp = 0;
            double value = Double.MAX_VALUE;
            for(RobinIterator.RobinPoint i: iter) {
                long ts = i.timestamp;
                double stepValue = i.value;
                if( !Double.isNaN(stepValue) && value > stepValue) {
                    timestamp = ts;
                    value = stepValue;
                }
            }
            return new Value(timestamp, value);
        }
    }

    /**
     * The biggest of the data points and it's time stamp (the first one) is stored.
     *
     */
    public static  class MAX extends Variable {
        @Override
        protected Value fill(RobinTimeSet iter) {
            long timestamp = 0;
            double value = Double.MIN_VALUE;
            for(RobinIterator.RobinPoint i: iter) {
                long ts = i.timestamp;
                double stepValue = i.value;
                if( !Double.isNaN(stepValue) && value < stepValue) {
                    timestamp = ts;
                    value = stepValue;
                }
            }
            return new Value(timestamp, value);
        }
    }

    /**
     * Calculate the sum of the data points.
     *
     */
    public static  class TOTAL extends Variable {
        @Override
        protected Value fill(RobinIterator i, long start, long end) {
            double value = 0;
            for(int i = values.length - 1 ; i >= 0 ; i--) {
                if( !Double.isNaN(values[i]) ) {
                    value = Double.isNaN(value) ?  values[i] : values[i] + value;
                }
            }
            return new Value(0, value);
        }
    }

    /**
     * Calculate the average of the data points.
     *
     */
    public static class AVERAGE extends Variable {
        @Override
        protected Value fill(RobinIterator i, long start, long end) {
            double value = 0;
            int count = 0;
            for(int i = values.length - 1 ; i >= 0 ; i--) {
                if( !Double.isNaN(values[i]) ) {
                    count++;
                    value = Double.isNaN(value) ?  values[i] : values[i] + value;
                }
            }
            if(! Double.isNaN(value)) {
                value = value / count;
            }
            else {
                value = Double.NaN;
            }
            return new Value(0, value);
        }
    }

    /**
     * Calculate the standard deviation for the data point.
     *
     */
    public static class STDDEV extends Variable {
        @Override
        protected Value fill(RobinIterator i) {
            double value = Double.NaN;
            int count = 0;
            double stdevM = 0.0;
            double stdevS = 0.0;
            for(int i = values.length -1 ; i >=0 ; i--) {
                if( !Double.isNaN(values[i])) {
                    if(count == 1) {
                        stdevM = values[i];
                        stdevS = 0;
                    }
                    // See Knuth TAOCP vol 2, 3rd edition, page 232 and http://www.johndcook.com/standard_deviation.html
                    double ds = values[i] - stdevM;                            
                    stdevM += ds/count;
                    stdevS += stdevS + ds*(values[i] - stdevM);
                    count++;
                }
            }
            if(count > 0) {
                value = Math.sqrt(( (count > 1) ? stdevS/(count - 1) : 0.0 ));
            }
            return new Value(0, value);
        }
    }

    /**
     * Find the point at the n-th percentile.
     *
     */
    public static class PERCENTILE extends Variable {
        private final double percentile;

        public PERCENTILE(double percentile) {
            this.percentile = percentile;
        }

        @Override
        protected Value fill(RobinIterator i, long start, long end) {
            int count = values.length;
            // sort array
            Arrays.sort(values);
            // skip top (100% - percentile) values
            double topPercentile = (100.0 - percentile) / 100.0;
            count -= (int) Math.ceil(count * topPercentile);
            // if we have anything left...
            if (count > 0) {
                double value = values[count - 1 ];
                long timestamp = timestamps[count - 1 ];
                return new Value(timestamp, value);
            }
            return new Value(0, Double.NaN);
        }
    }

    /**
     * Calculate the slop of the least squares line.
     *
     */
    public static class LSLSLOPE extends Variable {

        @Override
        protected Value fill(RobinIterator i, long start, long end) {
            int cnt = 0;
            int lslstep = 0;
            double SUMx = 0.0;
            double SUMy = 0.0;
            double SUMxy = 0.0;
            double SUMxx = 0.0;
            double lslslope;

            for(int i = 0; i < values.length; i++) {
                double value = values[i];

                if (!Double.isNaN(value)) {
                    cnt++;

                    SUMx += lslstep;
                    SUMxx += lslstep * lslstep;
                    SUMy  += value;
                    SUMxy += lslstep * value;

                }
                lslstep++;
            }
            if(cnt > 0) {
                /* Bestfit line by linear least squares method */
                lslslope = (SUMx * SUMy - cnt * SUMxy) / (SUMx * SUMx - cnt * SUMxx);
                return new Value(0, lslslope);

            }
            return new Value(0, Double.NaN);
        }

    }

    /**
     * Calculate the y-intercept of the least squares line.
     *
     */
    public static class LSLINT extends Variable {

        @Override
        protected Value fill(long[] timestamps, double[] values, long start, long end) {
            int cnt = 0;
            int lslstep = 0;
            double SUMx = 0.0;
            double SUMy = 0.0;
            double SUMxy = 0.0;
            double SUMxx = 0.0;
            double lslslope;
            double lslint;

            for(int i = 0; i < values.length; i++) {
                double value = values[i];

                if (!Double.isNaN(value)) {
                    cnt++;

                    SUMx += lslstep;
                    SUMxx += lslstep * lslstep;
                    SUMy  += value;
                    SUMxy += lslstep * value;
                }
                lslstep++;
            }
            if(cnt > 0) {
                /* Bestfit line by linear least squares method */
                lslslope = (SUMx * SUMy - cnt * SUMxy) / (SUMx * SUMx - cnt * SUMxx);
                lslint = (SUMy - lslslope * SUMx) / cnt;
                return new Value(0, lslint);
            }
            return new Value(0, Double.NaN);
        }

    }

    /**
     * Calculate the correlation coefficient of the least squares line.
     *
     */
    public static class LSLCORREL extends Variable {

        @Override
        protected Value fill(long[] timestamps, double[] values, long start, long end) {
            int cnt = 0;
            int lslstep = 0;
            double SUMx = 0.0;
            double SUMy = 0.0;
            double SUMxy = 0.0;
            double SUMxx = 0.0;
            double SUMyy = 0.0;
            double lslcorrel;

            for(int i = 0; i < values.length; i++) {
                double value = values[i];

                if (!Double.isNaN(value)) {
                    cnt++;

                    SUMx += lslstep;
                    SUMxx += lslstep * lslstep;
                    SUMy  += value;
                    SUMxy += lslstep * value;
                    SUMyy += value * value;
                }
                lslstep++;
            }
            if(cnt > 0) {
                /* Bestfit line by linear least squares method */
                lslcorrel =
                        (SUMxy - (SUMx * SUMy) / cnt) /
                        Math.sqrt((SUMxx - (SUMx * SUMx) / cnt) * (SUMyy - (SUMy * SUMy) / cnt));
                return new Value(0, lslcorrel);
            }
            return new Value(0, Double.NaN);
        }

    }
}
