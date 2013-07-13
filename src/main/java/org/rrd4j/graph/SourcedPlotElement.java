package org.rrd4j.graph;

import org.rrd4j.core.Util;
import org.rrd4j.data.DataProcessor;

import java.awt.*;

class SourcedPlotElement extends PlotElement {
    final String srcName;
    double[] values;

    SourcedPlotElement(String srcName, Paint color) {
        super(color);
        this.srcName = srcName;
    }

    void assignValues(DataProcessor dproc) {
        values = dproc.getValues(srcName);
    }

    double[] getValues() {
        return values;
    }

    double getMinValue() {
        return Util.min(values);
    }

    double getMaxValue() {
        return Util.max(values);
    }
}
