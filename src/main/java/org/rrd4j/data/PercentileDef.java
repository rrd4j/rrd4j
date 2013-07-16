package org.rrd4j.data;

class PercentileDef extends SDef {
    double percentile;
    String defName;

    PercentileDef(String name, String defName, double percentile) {
        super(name, defName, AggregateFun.FIRST);
        this.percentile = percentile;
        this.defName = defName;
    }
    
    /** {@inheritDoc} */
    public void calculate(long tStart, long tEnd, DataProcessor dataProcessor) {
        Source source = dataProcessor.getSource(defName);
        setValue(source.getPercentile(tStart, tEnd, percentile));
    }

}

