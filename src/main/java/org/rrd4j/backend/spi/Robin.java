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

    public class RobinIterable {

    }

    public static class RobinElem {
        protected double value;
        protected long timestamp;
        RobinElem() {}
        public double getValue() {
            return value;
        }
        public double getTimestamp() {
            return timestamp;
        }
    }

    public abstract class Iterator implements Iterable<RobinElem>, java.util.Iterator<RobinElem> {
        private final RobinElem proxy = new RobinElem();
        
        public Iterator(org.rrd4j.core.Archive archive, long tStart, long tEnd)  throws IOException {
            prepareQuery(archive, tStart, tEnd);
        }

        abstract protected void prepareQuery(org.rrd4j.core.Archive archive, long tStart, long tEnd)  throws IOException;

        @Override
        public java.util.Iterator<RobinElem> iterator() {
            return this;
        }

        @Override
        public abstract boolean hasNext();

        @Override
        public RobinElem next() {
            try {
                doNext();
                proxy.value = readValue();
                proxy.timestamp = readTimestamp();
            } catch (IOException e) {
                throw new RuntimeException("iteration failed", e);
            }
            return proxy;
        }

        protected abstract void doNext() throws IOException;
        protected abstract double readValue() throws IOException;
        protected abstract long readTimestamp() throws IOException;

        @Override
        public void remove() {
            throw new UnsupportedOperationException("can't remove data from a rrd");
        }
    }

    public abstract Iterator getValues(org.rrd4j.core.Archive archive, long tStart, long tEnd) throws IOException;
    
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
