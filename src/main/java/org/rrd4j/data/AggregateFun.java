package org.rrd4j.data;

public enum AggregateFun {
    /**
     * The average of the data points.
     */
    AVERAGE,

    /**
     * The smallest of the data points.
     */
    MIN,

    /**
     * The largest of the data points.
     */
    MAX,

    /**
     * The last data point is used.
     */
    LAST,

    /**
     * The fist data point is used.
     */
    FIRST,

    /**
     * The total of the data points.
     */
    TOTAL,

    /**
     * The standard deviation.
     */
    STDEV,

    /**
     * The least squares line, slope.
     */
    LSLSLOPE,

    /**
     * The least squares line, y-intercept.
     */
    LSLINT,

    /**
     * The least squares line, correlation coefficient.
     */
    LSLCORREL
}
