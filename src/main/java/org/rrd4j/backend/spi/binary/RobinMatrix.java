package org.rrd4j.backend.spi.binary;

import java.io.IOException;

/**
 * Class to represent archive values for a single datasource. Robin class is the heart of
 * the so-called "round robin database" concept. Basically, each Robin object is a
 * fixed length array of double values. Each double value reperesents consolidated, archived
 * value for the specific timestamp. When the underlying array of double values gets completely
 * filled, new values will replace the oldest ones.<p>
 * <p/>
 * Robin object does not hold values in memory - such object could be quite large.
 * Instead of it, Robin reads them from the backend I/O only when necessary.
 *
 * @author Fabrice Bacchella
 */
class RobinMatrix extends RobinBinary implements Allocated {
    private final RrdDoubleMatrix matrix;

    RobinMatrix(RrdBinaryBackend backend, RrdDoubleMatrix values, RrdInt pointer, final int column) throws IOException {
        super(backend, values.getRows());

        this.pointer = pointer;
        this.matrix = values;
        this.values = new RobinBinary.RrdDoubleVector() {
            public void set(int index, double value) throws IOException {
                matrix.set(column, index, value);
            }

            public void set(int index, double value, int count) throws IOException {
                matrix.set(column, index, value);
            }

            public void set(int index, double[] values) throws IOException {
                matrix.set(column, index, values);
            } 

            public double get(int index) throws IOException {
                return matrix.get(column, index);
            }

            public double[] get(int index, int count) throws IOException {
                return matrix.get(column, index, count);
            }
        };
    }

}
