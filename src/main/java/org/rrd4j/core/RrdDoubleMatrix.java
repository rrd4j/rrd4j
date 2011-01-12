package org.rrd4j.core;

import java.io.IOException;

class RrdDoubleMatrix extends RrdPrimitive {
    private final int rows;
    private final int columns;

    RrdDoubleMatrix(RrdUpdater updater, int row, int column, boolean shouldInitialize) throws IOException {
        super(updater, RrdPrimitive.RRD_DOUBLE, row * column, false);
        this.rows = row;
        this.columns = column;
        if (shouldInitialize)
            writeDouble(0, Double.NaN, rows * columns);
    }

    void set(int column, int index, double value) throws IOException {
        writeDouble(columns * index + column, value);
    }

    void set(int column, int index, double value, int count) throws IOException {
        // rollovers not allowed!
        assert index + count <= rows : "Invalid robin index supplied: index=" + index +
                ", count=" + count + ", length=" + rows;
        for (int i = columns * index + column, c = 0; c < count; i += columns, c++)
            writeDouble(i, value);
    }

    public void set(int column, int index, double[] newValues) throws IOException {
        int count = newValues.length;
        // rollovers not allowed!
        assert index + count <= rows : "Invalid robin index supplied: index=" + index +
                ", count=" + count + ", length=" + rows;
        for (int i = columns * index + column, c = 0; c < count; i += columns, c++)
            writeDouble(i, newValues[c]);
    }

    double get(int column, int index) throws IOException {
        assert index < rows : "Invalid index supplied: " + index + ", length=" + rows;
        return readDouble(columns * index + column);
    }

    double[] get(int column, int index, int count) throws IOException {
        assert index + count <= rows : "Invalid index/count supplied: " + index +
                "/" + count + " (length=" + rows + ")";
        double[] values = new double[count];
        for (int i = columns * index + column, c = 0; c < count; i += columns, c++) {
            values[c] = readDouble(i);
        }
        return values;
    }

    /** @return the column. */
    public int getColumns() {
        return columns;
    }

    /** @return the row. */
    public int getRows() {
        return rows;
    }
}
