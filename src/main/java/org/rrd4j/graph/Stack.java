package org.rrd4j.graph;

import org.rrd4j.data.DataProcessor;

import java.awt.*;

class Stack extends SourcedPlotElement {
    private final SourcedPlotElement parent;

    Stack(SourcedPlotElement parent, String srcName, Paint color) {
        super(srcName, color);
        this.parent = parent;
    }

    void assignValues(DataProcessor dproc) {
        double[] parentValues = parent.getValues();
        double[] procValues = dproc.getValues(srcName);
        values = new double[procValues.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = parentValues[i] + procValues[i];
        }
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
