/*
 * Copyright (C) 2001 Ciaran Treanor <ciaran@codeloop.com>
 *
 * Distributable under GPL license.
 * See terms of license at gnu.org.
 *
 * $Id: RRDFile.java,v 1.1 2006/02/03 08:27:16 sasam Exp $
 */
package org.rrd4j.core.jrrd;

import java.io.*;

/**
 * This class is a quick hack to read information from an RRD file. Writing
 * to RRD files is not currently supported. As I said, this is a quick hack.
 * Some thought should be put into the overall design of the file IO.
 * <p/>
 * Currently this can read RRD files that were generated on Solaris (Sparc)
 * and Linux (x86).
 *
 * @author <a href="mailto:ciaran@codeloop.com">Ciaran Treanor</a>
 * @version $Revision: 1.1 $
 */
public class RRDFile implements Constants {

    boolean bigEndian;
    int alignment;
    RandomAccessFile ras;
    byte[] buffer;

    RRDFile(String name) throws IOException {
        this(new File(name));
    }

    RRDFile(File file) throws IOException {

        ras = new RandomAccessFile(file, "r");
        buffer = new byte[128];

        initDataLayout(file);
    }

    private void initDataLayout(File file) throws IOException {

        if (file.exists()) {    // Load the data formats from the file
            ras.read(buffer, 0, 24);

            int index;

            if ((index = indexOf(FLOAT_COOKIE_BIG_ENDIAN, buffer)) != -1) {
                bigEndian = true;
            }
            else if ((index = indexOf(FLOAT_COOKIE_LITTLE_ENDIAN, buffer))
                    != -1) {
                bigEndian = false;
            }
            else {
                throw new IOException("Invalid RRD file");
            }

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
        }
        else {                // Default to data formats for this hardware architecture
        }

        ras.seek(0);    // Reset file pointer to start of file
    }

    private int indexOf(byte[] pattern, byte[] array) {
        return (new String(array)).indexOf(new String(pattern));
    }

    boolean isBigEndian() {
        return bigEndian;
    }

    int getAlignment() {
        return alignment;
    }

    double readDouble() throws IOException {

        //double value;
        byte[] tx = new byte[8];

        ras.read(buffer, 0, 8);

        if (bigEndian) {
            tx = buffer;
        }
        else {
            for (int i = 0; i < 8; i++) {
                tx[7 - i] = buffer[i];
            }
        }

        DataInputStream reverseDis =
                new DataInputStream(new ByteArrayInputStream(tx));

        return reverseDis.readDouble();
    }

    int readInt() throws IOException {
        return readInt(false);
    }

    int readInt(boolean dump) throws IOException {

        ras.read(buffer, 0, 4);

        int value;

        if (bigEndian) {
            value = (0xFF & buffer[3]) | ((0xFF & buffer[2]) << 8)
                    | ((0xFF & buffer[1]) << 16) | ((0xFF & buffer[0]) << 24);
        }
        else {
            value = (0xFF & buffer[0]) | ((0xFF & buffer[1]) << 8)
                    | ((0xFF & buffer[2]) << 16) | ((0xFF & buffer[3]) << 24);
        }

        return value;
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
}
