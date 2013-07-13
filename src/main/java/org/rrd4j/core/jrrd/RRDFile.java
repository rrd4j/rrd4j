package org.rrd4j.core.jrrd;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is used read information from an RRD file. Writing
 * to RRD files is not currently supported. It uses NIO's RandomAccessFile to read the file
 * <p/>
 * Currently this can read RRD files that were generated on Solaris (Sparc)
 * and Linux (x86).
 *
 * @author <a href="mailto:ciaran@codeloop.com">Ciaran Treanor</a>
 * @version $Revision: 1.1 $
 */
class RRDFile implements Constants {
    private int alignment;
    private int longSize = 4;
    final RandomAccessFile ras;

    private ByteBuffer bbuffer = ByteBuffer.allocate(1024);
    private byte[] buffer = bbuffer.array();
    private ByteOrder order;

    RRDFile(String name) throws IOException {
        this(new File(name));
    }

    RRDFile(File file) throws IOException {
        ras = new RandomAccessFile(file, "r");
        initDataLayout(file);
    }

    private int read(int len) throws IOException {
        bbuffer.clear();
        int read = ras.read(buffer, 0, len);
        return read;
    }

    private void initDataLayout(File file) throws IOException {

        if (file.exists()) {    // Load the data formats from the file
            read(32);

            int index;

            if ((index = indexOf(FLOAT_COOKIE_BIG_ENDIAN, buffer)) != -1) {
                order = ByteOrder.BIG_ENDIAN;
            }
            else if ((index = indexOf(FLOAT_COOKIE_LITTLE_ENDIAN, buffer))
                    != -1) {
                order = ByteOrder.LITTLE_ENDIAN;
            }
            else {
                throw new IOException("Invalid RRD file");
            }
            bbuffer.order(order);

            switch (index) {

            case 12:
                alignment = 4;
                break;

            case 16:
                alignment = 8;
                break;

            default:
                throw new RuntimeException("Unsupported architecture");
            }

            bbuffer.position(index + 8);
            //We cannot have dsCount && rracount == 0
            //If one is 0, it's a 64 bits rrd
            int int1 = bbuffer.getInt();  //Should be dsCount in ILP32
            int int2 = bbuffer.getInt();  //Should be rraCount in ILP32
            if(int1  == 0 || int2 ==0) {
                longSize = 8;
            }
        }
        else {                // Default to data formats for this hardware architecture
        }
        ras.seek(0);    // Reset file pointer to start of file
    }

    private int indexOf(byte[] pattern, byte[] array) {
        return (new String(array)).indexOf(new String(pattern));
    }

    boolean isBigEndian() {
        return order == ByteOrder.BIG_ENDIAN;
    }

    int getAlignment() {
        return alignment;
    }

    double readDouble() throws IOException {
        read(8);
        return bbuffer.getDouble();
    }

    int readInt() throws IOException {
        read(4);
        return bbuffer.getInt();
    }

    int readLong() throws IOException {
        read(longSize);
        if(longSize == 4) {
            return bbuffer.getInt();
        }
        else {
            return (int) bbuffer.getLong();
        }
    }

    String readString(int maxLength) throws IOException {

        ras.read(buffer, 0, maxLength);

        return new String(buffer, 0, maxLength).trim();
    }

    void skipBytes(int n) throws IOException {
        ras.skipBytes(n);
    }

    int align(int boundary) throws IOException {

        int skip = (int) (boundary - (ras.getFilePointer() % boundary)) % boundary;

        if (skip != 0) {
            ras.skipBytes(skip);
        }

        return skip;
    }

    int align() throws IOException {
        return align(alignment);
    }

    long info() throws IOException {
        return ras.getFilePointer();
    }

    long getFilePointer() throws IOException {
        return ras.getFilePointer();
    }

    void close() throws IOException {
        ras.close();
    }

    void read(ByteBuffer bb) throws IOException{
        ras.getChannel().read(bb);
    }

    UnivalArray getUnivalArray(int size) throws IOException {
        return new UnivalArray(this, size);
    }

    /**
     * @return the long size in bits for this file
     */
    int getBits() {
        return longSize * 8;
    }
}
