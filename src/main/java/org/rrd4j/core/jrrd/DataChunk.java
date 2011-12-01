package org.rrd4j.core.jrrd;

import java.util.Map;

import org.rrd4j.data.LinearInterpolator;
import org.rrd4j.data.Plottable;

/**
 * Models a chunk of result data from an RRDatabase.
 *
 * @author <a href="mailto:ciaran@codeloop.com">Ciaran Treanor</a>
 * @version $Revision: 1.1 $
 */
public class DataChunk {

    private static final String NEWLINE = System.getProperty("line.separator");
    private final long startTime;
    final int start;
    final int end;
    final private long step;
    final int dsCount;
    final double[][] data;
    final private int rows;
    private final Map<String, Integer> nameindex;

    DataChunk(Map<String, Integer> nameindex, long startTime, int start, int end, long step, int dsCount, int rows) {
        this.nameindex = nameindex;
        this.startTime = startTime;
        this.start = start;
        this.end = end;
        this.step = step;
        this.dsCount = dsCount;
        this.rows = rows;
        data = new double[rows][dsCount];
    }

    /**
     * Returns a summary of the contents of this data chunk. The first column is
     * the time (RRD format) and the following columns are the data source
     * values.
     *
     * @return a summary of the contents of this data chunk.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder();
        long time = startTime;

        for (int row = 0; row < rows; row++, time += step) {
            sb.append(time);
            sb.append(": ");

            for (int ds = 0; ds < dsCount; ds++) {
                sb.append(data[row][ds]);
                sb.append(" ");
            }

            sb.append(NEWLINE);
        }

        return sb.toString();
    }

    /**
     * @return the data
     */
    public double[][] getData() {
        return data;
    }

    /**
     * Extract a datasource from the datachunck given is name as a Plottable  
     * @param name the datasource name
     * @return a plottable for the datasource
     */
    public Plottable toPlottable(String name) {
        Integer dsId = nameindex.get(name);
        if(dsId == null)
            throw new RuntimeException("datasource not not found: " + name);
        long[] date =  new long[rows];
        double[] results =  new double[rows];
        long time = startTime;
        for (int row = 0; row < rows; row++, time += step) {
            date[row] = time;
            results[row] = data[row][dsId];
        }
        return new LinearInterpolator(date, results);
    }

}
