package org.rrd4j.graph;

import java.util.ArrayList;
import java.util.List;

class LegendComposer implements RrdGraphConstants {
    private final RrdGraphDef gdef;
    private final ImageWorker worker;
    private int legX, legY;
    private final int legWidth;

    private double interLegendSpace;
    private double leading;
    private double smallLeading;
    private double boxSpace;

    LegendComposer(RrdGraph rrdGraph, int legX, int legY, int legWidth) {
        this.gdef = rrdGraph.gdef;
        this.worker = rrdGraph.worker;
        this.legX = legX;
        this.legY = legY;
        this.legWidth = legWidth;
        interLegendSpace = rrdGraph.getInterLegendSpace();
        leading = rrdGraph.getLeading();
        smallLeading = rrdGraph.getSmallLeading();
        boxSpace = rrdGraph.getBoxSpace();
    }

    int placeComments() {
        Line line = new Line();
        for (CommentText comment : gdef.comments) {
            if (comment.isValidGraphElement()) {
                if (!line.canAccommodate(comment)) {
                    line.layoutAndAdvance(false);
                    line.clear();
                }
                line.add(comment);
            }
        }
        line.layoutAndAdvance(true);
        worker.dispose();
        return legY;
    }

    class Line {
        private String lastMarker;
        private double width;
        private int spaceCount;
        private boolean noJustification;
        private List<CommentText> comments = new ArrayList<CommentText>();

        Line() {
            clear();
        }

        void clear() {
            lastMarker = "";
            width = 0;
            spaceCount = 0;
            noJustification = false;
            comments.clear();
        }

        boolean canAccommodate(CommentText comment) {
            // always accommodate if empty
            if (comments.size() == 0) {
                return true;
            }
            // cannot accommodate if the last marker was \j, \l, \r, \c, \s
            if (lastMarker.equals(ALIGN_LEFT_MARKER) || lastMarker.equals(ALIGN_LEFTNONL_MARKER) || lastMarker.equals(ALIGN_CENTER_MARKER) ||
                    lastMarker.equals(ALIGN_RIGHT_MARKER) || lastMarker.equals(ALIGN_JUSTIFIED_MARKER) ||
                    lastMarker.equals(VERTICAL_SPACING_MARKER)) {
                return false;
            }
            // cannot accommodate if line would be too long
            double commentWidth = getCommentWidth(comment);
            if (!lastMarker.equals(GLUE_MARKER)) {
                commentWidth += interLegendSpace;
            }
            return width + commentWidth <= legWidth;
        }

        void add(CommentText comment) {
            double commentWidth = getCommentWidth(comment);
            if (comments.size() > 0 && !lastMarker.equals(GLUE_MARKER)) {
                commentWidth += interLegendSpace;
                spaceCount++;
            }
            width += commentWidth;
            lastMarker = comment.marker;
            noJustification |= lastMarker.equals(NO_JUSTIFICATION_MARKER);
            comments.add(comment);
        }

        void layoutAndAdvance(boolean isLastLine) {
            if (comments.size() > 0) {
                if (lastMarker.equals(ALIGN_LEFT_MARKER) || lastMarker.equals(ALIGN_LEFTNONL_MARKER)) {
                    placeComments(legX, interLegendSpace);
                }
                else if (lastMarker.equals(ALIGN_RIGHT_MARKER)) {
                    placeComments(legX + legWidth - width, interLegendSpace);
                }
                else if (lastMarker.equals(ALIGN_CENTER_MARKER)) {
                    placeComments(legX + (legWidth - width) / 2.0, interLegendSpace);
                }
                else if (lastMarker.equals(ALIGN_JUSTIFIED_MARKER)) {
                    // anything to justify?
                    if (spaceCount > 0) {
                        placeComments(legX, (legWidth - width) / spaceCount + interLegendSpace);
                    }
                    else {
                        placeComments(legX, interLegendSpace);
                    }
                }
                else if (lastMarker.equals(VERTICAL_SPACING_MARKER)) {
                    placeComments(legX, interLegendSpace);
                }
                else {
                    // nothing specified, align with respect to '\J'
                    if (noJustification || isLastLine) {
                        placeComments(legX, interLegendSpace);
                    }
                    else {
                        placeComments(legX, (legWidth - width) / spaceCount + interLegendSpace);
                    }
                }
                if (!lastMarker.equals(ALIGN_LEFTNONL_MARKER)) {
                    if (lastMarker.equals(VERTICAL_SPACING_MARKER)) {
                        legY += smallLeading;
                    }
                    else {
                        legY += leading;
                    }
                }
            }
        }

        private double getCommentWidth(CommentText comment) {
            double commentWidth = worker.getStringWidth(comment.resolvedText, gdef.smallFont);
            if (comment instanceof LegendText) {
                commentWidth += boxSpace;
            }
            return commentWidth;
        }

        private void placeComments(double xStart, double space) {
            double x = xStart;
            for (CommentText comment : comments) {
                comment.x = (int) x;
                comment.y = legY;
                x += getCommentWidth(comment);
                if (!comment.marker.equals(GLUE_MARKER)) {
                    x += space;
                }
            }
        }
    }
}