package org.rrd4j.graph;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ImageWriterTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testJPEG() throws IOException {
        ImageWorker iw = new ImageWorker(100, 100);
        int count = iw.saveImage(testFolder.newFile("test.jpeg").getCanonicalPath(), "jpeg", 1.0f, true).length;
        Assert.assertTrue(count > 0);
    }

    @Test
    public void testGif() throws IOException {
        ImageWorker iw = new ImageWorker(100, 100);
        // Ensure there is a least one, or the the GIF encoder can't create a palette
        iw.fillRect(0, 0, 100, 100, java.awt.Color.black);
        int count = iw.saveImage(testFolder.newFile("test.gif").getCanonicalPath(), "gif", 1.0f, true).length;
        Assert.assertTrue(count > 0);
    }

    @Test
    public void testPng() throws IOException {
        ImageWorker iw = new ImageWorker(100, 100);
        int count = iw.saveImage(testFolder.newFile("test.png").getCanonicalPath(), "png", 1.0f, true).length;
        Assert.assertTrue(count > 0);
    }

    @Test
    public void testBmp() throws IOException {
        ImageWorker iw = new ImageWorker(100, 100);
        int count = iw.saveImage(testFolder.newFile("test.bmp").getCanonicalPath(), "bmp", 1.0f, true).length;
        Assert.assertTrue(count > 0);
    }

    @Test(expected=RuntimeException.class)
    public void testWBmp() throws IOException {
        ImageWorker iw = new ImageWorker(100, 100);
        int count = iw.saveImage(testFolder.newFile("test.wbmp").getCanonicalPath(), "wbmp", 1.0f, true).length;
        Assert.assertTrue(count == 0);
    }

}

