package org.rrd4j.graph;

import java.awt.*;

class VRule extends Rule {
    final long timestamp;

    VRule(long timestamp, Paint color, LegendText legend, float width) {
        super(color, legend, width);
        this.timestamp = timestamp;
    }

    void setLegendVisibility(long minval, long maxval, boolean forceLegend) {
        legend.enabled &= (forceLegend || (timestamp >= minval && timestamp <= maxval));
    }
}
