package org.rrd4j.core.jrrd;

import java.io.IOException;

/**
 * Instances of this class model the header section of an RRD file.
 *
 * @author <a href="mailto:ciaran@codeloop.com">Ciaran Treanor</a>
 * @version $Revision: 1.1 $
 */
public class Header implements Constants {

    private final static double FLOAT_COOKIE = 8.642135E130;
    private static final long offset = 0;
    private long size;
    String version = UNDEFINED_VERSION;
    private int iVersion = UNDEFINED_VERSION_AS_INT;
    int dsCount;
    int rraCount;
    int pdpStep;

    Header(RRDFile file) throws IOException {

        if (!file.readString(4).equals(COOKIE)) {
            throw new IOException("Invalid COOKIE");
        }
        version = file.readString(5);
        try {
            iVersion = Integer.parseInt(version);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Unsupported RRD version (" + version + ")");
        }
        if (iVersion > MAX_SUPPORTED_VERSION) {
            throw new RuntimeException("Unsupported RRD version (" + version + ")");
        }

        file.align();

        // Consume the FLOAT_COOKIE
        double cookie = file.readDouble();
        if(cookie != FLOAT_COOKIE) {
            throw new RuntimeException("This RRD was created on another architecture");
        }

        dsCount = file.readLong();
        rraCount = file.readLong();
        pdpStep = file.readLong();

        // Skip rest of stat_head_t.par
        file.align();
        @SuppressWarnings("unused")
        UnivalArray par = file.getUnivalArray(10);

        size = file.getFilePointer() - offset;
    }

    /**
     * Returns the version of the database.
     *
     * @return the version of the database.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the version of the database as an integer.
     *
     * @return the version of the database.
     */
    public int getVersionAsInt() {
        return iVersion;
    }

   /**
     * Returns the number of <code>DataSource</code>s in the database.
     *
     * @return the number of <code>DataSource</code>s in the database.
     */
    public int getDSCount() {
        return dsCount;
    }

    /**
     * Returns the number of <code>Archive</code>s in the database.
     *
     * @return the number of <code>Archive</code>s in the database.
     */
    public int getRRACount() {
        return rraCount;
    }

    /**
     * Returns the primary data point interval in seconds.
     *
     * @return the primary data point interval in seconds.
     */
    public int getPDPStep() {
        return pdpStep;
    }

    /**
     * Returns a summary the contents of this header.
     *
     * @return a summary of the information contained in this header.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("[Header: OFFSET=0x00, SIZE=0x");

        sb.append(Long.toHexString(size));
        sb.append(", version=");
        sb.append(version);
        sb.append(", dsCount=");
        sb.append(dsCount);
        sb.append(", rraCount=");
        sb.append(rraCount);
        sb.append(", pdpStep=");
        sb.append(pdpStep);
        sb.append("]");

        return sb.toString();
    }
}
