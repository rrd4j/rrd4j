package org.rrd4j;

/**
 * Enumeration of available consolidation functions. Note that data aggregation inevitably leads to
 * loss of precision and information. The trick is to pick the aggregate function such that the interesting
 * properties of your data are kept across the aggregation process.
 */
public enum ConsolFun {
    /**
     * The average of the data points is stored.
     */
    AVERAGE,

    /**
     * The smallest of the data points is stored.
     */
    MIN,

    /**
     * The largest of the data points is stored.
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
     * The total of the data points is stored.
     */
    TOTAL
}
