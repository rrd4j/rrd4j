/*
 * Copyright (C) 2001 Ciaran Treanor <ciaran@codeloop.com>
 *
 * Distributable under GPL license.
 * See terms of license at gnu.org.
 *
 * $Id: DataChunk.java,v 1.1 2006/02/03 08:27:16 sasam Exp $
 */
package org.rrd4j.core.jrrd;

/**
 * Models a chunk of result data from an RRDatabase.
 *
 * @author <a href="mailto:ciaran@codeloop.com">Ciaran Treanor</a>
 * @version $Revision: 1.1 $
 */
public class DataChunk {

    private static final String NEWLINE = System.getProperty("line.separator");
    long startTime;
    int start;
    int end;
    long step;
    int dsCount;
    double[][] data;
    int rows;

    DataChunk(long startTime, int start, int end, long step, int dsCount, int rows) {
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
}
