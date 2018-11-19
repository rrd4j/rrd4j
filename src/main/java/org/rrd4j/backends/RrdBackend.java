package org.rrd4j.backends;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;

import org.rrd4j.backends.RrdBackendFactory.ClosingReference;
import org.rrd4j.core.RrdPrimitive;

/**
 * <p>Base implementation class for all backend classes. Each Round Robin Database object
 * ({@link org.rrd4j.core.RrdDb} object) is backed with a single RrdBackend object which performs
 * actual I/O operations on the underlying storage. Rrd4j supports
 * multiple backends out of the box. E.g.:</p>
 * <ul>
 * <li>{@link org.rrd4j.backends.RrdRandomAccessFileBackend}: objects of this class are created from the
 * {@link org.rrd4j.backends.RrdRandomAccessFileBackendFactory} class. This was the default backend used in all
 * Rrd4j releases prior to 1.4.0. It uses java.io.* package and
 * RandomAccessFile class to store RRD data in files on the disk.
 *
 * <li>{@link org.rrd4j.backends.RrdNioBackend}: objects of this class are created from the
 * {@link org.rrd4j.backends.RrdNioBackendFactory} class. The backend uses java.io.* and java.nio.*
 * classes (mapped ByteBuffer) to store RRD data in files on the disk. This backend is fast, very fast,
 * but consumes a lot of memory (borrowed not from the JVM but from the underlying operating system
 * directly). <b>This is the default backend used in Rrd4j since 1.4.0 release.</b>
 *
 * <li>{@link org.rrd4j.backends.RrdMemoryBackend}: objects of this class are created from the
 * {@link org.rrd4j.backends.RrdMemoryBackendFactory} class. This backend stores all data in memory. Once
 * JVM exits, all data gets lost. The backend is extremely fast and memory hungry.
 * </ul>
 *
 * <p>To create your own backend in order to provide some custom type of RRD storage,
 * you should do the following:</p>
 *
 * <ul>
 * <li>Create your custom RrdBackend class (RrdCustomBackend, for example)
 * by extending RrdBackend class. You have to implement all abstract methods defined
 * in the base class.
 *
 * <li>Create your custom RrdBackendFactory class (RrdCustomBackendFactory,
 * for example) by extending RrdBackendFactory class. You have to implement all
 * abstract methods defined in the base class. Your custom factory class will actually
 * create custom backend objects when necessary.
 *
 * <li>Create instance of your custom RrdBackendFactory and register it as a regular
 * factory available to Rrd4j framework. See javadoc for {@link org.rrd4j.backends.RrdBackendFactory} to
 * find out how to do this.
 * </ul>
 *
 * @author Sasa Markovic
 */
public abstract class RrdBackend {

    protected static final ByteOrder BYTEORDER = ByteOrder.BIG_ENDIAN;
    private static final char STARTPRIVATEAREA = '\ue000';
    private static final char ENDPRIVATEAREA = '\uf8ff';
    private static final int STARTPRIVATEAREACODEPOINT = Character.codePointAt(new char[]{STARTPRIVATEAREA}, 0);
    private static final int ENDPRIVATEAREACODEPOINT = Character.codePointAt(new char[]{ENDPRIVATEAREA}, 0);
    private static final int PRIVATEAREASIZE = ENDPRIVATEAREACODEPOINT - STARTPRIVATEAREACODEPOINT + 1;
    private static final int MAXUNSIGNEDSHORT = Short.MAX_VALUE - Short.MIN_VALUE;

    private static boolean instanceCreated = false;
    private final String path;
    private RrdBackendFactory factory;
    private long nextBigStringOffset = -1;
    private ClosingReference ref;

    /**
     * Creates backend for a RRD storage with the given path.
     *
     * @param path String identifying RRD storage. For files on the disk, this
     *             argument should represent file path. Other storage types might interpret
     *             this argument differently.
     */
    protected RrdBackend(String path) {
        this.path = path;
        instanceCreated = true;
    }

    /**
     * 
     * @param factory the factory to set
     */
    void done(RrdBackendFactory factory, ClosingReference ref) {
        this.factory = factory;
        this.ref = ref;
    }

    /**
     * @return the factory
     */
    public RrdBackendFactory getFactory() {
        return factory;
    }

    /**
     * Returns path to the storage.
     *
     * @return Storage path
     */
    public String getPath() {
        return path;
    }

    /**
     * Return the URI associated to this backend, using the factory to generate it from the path.
     * 
     * @return URI to this backend's rrd.
     */
    public URI getUri() {
        return factory.getUri(path);
    }

