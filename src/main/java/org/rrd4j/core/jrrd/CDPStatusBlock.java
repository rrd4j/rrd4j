/*
 * Copyright (C) 2001 Ciaran Treanor <ciaran@codeloop.com>
 *
 * Distributable under GPL license.
 * See terms of license at gnu.org.
 *
 * $Id: CDPStatusBlock.java,v 1.1 2006/02/03 08:27:16 sasam Exp $
 */
package org.rrd4j.core.jrrd;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Instances of this class model the consolidation data point status from an RRD file.
 *
 * @author <a href="mailto:ciaran@codeloop.com">Ciaran Treanor</a>
 * @version $Revision: 1.1 $
 */
public class CDPStatusBlock {

    long offset;
    long size;
    int unknownDatapoints;
    double value;

    CDPStatusBlock(RRDFile file) throws IOException {

        offset = file.getFilePointer();
        value = file.readDouble();
        unknownDatapoints = file.readInt();

        // Skip rest of cdp_prep_t.scratch
        file.skipBytes(68);

        size = file.getFilePointer() - offset;
    }

    /**
     * Returns the number of unknown primary data points that were integrated.
     *
     * @return the number of unknown primary data points that were integrated.
     */
    public int getUnknownDatapoints() {
        return unknownDatapoints;
    }

    /**
     * Returns the value of this consolidated data point.
     *
     * @return the value of this consolidated data point.
     */
    public double getValue() {
        return value;
    }

    void toXml(PrintStream s) {

        s.print("\t\t\t<ds><value> ");
        s.print(value);
        s.print(" </value>  <unknown_datapoints> ");
        s.print(unknownDatapoints);
        s.println(" </unknown_datapoints></ds>");
    }

    /**
     * Returns a summary the contents of this CDP status block.
     *
     * @return a summary of the information contained in the CDP status block.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("[CDPStatusBlock: OFFSET=0x");

        sb.append(Long.toHexString(offset));
        sb.append(", SIZE=0x");
        sb.append(Long.toHexString(size));
        sb.append(", unknownDatapoints=");
        sb.append(unknownDatapoints);
        sb.append(", value=");
        sb.append(value);
        sb.append("]");

        return sb.toString();
    }
}
