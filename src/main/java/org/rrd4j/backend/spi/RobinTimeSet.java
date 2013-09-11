package org.rrd4j.backend.spi;

import java.io.IOException;
import java.util.Iterator;

public abstract class RobinTimeSet implements Iterable<RobinIterator.RobinPoint> {

    private final long tStart;
    private final long tEnd;
    private final org.rrd4j.core.Archive archive;
    
    public RobinTimeSet(org.rrd4j.core.Archive archive, long tStart, long tEnd)  throws IOException {
        this.tStart = tStart;
        this.tEnd = tEnd;
        this.archive = archive;
    }

    public RobinTimeSet(long tStart, long tEnd) {
        this.tStart = tStart;
        this.tEnd = tEnd;
        this.archive = null;
    }

    public RobinIterator values() throws IOException {
        return prepareQuery(archive, tStart, tEnd);
    }
    
    protected abstract void doNext(RobinIterator c) throws IOException;
    protected abstract double readValue(RobinIterator c) throws IOException;
    protected abstract long readTimestamp(RobinIterator c) throws IOException;

    public long getStart() {
        return this.tStart;
    }

    public long getEnd() {
        return this.tEnd;
    }

    public abstract double getMax() throws IOException;

    public abstract double getMin() throws IOException;
    
    public abstract int count() throws IOException;
    
    abstract protected RobinIterator prepareQuery(org.rrd4j.core.Archive archive, long tStart, long tEnd) throws IOException;

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<RobinIterator.RobinPoint> iterator() {
        try {
            return prepareQuery(archive, tStart, tEnd);
        } catch (IOException e) {
            throw new RuntimeException("Can't iterate", e);
        }
    }

}
