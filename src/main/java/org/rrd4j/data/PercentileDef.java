package org.rrd4j.data;
import org.rrd4j.ConsolFun;

class PercentileDef extends SDef {
    double percentile;
    String defName;

    PercentileDef(String name, String defName, double percentile) {
        super(name, defName, ConsolFun.FIRST);
        this.percentile = percentile;
        this.defName = defName;
    }
    
    public void calculate(long tStart, long tEnd, DataProcessor dataProcessor) {
        Source source = dataProcessor.getSource(defName);
        setValue(source.getPercentile(tStart, tEnd, percentile));
    }

}

