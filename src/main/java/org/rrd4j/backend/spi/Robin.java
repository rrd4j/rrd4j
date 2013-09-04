package org.rrd4j.backend.spi;

import java.io.IOException;

public abstract class Robin implements Updater {
    
    public abstract void store(double newValue) throws IOException;
    
    public abstract void bulkStore(double newValue, int bulkCount) throws IOException;
    
    /**
     * Fetches all archived values.
     *
     * @return Array of double archive values, starting from the oldest one.
     * @throws java.io.IOException Thrown in case of I/O specific error.
     */
    public abstract double[] getValues() throws IOException;
    
    public abstract double[] getValues(int index, int count) throws IOException;


    /**
     * Returns the i-th value from the Robin archive.
     *
     * @param index Value index
     * @return Value stored in the i-th position (the oldest value has zero index)
     * @throws java.io.IOException Thrown in case of I/O specific error.
     */
    public abstract double getValue(int index) throws IOException;

    /**
     * Sets the i-th value in the Robin archive.
     *
     * @param index index in the archive (the oldest value has zero index)
     * @param value value to be stored
     * @throws java.io.IOException Thrown in case of I/O specific error.
     */
    public abstract void setValue(int index, double value) throws IOException;

    public abstract void setValues(double value) throws IOException;

    public abstract void setValues(double[] values) throws IOException;

    /**
     * Returns the size of the underlying array of archived values.
     *
     * @return Number of stored values
     */
    public abstract int getSize();

    /**
     * Filters values stored in this archive based on the given boundary.
     * Archived values found to be outside of <code>[minValue, maxValue]</code> interval (inclusive)
     * will be silently replaced with <code>NaN</code>.
     * @throws IOException 
     */
    public abstract void filterValues(double minValue, double nan) throws IOException;
    
    public abstract void copyStateTo(Robin other) throws IOException;

}
