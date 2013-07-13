package org.rrd4j.graph;

import org.rrd4j.core.Util;

import java.awt.*;

class ValueAxisLogarithmic implements RrdGraphConstants {
    private static final double[][] yloglab = {
        {1e9, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {1e3, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {1e1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {1e1, 1, 2.5, 5, 7.5, 0, 0, 0, 0, 0, 0, 0},
        {1e1, 1, 2, 4, 6, 8, 0, 0, 0, 0, 0, 0},
        {1e1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    private RrdGraph rrdGraph;
    private ImageParameters im;
    private ImageWorker worker;
    private RrdGraphDef gdef;

    ValueAxisLogarithmic(RrdGraph rrdGraph) {
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

        double pixpex = (double) im.ysize / (log10(im.maxval) - log10(im.minval));
        if (Double.isNaN(pixpex)) {
            return false;
        }
        double minstep, pixperstep;
        int minoridx = 0, majoridx = 0;
        for (int i = 0; yloglab[i][0] > 0; i++) {
            minstep = log10(yloglab[i][0]);
            for (int ii = 1; yloglab[i][ii + 1] > 0; ii++) {
                if (yloglab[i][ii + 2] == 0) {
                    minstep = log10(yloglab[i][ii + 1]) - log10(yloglab[i][ii]);
                    break;
                }
            }
            pixperstep = pixpex * minstep;
            if (pixperstep > 5) {
                minoridx = i;
            }
            if (pixperstep > 2 * fontHeight) {
                majoridx = i;
            }
        }

        // Draw minor grid for positive values
        double positiveMin = (im.minval > 0.0) ? im.minval : 0.0;
        int x0 = im.xorigin, x1 = x0 + im.xsize;
        for (double value = Math.pow(10, log10(positiveMin)
                - log10(positiveMin) % log10(yloglab[minoridx][0]));
                value <= im.maxval;
                value *= yloglab[minoridx][0]) {
            if (value < positiveMin) continue;
            int i = 0;
            while (yloglab[minoridx][++i] > 0) {
                int y = rrdGraph.mapper.ytr(value * yloglab[minoridx][i]);
                if (y <= im.yorigin - im.ysize) {
                    break;
                }
                worker.drawLine(x0 - 1, y, x0 + 1, y, gridColor, gdef.tickStroke);
                worker.drawLine(x1 - 1, y, x1 + 1, y, gridColor, gdef.tickStroke);
                worker.drawLine(x0, y, x1, y, gridColor, gdef.gridStroke);
            }
        }

        // Draw minor grid for negative values
        double negativeMin = -1.0 * ((im.maxval < 0.0) ? im.maxval : 0.0);
        for (double value = Math.pow(10, log10(negativeMin)
                - log10(negativeMin) % log10(yloglab[minoridx][0]));
                value <= -1.0 * im.minval;
                value *= yloglab[minoridx][0]) {
            if (value < negativeMin) continue;
            int i = 0;
            while (yloglab[minoridx][++i] > 0) {
                int y = rrdGraph.mapper.ytr(-1.0 * value * yloglab[minoridx][i]);
                if (y <= im.yorigin - im.ysize) {
                    break;
                }
                worker.drawLine(x0 - 1, y, x0 + 1, y, gridColor, gdef.tickStroke);
                worker.drawLine(x1 - 1, y, x1 + 1, y, gridColor, gdef.tickStroke);
                worker.drawLine(x0, y, x1, y, gridColor, gdef.gridStroke);
            }
        }

        // If it has positive and negative, always have a tick mark at 0
        boolean skipFirst = false;
        if (im.minval < 0.0 && im.maxval > 0.0) {
            skipFirst = true;
            int y = rrdGraph.mapper.ytr(0.0);
            worker.drawLine(x0 - 2, y, x0 + 2, y, mGridColor, gdef.tickStroke);
            worker.drawLine(x1 - 2, y, x1 + 2, y, mGridColor, gdef.tickStroke);
            worker.drawLine(x0, y, x1, y, mGridColor, gdef.gridStroke);
            String graph_label = Util.sprintf(gdef.locale, "%3.0e", 0.0);
            int length = (int) (worker.getStringWidth(graph_label, font));
            worker.drawString(graph_label, x0 - length - PADDING_VLABEL, y + labelOffset, font, fontColor);

        }

        // Draw major grid for positive values
        int iter = 0;
        for (double value = Math.pow(10, log10(positiveMin)
                - (log10(positiveMin) % log10(yloglab[majoridx][0])));
                value <= im.maxval;
                value *= yloglab[majoridx][0]) {
            if (value < positiveMin) {
                continue;
            }
            ++iter;
            if (skipFirst && iter == 1) {
                continue;
            }
            int i = 0;
            while (yloglab[majoridx][++i] > 0) {
                int y = rrdGraph.mapper.ytr(value * yloglab[majoridx][i]);
                if (y <= im.yorigin - im.ysize) {
                    break;
                }
                worker.drawLine(x0 - 2, y, x0 + 2, y, mGridColor, gdef.tickStroke);
                worker.drawLine(x1 - 2, y, x1 + 2, y, mGridColor, gdef.tickStroke);
                worker.drawLine(x0, y, x1, y, mGridColor, gdef.gridStroke);
                String graph_label = Util.sprintf(gdef.locale, "%3.0e", value * yloglab[majoridx][i]);
                int length = (int) (worker.getStringWidth(graph_label, font));
                worker.drawString(graph_label, x0 - length - PADDING_VLABEL, y + labelOffset, font, fontColor);
            }
        }

        // Draw major grid for negative values
        iter = 0;
        for (double value = Math.pow(10, log10(negativeMin)
                - (log10(negativeMin) % log10(yloglab[majoridx][0])));
                value <= -1.0 * im.minval;
                value *= yloglab[majoridx][0]) {
            if (value < negativeMin) {
                continue;
            }
            ++iter;
            if (skipFirst && iter == 1) {
                continue;
            }
            int i = 0;
            while (yloglab[majoridx][++i] > 0) {
                int y = rrdGraph.mapper.ytr(-1.0 * value * yloglab[majoridx][i]);
                if (y <= im.yorigin - im.ysize) {
                    break;
                }
                worker.drawLine(x0 - 2, y, x0 + 2, y, mGridColor, gdef.tickStroke);
                worker.drawLine(x1 - 2, y, x1 + 2, y, mGridColor, gdef.tickStroke);
                worker.drawLine(x0, y, x1, y, mGridColor, gdef.gridStroke);
                String graph_label = Util.sprintf(gdef.locale, "%3.0e", -1.0 * value * yloglab[majoridx][i]);
                int length = (int) (worker.getStringWidth(graph_label, font));
                worker.drawString(graph_label, x0 - length - PADDING_VLABEL, y + labelOffset, font, fontColor);
            }
        }

        return true;
    }

    /**
     * Compute logarithm for the purposes of y-axis. 
     */
    static double log10(double v) {
        if (v > 0.0) {
            return Math.log10(v);
        } else if (v < 0.0) {
            return -1.0 * Math.log10(-1.0 * v);
        } else {
            return 0.0;
        }
    }
}