    /**
     * Writes an array of bytes to the underlying storage starting from the given
     * storage offset.
     *
     * @param offset Storage offset.
     * @param b      Array of bytes that should be copied to the underlying storage
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public abstract void write(long offset, byte[] b) throws IOException;

    /**
     * Reads an array of bytes from the underlying storage starting from the given
     * storage offset.
     *
     * @param offset Storage offset.
     * @param b      Array which receives bytes from the underlying storage
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public abstract void read(long offset, byte[] b) throws IOException;

    /**
     * Returns the number of RRD bytes in the underlying storage.
     *
     * @return Number of RRD bytes in the storage.
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    public abstract long getLength() throws IOException;

    /**
     * Sets the number of bytes in the underlying RRD storage.
     * This method is called only once, immediately after a new RRD storage gets created.
     *
     * @param length Length of the underlying RRD storage in bytes.
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    public abstract void setLength(long length) throws IOException;

    /**
     * Closes the underlying backend.
     *
     * @throws java.io.IOException Thrown in case of I/O error
     */
    protected abstract void close() throws IOException;

    /**
     * Closes the underlying backend.
     *
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public void closing() throws IOException {
        try {
            close();
        } finally {
            if (ref != null) {
                ref.backend = null;
            }
        }
    }

    /**
     * This method suggests the caching policy to the Rrd4j frontend (high-level) classes. If <code>true</code>
     * is returned, frontend classes will cache frequently used parts of a RRD file in memory to improve
     * performance. If <code>false</code> is returned, high level classes will never cache RRD file sections
     * in memory.
     *
     * @return <code>true</code> if file caching is enabled, <code>false</code> otherwise. By default, the
     *         method returns <code>true</code> but it can be overridden in subclasses.
     */
    public boolean isCachingAllowed() {
        return true;
    }

    /**
     * Reads all RRD bytes from the underlying storage.
     *
     * @return RRD bytes
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public final byte[] readAll() throws IOException {
        byte[] b = new byte[(int) getLength()];
        read(0, b);
        return b;
    }

    public void writeShort(long offset, short value) throws IOException {
        byte[] b = new byte[2];
        b[0] = (byte) ((value >>> 8) & 0xFF);
        b[1] = (byte) ((value >>> 0) & 0xFF);
        write(offset, b);
    }

    public void writeInt(long offset, int value) throws IOException {
        write(offset, getIntBytes(value));
    }

    public void writeLong(long offset, long value) throws IOException {
        write(offset, getLongBytes(value));
    }

    public void writeDouble(long offset, double value) throws IOException {
        write(offset, getDoubleBytes(value));
    }

    public void writeDouble(long offset, double value, int count) throws IOException {
        byte[] b = getDoubleBytes(value);
        byte[] image = new byte[8 * count];
        int k = 0;
        for (int i = 0; i < count; i++) {
            image[k++] = b[0];
            image[k++] = b[1];
            image[k++] = b[2];
            image[k++] = b[3];
            image[k++] = b[4];
            image[k++] = b[5];
            image[k++] = b[6];
            image[k++] = b[7];
        }
        write(offset, image);
    }

    public void writeDouble(long offset, double[] values) throws IOException {
        int count = values.length;
        byte[] image = new byte[8 * count];
        int k = 0;
        for (int i = 0; i < count; i++) {
            byte[] b = getDoubleBytes(values[i]);
            image[k++] = b[0];
            image[k++] = b[1];
            image[k++] = b[2];
            image[k++] = b[3];
            image[k++] = b[4];
            image[k++] = b[5];
            image[k++] = b[6];
            image[k++] = b[7];
        }
        write(offset, image);
    }

    public void writeString(long offset, String value) throws IOException {
        if (nextBigStringOffset < 0) {
            nextBigStringOffset = getLength() - (Short.SIZE / 8);
        }
        value = value.trim();
        // Over-sized string are appended at the end of the RRD
        // The real position is encoded in the "short" ds name, using the private use area from Unicode
        // This area span the range E000-F8FF, that' a 6400 char area, 
        if (value.length() > RrdPrimitive.STRING_LENGTH) {
            String bigString = value;
            int byteStringLength = Math.min(MAXUNSIGNEDSHORT, bigString.length());
            long bigStringOffset = nextBigStringOffset;
            nextBigStringOffset -= byteStringLength * 2 + (Short.SIZE / 8);
            writeShort(bigStringOffset, (short)byteStringLength);
            writeString(bigStringOffset - bigString.length() * 2, bigString, byteStringLength);
            // Now we generate the new string that encode the position
            long reminder = bigStringOffset;
            StringBuilder newValue = new StringBuilder(value.substring(0, RrdPrimitive.STRING_LENGTH));
            int i = RrdPrimitive.STRING_LENGTH;
            // Read in inverse order, so write in inverse order
            while (reminder > 0) {
                // Only the first char is kept, as it will never byte a multi-char code point
                newValue.setCharAt(--i, Character.toChars((int)(reminder % PRIVATEAREASIZE + STARTPRIVATEAREACODEPOINT))[0]);
                reminder = (long) Math.floor( ((float)reminder) / (float)PRIVATEAREASIZE);
            }
            value = newValue.toString();
        }
        writeString(offset, value, RrdPrimitive.STRING_LENGTH);
    }

    protected void writeString(long offset, String value, int length) throws IOException {
        ByteBuffer bbuf = ByteBuffer.allocate(length * 2);
        bbuf.order(BYTEORDER);
        bbuf.position(0);
        bbuf.limit(length * 2);
        CharBuffer cbuf = bbuf.asCharBuffer();
        cbuf.put(value);
        while (cbuf.position() < cbuf.limit()) {
            cbuf.put(' ');
        }
        write(offset, bbuf.array());
    }

    public short readShort(long offset) throws IOException {
        byte[] b = new byte[2];
        read(offset, b);
        return (short) (((b[0] << 8) & 0x0000FF00) + (b[1] & 0x000000FF));
    }

    public int readInt(long offset) throws IOException {
        byte[] b = new byte[4];
        read(offset, b);
        return getInt(b);
    }

    public long readLong(long offset) throws IOException {
        byte[] b = new byte[8];
        read(offset, b);
        return getLong(b);
    }

    public double readDouble(long offset) throws IOException {
        byte[] b = new byte[8];
        read(offset, b);
        return getDouble(b);
    }

    public double[] readDouble(long offset, int count) throws IOException {
        int byteCount = 8 * count;
        byte[] image = new byte[byteCount];
        read(offset, image);
        double[] values = new double[count];
        int k = -1;
        for (int i = 0; i < count; i++) {
            byte[] b = new byte[]{
                    image[++k], image[++k], image[++k], image[++k],
                    image[++k], image[++k], image[++k], image[++k]
            };
            values[i] = getDouble(b);
        }
        return values;
    }
    
    /**
     * Extract a CharBuffer from the backend, used by readString
     * 
     * @param offset
     * @param size
     * @return
     * @throws IOException
     */
    protected abstract CharBuffer getCharBuffer(long offset, int size) throws IOException;

