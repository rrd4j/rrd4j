package org.rrd4j.core;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.Color;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class UtilTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testToDoubleArray() {
        Assert.assertArrayEquals(new double[] {10, 20, 30, 40}, Util.toDoubleArray(new long[] {10, 20, 30, 40}), 0);
    }

    @Test
    public void testNormalize() {
        Assert.assertEquals(250, Util.normalize(278, 50));
    }

    @Test
    public void testEqual() {
        Assert.assertTrue(Util.equal(Double.NaN, Double.NaN));
        Assert.assertFalse(Util.equal(Double.NaN, 0.0));
        Assert.assertFalse(Util.equal(1.5, 0));
    }

    @Test
    public void testFileExists() {
        Assert.assertTrue(Util.fileExists("/"));
    }

    @Test
    public void testFormatDouble() {
        Assert.assertEquals("10.0", Util.formatDouble(10.0, false));
        Assert.assertEquals("-10.0", Util.formatDouble(-10.0, false));
        Assert.assertEquals("+1.6100000000E02", Util.formatDouble(161));
        Assert.assertEquals("Infinity", Util.formatDouble(Double.POSITIVE_INFINITY, "NaN", false));
        Assert.assertEquals("Not A Number", Util.formatDouble(Double.NaN, "Not A Number", false));
        Assert.assertEquals(Double.toString(Double.NaN), Util.formatDouble(Double.NaN,false));
    }

    @Test
    public void testChildNodes() throws Exception {
        String testXml = "<sample>" +
                         "   <test>1</test>" +
                         "   <test>2</test>" +
                         "   <test>3</test>" +
                         "   <other>4</other>" +
                         "   <other>5</other>" +
                         "</sample>";
        Element root = Util.Xml.getRootElement(testXml);

        Assert.assertEquals(5, Util.Xml.getChildNodes(root).length);
        Assert.assertEquals(3, Util.Xml.getChildNodes(root, "test").length);
        Assert.assertEquals(2, Util.Xml.getChildNodes(root, "other").length);
        Assert.assertArrayEquals(new Node[] {}, Util.Xml.getChildNodes(root, "non-existent"));

        Assert.assertTrue(Util.Xml.hasChildNode(root, "other"));
        Assert.assertFalse(Util.Xml.hasChildNode(root, "non-existent"));

        Assert.assertEquals("other", Util.Xml.getFirstChildNode(root, "other").getNodeName());
        thrown.expect(IllegalArgumentException.class);
        Util.Xml.getFirstChildNode(root, "non-existent");
    }

    @Test
    public void testGetValue() throws Exception {
        Element intElement = Util.Xml.getRootElement("<sample>-8</sample>");
        Assert.assertEquals(-8, Util.Xml.getValueAsInt(intElement));
        Element doubleElement = Util.Xml.getRootElement("<sample>14.3</sample>");
        Assert.assertEquals(14.3, Util.Xml.getValueAsDouble(doubleElement), 0);
        Element longElement = Util.Xml.getRootElement("<sample>140000</sample>");
        Assert.assertEquals(140_000, Util.Xml.getValueAsLong(longElement));
        Element boolElement = Util.Xml.getRootElement("<sample>yes</sample>");
        Assert.assertTrue(Util.Xml.getValueAsBoolean(boolElement));
    }

    @Test
    public void testGetChildValue() throws Exception {
        String testXml =  "<sample>" +
                          "   <NaN>test</NaN>" +
                          "   <zero>0</zero>" +
                          "   <one>1</one>" +
                          "   <twopointthree>2.3</twopointthree>" +
                          "   <true>on</true>" +
                          "</sample>";
        Element root = Util.Xml.getRootElement(testXml);

        Assert.assertEquals("test", Util.Xml.getChildValue(root, "NaN"));
        Assert.assertEquals(2.3, Util.Xml.getChildValueAsDouble(root, "twopointthree"), 0.0);
        Assert.assertEquals(0, Util.Xml.getChildValueAsInt(root, "zero"));
        Assert.assertEquals(1, Util.Xml.getChildValueAsLong(root, "one"));
        Assert.assertEquals(true, Util.Xml.getChildValueAsBoolean(root, "true"));
    }

    @Test
    public void testGetDate() {
        Date date = Util.getDate(515L);
        Assert.assertNotNull(date);
        Assert.assertEquals(515_000L, date.getTime());
    }

    @Test
    public void testGetCalendar(){
        Calendar calendar1 = Util.getCalendar(100);
        Assert.assertEquals(100_000, calendar1.getTime().getTime());

        Calendar calendar2 = Util.getCalendar(new Date(100));
        Assert.assertEquals(100, calendar2.getTime().getTime());

        Calendar calendar3 = Util.getCalendar("100");
        Assert.assertEquals(100_000, calendar3.getTime().getTime());

        Calendar calendar4 = Util.getCalendar("2019-01-02 03:04:05");
        Assert.assertEquals(1_546_398_245_000L, calendar4.getTime().getTime());
    }

    @Test
    public void testGetTimestamp() {
        Date date = new Date(1501);
        Assert.assertEquals(2, Util.getTimestamp(date));

        Calendar calendar = Util.getCalendar(200);
        Assert.assertEquals(200, Util.getTimestamp(calendar));

        Assert.assertEquals(1_549_076_640L, Util.getTimestamp(2019, 1, 2, 3, 4));

        Assert.assertEquals(1_549_065_600L, Util.getTimestamp(2019, 1, 2));
    }

    @Test
    public void testIsDouble() {
        Assert.assertTrue(Util.isDouble("-5"));
        Assert.assertTrue(Util.isDouble("120.1"));
        Assert.assertTrue(Util.isDouble("NaN"));
        Assert.assertTrue(Util.isDouble("Infinity"));

        Assert.assertFalse(Util.isDouble("bad120.1"));
        Assert.assertFalse(Util.isDouble("zero"));
        Assert.assertFalse(Util.isDouble("13b0"));
    }

    @Test
    public void testParseDouble() {
        Assert.assertEquals(-5, Util.parseDouble("-5"), 0);
        Assert.assertEquals(Double.NaN, Util.parseDouble("NaN"), 0);
    }

    @Test
    public void testGetRootElement() throws IOException {
        String testXml = "<sample>" +
                         "   <test>1</test>" +
                         "   <test>2</test>" +
                         "   <test>3</test>" +
                         "   <other>4</other>" +
                         "   <other>5</other>" +
                         "</sample>";

        Assert.assertNotNull(Util.Xml.getRootElement(testXml));
    }

    @Test
    public void testGetTmpFilename() throws IOException {
        Assert.assertNotNull(Util.getTmpFilename());
    }

    @Test
    public void testMax() {
        // Test with arrays of doubles
        Assert.assertEquals(Double.NaN, Util.max(new double[] {}), 0.0);
        Assert.assertEquals(0, Util.max(new double[] {Double.NaN, 0}), 0.0);
        Assert.assertEquals(Double.POSITIVE_INFINITY, Util.max(new double[] {Double.NaN, Double.POSITIVE_INFINITY}), 0.0);
        Assert.assertEquals(500, Util.max(new double[] {500, -500}), 0.0);
        Assert.assertEquals(0, Util.max(new double[] {-500, 0}), 0.0);
        Assert.assertEquals(-500, Util.max(new double[] {-500, -1000}), 0.0);

        // Test with pair of doubles
        Assert.assertEquals(0, Util.max(Double.NaN, 0), 0.0);
        Assert.assertEquals(Double.POSITIVE_INFINITY, Util.max(Double.NaN, Double.POSITIVE_INFINITY), 0.0);
        Assert.assertEquals(500, Util.max(500, -500), 0.0);
        Assert.assertEquals(0, Util.max(-500, 0), 0.0);
        Assert.assertEquals(-500, Util.max(-500, -1000), 0.0);
    }

    @Test
    public void testMin() {
        // Test with arrays of doubles
        Assert.assertEquals(Double.NaN, Util.min(new double[] {}), 0.0);
        Assert.assertEquals(0, Util.min(new double[] {Double.NaN, 0}), 0.0);
        Assert.assertEquals(Double.POSITIVE_INFINITY, Util.min(new double[] {Double.NaN, Double.POSITIVE_INFINITY}), 0.0);
        Assert.assertEquals(-500, Util.min(new double[] {500, -500}), 0.0);
        Assert.assertEquals(-500, Util.min(new double[] {-500, 0}), 0.0);
        Assert.assertEquals(-1000, Util.min(new double[] {-500, -1000}), 0.0);

        // Test with pair of doubles
        Assert.assertEquals(0, Util.min(Double.NaN, 0), 0.0);
        Assert.assertEquals(Double.POSITIVE_INFINITY, Util.min(Double.NaN, Double.POSITIVE_INFINITY), 0.0);
        Assert.assertEquals(-500, Util.min(500, -500), 0.0);
        Assert.assertEquals(-500, Util.min(-500, 0), 0.0);
        Assert.assertEquals(-1000, Util.min(-500, -1000), 0.0);
    }

    @Test
    public void testParseBoolean() {
        Assert.assertFalse(Util.parseBoolean("false"));
        Assert.assertFalse(Util.parseBoolean("o== hh"));
        Assert.assertFalse(Util.parseBoolean(null));

        Assert.assertTrue(Util.parseBoolean("on"));
        Assert.assertTrue(Util.parseBoolean("y"));
        Assert.assertTrue(Util.parseBoolean("yes"));
        Assert.assertTrue(Util.parseBoolean("1"));
        Assert.assertTrue(Util.parseBoolean("true"));
    }

    @Test
    public void testParseColor() {
        // Test without alpha
        Color color1 = (Color) Util.parseColor("#AABBCC");
        Assert.assertEquals(170, color1.getRed());
        Assert.assertEquals(187, color1.getGreen());
        Assert.assertEquals(204, color1.getBlue());
        Assert.assertEquals(255, color1.getAlpha());

        // Test with alpha
        Color color2 = (Color) Util.parseColor("#AABBCCDD");
        Assert.assertEquals(170, color2.getRed());
        Assert.assertEquals(187, color2.getGreen());
        Assert.assertEquals(204, color2.getBlue());
        Assert.assertEquals(221, color2.getAlpha());

        // Test without hash
        Color color3 = (Color) Util.parseColor("AABBCCDD");
        Assert.assertEquals(170, color3.getRed());
        Assert.assertEquals(187, color3.getGreen());
        Assert.assertEquals(204, color3.getBlue());
        Assert.assertEquals(221, color3.getAlpha());

        // Test invalid
        thrown.expect(IllegalArgumentException.class);
        Util.parseColor("not a  color string");
    }

    @Test
    public void testSum() {
        Assert.assertEquals(0.0, Util.sum(Double.NaN, 0), 0.0);
        Assert.assertEquals(5.0, Util.sum(5, Double.NaN), 0.0);
        Assert.assertEquals(15.0, Util.sum(7, 8), 0.0);
        Assert.assertEquals(1, Util.sum(-4, 5), 0.0);
        Assert.assertEquals(-50, Util.sum(-20, -30), 0.0);
    }

}
