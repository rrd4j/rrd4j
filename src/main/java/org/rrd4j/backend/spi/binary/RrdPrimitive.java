package org.rrd4j.backend.spi.binary;

import java.io.IOException;

abstract class RrdPrimitive {
    protected enum RrdType {
        INT(4),
        LONG(8),
        DOUBLE(8),
        STRING(2 * STRING_LENGTH);

        public final int size;
        private RrdType(int size) {
            this.size = size;
        };
    };

    static final int STRING_LENGTH = 20;

    private RrdBinaryBackend backend;
    private int byteCount;
    private final long pointer;
    private final boolean cachingAllowed;

    RrdPrimitive(Allocated updater, RrdType type, boolean isConstant) throws IOException {
        this(updater, type, 1, isConstant);
    }

    RrdPrimitive(Allocated updater, RrdType type, int count, boolean isConstant) throws IOException {
        this.backend = updater.getRrdBackend();
        this.byteCount = type.size * count;
        this.pointer = updater.getRrdAllocator().allocate(byteCount);
        this.cachingAllowed = isConstant || backend.isCachingAllowed();
    }

    final byte[] readBytes() throws IOException {
        byte[] b = new byte[byteCount];
        backend.read(pointer, b);
        return b;
    }

    final void writeBytes(byte[] b) throws IOException {
        assert b.length == byteCount : "Invalid number of bytes supplied to RrdPrimitive.write method";
        backend.write(pointer, b);
    }

    final int readInt() throws IOException {
        return backend.readInt(pointer);
    }

    final void writeInt(int value) throws IOException {
        backend.writeInt(pointer, value);
    }

    final long readLong() throws IOException {
        return backend.readLong(pointer);
    }

    final void writeLong(long value) throws IOException {
        backend.writeLong(pointer, value);
    }

    final double readDouble() throws IOException {
        return backend.readDouble(pointer);
    }

    final double readDouble(int index) throws IOException {
        long offset = pointer + index * RrdType.DOUBLE.size;
        return backend.readDouble(offset);
    }

    final double[] readDouble(int index, int count) throws IOException {
        long offset = pointer + index * RrdType.DOUBLE.size;
        return backend.readDouble(offset, count);
    }

    final void writeDouble(double value) throws IOException {
        backend.writeDouble(pointer, value);
    }

    final void writeDouble(int index,  double value) throws IOException {
        long offset = pointer + index * RrdType.DOUBLE.size;
        backend.writeDouble(offset, value);
    }

    final void writeDouble(int index, double value, int count) throws IOException {
        long offset = pointer + index * RrdType.DOUBLE.size;
        backend.writeDouble(offset, value, count);
    }

    final void writeDouble(int index, double[] values) throws IOException {
        long offset = pointer + index * RrdType.DOUBLE.size;
        backend.writeDouble(offset, values);
    }

    final String readString() throws IOException {
        return backend.readString(pointer);
    }

    final void writeString(String value) throws IOException {
        backend.writeString(pointer, value);
    }

    final boolean isCachingAllowed() {
        return cachingAllowed;
    }

}
