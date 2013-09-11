package org.rrd4j.backend.spi;

import java.io.IOException;
import java.util.Iterator;

public abstract class RobinIterator implements Iterator<RobinIterator.RobinPoint> {

    public static final class RobinPoint {
        public long timestamp = 0;
        public double value = Double.NaN;
    }

    private final RobinPoint proxy = new RobinPoint();

    private final RobinTimeSet set;

    public RobinIterator(RobinTimeSet set)  throws IOException {
        this.set = set;
    }

    @Override
    public abstract boolean hasNext();

    protected void doNext() throws IOException {
        set.doNext(this);
    }

    protected double readValue() throws IOException {
        return set.readValue(this);
    }

    protected long readTimestamp() throws IOException {
        return set.readTimestamp(this);
    }

    @Override
    public RobinPoint next() {
        try {
            doNext();
            proxy.value = readValue();
            proxy.timestamp = readTimestamp();
        } catch (IOException e) {
            throw new RuntimeException("iteration failed", e);
        }
        return proxy;
    }

    @Override
    public void remove() {
        // TODO Auto-generated method stub

    }

}
