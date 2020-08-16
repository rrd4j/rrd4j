package org.rrd4j.graph;

import static org.rrd4j.graph.RrdGraphConstants.PROPERTYFONTBOLD;
import static org.rrd4j.graph.RrdGraphConstants.PROPERTYFONTPLAIN;
import static org.rrd4j.graph.RrdGraphConstants.PROPERTYFONTBOLDURL;
import static org.rrd4j.graph.RrdGraphConstants.PROPERTYFONTPLAINURL;
import static org.rrd4j.graph.RrdGraphConstants.PROPERTYFONTSPROPERTIES;
import static org.rrd4j.graph.RrdGraphConstants.PROPERTYFONTSURL;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FontTest {

    @After @Before
    public void clean() {
        System.clearProperty(PROPERTYFONTSPROPERTIES);
        System.clearProperty(PROPERTYFONTSURL);
        System.clearProperty(PROPERTYFONTPLAIN);
        System.clearProperty(PROPERTYFONTBOLD);
    }

    private Font loadFont(String path) throws FontFormatException, IOException {
        try (InputStream fontstream = RrdGraphConstants.class.getResourceAsStream(path)) {
           return Font.createFont(Font.TRUETYPE_FONT, fontstream);
        }
    }

    @Test
    public void testClean() throws FontFormatException, IOException {
        RrdGraphConstants.FontConstructor.refreshConf();
        Font plain = RrdGraphConstants.FontConstructor.getFont(Font.PLAIN, 10);
        Font bold = RrdGraphConstants.FontConstructor.getFont(Font.BOLD, 10);
        Assert.assertEquals(loadFont("/DejaVuSansMono.ttf").deriveFont((float)10), plain);
        Assert.assertEquals(loadFont("/DejaVuSansMono-Bold.ttf").deriveFont((float)10), bold);
    }

    @Test
    public void testPropertyFile() throws FontFormatException, IOException {
        System.setProperty(PROPERTYFONTSPROPERTIES, "/rrd4jtestfonts.properties");
        RrdGraphConstants.FontConstructor.refreshConf();
        Font plain = RrdGraphConstants.FontConstructor.getFont(Font.PLAIN, 10);
        Font bold = RrdGraphConstants.FontConstructor.getFont(Font.BOLD, 10);
        Assert.assertEquals(loadFont("/Cousine/Cousine-Regular.ttf").deriveFont((float)10), plain);
        Assert.assertEquals(loadFont("/Cousine/Cousine-Bold.ttf").deriveFont((float)10), bold);
    }

    @Test
    public void testUrlProperties() throws FontFormatException, IOException {
        System.setProperty(PROPERTYFONTSURL, FontTest.class.getResource("/rrd4jtestfonts.properties").toExternalForm());
        RrdGraphConstants.FontConstructor.refreshConf();
        Font plain = RrdGraphConstants.FontConstructor.getFont(Font.PLAIN, 10);
        Font bold = RrdGraphConstants.FontConstructor.getFont(Font.BOLD, 10);
        Assert.assertEquals(loadFont("/Cousine/Cousine-Regular.ttf").deriveFont((float)10), plain);
        Assert.assertEquals(loadFont("/Cousine/Cousine-Bold.ttf").deriveFont((float)10), bold);
    }

   @Test
    public void testExplicit() throws FontFormatException, IOException {
        System.setProperty(PROPERTYFONTBOLD, "/Cousine/Cousine-Bold.ttf");
        System.setProperty(PROPERTYFONTPLAIN, "/Cousine/Cousine-Regular.ttf");
        RrdGraphConstants.FontConstructor.refreshConf();
        Font plain = RrdGraphConstants.FontConstructor.getFont(Font.PLAIN, 10);
        Font bold = RrdGraphConstants.FontConstructor.getFont(Font.BOLD, 10);
        Assert.assertEquals(loadFont("/Cousine/Cousine-Regular.ttf").deriveFont((float)10), plain);
        Assert.assertEquals(loadFont("/Cousine/Cousine-Bold.ttf").deriveFont((float)10), bold);
    }

   @Test
   public void testExplicitURL() throws FontFormatException, IOException {
       System.setProperty(PROPERTYFONTBOLDURL, FontTest.class.getResource("/Cousine/Cousine-Bold.ttf").toExternalForm());
       System.setProperty(PROPERTYFONTPLAINURL, FontTest.class.getResource("/Cousine/Cousine-Regular.ttf").toExternalForm());
       RrdGraphConstants.FontConstructor.refreshConf();
       Font plain = RrdGraphConstants.FontConstructor.getFont(Font.PLAIN, 10);
       Font bold = RrdGraphConstants.FontConstructor.getFont(Font.BOLD, 10);
       Assert.assertEquals(loadFont("/Cousine/Cousine-Regular.ttf").deriveFont((float)10), plain);
       Assert.assertEquals(loadFont("/Cousine/Cousine-Bold.ttf").deriveFont((float)10), bold);
   }

}
