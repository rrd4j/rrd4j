package org.rrd4j.backend.spi.binary;

import java.io.IOException;

import org.rrd4j.backend.spi.Robin;
import org.rrd4j.backend.spi.RobinIterator;
import org.rrd4j.core.Util;

public abstract class RobinBinary extends Robin implements Allocated {

    protected interface RrdDoubleVector {
        void set(int index, double value) throws IOException;
        void set(int index, double value, int count) throws IOException;
        void set(int index, double[] values) throws IOException;
        double get(int index) throws IOException;
        double[] get(int index, int count) throws IOException;
    }

    protected final RrdBinaryBackend backend;

    protected RrdInt pointer;
    protected final int rows;
    protected RrdDoubleVector values;

    RobinBinary(RrdBinaryBackend backend, int rows) throws IOException {
        this.backend = backend;
        this.rows = rows;
    }

    /** {@inheritDoc} */
    @Override
    public void store(double newValue) throws IOException {
        int position = pointer.get();
        values.set(position, newValue);
        pointer.set((position + 1) % rows);
    }

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
     * <p>Getter for the field <code>values</code>.</p>
     *
     * @return an array of double.
     * @throws java.io.IOException if any.
     */
    @Override
    public double[] getValues() throws IOException {
        return getValues(0, rows);
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.Robin#setValues(double)
     */
    /** {@inheritDoc} */
    @Override
    public void setValues(double[] newValue) throws IOException {
        values.set(0, newValue);
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
        RobinBinary robin = (RobinBinary) other;
        int rowsDiff = rows - robin.getSize();
        for (int i = 0; i < robin.getSize(); i++) {
            int j = i + rowsDiff;
            robin.store(j >= 0 ? getValue(j) : Double.NaN);
        }
    }

    /**
     * <p>getRrdAllocator.</p>
     *
     * @return a {@link org.rrd4j.core.RrdAllocator} object.
     */
    @Override
    public RrdAllocator getRrdAllocator() {
        return backend.getRrdAllocator();
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

    @Override
    public RobinIterator getValues(org.rrd4j.core.Archive archive, long tStart, long tEnd) throws IOException {

        final long arcStep = archive.getArcStep();
        final long fetchStart = Util.normalize(tStart, arcStep);
        long fetchEndBuffer = Util.normalize(tEnd, arcStep);
        if (fetchEndBuffer < tEnd) {
            fetchEndBuffer += arcStep;
        }
        final long fetchEnd = fetchEndBuffer;
        long startTime = archive.getStartTime();
        long endTime = archive.getEndTime();
        final int ptsCount = (int) ((fetchEnd - fetchStart) / arcStep + 1);
        long matchStartTime = Math.max(fetchStart, startTime);
        long matchEndTime = Math.min(fetchEnd, endTime);
        final int matchCount = matchStartTime <= matchEndTime ? (int) ((matchEndTime - matchStartTime) / arcStep + 1) : 0;
        final int matchStartIndex = matchStartTime <= matchEndTime ?  (int) ((matchStartTime - startTime) / arcStep) : 0;

        return new RobinIterator(archive, tStart, tEnd) {
            int pos = 0;
            @Override
            protected void prepareQuery(org.rrd4j.core.Archive archive, long tStart, long tEnd) {
            }

            @Override
            public boolean hasNext() {
                return pos < ptsCount;
            }

            @Override
            protected void doNext() throws IOException {
                pos++;
            }

            @Override
            protected double readValue() throws IOException {
                if(pos >= matchStartIndex && pos < (matchCount + matchStartIndex)) {
                    return RobinBinary.this.getValue(pos + matchStartIndex );                    
                }
                else {
                    return Double.NaN;
                }
            }

            @Override
            protected long readTimestamp() throws IOException {
                return fetchStart + pos * arcStep;
            }

            @Override
            public int count() {
                return ptsCount;
            }

        };
    }

}
