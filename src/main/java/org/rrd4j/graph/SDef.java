package org.rrd4j.graph;

import org.rrd4j.data.DataProcessor;
import org.rrd4j.ConsolFun;

class SDef extends Source {
    private String defName;
    private ConsolFun consolFun;

    SDef(String name, String defName, ConsolFun consolFun) {
        super(name);
        this.defName = defName;
        this.consolFun = consolFun;
    }

    void requestData(DataProcessor dproc) {
        dproc.addDatasource(name, defName, consolFun);
    }
}
