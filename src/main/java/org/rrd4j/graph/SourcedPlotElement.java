package org.rrd4j.graph;

import java.awt.Paint;
import java.io.IOException;

import org.rrd4j.backend.spi.RobinTimeSet;
import org.rrd4j.backend.spi.StackedRobinTimeSet;
import org.rrd4j.data.DataProcessor;

class SourcedPlotElement extends PlotElement {
    final String srcName;
    final SourcedPlotElement parent;
    double[] values;
    protected RobinTimeSet iter;

    SourcedPlotElement(String srcName, Paint color) {
        super(color);
        this.srcName = srcName;
        this.parent = null;
    }

    SourcedPlotElement(String srcName, Paint color, SourcedPlotElement parent) {
        super(color);
        this.srcName = srcName;
        this.parent = parent;
    }

    void assignValues(DataProcessor dproc) {
        if(parent == null) {
            iter = dproc.getIterator(srcName);
        }
        else {
            iter = new StackedRobinTimeSet(dproc.getIterator(srcName), parent.getIterator());
        }
    }

    Paint getParentColor() {
        return parent != null ? parent.color : null;
    }

    RobinTimeSet getIterator() {
        return iter;
    }

    double getMinValue() {
        try {
            return iter.getMin();
        } catch (IOException e) {
            return Double.NaN;
        }
    }

    double getMaxValue() {
        try {
            return iter.getMax();
        } catch (IOException e) {
            return Double.NaN;
        }
    }
}
