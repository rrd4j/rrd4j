package org.rrd4j.data;

import org.rrd4j.backend.spi.RobinTimeSet;

abstract class Source {
    private final String name;

    protected double[] values;
    protected long[] timestamps;

    Source(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }

    @Deprecated
    Aggregates getAggregates(long tStart, long tEnd) {
        Aggregator agg = new Aggregator(timestamps, values);
        return agg.getAggregates(tStart, tEnd);
    }

    @Deprecated
    double getPercentile(long tStart, long tEnd, double percentile) {
        Variable vpercent = new Variable.PERCENTILE((float) percentile);
        vpercent.calculate(this, tStart, tEnd);
        return vpercent.getValue().getValue();
    }

    public abstract RobinTimeSet getIterator();

}
