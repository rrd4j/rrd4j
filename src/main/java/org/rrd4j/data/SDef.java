package org.rrd4j.data;

import org.rrd4j.ConsolFun;

class SDef extends Source implements NonRrdSource  {
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

    public void calculate(long tStart, long tEnd, DataProcessor dataProcessor) {
        String defName = getDefName();
        ConsolFun consolFun = getConsolFun();
        Source source = dataProcessor.getSource(defName);
        double value = source.getAggregates(tStart, tEnd).getAggregate(consolFun);
        setValue(value);
    }

}
