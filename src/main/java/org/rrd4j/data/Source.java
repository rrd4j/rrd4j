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

    @Deprecated
    Aggregates getAggregates(long tStart, long tEnd) {
        Aggregator agg = new Aggregator(timestamps, values);
        return agg.getAggregates(tStart, tEnd);
    }

    @Deprecated
    double getPercentile(long tStart, long tEnd, double percentile) {
        Variable vpercent = new Variable.PERCENTILE((float) percentile);
        vpercent.calculate(this, tStart, tEnd);
        return vpercent.getValue().value;
    }

}
