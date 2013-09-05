package org.rrd4j.core;

import java.io.IOException;

/**
 * Class to represent RRD header. Header information is mainly static (once set, it
 * cannot be changed), with the exception of last update time (this value is changed whenever
 * RRD gets updated).<p>
 *
 * Normally, you don't need to manipulate the Header object directly - Rrd4j framework
 * does it for you.<p>
 *
 * @author Sasa Markovic*
 */
public class Header implements RrdUpdater {
    private final RrdDb parentDb;

    private final org.rrd4j.backend.spi.Header spi;

    Header(RrdDb parentDb, RrdDef rrdDef) throws IOException {
        this.parentDb = parentDb;
        spi = parentDb.getRrdBackend().getHeader();

        if (rrdDef != null) {
            spi.version = rrdDef.getVersion();
            spi.step = rrdDef.getStep();
            spi.dsCount = rrdDef.getDsCount();
            spi.arcCount = rrdDef.getArcCount();
            spi.setLastUpdateTime(rrdDef.getStartTime());
            spi.save();
        }
    }

    Header(RrdDb parentDb, DataImporter reader) throws IOException {
        this(parentDb, (RrdDef) null);

        int version = reader.getVersion();
        if (version != 1 && version != 3 ) {
            throw new IllegalArgumentException("Could not unserialize xml version " + version);
        }
        spi.step = reader.getStep();
        spi.dsCount = reader.getDsCount();
        spi.arcCount = reader.getArcCount();
        spi.setLastUpdateTime(reader.getLastUpdateTime());
        spi.save();
    }

    /**
     * Returns RRD signature. Initially, the returned string will be
     * of the form <b><i>Rrd4j, version x.x</i></b>.
     *
     * @return RRD signature
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public String getSignature() throws IOException {
        return spi.getSignature();
    }

    /**
     * <p>getInfo.</p>
     *
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public String getInfo() throws IOException {
        return spi.getInfo();
    }

    /**
     * Returns the last update time of the RRD.
     *
     * @return Timestamp (Unix epoch, no milliseconds) corresponding to the last update time.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public long getLastUpdateTime() throws IOException {
        return spi.getLastUpdateTime();
    }

    /**
     * Returns primary RRD time step.
     *
     * @return Primary time step in seconds
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public long getStep() throws IOException {
        return spi.step;
    }

    /**
     * Returns the number of datasources defined in the RRD.
     *
     * @return Number of datasources defined
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public int getDsCount() throws IOException {
        return spi.dsCount;
    }

    /**
     * Returns the number of archives defined in the RRD.
     *
     * @return Number of archives defined
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public int getArcCount() {
        return spi.arcCount;
    }

    void setLastUpdateTime(long lastUpdateTime) throws IOException {
        spi.setLastUpdateTime(lastUpdateTime);
    }

    String dump() throws IOException {
        return "== HEADER ==\n" +
                "signature:" + getSignature() +
                " lastUpdateTime:" + getLastUpdateTime() +
                " step:" + spi.step +
                " dsCount:" + getDsCount() +
                " arcCount:" + getArcCount() + "\n";
    }

    void appendXml(XmlWriter writer) throws IOException {
        writer.writeComment(spi.getSignature());
        writer.writeTag("version", "0003");
        writer.writeComment("Seconds");
        writer.writeTag("step", spi.step);
        writer.writeComment(Util.getDate(spi.getLastUpdateTime()));
        writer.writeTag("lastupdate", spi.getLastUpdateTime());
    }

    /**
     * {@inheritDoc}
     *
     * Copies object's internal state to another Header object.
     */
    public void copyStateTo(RrdUpdater other) throws IOException {
        if (!(other instanceof Header)) {
            throw new IllegalArgumentException(
                    "Cannot copy Header object to " + other.getClass().getName());
        }
        Header header = (Header) other;
        header.setLastUpdateTime(spi.getLastUpdateTime());
    }

    /**
     * Returns the underlying storage (backend) object which actually performs all
     * I/O operations.
     *
     * @return I/O backend object
     */
    public RrdBackend getRrdBackend() {
        return parentDb.getRrdBackend();
    }

    /**
     * Return the RRD version.
     *
     * @return RRD version
     * @throws java.io.IOException if any.
     */
    public int getVersion() throws IOException {
        return spi.version;
    }

    public void validateHeader() throws IOException {
        spi.validateHeader();
    }

}
