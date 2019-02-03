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
        //The first writer is arbitratry choosen
        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(type);
        ImageWriter writer = iter.next();
        ImageWorker iw = new ImageWorker(100, 100);
        ImageWriteParam iwp = writer.getDefaultWriteParam();
        int count = iw.saveImage(destination.getCanonicalPath(), writer, iwp).available();
        writer.dispose();
        Assert.assertTrue(destination.exists());
        Assert.assertEquals(destination.length(), count);
        ImageReader reader = ImageIO.getImageReader(writer);
        reader.setInput(new FileImageInputStream(destination));
        BufferedImage img = reader.read(0);
        Assert.assertEquals(100, img.getWidth());
        Assert.assertEquals(100, img.getHeight());
    }

}
