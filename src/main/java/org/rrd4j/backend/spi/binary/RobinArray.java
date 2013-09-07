package org.rrd4j.backend.spi.binary;

import java.io.IOException;

/**
 * Class to represent archive values for a single datasource. Robin class is the heart of
 * the so-called "round robin database" concept. Basically, each Robin object is a
 * fixed length array of double values. Each double value represents consolidated, archived
 * value for the specific timestamp. When the underlying array of double values gets completely
 * filled, new values will replace the oldest ones.<p>
 * <p/>
 * Robin object does not hold values in memory - such object could be quite large.
 * Instead of it, Robin reads them from the backend I/O only when necessary.
 *
 * @author Sasa Markovic
 */
class RobinArray extends RobinBinary {

    RobinArray(RrdBinaryBackend backend, int rows) throws IOException {
        super(backend, rows);
        this.pointer = new RrdInt(this);
        final RrdDoubleArray valuesVector =  new RrdDoubleArray(this, rows);
        this.values = new RobinBinary.RrdDoubleVector() {

            public void set(int index, double value) throws IOException {
                valuesVector.set(index, value);
            }

            public void set(int index, double value, int count) throws IOException {
                valuesVector.set(index, value);
            }
            
            public void set(int index, double[] values) throws IOException {
                valuesVector.writeDouble(index, values);
            }

            public double get(int index) throws IOException {
                return valuesVector.get(index);
            }

            public double[] get(int index, int count) throws IOException {
                return valuesVector.get(index, count);
            }
        };

    }

}
