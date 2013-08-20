package org.rrd4j.graph;

import java.awt.*;

class Line extends SourcedPlotElement {
    final float width;

    Line(String srcName, Paint color, float width, SourcedPlotElement parent) {
        super(srcName, color, parent);
        this.width = width;
    }
}
