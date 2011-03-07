package org.rrd4j.graph;

import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.Plottable;

class VDef extends Source {
    private class Percentile extends Plottable {
        private final double percent;
        private final DataProcessor dproc;
        private double percentile = Double.NaN;

        /**
         * Create the constant plottable
         * @param value the value that will be always returned.
         */
        public Percentile(DataProcessor dproc, double percent) {
            this.dproc = dproc;
            this.percent = percent;
        }

        public double getValue(long timestamp) {
            if(Double.isNaN(percentile))
                percentile = dproc.getPercentile(defName, percent);
            return percentile;
        }

    }

    private final double percent;
    private final String defName;

    VDef(String name, String defName, double percent) {
        super(name);
        this.defName = defName;
        this.percent = percent;
    }

    @Override
    void requestData(DataProcessor dproc) {
        dproc.addDatasource(name, new Percentile(dproc,percent));
    }

}
