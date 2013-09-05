package org.rrd4j.backend.spi.binary;

import java.io.IOException;

import org.rrd4j.backend.spi.Robin;

/**
 * Class to represent archive values for a single datasource. Robin class is the heart of
 * the so-called "round robin database" concept. Basically, each Robin object is a
 * fixed length array of double values. Each double value represents consolidated, archived
 * value for the specific timestamp. When the underlying array of double values gets completely
 * filled, new values will replace the oldest ones.<p>
 * <p/>
 * Robin object does not hold values in memory - such object could be quite large.
 * Instead of it, Robin reads them from the backend I/O only when necessary.
 *
 * @author Sasa Markovic
 */
class RobinArray extends Robin implements Allocated {
    protected final RrdAllocator allocator;
    protected final RrdBinaryBackend backend;

    private final RrdInt pointer;
    private final RrdDoubleArray values;
    private final int rows;

    RobinArray(RrdAllocator allocator, RrdBinaryBackend backend, int rows) throws IOException {
        this.allocator = allocator;
        this.backend = backend;
            
        this.pointer = new RrdInt(this);
        this.values = new RrdDoubleArray(this, rows);
        this.rows = rows;
    }

    /**
     * <p>Getter for the field <code>values</code>.</p>
     *
     * @return an array of double.
     * @throws java.io.IOException if any.
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
        values.set(position, newValue);
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

        values.set(position, newValue, tailUpdateCount);
        pointer.set((position + tailUpdateCount) % rows);

        // do we need to update from the start?
        int headUpdateCount = bulkCount - tailUpdateCount;
        if (headUpdateCount > 0) {
            values.set(0, newValue, headUpdateCount);
            pointer.set(headUpdateCount);
        }
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.Robin#setValues(double)
     */
    /** {@inheritDoc} */
    @Override
    public void setValues(double[] newValues) throws IOException {
        pointer.set(0);
        values.writeDouble(0, newValues);
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.Robin#setValues(double)
     */
    /** {@inheritDoc} */
    @Override
    public void setValues(double newValue) throws IOException {
        double[] values = new double[rows];
        for (int i = 0; i < values.length; i++) {
            values[i] = newValue;
        }
        setValues(values);
    }

    /** {@inheritDoc} */
    @Override
    public double getValue(int index) throws IOException {
        int arrayIndex = (pointer.get() + index) % rows;
        return values.get(arrayIndex);
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.Robin#setValue(int, double)
     */
    /** {@inheritDoc} */
    @Override
    public void setValue(int index, double value) throws IOException {
        int arrayIndex = (pointer.get() + index) % rows;
        values.set(arrayIndex, value);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getValues(int index, int count) throws IOException {
        assert count <= rows: "Too many values requested: " + count + " rows=" + rows;

        int startIndex = (pointer.get() + index) % rows;
        int tailReadCount = Math.min(rows - startIndex, count);
        double[] tailValues = values.get(startIndex, tailReadCount);
        if (tailReadCount < count) {
            int headReadCount = count - tailReadCount;
            double[] headValues = values.get(0, headReadCount);
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

    /* (non-Javadoc)
     * @see org.rrd4j.core.Robin#getSize()
     */
    /**
     * <p>getSize.</p>
     *
     * @return a int.
     */
    @Override
    public int getSize() {
        return rows;
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.Robin#copyStateTo(org.rrd4j.core.RrdUpdater)
     */
    /** {@inheritDoc} */
    @Override
    public void copyStateTo(Robin other) throws IOException {
        Robin robin = (Robin) other;
        int rowsDiff = rows - robin.getSize();
        for (int i = 0; i < robin.getSize(); i++) {
            int j = i + rowsDiff;
            robin.store(j >= 0 ? getValue(j) : Double.NaN);
        }
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.Robin#filterValues(double, double)
     */
    /** {@inheritDoc} */
    @Override
    public void filterValues(double minValue, double maxValue) throws IOException {
        for (int i = 0; i < rows; i++) {
            double value = values.get(i);
            if (!Double.isNaN(minValue) && !Double.isNaN(value) && minValue > value) {
                values.set(i, Double.NaN);
            }
            if (!Double.isNaN(maxValue) && !Double.isNaN(value) && maxValue < value) {
                values.set(i, Double.NaN);
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
     * @return a {@link org.rrd4j.backend.RrdBackend} object.
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
