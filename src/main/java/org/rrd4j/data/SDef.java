package org.rrd4j.data;

import org.rrd4j.ConsolFun;

class SDef extends Source {
    private String defName;
    private ConsolFun consolFun;
    private double value;

    SDef(String name, String defName, ConsolFun consolFun) {
        super(name);
        this.defName = defName;
        this.consolFun = consolFun;
    }

    String getDefName() {
        return defName;
    }

    ConsolFun getConsolFun() {
        return consolFun;
    }

    void setValue(double value) {
        this.value = value;
        int count = getTimestamps().length;
        double[] values = new double[count];
        for (int i = 0; i < count; i++) {
            values[i] = value;
        }
        setValues(values);
    }

    Aggregates getAggregates(long tStart, long tEnd) {
        Aggregates agg = new Aggregates();
        agg.first = agg.last = agg.min = agg.max = agg.average = value;
        agg.total = value * (tEnd - tStart);
        return agg;
    }

    double getPercentile(long tStart, long tEnd, double percentile) {
        return value;
    }
}
