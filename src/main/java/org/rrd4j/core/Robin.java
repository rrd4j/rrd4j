package org.rrd4j.core;

import java.io.IOException;

public interface Robin extends RrdUpdater {
    
    /**
     * Fetches all archived values.
     *
     * @return Array of double archive values, starting from the oldest one.
     * @throws java.io.IOException Thrown in case of I/O specific error.
     */
    public double[] getValues() throws IOException;

    /**
     * Updates archived values in bulk.
     *
     * @param newValues Array of double values to be stored in the archive
     * @throws java.io.IOException              Thrown in case of I/O error
     * @throws java.lang.IllegalArgumentException Thrown if the length of the input array is different from the length of
     *                                  this archive
     */
    public void setValues(double... newValues) throws IOException;

    /**
     * (Re)sets all values in this archive to the same value.
     *
     * @param newValue New value
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public void setValues(double newValue) throws IOException;

    /**
     * Returns the i-th value from the Robin archive.
     *
     * @param index Value index
     * @return Value stored in the i-th position (the oldest value has zero index)
     * @throws java.io.IOException Thrown in case of I/O specific error.
     */
    public double getValue(int index) throws IOException;

    /**
     * Sets the i-th value in the Robin archive.
     *
     * @param index index in the archive (the oldest value has zero index)
     * @param value value to be stored
     * @throws java.io.IOException Thrown in case of I/O specific error.
     */
    public void setValue(int index, double value) throws IOException;

    /**
     * Returns the size of the underlying array of archived values.
     *
     * @return Number of stored values
     */
    public int getSize();

    /**
     * <p>update.</p>
     *
     * @param newValues an array of double.
     * @throws java.io.IOException if any.
     */
    public void update(double[] newValues) throws IOException;

    /**
     * <p>store.</p>
     *
     * @param newValue a double.
     * @throws java.io.IOException if any.
     */
    public void store(double newValue) throws IOException;

    /**
     * <p>bulkStore.</p>
     *
     * @param newValue a double.
     * @param bulkCount a int.
     * @throws java.io.IOException if any.
     */
    public void bulkStore(double newValue, int bulkCount) throws IOException;

    /**
     * <p>getValues.</p>
     *
     * @param index a int.
     * @param count a int.
     * @return an array of double.
     * @throws java.io.IOException if any.
     */
    public double[] getValues(int index, int count) throws IOException;
    
    /**
     * <p>dump.</p>
     *
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public String dump() throws IOException;

    /**
     * Filters values stored in this archive based on the given boundary.
     * Archived values found to be outside of <code>[minValue, maxValue]</code> interval (inclusive)
     * will be silently replaced with <code>NaN</code>.
     * @throws IOException 
     */
    public void filterValues(double minValue, double nan) throws IOException;

}
