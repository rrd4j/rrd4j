package org.rrd4j.data;

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

    void setValues(double[] values) {
        this.values = values;
    }

    void setTimestamps(long[] timestamps) {
        this.timestamps = timestamps;
    }

    double[] getValues() {
        return values;
    }

    long[] getTimestamps() {
        return timestamps;
    }

    Aggregates getAggregates(long tStart, long tEnd) {
        Aggregator agg = new Aggregator(timestamps, values);
        return agg.getAggregates(tStart, tEnd);
    }

    double getPercentile(long tStart, long tEnd, double percentile) {
        Aggregator agg = new Aggregator(timestamps, values);
        return agg.getPercentile(tStart, tEnd, percentile);
    }
}