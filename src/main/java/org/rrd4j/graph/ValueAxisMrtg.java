package org.rrd4j.graph;

import org.rrd4j.core.Util;

import java.awt.*;

class ValueAxisMrtg implements RrdGraphConstants {
    private ImageParameters im;
    private ImageWorker worker;
    private RrdGraphDef gdef;

    ValueAxisMrtg(RrdGraph rrdGraph) {
        this.im = rrdGraph.im;
        this.gdef = rrdGraph.gdef;
        this.worker = rrdGraph.worker;
        im.unit = gdef.unit;
    }

    boolean draw() {
        Font font = gdef.smallFont;
        Paint mGridColor = gdef.colors[COLOR_MGRID];
        Paint fontColor = gdef.colors[COLOR_FONT];
        int labelOffset = (int) (worker.getFontAscent(font) / 2);

        if (Double.isNaN((im.maxval - im.minval) / im.magfact)) {
            return false;
        }

        int xLeft = im.xorigin;
        int xRight = im.xorigin + im.xsize;
        String labfmt;
        if (im.scaledstep / im.magfact * Math.max(Math.abs(im.quadrant), Math.abs(4 - im.quadrant)) <= 1.0) {
            labfmt = "%5.2f";
        }
        else {
            labfmt = Util.sprintf(gdef.locale, "%%4.%df", 1 - ((im.scaledstep / im.magfact > 10.0 || Math.ceil(im.scaledstep / im.magfact) == im.scaledstep / im.magfact) ? 1 : 0));
        }
        if (im.symbol != ' ' || im.unit != null) {
            labfmt += " ";
        }
        if (im.symbol != ' ') {
            labfmt += im.symbol;
        }
        if (im.unit != null) {
            labfmt += im.unit;
        }
        for (int i = 0; i <= 4; i++) {
            int y = im.yorigin - im.ysize * i / 4;
            if (y >= im.yorigin - im.ysize && y <= im.yorigin) {
                String graph_label = Util.sprintf(gdef.locale, labfmt, im.scaledstep / im.magfact * (i - im.quadrant));
                int length = (int) (worker.getStringWidth(graph_label, font));
                worker.drawString(graph_label, xLeft - length - PADDING_VLABEL, y + labelOffset, font, fontColor);
                worker.drawLine(xLeft - 2, y, xLeft + 2, y, mGridColor, gdef.tickStroke);
                worker.drawLine(xRight - 2, y, xRight + 2, y, mGridColor, gdef.tickStroke);
                worker.drawLine(xLeft, y, xRight, y, mGridColor, gdef.gridStroke);
            }
        }
        return true;
    }

}
