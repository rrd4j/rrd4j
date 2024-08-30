package org.rrd4j.graph;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.same;

import java.awt.Font;
import java.awt.Paint;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Test;
import org.rrd4j.graph.RrdGraphConstants.FontConstructor;

public class TimeAxisTest extends AxisTester<TimeAxis> {

    private Function<TimeUnit, Optional<TimeLabelFormat>> formatter;

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

        expectMinorGridLines(24);
        expectMajorGridLine();
        expectMajorGridLine();
        expectMajorGridLine();
        expectMajorGridLine();
        imageWorker.drawString("06:00", 132, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);
        imageWorker.drawString("12:00", 232, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);
        imageWorker.drawString("18:00", 332, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);
        imageWorker.drawString("00:00", 432, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);

        run();
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
        prepareGraph("TimeAxisTest", "firstTest");

        expectMinorGridLines(24);
        expectMajorGridLine();
        expectMajorGridLine();
        expectMajorGridLine();
        expectMajorGridLine();
        imageWorker.drawString("06 00", 132, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);
        imageWorker.drawString("12 00", 232, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);
        imageWorker.drawString("18 00", 332, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);
        imageWorker.drawString("00 00", 432, 125, FontConstructor.getFont(Font.PLAIN, 10), java.awt.Color.BLACK);

        run();
    }

    @Override
    TimeAxis makeAxis(RrdGraph graph) {
        return new TimeAxis(graph, imageWorker);
    }

}
