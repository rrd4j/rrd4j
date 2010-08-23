package org.rrd4j.graph;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;

class ImageWorker {
    private static final String DUMMY_TEXT = "Dummy";

    static final int IMG_BUFFER_CAPACITY = 10000; // bytes

    private BufferedImage img;
    private Graphics2D gd;
    private int imgWidth, imgHeight;
    private AffineTransform aftInitial;

    ImageWorker(int width, int height) {
        resize(width, height);
    }

    void resize(int width, int height) {
        if (gd != null) {
            dispose();
        }
        this.imgWidth = width;
        this.imgHeight = height;
        this.img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.gd = img.createGraphics();
        this.aftInitial = gd.getTransform();
        this.setAntiAliasing(false);
        this.gd.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON); 
        this.gd.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
    }

    void clip(int x, int y, int width, int height) {
        gd.setClip(x, y, width, height);
    }

    void transform(int x, int y, double angle) {
        gd.translate(x, y);
        gd.rotate(angle);
    }

    void reset() {
        gd.setTransform(aftInitial);
        gd.setClip(0, 0, imgWidth, imgHeight);
    }

    void fillRect(int x, int y, int width, int height, Paint paint) {
        gd.setPaint(paint);
        gd.fillRect(x, y, width, height);
    }

    void fillPolygon(int[] x, int[] y, Paint paint) {
        gd.setPaint(paint);
        gd.fillPolygon(x, y, x.length);
    }

    void fillPolygon(double[] x, double yBottom, double[] yTop, Paint paint) {
        gd.setPaint(paint);
        PathIterator path = new PathIterator(yTop);
        for (int[] pos = path.getNextPath(); pos != null; pos = path.getNextPath()) {
            int start = pos[0], end = pos[1], n = end - start;
            int[] xDev = new int[n + 2], yDev = new int[n + 2];
            for (int i = start; i < end; i++) {
                xDev[i - start] = (int) x[i];
                yDev[i - start] = (int) yTop[i];
            }
            xDev[n] = xDev[n - 1];
            xDev[n + 1] = xDev[0];
            yDev[n] = yDev[n + 1] = (int) yBottom;
            gd.fillPolygon(xDev, yDev, xDev.length);
            gd.drawPolygon(xDev, yDev, xDev.length);
        }
    }

    void fillPolygon(double[] x, double[] yBottom, double[] yTop, Paint paint) {
        gd.setPaint(paint);
        PathIterator path = new PathIterator(yTop);
        for (int[] pos = path.getNextPath(); pos != null; pos = path.getNextPath()) {
            int start = pos[0], end = pos[1], n = end - start;
            int[] xDev = new int[n * 2], yDev = new int[n * 2];
            for (int i = start; i < end; i++) {
                int ix1 = i - start, ix2 = n * 2 - 1 - i + start;
                xDev[ix1] = xDev[ix2] = (int) x[i];
                yDev[ix1] = (int) yTop[i];
                yDev[ix2] = (int) yBottom[i];
            }
            gd.fillPolygon(xDev, yDev, xDev.length);
            gd.drawPolygon(xDev, yDev, xDev.length);
        }
    }


    void drawLine(int x1, int y1, int x2, int y2, Paint paint, Stroke stroke) {
        gd.setStroke(stroke);
        gd.setPaint(paint);
        gd.drawLine(x1, y1, x2, y2);
    }

    void drawPolyline(int[] x, int[] y, Paint paint, Stroke stroke) {
        gd.setStroke(stroke);
        gd.setPaint(paint);
        gd.drawPolyline(x, y, x.length);
    }

    void drawPolyline(double[] x, double[] y, Paint paint, Stroke stroke) {
        gd.setPaint(paint);
        gd.setStroke(stroke);
        PathIterator path = new PathIterator(y);
        for (int[] pos = path.getNextPath(); pos != null; pos = path.getNextPath()) {
            int start = pos[0], end = pos[1];
            int[] xDev = new int[end - start], yDev = new int[end - start];
            for (int i = start; i < end; i++) {
                xDev[i - start] = (int) x[i];
                yDev[i - start] = (int) y[i];
            }
            gd.drawPolyline(xDev, yDev, xDev.length);
        }
    }

    void drawString(String text, int x, int y, Font font, Paint paint) {
        gd.setFont(font);
        gd.setPaint(paint);
        gd.drawString(text, x, y);
    }

    double getFontAscent(Font font) {
        LineMetrics lm = font.getLineMetrics(DUMMY_TEXT, gd.getFontRenderContext());
        return lm.getAscent();
    }

    double getFontHeight(Font font) {
        LineMetrics lm = font.getLineMetrics(DUMMY_TEXT, gd.getFontRenderContext());
        return lm.getAscent() + lm.getDescent();
    }

    double getStringWidth(String text, Font font) {
        return font.getStringBounds(text, 0, text.length(), gd.getFontRenderContext()).getBounds().getWidth();
    }

    void setAntiAliasing(boolean enable) {
        gd.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                enable ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    void setTextAntiAliasing(boolean enable) {
        gd.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                enable ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }

    void dispose() {
        gd.dispose();
    }

    void saveImage(OutputStream stream, String type, float quality) throws IOException {
        if (type.equalsIgnoreCase("png")) {
            ImageIO.write(img, "png", stream);
        }
        else if (type.equalsIgnoreCase("gif")) {
            GifEncoder gifEncoder = new GifEncoder(img);
            gifEncoder.encode(stream);
        }
        else if (type.equalsIgnoreCase("jpg") || type.equalsIgnoreCase("jpeg")) {
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(stream);
            JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(img);
            param.setQuality(quality, false);
            encoder.setJPEGEncodeParam(param);
            encoder.encode(img);
        }
        else {
            throw new IOException("Unsupported image format: " + type);
        }
        stream.flush();
    }

    byte[] saveImage(String path, String type, float quality) throws IOException {
        byte[] bytes = getImageBytes(type, quality);
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(path));
            out.write(bytes);
            return bytes;
        }
        finally {
            if (out != null) out.close();
        }
    }

    byte[] getImageBytes(String type, float quality) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(IMG_BUFFER_CAPACITY);
        try {
            saveImage(stream, type, quality);
            return stream.toByteArray();
        }
        finally {
            stream.close();
        }
    }

    public void loadImage(String imageFile) throws IOException {
        BufferedImage wpImage = ImageIO.read(new File(imageFile));
        TexturePaint paint = new TexturePaint(wpImage, new Rectangle(0, 0, wpImage.getWidth(), wpImage.getHeight()));
        gd.setPaint(paint);
        gd.fillRect(0, 0, wpImage.getWidth(), wpImage.getHeight());
    }
}
