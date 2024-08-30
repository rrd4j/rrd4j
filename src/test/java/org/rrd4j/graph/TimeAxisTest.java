package org.rrd4j.graph;

import java.awt.*;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Assert;
import org.junit.Test;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.same;

public class TimeAxisTest extends AxisTester<TimeAxis> {

    private Function<TimeUnit, Optional<TimeLabelFormat>> formatter;
    Capture<String> labels = Capture.newInstance(CaptureType.ALL);

    private void expectMajorGridLine() {
        //Note the use of "same" for the strokes; in RrdGraphConstants, these are both BasicStroke(1)
        // so we want to be sure exactly the same object was used

        Paint color = graphDef.getColor(ElementsNames.mgrid);
        imageWorker.drawLine(anyInt(), anyInt(), anyInt(), anyInt(), eq(color), same(graphDef.tickStroke));
        //Horizontal tick on the right
        imageWorker.drawLine(anyInt(), anyInt(), anyInt(), anyInt(), eq(color), same(graphDef.gridStroke));
    }

    private void expectMinorGridLines(int count) {
        //Note the use of "same" for the strokes; in RrdGraphConstants, these are both BasicStroke(1)
        // so we want to be sure exactly the same object was used

        Paint color = graphDef.getColor(ElementsNames.grid);
        for(int i=0; i < count; i++) {
            imageWorker.drawLine(anyInt(), anyInt(), anyInt(), anyInt(), eq(color), same(graphDef.tickStroke));
            imageWorker.drawLine(anyInt(), anyInt(), anyInt(), anyInt(), eq(color), same(graphDef.gridStroke));
        }
    }

    @Test
    public void firstTest() throws IOException {
        formatter = t -> Optional.empty();
        createGaugeRrd(100);
        prepareGraph("TimeAxisTest", "firstTest");
        runTest();
        Assert.assertArrayEquals(new String[]{"06:00", "12:00", "18:00"}, labels.getValues().toArray());
    }

    @Override
    void setupGraphDef() {
        graphDef.setTimeLabelFormatter(formatter);
    }

    @Test
    public void customFormatting() throws IOException {
        formatter = t -> {
            if (t == TimeUnit.HOUR) {
                return Optional.of(new SimpleTimeLabelFormat("HH mm"));
            } else {
                return Optional.empty();
            }
        };
        createGaugeRrd(100);
        prepareGraph("TimeAxisTest", "customFormatting");

        runTest();
        Assert.assertArrayEquals(new String[]{"06 00", "12 00", "18 00"}, labels.getValues().toArray());

    }

    @Override
    TimeAxis makeAxis(RrdGraph graph) {
        return new TimeAxis(graph, imageWorker);
    }

    private void runTest() {
        expectMinorGridLines(24);
        expectMajorGridLine();
        expectMajorGridLine();
        expectMajorGridLine();
        expectMajorGridLine();
        imageWorker.drawString(capture(labels), eq(148), eq(125), anyObject(), anyObject());
        imageWorker.drawString(capture(labels), eq(248), eq(125), anyObject(), anyObject());
        imageWorker.drawString(capture(labels),  eq(348), eq(125), anyObject(), anyObject());
        run();
    }

}
