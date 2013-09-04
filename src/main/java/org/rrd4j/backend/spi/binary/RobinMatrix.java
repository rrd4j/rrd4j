package org.rrd4j.backend.spi.binary;

import java.io.IOException;

import org.rrd4j.backend.spi.Robin;

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
class RobinMatrix extends Robin implements Allocated {
    protected final RrdAllocator allocator;
    protected final RrdBinaryBackend backend;

    private final RrdInt pointer;
    private final RrdDoubleMatrix values;
    private int rows;
    private int column;

    RobinMatrix(RrdAllocator allocator, RrdBinaryBackend backend, RrdDoubleMatrix values, RrdInt pointer, int column) throws IOException {
        this.allocator = allocator;
        this.backend = backend;

        this.pointer = pointer; 
        this.values = values;
        this.rows = values.getRows();
        this.column = column;
    }

    /**
     * Fetches all archived values.
     *
     * @return Array of double archive values, starting from the oldest one.
     * @throws java.io.IOException Thrown in case of I/O specific error.
     */
    @Override
    public double[] getValues() throws IOException {
        return getValues(0, rows);
    }

    // stores single value
    /** {@inheritDoc} */
    @Override
    public void store(double newValue) throws IOException {
        int position = pointer.get();
        values.set(column, position, newValue);
        pointer.set((position + 1) % rows);
    }

    // stores the same value several times
    /** {@inheritDoc} */
    @Override
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

    /**
     * {@inheritDoc}
     *
     * Updates archived values in bulk.
     */
    @Override
   public void setValues(double[] newValues) throws IOException {
        if (rows != newValues.length) {
            throw new IllegalArgumentException("Invalid number of robin values supplied (" + newValues.length +
                    "), exactly " + rows + " needed");
        }
        pointer.set(0);
        values.set(column, 0, newValues);
    }

    /**
     * {@inheritDoc}
     *
     * (Re)sets all values in this archive to the same value.
     */
    @Override
    public void setValues(double newValue) throws IOException {
        double[] values = new double[rows];
        for (int i = 0; i < values.length; i++) {
            values[i] = newValue;
        }
        setValues(values);
    }

    /**
     * {@inheritDoc}
     *
     * Returns the i-th value from the Robin archive.
     */
    @Override
    public double getValue(int index) throws IOException {
        int arrayIndex = (pointer.get() + index) % rows;
        return values.get(column, arrayIndex);
    }

    /**
     * {@inheritDoc}
     *
     * Sets the i-th value in the Robin archive.
     */
    @Override
    public void setValue(int index, double value) throws IOException {
        int arrayIndex = (pointer.get() + index) % rows;
        values.set(column, arrayIndex, value);
    }

    /** {@inheritDoc} */
    @Override
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
     * Returns the size of the underlying array of archived values.
     *
     * @return Number of stored values
     */
    @Override
    public int getSize() {
        return rows;
    }

    /**
     * {@inheritDoc}
     *
     * Copies object's internal state to another Robin object.
     */
    @Override
    public void copyStateTo(Robin other) throws IOException {
        Robin robin = (Robin) other;
        int rowsDiff = rows - robin.getSize();
        for (int i = 0; i < robin.getSize(); i++) {
            int j = i + rowsDiff;
            robin.store(j >= 0 ? getValue(j) : Double.NaN);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Filters values stored in this archive based on the given boundary.
     * Archived values found to be outside of <code>[minValue, maxValue]</code> interval (inclusive)
     * will be silently replaced with <code>NaN</code>.
     */
    @Override
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
     * <p>getRrdAllocator.</p>
     *
     * @return a {@link org.rrd4j.core.RrdAllocator} object.
     */
    @Override
    public RrdAllocator getRrdAllocator() {
        return allocator;
    }

    /**
     * <p>getRrdBackend.</p>
     *
     * @return a {@link org.rrd4j.core.RrdBackend} object.
     */
    @Override
    public RrdBinaryBackend getRrdBackend() {
        return backend;
    }

    @Override
    public void save() throws IOException {
        
    }

    @Override
    public void load() throws IOException {
    }

}
