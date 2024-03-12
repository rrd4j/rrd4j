package org.rrd4j.graph;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageInputStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ImageWriterTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testJPEG() throws IOException {
        run(testFolder.newFile("test.jpeg"), "jpeg");
    }

    @Test
    public void testGif() throws IOException {
        run(testFolder.newFile("test.gif"), "gif");
    }

    @Test
    public void testPng() throws IOException {
        run(testFolder.newFile("test.png"), "png");
    }

    @Test
    public void testBmp() throws IOException {
        run(testFolder.newFile("test.bmp"), "bmp");
    }

    @Test(expected=RuntimeException.class)
    public void testWBmp() throws IOException {
        run(testFolder.newFile("test.wbmp"), "wbmp");
    }

    private void run(File destination, String type) throws IOException {
        // The first writer is arbitrary chosen
        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(type);
        ImageWriter writer = iter.next();
        ImageWriteParam iwp = writer.getDefaultWriteParam();
        BufferedImageWorker iw = BufferedImageWorker.getBuilder().setHeight(100).setWidth(100).setImageWriteParam(iwp).setWriter(writer).build();
        iw.saveImage(destination.getCanonicalPath());
        writer.dispose();
        Assert.assertTrue(destination.exists());
        ImageReader reader = ImageIO.getImageReader(writer);
        reader.setInput(new FileImageInputStream(destination));
        BufferedImage img = reader.read(0);
        Assert.assertEquals(100, img.getWidth());
        Assert.assertEquals(100, img.getHeight());
    }

}
