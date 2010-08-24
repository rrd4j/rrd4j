/*
 * Copyright (C) 2001 Ciaran Treanor <ciaran@codeloop.com>
 *
 * Distributable under GPL license.
 * See terms of license at gnu.org.
 *
 * $Id: Header.java,v 1.1 2006/02/03 08:27:16 sasam Exp $
 */
package org.rrd4j.core.jrrd;

import java.io.IOException;

/**
 * Instances of this class model the header section of an RRD file.
 *
 * @author <a href="mailto:ciaran@codeloop.com">Ciaran Treanor</a>
 * @version $Revision: 1.1 $
 */
public class Header implements Constants {

    static final long offset = 0;
    long size;
    String version;
    int dsCount;
    int rraCount;
    int pdpStep;

    Header(RRDFile file) throws IOException {

        if (!file.readString(4).equals(COOKIE)) {
            throw new IOException("Invalid COOKIE");
        }

        if (!(version = file.readString(5)).equals(VERSION)) {
            throw new IOException("Unsupported RRD version (" + version + ")");
        }

        file.align();

        // Consume the FLOAT_COOKIE
        file.readDouble();

        dsCount = file.readInt();
        rraCount = file.readInt();
        pdpStep = file.readInt();

        // Skip rest of stat_head_t.par
        file.align();
        file.skipBytes(80);

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
