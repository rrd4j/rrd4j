package org.rrd4j.graph;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.OutputStream;

import org.jfree.graphics2d.svg.SVGGraphics2D;

public class SVGImageWorker extends ImageWorker {
    private SVGGraphics2D g2d;
    private AffineTransform initialAffineTransform;
    private int imgWidth;
    private int imgHeight;

    public SVGImageWorker() {
        resize(1, 1);
    }

    protected void resize(int width, int height) {
        imgWidth = width;
        imgHeight = height;
        g2d = new SVGGraphics2D(imgWidth, imgHeight);
        setG2d(g2d);
        initialAffineTransform = g2d.getTransform();
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    }

    protected void reset(Graphics2D g2d) {
        g2d.setTransform(initialAffineTransform);
        g2d.setClip(0, 0, imgWidth, imgHeight);
    }

    protected void makeImage(OutputStream os) throws IOException {
        os.write(g2d.getSVGDocument().getBytes("UTF-8"));
    }

    /**
     *  Overridden because the SVG format essentially strips leading/trailing spaces,
     *  causing alignment issues in ValueAxis with the %x.y number formatting.
     *  Consecutive spaces within text are also probably collapsed, that is not addressed here.
     */
    @Override
    protected void drawString(String text, int x, int y, Font font, Paint paint) {
        super.drawString(text.trim(), x, y, font, paint);
    }

    /**
     *  Overridden because the SVG format essentially strips leading/trailing spaces,
     *  causing alignment issues in ValueAxis with the %x.y number formatting.
     *  Consecutive spaces within text are also probably collapsed, that is not addressed here.
     */
    @Override
    protected double getStringWidth(String text, Font font) {
        return super.getStringWidth(text.trim(), font);
    }
}
