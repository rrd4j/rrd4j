package org.rrd4j.graph;

import org.rrd4j.data.AggregateFun;
import org.rrd4j.data.DataProcessor;

class SDef extends Source {
    private String defName;
    private AggregateFun aggregate;

    SDef(String name, String defName, AggregateFun aggregate) {
        super(name);
        this.defName = defName;
        this.aggregate = aggregate;
    }

    void requestData(DataProcessor dproc) {
        dproc.addDatasource(name, defName, aggregate);
    }
}
