package org.rrd4j.graph;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdNioBackendFactory;

public class ImageTest {
    private static final String rrdpath = ImageFormatTest.class.getResource("/demo1.rrd").getFile(); 
    static private RrdBackendFactory previousBackend;

    @BeforeClass
    public static void setBackendBefore() {
        previousBackend = RrdBackendFactory.getDefaultFactory();
        RrdBackendFactory.setActiveFactories(new RrdNioBackendFactory(0));
    }

    @AfterClass
    public static void setBackendAfter() {
        RrdBackendFactory.setActiveFactories(previousBackend);
    }

    @Ignore
    @Test
    public void testOne() throws IOException {
        RrdGraphDef gDef = new RrdGraphDef(1, 100);
        gDef.setImageFormat("png");
        gDef.setWidth(200);
        gDef.setHeight(200);
        gDef.datasource("sun", rrdpath, "sun", ConsolFun.AVERAGE);
        gDef.line("sun", Color.BLACK, "sun temp");
        gDef.setStartTime(1);
        gDef.setEndTime(100000);

        gDef.setBackgroundImage(getClass().getClassLoader().getResource("top.png"));
        gDef.setOverlayImage(getClass().getClassLoader().getResource("bottom.png").getFile());
        gDef.setCanvasImage(getClass().getClassLoader().getResource("canvas.png").getFile());
        gDef.setColor(ElementsNames.canvas, new Color(0, 0, 0, 0));
        gDef.setAntiAliasing(false);

        RrdGraph graph = new RrdGraph(gDef);
        BufferedImage wpImage = ImageIO.read(new ByteArrayInputStream(graph.getRrdGraphInfo().getBytes()));
        BufferedImage refImage = ImageIO.read(getClass().getClassLoader().getResource("ref.png"));
        for (int x=0 ; x < wpImage.getWidth() ; x++) {
            for (int y=0 ; y< wpImage.getHeight() ; y++) {
                Assert.assertEquals(new Color(refImage.getRGB(x, y)), new Color(wpImage.getRGB(x, y)));
            }
        }
    }

}
