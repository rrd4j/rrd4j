package org.rrd4j.graph;

import org.rrd4j.core.XmlWriter;
import org.rrd4j.data.Variable;
import org.rrd4j.data.DataProcessor;

class VDef extends Source {
    private final String defName;
    private final Variable var;

    VDef(String name, String defName, Variable var) {
        super(name);
        this.defName = defName;
        this.var = var;
    }

    void requestData(DataProcessor dproc) {
        dproc.addDatasource(name, defName, var);
    }

    @Override
    void dotemplate(XmlWriter xml) {
        xml.startTag("sdef");
        xml.writeTag("name", this.name);
        xml.writeTag("source", defName);
        xml.writeTag("function", var.getClass().getCanonicalName());
        xml.closeTag();
    }

}
