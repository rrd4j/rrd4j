package org.rrd4j.graph;

import org.rrd4j.core.Util;

import java.awt.*;

class ValueAxis implements RrdGraphConstants {
    private static final YLabel[] ylabels = {
            new YLabel(0.1, 1, 2, 5, 10),
            new YLabel(0.2, 1, 5, 10, 20),
            new YLabel(0.5, 1, 2, 4, 10),
            new YLabel(1.0, 1, 2, 5, 10),
            new YLabel(2.0, 1, 5, 10, 20),
            new YLabel(5.0, 1, 2, 4, 10),
            new YLabel(10.0, 1, 2, 5, 10),
            new YLabel(20.0, 1, 5, 10, 20),
            new YLabel(50.0, 1, 2, 4, 10),
            new YLabel(100.0, 1, 2, 5, 10),
            new YLabel(200.0, 1, 5, 10, 20),
            new YLabel(500.0, 1, 2, 4, 10),
            new YLabel(1000.0, 1, 2, 5, 10),
            new YLabel(2000.0, 1, 5, 10, 20),
            new YLabel(5000.0, 1, 2, 4, 10),
            new YLabel(10000.0, 1, 2, 5, 10),
            new YLabel(20000.0, 1, 5, 10, 20),
            new YLabel(50000.0, 1, 2, 4, 10),
            new YLabel(100000.0, 1, 2, 5, 10),
            new YLabel(0.0, 0, 0, 0, 0)
    };

    private RrdGraph rrdGraph;
    private ImageParameters im;
    private ImageWorker worker;
    private RrdGraphDef gdef;

    ValueAxis(RrdGraph rrdGraph) {
        this.rrdGraph = rrdGraph;
        this.im = rrdGraph.im;
        this.gdef = rrdGraph.gdef;
        this.worker = rrdGraph.worker;
    }

    boolean draw() {
        Font font = gdef.smallFont;
        Paint gridColor = gdef.colors[COLOR_GRID];
        Paint mGridColor = gdef.colors[COLOR_MGRID];
        Paint fontColor = gdef.colors[COLOR_FONT];
        int fontHeight = (int) Math.ceil(rrdGraph.getSmallFontHeight());
        int labelOffset = (int) (worker.getFontAscent(font) / 2);
        int labfact = 2, gridind = -1;
        double range = im.maxval - im.minval;
        double scaledrange = range / im.magfact;
        double gridstep;
        if (Double.isNaN(scaledrange)) {
            return false;
        }
        int pixel = 1;
        String labfmt = null;
        if (Double.isNaN(im.ygridstep)) {
            if (gdef.altYGrid) {
                /* find the value with max number of digits. Get number of digits */
                int decimals = (int) Math.ceil(Math.log10(Math.max(Math.abs(im.maxval),
                        Math.abs(im.minval))));
                if (decimals <= 0) /* everything is small. make place for zero */ {
                    decimals = 1;
                }
                int fractionals = (int) Math.floor(Math.log10(range));
                if (fractionals < 0) /* small amplitude. */ {
                    labfmt = Util.sprintf("%%%d.%df", decimals - fractionals + 1, -fractionals + 1);
                }
                else {
                    labfmt = Util.sprintf("%%%d.1f", decimals + 1);
                }
                gridstep = Math.pow(10, fractionals);
                if (gridstep == 0) /* range is one -> 0.1 is reasonable scale */ {
                    gridstep = 0.1;
                }
                /* should have at least 5 lines but no more then 15 */
                if (range / gridstep < 5) {
                    gridstep /= 10;
                }
                if (range / gridstep > 15) {
                    gridstep *= 10;
                }
                if (range / gridstep > 5) {
                    labfact = 1;
                    if (range / gridstep > 8) {
                        labfact = 2;
                    }
                }
                else {
                    gridstep /= 5;
                    labfact = 5;
                }
            }
            else {
                for (int i = 0; ylabels[i].grid > 0; i++) {
                    pixel = (int) (im.ysize / (scaledrange / ylabels[i].grid));
                    if (gridind == -1 && pixel > 5) {
                        gridind = i;
                        break;
                    }
                }
                if (gridind == -1) gridind = 0;

                for (int i = 0; i < 4; i++) {
                    if (pixel * ylabels[gridind].labelFacts[i] >= 2 * fontHeight) {
                        labfact = ylabels[gridind].labelFacts[i];
                        break;
                    }
                }
                gridstep = ylabels[gridind].grid * im.magfact;
            }
        }
        else {
            gridstep = im.ygridstep;
            labfact = im.ylabfact;
        }
        int x0 = im.xorigin, x1 = x0 + im.xsize;
        int sgrid = (int) (im.minval / gridstep - 1);
        int egrid = (int) (im.maxval / gridstep + 1);
        double scaledstep = gridstep / im.magfact;
        for (int i = sgrid; i <= egrid; i++) {
            int y = rrdGraph.mapper.ytr(gridstep * i);
            if (y >= im.yorigin - im.ysize && y <= im.yorigin) {
                if (i % labfact == 0) {
                    String graph_label;
                    if (i == 0 || im.symbol == ' ') {
                        if (scaledstep < 1) {
                            if (i != 0 && gdef.altYGrid) {
                                graph_label = Util.sprintf(labfmt, scaledstep * i);
                            }
                            else {
                                graph_label = Util.sprintf("%4.1f", scaledstep * i);
                            }
                        }
                        else {
                            graph_label = Util.sprintf("%4.0f", scaledstep * i);
                        }
                    }
                    else {
                        if (scaledstep < 1) {
                            graph_label = Util.sprintf("%4.1f %c", scaledstep * i, im.symbol);
                        }
                        else {
                            graph_label = Util.sprintf("%4.0f %c", scaledstep * i, im.symbol);
                        }
                    }
                    int length = (int) (worker.getStringWidth(graph_label, font));
                    worker.drawString(graph_label, x0 - length - PADDING_VLABEL, y + labelOffset, font, fontColor);
                    worker.drawLine(x0 - 2, y, x0 + 2, y, mGridColor, TICK_STROKE);
                    worker.drawLine(x1 - 2, y, x1 + 2, y, mGridColor, TICK_STROKE);
                    worker.drawLine(x0, y, x1, y, mGridColor, GRID_STROKE);
                }
                else if (!(gdef.noMinorGrid)) {
                    worker.drawLine(x0 - 1, y, x0 + 1, y, gridColor, TICK_STROKE);
                    worker.drawLine(x1 - 1, y, x1 + 1, y, gridColor, TICK_STROKE);
                    worker.drawLine(x0, y, x1, y, gridColor, GRID_STROKE);
                }
            }
        }
        return true;
    }

    static class YLabel {
        final double grid;
        final int[] labelFacts;

        YLabel(double grid, int lfac1, int lfac2, int lfac3, int lfac4) {
            this.grid = grid;
            labelFacts = new int[]{lfac1, lfac2, lfac3, lfac4};
        }
    }
}
