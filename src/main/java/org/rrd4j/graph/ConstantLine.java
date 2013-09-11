package org.rrd4j.graph;

import java.awt.Paint;

import org.rrd4j.backend.spi.AddedRobinTimeSet;
import org.rrd4j.backend.spi.RobinTimeSet;
import org.rrd4j.data.DataProcessor;

public class ConstantLine extends Line {
    private final double value;

    ConstantLine(double value, Paint color, float width, SourcedPlotElement parent) {
        super(Double.toString(value), color, width, parent);
        this.value = value;
    }

    void assignValues(DataProcessor dproc) {
        final RobinTimeSet riParent = parent.getIterator();
        iter = new AddedRobinTimeSet(value, riParent);
    }

}
