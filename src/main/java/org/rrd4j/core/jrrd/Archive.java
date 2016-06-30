package org.rrd4j.core.jrrd;

import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Instances of this class model an archive section of an RRD file.
 *
 * @author <a href="mailto:ciaran@codeloop.com">Ciaran Treanor</a>
 * @version $Revision: 1.1 $
 */
public class Archive {

    private static enum rra_par_en {RRA_CDP_XFF_VAL, RRA_HW_ALPHA};

    final RRDatabase db;
    final long offset;
    long dataOffset;
    long size;
    final ConsolidationFunctionType type;
    final int rowCount;
    final int pdpCount;
    final double xff;
    List<CDPStatusBlock> cdpStatusBlocks;
    int currentRow;

    private double[][] values;

    Archive(RRDatabase db) throws IOException {

        this.db = db;

        RRDFile file = db.rrdFile;

        offset = file.getFilePointer();
        type = ConsolidationFunctionType.valueOf(file.readString(Constants.CF_NAM_SIZE).toUpperCase());
        file.align(file.getBits() / 8);
        rowCount = file.readLong();
        pdpCount = file.readLong();

        UnivalArray par = file.getUnivalArray(10);
        xff = par.getDouble(rra_par_en.RRA_CDP_XFF_VAL);

        size = file.getFilePointer() - offset;
    }

    /**
     * Returns the type of function used to calculate the consolidated data point.
     *
     * @return the type of function used to calculate the consolidated data point.
     */
    public ConsolidationFunctionType getType() {
        return type;
    }

    void loadCDPStatusBlocks(RRDFile file, int numBlocks) throws IOException {

        cdpStatusBlocks = new ArrayList<CDPStatusBlock>();

        for (int i = 0; i < numBlocks; i++) {
            cdpStatusBlocks.add(new CDPStatusBlock(file));
        }
    }

    /**
     * Returns the <code>CDPStatusBlock</code> at the specified position in this archive.
     *
     * @param index index of <code>CDPStatusBlock</code> to return.
     * @return the <code>CDPStatusBlock</code> at the specified position in this archive.
     */
    public CDPStatusBlock getCDPStatusBlock(int index) {
        return cdpStatusBlocks.get(index);
    }

    /**
     * Returns an iterator over the CDP status blocks in this archive in proper sequence.
     *
     * @return an iterator over the CDP status blocks in this archive in proper sequence.
     * @see CDPStatusBlock
     */
    public Iterator<CDPStatusBlock> getCDPStatusBlocks() {
        return cdpStatusBlocks.iterator();
    }

    void loadCurrentRow(RRDFile file) throws IOException {
        currentRow = file.readLong();
    }

    void loadData(RRDFile file, int dsCount) throws IOException {

        dataOffset = file.getFilePointer();

        // Skip over the data to position ourselves at the start of the next archive
        file.skipBytes(8 * rowCount * dsCount);
    }

    void loadData(DataChunk chunk)
            throws IOException {

        long pointer;

        if (chunk.start < 0) {
            pointer = currentRow + 1;
        }
        else {
            pointer = currentRow + chunk.start + 1;
        }

        db.rrdFile.seek((dataOffset + (chunk.dsCount * pointer * 8)));

        double[][] data = chunk.data;

        /*
         * This is also terrible - cleanup - CT
         */
        int row = 0;
        for (int i = chunk.start; i < rowCount - chunk.end; i++, row++) {
            if (i < 0) {                   // no valid data yet
                for (int ii = 0; ii < chunk.dsCount; ii++) {
                    data[row][ii] = Double.NaN;
                }
            }
            else if (i >= rowCount) {    // past valid data area
                for (int ii = 0; ii < chunk.dsCount; ii++) {
                    data[row][ii] = Double.NaN;
                }
            }
            else {                       // inside the valid are but the pointer has to be wrapped
                if (pointer >= rowCount) {
                    pointer -= rowCount;

                    db.rrdFile.seek(dataOffset + (chunk.dsCount * pointer * 8));
                }

                for (int ii = 0; ii < chunk.dsCount; ii++) {
                    data[row][ii] = db.rrdFile.readDouble();
                }

                pointer++;
            }
        }
    }

    void printInfo(PrintStream s, NumberFormat numberFormat, int index) {

        StringBuilder sb = new StringBuilder("rra[");

        sb.append(index);
        s.print(sb);
        s.print("].cf = \"");
        s.print(type);
        s.println("\"");
        s.print(sb);
        s.print("].rows = ");
        s.println(rowCount);
        s.print(sb);
        s.print("].pdp_per_row = ");
        s.println(pdpCount);
        s.print(sb);
        s.print("].xff = ");
        s.println(xff);
        sb.append("].cdp_prep[");

        int cdpIndex = 0;

        for (Iterator<CDPStatusBlock> i = cdpStatusBlocks.iterator(); i.hasNext();) {
            CDPStatusBlock cdp = i.next();

            s.print(sb);
            s.print(cdpIndex);
            s.print("].value = ");

            double value = cdp.value;

            s.println(Double.isNaN(value)
                    ? "NaN"
                            : numberFormat.format(value));
            s.print(sb);
            s.print(cdpIndex++);
            s.print("].unknown_datapoints = ");
            s.println(cdp.unknownDatapoints);
        }
    }