    public final String readString(long offset) throws IOException {
        CharBuffer cbuf = getCharBuffer(offset, RrdPrimitive.STRING_LENGTH);
        long realStringOffset = 0;
        int i = -1;
        while (++i < RrdPrimitive.STRING_LENGTH) {
            char c = cbuf.charAt(RrdPrimitive.STRING_LENGTH - i - 1);
            if (c >= STARTPRIVATEAREA && c <= ENDPRIVATEAREA) {
                realStringOffset += ((long) c - STARTPRIVATEAREACODEPOINT) * Math.pow(PRIVATEAREASIZE, i);
                cbuf.put(i, ' ');
            } else {
                break;
            }
        }
        if (realStringOffset > 0) {
            int bigStringSize = readShort(realStringOffset);
            // Signed to unsigned arithmetic
            if (bigStringSize < 0) {
                bigStringSize += MAXUNSIGNEDSHORT + 1;
            }
            return getCharBuffer(realStringOffset - bigStringSize * 2, bigStringSize).toString();
        } else {
            return cbuf.toString().trim();
        }
    }

    // static helper methods

    private static byte[] getIntBytes(int value) {
        byte[] b = new byte[4];
        b[0] = (byte) ((value >>> 24) & 0xFF);
        b[1] = (byte) ((value >>> 16) & 0xFF);
        b[2] = (byte) ((value >>> 8) & 0xFF);
        b[3] = (byte) ((value >>> 0) & 0xFF);
        return b;
    }

    private static byte[] getLongBytes(long value) {
        byte[] b = new byte[8];
        b[0] = (byte) ((int) (value >>> 56) & 0xFF);
        b[1] = (byte) ((int) (value >>> 48) & 0xFF);
        b[2] = (byte) ((int) (value >>> 40) & 0xFF);
        b[3] = (byte) ((int) (value >>> 32) & 0xFF);
        b[4] = (byte) ((int) (value >>> 24) & 0xFF);
        b[5] = (byte) ((int) (value >>> 16) & 0xFF);
        b[6] = (byte) ((int) (value >>> 8) & 0xFF);
        b[7] = (byte) ((int) (value >>> 0) & 0xFF);
        return b;
    }

    private static byte[] getDoubleBytes(double value) {
        return getLongBytes(Double.doubleToLongBits(value));
    }

    private static int getInt(byte[] b) {
        assert b.length == 4 : "Invalid number of bytes for integer conversion";
        return ((b[0] << 24) & 0xFF000000) + ((b[1] << 16) & 0x00FF0000) +
                ((b[2] << 8) & 0x0000FF00) + (b[3] & 0x000000FF);
    }

    private static long getLong(byte[] b) {
        assert b.length == 8 : "Invalid number of bytes for long conversion";
        int high = getInt(new byte[]{b[0], b[1], b[2], b[3]});
        int low = getInt(new byte[]{b[4], b[5], b[6], b[7]});
        return ((long) (high) << 32) + (low & 0xFFFFFFFFL);
    }

    private static double getDouble(byte[] b) {
        assert b.length == 8 : "Invalid number of bytes for double conversion";
        return Double.longBitsToDouble(getLong(b));
    }

    static boolean isInstanceCreated() {
        return instanceCreated;
    }

}
