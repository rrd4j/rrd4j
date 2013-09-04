package org.rrd4j.backend.spi;

import java.io.IOException;

import org.rrd4j.DsType;

public abstract class Datasource implements Updater {
    public String dsName;
    public DsType dsType;
    public long heartbeat;
    public double minValue, maxValue;

    /**
     * @return the lastValue
     * @throws IOException 
     */
    public abstract double getLastValue() throws IOException;

    /**
     * @param lastValue the lastValue to set
     */
    public abstract void setLastValue(double lastValue) throws IOException;

    /**
     * @return the nanSeconds
     */
    public abstract long getNanSeconds() throws IOException;

    /**
     * @param nanSeconds the nanSeconds to set
     */
    public abstract void setNanSeconds(long nanSeconds) throws IOException;

    /**
     * @return the accumValue
     */
    public abstract double getAccumValue() throws IOException;

    /**
     * @param accumValue the accumValue to set
     */
    public abstract void setAccumValue(double accumValue) throws IOException;

}
