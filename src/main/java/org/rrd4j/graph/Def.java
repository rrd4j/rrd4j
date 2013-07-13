package org.rrd4j.graph;

import org.rrd4j.data.DataProcessor;
import org.rrd4j.ConsolFun;

class Def extends Source {
    private final String rrdPath, dsName, backend;
    private final ConsolFun consolFun;

    Def(String name, String rrdPath, String dsName, ConsolFun consolFun) {
        this(name, rrdPath, dsName, consolFun, null);
    }

    Def(String name, String rrdPath, String dsName, ConsolFun consolFun, String backend) {
        super(name);
        this.rrdPath = rrdPath;
        this.dsName = dsName;
        this.consolFun = consolFun;
        this.backend = backend;
    }

    void requestData(DataProcessor dproc) {
        if (backend == null) {
            dproc.addDatasource(name, rrdPath, dsName, consolFun);
        }
        else {
            dproc.addDatasource(name, rrdPath, dsName, consolFun, backend);
        }
    }
}
