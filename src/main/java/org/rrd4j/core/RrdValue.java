package org.rrd4j.core;

/**
 * A abstract class that's used to read only access a rrd value
 *
 */
public abstract class RrdValue {
    protected long timestamp;
    protected double value;
    protected RrdValue() {
    }
    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    /**
     * @return the value
     */
    public double getValue() {
        return value;
    }

}
