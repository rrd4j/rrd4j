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
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.gt;
import static org.easymock.EasyMock.lt;
import static org.easymock.EasyMock.same;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.Sample;


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

public class ValueAxisTest extends AxisTester<ValueAxis> {

    @Override
    ValueAxis makeAxis(RrdGraph rrdGraph) {
        return new ValueAxis(rrdGraph, imageWorker);
    }

    public void checkForBasicGraph() {
        expectMajorGridLine(" 0.0");

        expectMinorGridLines(4);

        expectMajorGridLine(" 0.5");

        expectMinorGridLines(4);

        expectMajorGridLine(" 1.0");

        run();
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

        imageWorker.drawString(eq(label), lt(quarterX), anyInt(), eq(graphDef.getFont(RrdGraphDef.FONTTAG_AXIS)), eq(RrdGraphDef.DEFAULT_FONT_COLOR));
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
            imageWorker.drawLine(gt(threeQuartersX), anyInt(), gt(threeQuartersX), anyInt(), eq(RrdGraphDef.DEFAULT_GRID_COLOR), same(RrdGraphDef.TICK_STROKE));
            imageWorker.drawLine(lt(quarterX), anyInt(), gt(midX), anyInt(), eq(RrdGraphDef.DEFAULT_GRID_COLOR), same(RrdGraphDef.GRID_STROKE));
        }
    }

    @Test
    public void testBasicEmptyRrd() throws IOException {
        createGaugeRrd(100);
        prepareGraph("ValueAxisTest", "testBasicEmptyRrd");
        checkForBasicGraph();
    }

    @Test
    public void testOneEntryInRrd() throws IOException {
        createGaugeRrd(100);
        RrdDb rrd = RrdDb.getBuilder().setPath(jrbFileName).build();
        long nowSeconds = new Date().getTime();
        long fiveMinutesAgo = nowSeconds - (5 * 60);
        Sample sample = rrd.createSample();
        sample.setAndUpdate(fiveMinutesAgo+":10");
        rrd.close();
        prepareGraph("ValueAxisTest", "testOneEntryInRrd");
        checkForBasicGraph();
    }

    @Test
    public void testTwoEntriesInRrd() throws IOException {
        createGaugeRrd(100);
        RrdDb rrd = RrdDb.getBuilder().setPath(jrbFileName).build();

        for(int i=0; i<2; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp+":100");
        }
        rrd.close();
        prepareGraph("ValueAxisTest", "testTwoEntriesInRrd");

        expectMajorGridLine("  90");
        expectMinorGridLines(1);
        expectMajorGridLine(" 100");
        expectMinorGridLines(1);
        expectMajorGridLine(" 110");
        expectMinorGridLines(1);
        expectMajorGridLine(" 120");
        expectMinorGridLines(1);

        run();

    }

    @Test
    public void testEntriesZeroTo100InRrd() throws IOException {
        createGaugeRrd(105); //Make sure all entries are recorded (5 is just a buffer for consolidation)
        RrdDb rrd = RrdDb.getBuilder().setPath(jrbFileName).build();

        for(int i=0; i<100; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + i);
        }
        rrd.close();
        prepareGraph("ValueAxisTest", "testEntriesZeroTo100InRrd");
        expectMinorGridLines(4);
        expectMajorGridLine("  50");
        expectMinorGridLines(4);
        expectMajorGridLine(" 100");

        run();

    }

    @Test
    public void testEntriesNeg50To100InRrd() throws IOException {
        createGaugeRrd(155);
        RrdDb rrd = RrdDb.getBuilder().setPath(jrbFileName).build();

        for(int i=0; i<150; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + (i -50));
        }
        rrd.close();
        prepareGraph("ValueAxisTest", "testEntriesNeg50To100InRrd");
        expectMajorGridLine(" -50");
        expectMinorGridLines(4);
        expectMajorGridLine("   0");
        expectMinorGridLines(4);
        expectMajorGridLine("  50");
        expectMinorGridLines(4);
        expectMajorGridLine(" 100");

        run();

    }

    @Test
    public void testEntriesNeg55To105InRrd() throws IOException {
        createGaugeRrd(165);
        RrdDb rrd = RrdDb.getBuilder().setPath(jrbFileName).build();

        for(int i=0; i<160; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + (i -55));
        }
        rrd.close();
        prepareGraph("ValueAxisTest", "testEntriesNeg55To105InRrd");
        /* Prior to JRB-12 fix, this was the behaviour.  Note the lack of a decent negative label
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

        run();

    }

    @Test
    public void testEntriesNeg50To0InRrd() throws IOException {
        createGaugeRrd(100);
        RrdDb rrd = RrdDb.getBuilder().setPath(jrbFileName).build();

        for(int i=0; i<50; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + (i -50));
        }
        rrd.close();
        prepareGraph("ValueAxisTest", "testEntriesNeg50To0InRrd");
        expectMinorGridLines(2);
        expectMajorGridLine(" -40");
        expectMinorGridLines(3);
        expectMajorGridLine(" -20");
        expectMinorGridLines(3);

        run();

    }

    /**
     * Test specifically for JRB-12 (http://issues.opennms.org/browse/JRB-12) 
     * In the original, when the values go from -80 to 90 on a default height graph 
     * (i.e. limited pixels available for X-axis labelling),ValueAxis gets all confused 
     * and decides it can only display "0" on the X-axis  (there's not enough pixels
     * for more labels, and none of the Y-label factorings available work well enough
     */
    @Test
    public void testEntriesNeg80To90InRrd() throws IOException {
        createGaugeRrd(180);
        RrdDb rrd = RrdDb.getBuilder().setPath(jrbFileName).build();

        for(int i=0; i<170; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + (i -80));
        }
        rrd.close();
        prepareGraph("ValueAxisTest", "testEntriesNeg80To90InRrd");
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

        run();

    }

    /**
     * Test specifically for JRB-12 (http://issues.opennms.org/browse/JRB-12)
     * Related to testEntriesNeg80To90InRrd, except in the original code
     * this produced sensible labelling.  Implemented to check that the 
     * changes don't break the sanity.
     */
    @Test
    public void testEntriesNeg80To80InRrd() throws IOException {
        createGaugeRrd(180);
        RrdDb rrd = RrdDb.getBuilder().setPath(jrbFileName).build();

        for(int i=0; i<160; i++) {
            long timestamp = startTime + 1 + (i * 60);
            Sample sample = rrd.createSample();
            sample.setAndUpdate(timestamp + ":" + (i -80));
        }
        rrd.close();
        prepareGraph("ValueAxisTest", "testEntriesNeg80To80InRrd");

        // Original
        expectMinorGridLines(3);
        expectMajorGridLine(" -50");
        expectMinorGridLines(4);
        expectMajorGridLine("   0");
        expectMinorGridLines(4);
        expectMajorGridLine("  50");
        expectMinorGridLines(3);

        run();

    }

}
