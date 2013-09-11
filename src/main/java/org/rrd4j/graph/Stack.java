package org.rrd4j.graph;

import java.awt.Paint;

import org.rrd4j.backend.spi.StackedRobinTimeSet;
import org.rrd4j.data.DataProcessor;

class Stack extends SourcedPlotElement {
    private final SourcedPlotElement parent;

    Stack(SourcedPlotElement parent, String srcName, Paint color) {
        super(srcName, color);
        this.parent = parent;
    }

    void assignValues(DataProcessor dproc) {
        iter = new StackedRobinTimeSet(dproc.getIterator(srcName), parent.getIterator());
    }

    float getParentLineWidth() {
        if (parent instanceof Line) {
            return ((Line) parent).width;
        }
        else if (parent instanceof Area) {
            return -1F;
        }
        else /* if(parent instanceof Stack) */ {
            return ((Stack) parent).getParentLineWidth();
        }
    }

    Paint getParentColor() {
        return parent.color;
    }
}