    void toXml(PrintStream s) {

        try {
            s.println("\t<rra>");
            s.print("\t\t<cf> ");
            s.print(type);
            s.println(" </cf>");
            s.print("\t\t<pdp_per_row> ");
            s.print(pdpCount);
            s.print(" </pdp_per_row> <!-- ");
            s.print(db.header.pdpStep * pdpCount);
            s.println(" seconds -->");
            s.print("\t\t<xff> ");
            s.print(xff);
            s.println(" </xff>");
            s.println();
            s.println("\t\t<cdp_prep>");

            for (int i = 0; i < cdpStatusBlocks.size(); i++) {
                cdpStatusBlocks.get(i).toXml(s);
            }

            s.println("\t\t</cdp_prep>");
            s.println("\t\t<database>");

            long timer = -(rowCount - 1);
            int counter = 0;
            int row = currentRow;

            db.rrdFile.seek(dataOffset + (row + 1) * db.header.dsCount * 8);

            long lastUpdate = db.lastUpdate.getTime() / 1000;
            int pdpStep = db.header.pdpStep;
            NumberFormat numberFormat = new DecimalFormat("0.0000000000E0", DecimalFormatSymbols.getInstance(Locale.US));
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

            while (counter++ < rowCount) {
                row++;

                if (row == rowCount) {
                    row = 0;

                    db.rrdFile.seek(dataOffset);
                }

                long now = (lastUpdate - lastUpdate % (pdpCount * pdpStep))
                        + (timer * pdpCount * pdpStep);

                timer++;

                s.print("\t\t\t<!-- ");
                s.print(dateFormat.format(new Date(now * 1000)));
                s.print(" / ");
                s.print(now);
                s.print(" --> ");

                s.println("<row>");
                for (int col = 0; col < db.header.dsCount; col++) {
                    s.print("<v> ");

                    double value = db.rrdFile.readDouble();

                    // NumberFormat doesn't know how to handle NaN
                    if (Double.isNaN(value)) {
                        s.print("NaN");
                    }
                    else {
                        s.print(numberFormat.format(value));
                    }

                    s.print(" </v>");
                }

                s.println("</row>");
            }

            s.println("\t\t</database>");
            s.println("\t</rra>");
        }
        catch (IOException e) {    // Is the best thing to do here?
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * <p>Getter for the field <code>values</code>.</p>
     *
     * @return an array of double.
     * @throws java.io.IOException if any.
     */
    public double[][] getValues() throws IOException {
        if (values != null) {
            return values;
        }
        values = new double[db.header.dsCount][rowCount];
        int row = currentRow;
        db.rrdFile.seek(dataOffset + (row + 1) * db.header.dsCount * 8);
        for (int counter = 0; counter < rowCount; counter++) {
            row++;
            if (row == rowCount) {
                row = 0;
                db.rrdFile.seek(dataOffset);
            }
            for (int col = 0; col < db.header.dsCount; col++) {
                double value = db.rrdFile.readDouble();
                values[col][counter] = value;
            }
        }
        return values;
    }

    /**
     * Returns the number of primary data points required for a consolidated
     * data point in this archive.
     *
     * @return the number of primary data points required for a consolidated
     *         data point in this archive.
     */
    public int getPdpCount() {
        return pdpCount;
    }

    /**
     * Returns the number of entries in this archive.
     *
     * @return the number of entries in this archive.
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * Returns the X-Files Factor for this archive.
     *
     * @return the X-Files Factor for this archive.
     */
    public double getXff() {
        return xff;
    }

    /**
     * Returns a summary the contents of this archive.
     *
     * @return a summary of the information contained in this archive.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("[Archive: OFFSET=0x");

        sb.append(Long.toHexString(offset))
          .append(", SIZE=0x")
          .append(Long.toHexString(size))
          .append(", type=")
          .append(type)
          .append(", rowCount=")
          .append(rowCount)
          .append(", pdpCount=")
          .append(pdpCount)
          .append(", xff=")
          .append(xff)
          .append(", currentRow=")
          .append(currentRow)
          .append("]");

        for(CDPStatusBlock cdp: cdpStatusBlocks) {
            sb.append("\n\t\t");
            sb.append(cdp.toString());
        }

        return sb.toString();
    }
}
