package org.rrd4j.backends;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

public abstract class ByteBufferBackend extends RrdBackend {

    private ByteBuffer byteBuffer;

    protected ByteBufferBackend(String path) {
        super(path);
    }

    protected void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        byteBuffer.order(BYTEORDER);
    }

    /**
     * Writes bytes to the underlying RRD file on the disk
     *
     * @param offset Starting file offset
     * @param b      Bytes to be written.
     * @throws java.io.IOException if any.
     * @throws java.lang.IllegalArgumentException if offset is bigger that the possible mapping position (2GiB).
     */
    public synchronized void write(long offset, byte[] b) throws IOException {
        checkOffset(offset);
        byteBuffer.put(b, (int) offset, b.length);
    }

    @Override
    public void writeShort(long offset, short value) throws IOException {
        checkOffset(offset);
        byteBuffer.putShort((int)offset, value);
    }

    @Override
    public void writeInt(long offset, int value) throws IOException {
        checkOffset(offset);
        byteBuffer.putInt((int)offset, value);
    }

    @Override
    public void writeLong(long offset, long value) throws IOException {
        checkOffset(offset);
        byteBuffer.putLong((int)offset, value);
    }

    @Override
    public void writeDouble(long offset, double value) throws IOException {
        checkOffset(offset);
        byteBuffer.putDouble((int)offset, value);
    }

    @Override
    public void writeDouble(long offset, double value, int count)
            throws IOException {
        checkOffset(offset);
        double[] values = new double[count];
        Arrays.fill(values, value);
        // position must be set in the original ByteByffer, as DoubleBuffer is a "double" offset
        byteBuffer.position((int)offset);
        byteBuffer.asDoubleBuffer().put(values, 0, count);
    }

    @Override
    public void writeDouble(long offset, double[] values) throws IOException {
        checkOffset(offset);
        // position must be set in the original ByteByffer, as DoubleBuffer is a "double" offset
        byteBuffer.position((int)offset);
        byteBuffer.asDoubleBuffer().put(values, 0, values.length);
    }

    
    @Override
    protected void writeString(long offset, String value, int length) throws IOException {
        checkOffset(offset);
        byteBuffer.position((int)offset);
        CharBuffer cbuff = byteBuffer.asCharBuffer();
        cbuff.limit(length);
        cbuff.put(value);
        while (cbuff.position() < cbuff.limit()) {
            cbuff.put(' ');
        }
    }

    /**
     * Reads a number of bytes from the RRD file on the disk
     *
     * @param offset Starting file offset
     * @param b      Buffer which receives bytes read from the file.
     * @throws java.io.IOException Thrown in case of I/O error.
     * @throws java.lang.IllegalArgumentException if offset is bigger that the possible mapping position (2GiB).
     */
    public synchronized void read(long offset, byte[] b) throws IOException {
        checkOffset(offset);
        byteBuffer.get(b, (int) offset, b.length);
    }

    @Override
    public short readShort(long offset) throws IOException {
        checkOffset(offset);
        return byteBuffer.getShort((int)offset);
    }

    @Override
    public int readInt(long offset) throws IOException {
        checkOffset(offset);
        return byteBuffer.getInt((int)offset);
    }

    @Override
    public long readLong(long offset) throws IOException {
        checkOffset(offset);
        return byteBuffer.getLong((int)offset);
    }

    @Override
    public double readDouble(long offset) throws IOException {
        checkOffset(offset);
        return byteBuffer.getDouble((int)offset);
    }

    @Override
    public double[] readDouble(long offset, int count) throws IOException {
        checkOffset(offset);
        double[] values = new double[count];
        // position must be set in the original ByteByffer, as DoubleBuffer is a "double" offset
        byteBuffer.position((int)offset);
        byteBuffer.asDoubleBuffer().get(values, 0, count);
        return values;
    }

    @Override
    protected CharBuffer getCharBuffer(long offset, int size) {
        checkOffset(offset);
        byteBuffer.position((int)offset);
        CharBuffer cbuffer = byteBuffer.asCharBuffer();
        cbuffer.limit(size);
        return cbuffer;
    }

    public void close() throws IOException {
        byteBuffer = null;
    }

    /**
     * Ensure that the conversion from long offset to integer offset will not overflow
     * @param offset
     */
    private void checkOffset(long offset) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Illegal offset: " + offset);
        }
    }

}
