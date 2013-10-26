/*******************************************************************************
 * Copyright (c) 2011 The OpenNMS Group, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *******************************************************************************/
package org.rrd4j.graph;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.gt;
import static org.easymock.EasyMock.lt;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;

import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.data.DataProcessor;


/**
 * The form of these tests is fairly generic, using a Mock ImageWorker to see if the expected 
 * sort of calls were made.  We generally don't check co-ordinates.  Rather, we check for a certain 
 * number of major grid lines, with the right string labels, and a certain number of minor lines.
 * Presumably if there are the right number of lines they'll probably be around the right locations
 * This will test the actual complex behaviour of ValueAxis which is working out how many lines to
 * draw and at what major values.  Subtleties like the actual pixel values are generally less important.  
 * If we find a specific bug with placement of the grid lines, then we can writes tests to check
 * for specific co-ordinates, knowing exactly why we're checking
 * 
 * @author cmiskell
 *
 */

public class ValueAxisTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private ValueAxis valueAxis;
    private ImageWorker imageWorker;
    private RrdGraphDef graphDef;
    private ImageParameters imageParameters;
    private String jrbFileName;
    private final long startTime = 1;

    @Before 
    public void setup() throws IOException {
        jrbFileName = testFolder.newFile("test-value-axis.rrd").getCanonicalPath();
    }

    private void createGaugeRrd(int rowCount) throws IOException {
        RrdDef def = new RrdDef(jrbFileName);
        def.setStartTime(startTime);
        def.setStep(60);
        def.addDatasource("testvalue", DsType.GAUGE, 120, Double.NaN, Double.NaN);
        def.addArchive("RRA:AVERAGE:0.5:1:"+rowCount);

        //Create the empty rrd.  Other code may open and append data
        RrdDb rrd = new RrdDb(def);
        rrd.close();
    }

    //Cannot be called until the RRD has been populated; wait
    public void prepareGraph() throws IOException, FontFormatException {

        graphDef = new RrdGraphDef();
        graphDef.datasource("testvalue", jrbFileName, "testvalue", ConsolFun.AVERAGE);
        graphDef.area("testvalue", Util.parseColor("#FF0000"), "TestValue");
        graphDef.setStartTime(startTime);
        graphDef.setEndTime(startTime + (60*60*24));
        graphDef.setLocale(Locale.US);

        //We really need to specify exactly our own fonts, otherwise we get different behaviour on 
        // different systems (not conducive to reliably testing)
        // We expect the TTF file to be on the class path.
        //Font monoSpacedFont = Font.createFont(Font.TRUETYPE_FONT, new File("target/classes/DejaVuSansMono.ttf"));
        //graphDef.setFont(RrdGraphDef.FONTTAG_DEFAULT, monoSpacedFont.deriveFont(10.0f), true, true);
        //graphDef.setLargeFont(monoSpacedFont.deriveFont(12.0f));

        //There's only a couple of methods of ImageWorker that we actually care about in this test.
        // More to the point, we want the rest to work as normal (like getFontHeight, getFontAscent etc)
        imageWorker = createMockBuilder(ImageWorker.class)
                .addMockedMethod("drawLine")
                .addMockedMethod("drawString")
                .createStrictMock(); //Order is important!

        //For the test only; when creating the mock object above, if we include ".withConstructor(100,100)"
        // easymock calls the constructor but the call to resize doesn't happen properly.
        // But it's imperative to setup internal state in the ImageWorker.  So, call it here.  
        imageWorker.resize(100,100);

        //The imageParameters setup code is duplicated and simplified from that in similarly named functions
        // in RrdGraph.  It would be nice if this could be re-used somehow, but that would require
        // being able to pass in an ImageWorker to RrdGraph.  But currently the imageworker is created 
        // and used immediately in the constructor of RrdGraph.
        // And we need some existing unit tests to be able to prove that it's doing the right thing first
        // before we can refactor it so that we can write better unit tests.  Mr Chicken, meet Mr Egg.
        // So, this is about the best we can do at this stage.  
        // The called functions are named the same as in RrdGraph, but are generally simplified for the purposes of this test
        // eliminating code that we don't use
        imageParameters = new ImageParameters();
        DataProcessor dataProcessor = this.fetchData(imageParameters, graphDef);
        calculatePlotValues(graphDef, dataProcessor);
        findMinMaxValues(imageParameters, graphDef);
        identifySiUnit(imageParameters, graphDef);
        expandValueRange(imageParameters, graphDef);
        initializeLimits(imageParameters, graphDef);
        imageWorker.resize(imageParameters.xgif, imageParameters.ygif);

        Mapper graphMapper = new Mapper(graphDef, imageParameters);

        this.valueAxis = new ValueAxis(imageParameters, imageWorker, graphDef, graphMapper);
    }

    private double getSmallFontCharWidth() {
        return imageWorker.getStringWidth("a", graphDef.getFont(RrdGraphDef.FONTTAG_LEGEND));
    }

    private double getSmallFontHeight() {
        return imageWorker.getFontHeight(graphDef.getFont(RrdGraphDef.FONTTAG_LEGEND));
    }
    private double getLargeFontHeight() {
        return imageWorker.getFontHeight(graphDef.getFont(RrdGraphDef.FONTTAG_TITLE));
    }

    private void initializeLimits(ImageParameters imageParameters, RrdGraphDef gdef) {
        imageParameters.xsize = gdef.width;
        imageParameters.ysize = gdef.height;
        imageParameters.unitslength = gdef.unitsLength;
        if (gdef.onlyGraph) {
            if (imageParameters.ysize > 64) {
                throw new RuntimeException("Cannot create graph only, height too big");
            }
            imageParameters.xorigin = 0;
        }
        else {
            imageParameters.xorigin = (int) (RrdGraphConstants.PADDING_LEFT + imageParameters.unitslength * getSmallFontCharWidth());
        }
        if (gdef.verticalLabel != null) {
            imageParameters.xorigin += getSmallFontHeight();
        }
        if (gdef.onlyGraph) {
            imageParameters.yorigin = imageParameters.ysize;
        }
        else {
            imageParameters.yorigin = RrdGraphConstants.PADDING_TOP + imageParameters.ysize;
        }
        if (gdef.title != null) {
            imageParameters.yorigin += getLargeFontHeight() + RrdGraphConstants.PADDING_TITLE;
        }
        if (gdef.onlyGraph) {
            imageParameters.xgif = imageParameters.xsize;
            imageParameters.ygif = imageParameters.yorigin;
        }
        else {
            imageParameters.xgif = RrdGraphConstants.PADDING_RIGHT + imageParameters.xsize + imageParameters.xorigin;
            imageParameters.ygif = imageParameters.yorigin + (int) (RrdGraphConstants.PADDING_PLOT * getSmallFontHeight());
        }
    }

    private void expandValueRange(ImageParameters imageParameters, RrdGraphDef gdef) {
        imageParameters.ygridstep = (gdef.valueAxisSetting != null) ? gdef.valueAxisSetting.gridStep : Double.NaN;
        imageParameters.ylabfact = (gdef.valueAxisSetting != null) ? gdef.valueAxisSetting.labelFactor : 0;
        if (!gdef.rigid && !gdef.logarithmic) {
            double sensiblevalues[] = {
                    1000.0, 900.0, 800.0, 750.0, 700.0, 600.0, 500.0, 400.0, 300.0, 250.0, 200.0, 125.0, 100.0,
                    90.0, 80.0, 75.0, 70.0, 60.0, 50.0, 40.0, 30.0, 25.0, 20.0, 10.0,
                    9.0, 8.0, 7.0, 6.0, 5.0, 4.0, 3.5, 3.0, 2.5, 2.0, 1.8, 1.5, 1.2, 1.0,
                    0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0.0, -1
            };
            double scaled_min, scaled_max, adj;
            if (Double.isNaN(imageParameters.ygridstep)) {
                if (gdef.altYMrtg) { /* mrtg */
                    imageParameters.decimals = Math.ceil(Math.log10(Math.max(Math.abs(imageParameters.maxval), Math.abs(imageParameters.minval))));
                    imageParameters.quadrant = 0;
                    if (imageParameters.minval < 0) {
                        imageParameters.quadrant = 2;
                        if (imageParameters.maxval <= 0) {
                            imageParameters.quadrant = 4;
                        }
                    }
                    switch (imageParameters.quadrant) {
                    case 2:
                        imageParameters.scaledstep = Math.ceil(50 * Math.pow(10, -(imageParameters.decimals)) * Math.max(Math.abs(imageParameters.maxval),
                                Math.abs(imageParameters.minval))) * Math.pow(10, imageParameters.decimals - 2);
                        scaled_min = -2 * imageParameters.scaledstep;
                        scaled_max = 2 * imageParameters.scaledstep;
                        break;
                    case 4:
                        imageParameters.scaledstep = Math.ceil(25 * Math.pow(10,
                                -(imageParameters.decimals)) * Math.abs(imageParameters.minval)) * Math.pow(10, imageParameters.decimals - 2);
                        scaled_min = -4 * imageParameters.scaledstep;
                        scaled_max = 0;
                        break;
                    default: /* quadrant 0 */
                        imageParameters.scaledstep = Math.ceil(25 * Math.pow(10, -(imageParameters.decimals)) * imageParameters.maxval) *
                        Math.pow(10, imageParameters.decimals - 2);
                        scaled_min = 0;
                        scaled_max = 4 * imageParameters.scaledstep;
                        break;
                    }
                    imageParameters.minval = scaled_min;
                    imageParameters.maxval = scaled_max;
                }
                else if (gdef.altAutoscale) {
                    /* measure the amplitude of the function. Make sure that
                                           graph boundaries are slightly higher then max/min vals
                                           so we can see amplitude on the graph */
                    double delt, fact;

                    delt = imageParameters.maxval - imageParameters.minval;
                    adj = delt * 0.1;
                    fact = 2.0 * Math.pow(10.0,
                            Math.floor(Math.log10(Math.max(Math.abs(imageParameters.minval), Math.abs(imageParameters.maxval)))) - 2);
                    if (delt < fact) {
                        adj = (fact - delt) * 0.55;
                    }
                    imageParameters.minval -= adj;
                    imageParameters.maxval += adj;
                }
                else if (gdef.altAutoscaleMax) {
                    /* measure the amplitude of the function. Make sure that
                                           graph boundaries are slightly higher than max vals
                                           so we can see amplitude on the graph */
                    adj = (imageParameters.maxval - imageParameters.minval) * 0.1;
                    imageParameters.maxval += adj;
                }
                else {
                    scaled_min = imageParameters.minval / imageParameters.magfact;
                    scaled_max = imageParameters.maxval / imageParameters.magfact;
                    for (int i = 1; sensiblevalues[i] > 0; i++) {
                        if (sensiblevalues[i - 1] >= scaled_min && sensiblevalues[i] <= scaled_min) {
                            imageParameters.minval = sensiblevalues[i] * imageParameters.magfact;
                        }
                        if (-sensiblevalues[i - 1] <= scaled_min && -sensiblevalues[i] >= scaled_min) {
                            imageParameters.minval = -sensiblevalues[i - 1] * imageParameters.magfact;
                        }
                        if (sensiblevalues[i - 1] >= scaled_max && sensiblevalues[i] <= scaled_max) {
                            imageParameters.maxval = sensiblevalues[i - 1] * imageParameters.magfact;
                        }
                        if (-sensiblevalues[i - 1] <= scaled_max && -sensiblevalues[i] >= scaled_max) {
                            imageParameters.maxval = -sensiblevalues[i] * imageParameters.magfact;
                        }
                    }
                }
            }
            else {
                imageParameters.minval = (double) imageParameters.ylabfact * imageParameters.ygridstep *
                        Math.floor(imageParameters.minval / ((double) imageParameters.ylabfact * imageParameters.ygridstep));
                imageParameters.maxval = (double) imageParameters.ylabfact * imageParameters.ygridstep *
                        Math.ceil(imageParameters.maxval / ((double) imageParameters.ylabfact * imageParameters.ygridstep));
            }

        }
    }

    private void identifySiUnit(ImageParameters imageParameters, RrdGraphDef gdef) {
        imageParameters.unitsexponent = gdef.unitsExponent;
        imageParameters.base = gdef.base;
        if (!gdef.logarithmic) {
            final char symbol[] = {'a', 'f', 'p', 'n', 'u', 'm', ' ', 'k', 'M', 'G', 'T', 'P', 'E'};
            int symbcenter = 6;
            double digits;
            if (imageParameters.unitsexponent != Integer.MAX_VALUE) {
                digits = Math.floor(imageParameters.unitsexponent / 3.0);
            }
            else {
                digits = Math.floor(Math.log(Math.max(Math.abs(imageParameters.minval), Math.abs(imageParameters.maxval))) / Math.log(imageParameters.base));
            }
            imageParameters.magfact = Math.pow(imageParameters.base, digits);
            if (((digits + symbcenter) < symbol.length) && ((digits + symbcenter) >= 0)) {
                imageParameters.symbol = symbol[(int) digits + symbcenter];
            }
            else {
                imageParameters.symbol = '?';
            }
        }
    }

    private void findMinMaxValues(ImageParameters imageParameters, RrdGraphDef gdef) {
        double minval = Double.NaN, maxval = Double.NaN;
        for (PlotElement pe : gdef.plotElements) {
            if (pe instanceof SourcedPlotElement) {
                minval = Util.min(((SourcedPlotElement) pe).getMinValue(), minval);
                maxval = Util.max(((SourcedPlotElement) pe).getMaxValue(), maxval);
            }
        }
        if (Double.isNaN(minval)) {
            minval = 0D;
        }
        if (Double.isNaN(maxval)) {
            maxval = 1D;
        }
        imageParameters.minval = gdef.minValue;
        imageParameters.maxval = gdef.maxValue;
        /* adjust min and max values */
        if (Double.isNaN(imageParameters.minval) || ((!gdef.logarithmic && !gdef.rigid) && imageParameters.minval > minval)) {
            imageParameters.minval = minval;
        }
        if (Double.isNaN(imageParameters.maxval) || (!gdef.rigid && imageParameters.maxval < maxval)) {
            if (gdef.logarithmic) {
                imageParameters.maxval = maxval * 1.1;
            }
            else {
                imageParameters.maxval = maxval;
            }
        }
        /* make sure min is smaller than max */
        if (imageParameters.minval > imageParameters.maxval) {
            imageParameters.minval = 0.99 * imageParameters.maxval;
        }
        /* make sure min and max are not equal */
        if (Math.abs(imageParameters.minval - imageParameters.maxval) < .0000001) {
            imageParameters.maxval *= 1.01;
            if (!gdef.logarithmic) {
                imageParameters.minval *= 0.99;
            }
            /* make sure min and max are not both zero */
            if (imageParameters.maxval == 0.0) {
                imageParameters.maxval = 1.0;
            }
        }
    }

    private void calculatePlotValues(RrdGraphDef gdef, DataProcessor dproc) {
        for (PlotElement pe : gdef.plotElements) {
            if (pe instanceof SourcedPlotElement) {
                ((SourcedPlotElement) pe).assignValues(dproc);
            }
        }
    }

    private DataProcessor fetchData(ImageParameters imageParameters, RrdGraphDef gdef) throws IOException {
        DataProcessor dproc = new DataProcessor(gdef.startTime, gdef.endTime);
        dproc.setPoolUsed(gdef.poolUsed);
        if (gdef.step > 0) {
            dproc.setStep(gdef.step);
        }
        for (Source src : gdef.sources) {
            src.requestData(dproc);
        }
        dproc.processData();
        imageParameters.start = gdef.startTime;
        imageParameters.end = gdef.endTime;
        return dproc;
    }

    @After
    public void tearDown() {
        File jrbFile = new File(jrbFileName);
        jrbFile.delete();
    }

    public void checkForBasicGraph() {
        expectMajorGridLine(" 0.0");

        expectMinorGridLines(4);

        expectMajorGridLine(" 0.5");

        expectMinorGridLines(4);

        expectMajorGridLine(" 1.0");

        replay(imageWorker);

        this.valueAxis.draw();
        //Validate the calls to the imageWorker
        verify(imageWorker);
    }

    private void expectMajorGridLine(String label) {
        //We don't care what y value is used for any of the gridlines; the fact that they're there 
        // is good enough.  Our expectations for any given graph will be a certain set of grid lines
        // in a certain order, which pretty much implies y values.  If we find a bug where that 
        // assumption is wrong, a more specific test can be written to find that.
        //The x-values of the lines are important though, to be sure they've been drawn correctly

        //Note the use of "same" for the strokes; in RrdGraphConstants, these are both BasicStroke(1)
        // so we want to be sure exactly the same object was used

        int quarterX = imageParameters.xgif/4;
        int midX = imageParameters.xgif/2;
        int threeQuartersX = quarterX*3;

        imageWorker.drawString(eq(label), lt(quarterX), anyInt(), eq(graphDef.getFont(RrdGraphDef.FONTTAG_LEGEND)), eq(RrdGraphDef.DEFAULT_FONT_COLOR));
        //Horizontal tick on the left
        imageWorker.drawLine(lt(quarterX), anyInt(), lt(midX), anyInt(), eq(RrdGraphDef.DEFAULT_MGRID_COLOR), same(RrdGraphDef.TICK_STROKE));
        //Horizontal tick on the right
        imageWorker.drawLine(gt(threeQuartersX), anyInt(), gt(threeQuartersX), anyInt(), eq(RrdGraphDef.DEFAULT_MGRID_COLOR), same(RrdGraphDef.TICK_STROKE));
        //Line in between the ticks (but overlapping a bit)
        imageWorker.drawLine(lt(quarterX), anyInt(), gt(midX),anyInt(), eq(RrdGraphDef.DEFAULT_MGRID_COLOR), same(RrdGraphDef.GRID_STROKE));

    }

    private void expectMinorGridLines(int count) {
        //Note the use of "same" for the strokes; in RrdGraphConstants, these are both BasicStroke(1)
        // so we want to be sure exactly the same object was used

        int quarterX = imageParameters.xgif/4;
        int midX = quarterX*2;
        int threeQuartersX = quarterX*3;

        for(int i=0; i<count; i++) {
            imageWorker.drawLine(lt(quarterX), anyInt(), lt(quarterX), anyInt(), eq(RrdGraphDef.DEFAULT_GRID_COLOR), same(RrdGraphDef.TICK_STROKE));
            imageWorker.drawLine(gt(threeQuartersX),  anyInt(), gt(threeQuartersX),  anyInt(), eq(RrdGraphDef.DEFAULT_GRID_COLOR), same(RrdGraphDef.TICK_STROKE));
            imageWorker.drawLine(lt(quarterX),  anyInt(), gt(midX),  anyInt(), eq(RrdGraphDef.DEFAULT_GRID_COLOR), same(RrdGraphDef.GRID_STROKE));
        }
    }

    @Test
    public void testBasicEmptyRrd() throws IOException, FontFormatException {
        createGaugeRrd(100);
        prepareGraph();
        checkForBasicGraph();
    }


    @Test
    public void testOneEntryInRrd() throws IOException, FontFormatException {
        createGaugeRrd(100);
        RrdDb rrd = new RrdDb(jrbFileName);
        long nowSeconds = new Date().getTime();
        long fiveMinutesAgo = nowSeconds - (5 * 60);
        Sample sample = rrd.createSample();
        sample.setAndUpdate(fiveMinutesAgo+":10");
        rrd.close();
        prepareGraph();
        checkForBasicGraph();
    }

    @Test
    public void testTwoEntriesInRrd() throws IOException, FontFormatException {
        createGaugeRrd(100);
        RrdDb rrd = new RrdDb(jrbFileName);

        for(int i=0; i<2; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp+":100");
        }
        rrd.close();
        prepareGraph();

        expectMajorGridLine("  90");
        expectMinorGridLines(1);
        expectMajorGridLine(" 100");
        expectMinorGridLines(1);
        expectMajorGridLine(" 110");
        expectMinorGridLines(1);
        expectMajorGridLine(" 120");
        expectMinorGridLines(1);

        replay(imageWorker);

        this.valueAxis.draw();
        //Validate the calls to the imageWorker
        verify(imageWorker);

    }

    @Test
    public void testEntriesZeroTo100InRrd() throws IOException, FontFormatException {
        createGaugeRrd(105); //Make sure all entries are recorded (5 is just a buffer for consolidation)
        RrdDb rrd = new RrdDb(jrbFileName);

        for(int i=0; i<100; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + i);
        }
        rrd.close();
        prepareGraph();
        expectMinorGridLines(4);
        expectMajorGridLine("  50");
        expectMinorGridLines(4);
        expectMajorGridLine(" 100");

        replay(imageWorker);

        this.valueAxis.draw();
        //Validate the calls to the imageWorker
        verify(imageWorker);

    }

    @Test
    public void testEntriesNeg50To100InRrd() throws IOException, FontFormatException {
        createGaugeRrd(155);
        RrdDb rrd = new RrdDb(jrbFileName);

        for(int i=0; i<150; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + (i -50));
        }
        rrd.close();
        prepareGraph();
        expectMajorGridLine(" -50");
        expectMinorGridLines(4);
        expectMajorGridLine("   0");
        expectMinorGridLines(4);
        expectMajorGridLine("  50");
        expectMinorGridLines(4);
        expectMajorGridLine(" 100");

        replay(imageWorker);

        this.valueAxis.draw();
        //Validate the calls to the imageWorker
        verify(imageWorker);

    }

    @Test
    public void testEntriesNeg55To105InRrd() throws IOException, FontFormatException {
        createGaugeRrd(165);
        RrdDb rrd = new RrdDb(jrbFileName);

        for(int i=0; i<160; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + (i -55));
        }
        rrd.close();
        prepareGraph();
        /**
         * Prior to JRB-12 fix, this was the behaviour.  Note the lack of a decent negative label
                expectMinorGridLines(3);
                expectMajorGridLine("   0");
                expectMinorGridLines(4);
                expectMajorGridLine(" 100");
                expectMinorGridLines(1);
         */
        //New behaviour is better; no minor grid lines, which is interesting, but much better representation
        expectMajorGridLine(" -50");
        expectMajorGridLine("   0");
        expectMajorGridLine("  50");
        expectMajorGridLine(" 100");



        replay(imageWorker);

        this.valueAxis.draw();
        //Validate the calls to the imageWorker
        verify(imageWorker);

    }

    @Test
    public void testEntriesNeg50To0InRrd() throws IOException, FontFormatException {
        createGaugeRrd(100);
        RrdDb rrd = new RrdDb(jrbFileName);

        for(int i=0; i<50; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + (i -50));
        }
        rrd.close();
        prepareGraph();
        expectMinorGridLines(2);
        expectMajorGridLine(" -40");
        expectMinorGridLines(3);
        expectMajorGridLine(" -20");
        expectMinorGridLines(3);

        replay(imageWorker);

        this.valueAxis.draw();
        //Validate the calls to the imageWorker
        verify(imageWorker);

    }

    /**
     * Test specifically for JRB-12 (http://issues.opennms.org/browse/JRB-12) 
     * In the original, when the values go from -80 to 90 on a default height graph 
     * (i.e. limited pixels available for X-axis labelling),ValueAxis gets all confused 
     * and decides it can only display "0" on the X-axis  (there's not enough pixels
     * for more labels, and none of the Y-label factorings available work well enough
     * @throws FontFormatException 
     */
    @Test
    public void testEntriesNeg80To90InRrd() throws IOException, FontFormatException {
        createGaugeRrd(180);
        RrdDb rrd = new RrdDb(jrbFileName);

        for(int i=0; i<170; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + (i -80));
        }
        rrd.close();
        prepareGraph();
        /**
         * Original behaviour; a single major X-axis label (0) only.
                expectMinorGridLines(4);
                expectMajorGridLine("   0");
                expectMinorGridLines(4);
         */
        //New behaviour post JRB-12 fix:
        expectMajorGridLine(" -50");
        expectMajorGridLine("   0");
        expectMajorGridLine("  50");

        replay(imageWorker);

        this.valueAxis.draw();
        //Validate the calls to the imageWorker
        verify(imageWorker);

    }

    /**
     * Test specifically for JRB-12 (http://issues.opennms.org/browse/JRB-12)
     * Related to testEntriesNeg80To90InRrd, except in the original code
     * this produced sensible labelling.  Implemented to check that the 
     * changes don't break the sanity.
     * @throws FontFormatException 
     */
    @Test
    public void testEntriesNeg80To80InRrd() throws IOException, FontFormatException {
        createGaugeRrd(180);
        RrdDb rrd = new RrdDb(jrbFileName);

        for(int i=0; i<160; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + (i -80));
        }
        rrd.close();
        prepareGraph();

        // Original
        expectMinorGridLines(3);
        expectMajorGridLine(" -50");
        expectMinorGridLines(4);
        expectMajorGridLine("   0");
        expectMinorGridLines(4);
        expectMajorGridLine("  50");
        expectMinorGridLines(3);


        replay(imageWorker);

        this.valueAxis.draw();
        //Validate the calls to the imageWorker
        verify(imageWorker);

    }

}
