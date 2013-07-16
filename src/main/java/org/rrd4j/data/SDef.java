package org.rrd4j.data;

class SDef extends Source implements NonRrdSource  {
    private String defName;
    private AggregateFun aggregate;
    private double value;

    SDef(String name, String defName, AggregateFun aggregate) {
        super(name);
        this.defName = defName;
        this.aggregate = aggregate;
    }

    String getDefName() {
        return defName;
    }

    AggregateFun getAggregateFun() {
        return aggregate;
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

    /** {@inheritDoc} */
    public void calculate(long tStart, long tEnd, DataProcessor dataProcessor) {
        String defName = getDefName();
        AggregateFun aggregate = getAggregateFun();
        Source source = dataProcessor.getSource(defName);
        double value = source.getAggregates(tStart, tEnd).getAggregate(aggregate);
        setValue(value);
    }

}
