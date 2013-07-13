package org.rrd4j.graph;

import java.awt.*;

class Rule extends PlotElement {
    final LegendText legend;
    final float width;

    Rule(Paint color, LegendText legend, float width) {
        super(color);
        this.legend = legend;
        this.width = width;
    }
}
