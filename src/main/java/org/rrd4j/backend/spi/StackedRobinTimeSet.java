package org.rrd4j.backend.spi;

import java.io.IOException;

import org.rrd4j.backend.spi.RobinIterator.RobinPoint;
import org.rrd4j.core.Archive;
import org.rrd4j.core.Util;

public class StackedRobinTimeSet extends RobinTimeSet  {
    
    private class StackedRobinIterator extends RobinIterator {
        protected final RobinIterator ri1;
        protected final RobinIterator ri2;
        private RobinPoint re1;
        private RobinPoint re2;
        
       protected StackedRobinIterator() throws IOException {
            super(StackedRobinTimeSet.this);
            this.ri1 = StackedRobinTimeSet.this.timeset1.values();
            this.ri2 = StackedRobinTimeSet.this.timeset2.values();
        }
        
        @Override
        public boolean hasNext() {
            return ri1.hasNext() && ri2.hasNext();
        }

        @Override
        protected void doNext() throws IOException {
            re1 = ri1.next();
            re2 = ri2.next();
        }

        @Override
        protected double readValue() throws IOException {
            double val1 = re1.value;
            double val2 = re2.value;
            double sum;
            if (Double.isNaN(val2)) {
                sum = val2;
            }
            else if (Double.isNaN(val1)){
                sum = val2;
            }
            else {
                sum = val1 + val2;
            }

            return sum; 
        }

        @Override
        protected long readTimestamp() throws IOException {
            return re1.timestamp;
        }

    }

    private final RobinTimeSet timeset1;
    private final RobinTimeSet timeset2;

    private double max = Double.MIN_VALUE;
    private double min = Double.MAX_VALUE;

    public StackedRobinTimeSet(RobinTimeSet timeset1, RobinTimeSet timeset2) {
        super(timeset1.getStart(), timeset1.getEnd());
        this.timeset1 = timeset1;
        this.timeset2 = timeset2;
    }

    @Override
    protected RobinIterator prepareQuery(Archive archive, long tStart, long tEnd)
            throws IOException {
        return new StackedRobinIterator();
    }

    @Override
    public double getMax() {
        if(max == Double.MIN_VALUE && min == Double.MAX_VALUE)
            for(RobinPoint i: this) {
                max = Math.max(i.value, max);
                min = Math.max(i.value, min);
            }
        return max;
    }

    @Override
    public double getMin() {
        if(max == Double.MIN_VALUE && min == Double.MAX_VALUE)
            for(RobinPoint i: this) {
                max = Util.max(i.value, max);
                min = Util.min(i.value, min);
            }
        return min;
    }

    @Override
    public int count() throws IOException {
        return timeset1.count();
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

}
