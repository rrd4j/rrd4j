package org.rrd4j.core;

import java.io.IOException;

import org.rrd4j.backend.RrdBackend;

public class Robin implements RrdUpdater {

    private final Archive parentArc;

    private final org.rrd4j.backend.spi.Robin spi;

    Robin(Archive parentArc, int arcIndex, int dsIndex) throws IOException {
        this.parentArc = parentArc;
        spi = parentArc.getRrdBackend().getRobin(arcIndex, dsIndex);
    }

    /**
     * Fetches all archived values.
     *
     * @return Array of double archive values, starting from the oldest one.
     * @throws java.io.IOException Thrown in case of I/O specific error.
     */
    public double[] getValues() throws IOException {
        return spi.getValues();
    }

    /**
     * Updates archived values in bulk.
     *
     * @param newValues Array of double values to be stored in the archive
     * @throws java.io.IOException              Thrown in case of I/O error
     * @throws java.lang.IllegalArgumentException Thrown if the length of the input array is different from the length of
     *                                  this archive
     */
    public void setValues(double... newValues) throws IOException {
        spi.setValues(newValues);
    }

    /**
     * (Re)sets all values in this archive to the same value.
     *
     * @param newValue New value
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public void setValues(double newValue) throws IOException {
        spi.setValues(newValue);
    }

    /**
     * Returns the i-th value from the Robin archive.
     *
     * @param index Value index
     * @return Value stored in the i-th position (the oldest value has zero index)
     * @throws java.io.IOException Thrown in case of I/O specific error.
     */
    public double getValue(int index) throws IOException {
        return spi.getValue(index);
    }

    /**
     * Sets the i-th value in the Robin archive.
     *
     * @param index index in the archive (the oldest value has zero index)
     * @param value value to be stored
     * @throws java.io.IOException Thrown in case of I/O specific error.
     */
    public void setValue(int index, double value) throws IOException {
        spi.setValue(index, value);
    }

    /**
     * <p>update.</p>
     *
     * @param newValues an array of double.
     * @throws java.io.IOException if any.
     */
    public void update(double[] newValues) throws IOException {
        spi.setValues(newValues);
    }

    /**
     * <p>store.</p>
     *
     * @param newValue a double.
     * @throws java.io.IOException if any.
     */
    public void store(double newValue) throws IOException {
        spi.store(newValue);
    }

    /**
     * <p>bulkStore.</p>
     *
     * @param newValue a double.
     * @param bulkCount a int.
     * @throws java.io.IOException if any.
     */
    public void bulkStore(double newValue, int bulkCount) throws IOException {
        spi.bulkStore(newValue, bulkCount);
    }

    /**
     * <p>getValues.</p>
     *
     * @param index a int.
     * @param count a int.
     * @return an array of double.
     * @throws java.io.IOException if any.
     */
    public double[] getValues(int index, int count) throws IOException {
        return spi.getValues(index, count);
    }

    public int getSize() {
        return spi.getSize();
    }

    /**
     * <p>dump.</p>
     *
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public String dump() throws IOException {
        StringBuilder buffer = new StringBuilder("Robin " + "" + "/" + "" + ": ");
        double[] values = getValues();
        for (double value : values) {
            buffer.append(Util.formatDouble(value, true)).append(" ");
        }
        buffer.append("\n");
        return buffer.toString();
    }

    /**
     * Filters values stored in this archive based on the given boundary.
     * Archived values found to be outside of <code>[minValue, maxValue]</code> interval (inclusive)
     * will be silently replaced with <code>NaN</code>.
     * @throws IOException 
     */
    public void filterValues(double minValue, double maxValue) throws IOException {
        spi.filterValues(minValue, maxValue);
    }

    @Override
    public RrdBackend getRrdBackend() {
        return parentArc.getRrdBackend();
    }

    @Override
    public void copyStateTo(RrdUpdater other) throws IOException {
        if (!(other instanceof Robin)) {
            throw new IllegalArgumentException(
                    "Cannot copy Archive object to " + other.getClass().getName());
        }
        org.rrd4j.backend.spi.Robin robin = (org.rrd4j.backend.spi.Robin) other;
        spi.copyStateTo(robin);
    }

}
