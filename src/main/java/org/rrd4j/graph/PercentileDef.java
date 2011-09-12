package org.rrd4j.graph;

import org.rrd4j.data.DataProcessor;

class PercentileDef extends Source {
    private final String defName;
    private final double percent;

    PercentileDef(String name, String defName, double percent) {
        super(name);
        this.defName = defName;
        this.percent = percent;
    }

    @Override
    void requestData(DataProcessor dproc) {
        dproc.addDatasource(name, defName, percent);
    }
}
