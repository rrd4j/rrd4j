package org.rrd4j.backend.spi;

import java.io.IOException;
import java.util.Iterator;

import org.rrd4j.core.Archive;
import org.rrd4j.core.RrdValue;

public class AddedRobinTimeSet extends RobinTimeSet {
    private class AddedRobinIterator extends RobinIterator {
        private final RobinIterator ri;

        public AddedRobinIterator() throws IOException {
            super(AddedRobinTimeSet.this);
            ri = AddedRobinTimeSet.this.values();
        }

        @Override
        public boolean hasNext() {
            return ri.hasNext();
        }

        /* (non-Javadoc)
         * @see org.rrd4j.backend.spi.RobinIterator#doNext()
         */
        @Override
        protected void doNext() throws IOException {
            ri.doNext();
        }

        /* (non-Javadoc)
         * @see org.rrd4j.backend.spi.RobinIterator#readValue()
         */
        @Override
        protected double readValue() throws IOException {
            return Double.isNaN(ri.readValue()) ? Double.NaN : ri.readValue() + value;
        }

    }

    private final RobinTimeSet parent;
    private final double value;

    public AddedRobinTimeSet(double value, RobinTimeSet parent) {
        super(parent.getStart(), parent.getEnd());
        this.parent = parent;
        this.value = value;
    }

    @Override
    public double getMax() throws IOException {
        double parentValue = parent.getMax();
        if (Double.isNaN(value)) {
            return Double.NaN;
        }
        else {
            return value + parentValue;
        }
    }

    @Override
    public double getMin() throws IOException {
        double parentValue = parent.getMin();
        if (Double.isNaN(value)) {
            return Double.NaN;
        }
        else {
            return value + parentValue;
        }
    }

    @Override
    public int count() throws IOException {
        return parent.count();
    }

    @Override
    protected void doNext(RobinIterator c) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected double readValue(RobinIterator c) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected long readTimestamp(RobinIterator c) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected RobinIterator prepareQuery(Archive archive, long tStart, long tEnd)
            throws IOException {

        return new AddedRobinIterator();
    }


}
