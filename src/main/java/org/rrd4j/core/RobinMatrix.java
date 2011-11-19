package org.rrd4j.core;

import java.io.IOException;

/**
 * Class to represent archive values for a single datasource. Robin class is the heart of
 * the so-called "round robin database" concept. Basically, each Robin object is a
 * fixed length array of double values. Each double value reperesents consolidated, archived
 * value for the specific timestamp. When the underlying array of double values gets completely
 * filled, new values will replace the oldest ones.<p>
 * <p/>
 * Robin object does not hold values in memory - such object could be quite large.
 * Instead of it, Robin reads them from the backend I/O only when necessary.
 *
 * @author Fabrice Bacchella
 */
class RobinMatrix implements Robin {
    private final Archive parentArc;
    private final RrdInt pointer;
    private final RrdDoubleMatrix values;
    private int rows;
    private int column;

    RobinMatrix(Archive parentArc, RrdDoubleMatrix values, RrdInt pointer, int column) throws IOException {
        this.parentArc = parentArc;
        this.pointer = pointer; 
        this.values = values;
        this.rows = values.getRows();
        this.column = column;
    }

    /**
     * Fetches all archived values.
     *
     * @return Array of double archive values, starting from the oldest one.
     * @throws IOException Thrown in case of I/O specific error.
     */
    public double[] getValues() throws IOException {
        return getValues(0, rows);
    }

    // stores single value
    public void store(double newValue) throws IOException {
        int position = pointer.get();
        values.set(column, position, newValue);
        pointer.set((position + 1) % rows);
    }

    // stores the same value several times
    public void bulkStore(double newValue, int bulkCount) throws IOException {
        assert bulkCount <= rows: "Invalid number of bulk updates: " + bulkCount + " rows=" + rows;

        int position = pointer.get();

        // update tail
        int tailUpdateCount = Math.min(rows - position, bulkCount);

        values.set(column, position, newValue, tailUpdateCount);
        pointer.set((position + tailUpdateCount) % rows);

        // do we need to update from the start?
        int headUpdateCount = bulkCount - tailUpdateCount;
        if (headUpdateCount > 0) {
            values.set(column, 0, newValue, headUpdateCount);
            pointer.set(headUpdateCount);
        }
    }

    public void update(double[] newValues) throws IOException {
        assert rows == newValues.length: "Invalid number of robin values supplied (" + newValues.length +
        "), exactly " + rows + " needed";
        pointer.set(0);
        values.set(column, 0, newValues);
    }

    /**
     * Updates archived values in bulk.
     *
     * @param newValues Array of double values to be stored in the archive
     * @throws IOException              Thrown in case of I/O error
     * @throws IllegalArgumentException Thrown if the length of the input array is different from the length of
     *                                  this archive
     */
    public void setValues(double... newValues) throws IOException {
        if (rows != newValues.length) {
            throw new IllegalArgumentException("Invalid number of robin values supplied (" + newValues.length +
                    "), exactly " + rows + " needed");
        }
        update(newValues);
    }

    /**
     * (Re)sets all values in this archive to the same value.
     *
     * @param newValue New value
     * @throws IOException Thrown in case of I/O error
     */
    public void setValues(double newValue) throws IOException {
        double[] values = new double[rows];
        for (int i = 0; i < values.length; i++) {
            values[i] = newValue;
        }
        update(values);
    }

    public String dump() throws IOException {
        StringBuilder buffer = new StringBuilder("Robin " + pointer.get() + "/" + rows + ": ");
        double[] values = getValues();
        for (double value : values) {
            buffer.append(Util.formatDouble(value, true)).append(" ");
        }
        buffer.append("\n");
        return buffer.toString();
    }

    /**
     * Returns the i-th value from the Robin archive.
     *
     * @param index Value index
     * @return Value stored in the i-th position (the oldest value has zero index)
     * @throws IOException Thrown in case of I/O specific error.
     */
    public double getValue(int index) throws IOException {
        int arrayIndex = (pointer.get() + index) % rows;
        return values.get(column, arrayIndex);
    }

    /**
     * Sets the i-th value in the Robin archive.
     *
     * @param index index in the archive (the oldest value has zero index)
     * @param value value to be stored
     * @throws IOException Thrown in case of I/O specific error.
     */
    public void setValue(int index, double value) throws IOException {
        int arrayIndex = (pointer.get() + index) % rows;
        values.set(column, arrayIndex, value);
    }

    public double[] getValues(int index, int count) throws IOException {
        assert count <= rows: "Too many values requested: " + count + " rows=" + rows;

        int startIndex = (pointer.get() + index) % rows;
        int tailReadCount = Math.min(rows - startIndex, count);
        double[] tailValues = values.get(column, startIndex, tailReadCount);
        if (tailReadCount < count) {
            int headReadCount = count - tailReadCount;
            double[] headValues = values.get(column, 0, headReadCount);
            double[] values = new double[count];
            int k = 0;
            for (double tailValue : tailValues) {
                values[k++] = tailValue;
            }
            for (double headValue : headValues) {
                values[k++] = headValue;
            }
            return values;
        }
        else {
            return tailValues;
        }
    }

    /**
     * Returns the Archive object to which this Robin object belongs.
     *
     * @return Parent Archive object
     */
    public Archive getParent() {
        return parentArc;
    }

    /**
     * Returns the size of the underlying array of archived values.
     *
     * @return Number of stored values
     */
    public int getSize() {
        return rows;
    }

    /**
     * Copies object's internal state to another Robin object.
     *
     * @param other New Robin object to copy state to
     * @throws IOException Thrown in case of I/O error
     */
    public void copyStateTo(RrdUpdater other) throws IOException {
        if (!(other instanceof Robin)) {
            throw new IllegalArgumentException(
                    "Cannot copy Robin object to " + other.getClass().getName());
        }
        Robin robin = (Robin) other;
        int rowsDiff = rows - robin.getSize();
        for (int i = 0; i < robin.getSize(); i++) {
            int j = i + rowsDiff;
            robin.store(j >= 0 ? getValue(j) : Double.NaN);
        }
    }

    /**
     * Filters values stored in this archive based on the given boundary.
     * Archived values found to be outside of <code>[minValue, maxValue]</code> interval (inclusive)
     * will be silently replaced with <code>NaN</code>.
     *
     * @param minValue lower boundary
     * @param maxValue upper boundary
     * @throws IOException Thrown in case of I/O error
     */
    public void filterValues(double minValue, double maxValue) throws IOException {
        for (int i = 0; i < rows; i++) {
            double value = values.get(column, i);
            if (!Double.isNaN(minValue) && !Double.isNaN(value) && minValue > value) {
                values.set(column, i, Double.NaN);
            }
            if (!Double.isNaN(maxValue) && !Double.isNaN(value) && maxValue < value) {
                values.set(column, i, Double.NaN);
            }
        }
    }

    /**
     * Returns the underlying storage (backend) object which actually performs all
     * I/O operations.
     *
     * @return I/O backend object
     */
    public RrdBackend getRrdBackend() {
        return parentArc.getRrdBackend();
    }

    /**
     * Required to implement RrdUpdater interface. You should never call this method directly.
     *
     * @return Allocator object
     */
    public RrdAllocator getRrdAllocator() {
        return parentArc.getRrdAllocator();
    }
}
