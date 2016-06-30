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

    private static enum cdp_par_en {
        CDP_VAL, CDP_UNKN_PDP_CNT, CDP_HW_INTERCEPT, CDP_HW_LAST_INTERCEPT, CDP_HW_SLOPE,
        CDP_HW_LAST_SLOPE, CDP_NULL_COUNT,
        CDP_LAST_NULL_COUNT, CDP_PRIMARY_VAL, CDP_SECONDARY_VAL
    };

    final long offset;
    final long size;
    final int unknownDatapoints;
    final double value;

    final double secondary_value;
    final double primary_value;

    CDPStatusBlock(RRDFile file) throws IOException {
        //Should read MAX_CDP_PAR_EN = 10
        //Size should be 0x50
        offset = file.getFilePointer();
        UnivalArray scratch = file.getUnivalArray(10);
        value = scratch.getDouble(cdp_par_en.CDP_VAL);
        unknownDatapoints = (int) scratch.getDouble(cdp_par_en.CDP_UNKN_PDP_CNT);
        primary_value = scratch.getDouble(cdp_par_en.CDP_PRIMARY_VAL);
        secondary_value = scratch.getDouble(cdp_par_en.CDP_SECONDARY_VAL);

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
        sb.append(", primaryValue=");
        sb.append(primary_value);
        sb.append(", secondaryValue=");
        sb.append(secondary_value);
        sb.append("]");

        return sb.toString();
    }
}
