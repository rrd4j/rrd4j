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
    static final int SIGNATURE_LENGTH = 5;
    static final String SIGNATURE = "RRD4J";

    static final String DEFAULT_SIGNATURE = "RRD4J, version 0.1";
    static final String RRDTOOL_VERSION1 = "0001";
    static final String RRDTOOL_VERSION3 = "0003";
    static final private String VERSIONS[] = {"version 0.1", "version 0.2"};

    private RrdDb parentDb;
    private int version = -1;

    //SPI
    private org.rrd4j.backend.spi.Header spi;

    Header(RrdDb parentDb, RrdDef rrdDef) throws IOException {
        this.parentDb = parentDb;
        spi = parentDb.getRrdBackend().getHeader();

        String initSignature = null;		
        if(rrdDef != null) {
            version = rrdDef.getVersion(); 
            initSignature = SIGNATURE + ", " + VERSIONS[ version - 1];
        }
        else {
            initSignature = DEFAULT_SIGNATURE;
        }


        if (rrdDef != null) {
            spi.setSignature(initSignature);
            spi.step = rrdDef.getStep();
            spi.dsCount = rrdDef.getDsCount();
            spi.arcCount = rrdDef.getArcCount();
            spi.setLastUpdateTime(rrdDef.getStartTime());
        }
        spi.update();
    }

    Header(RrdDb parentDb, DataImporter reader) throws IOException {
        this(parentDb, (RrdDef) null);
        String version = reader.getVersion();
        if (!RRDTOOL_VERSION1.equals(version) && !RRDTOOL_VERSION3.equals(version) ) {
            throw new IllegalArgumentException("Could not unserialize xml version " + version);
        }
        spi.setSignature(DEFAULT_SIGNATURE);
        spi.step = reader.getStep();
        spi.dsCount = reader.getDsCount();
        spi.arcCount = reader.getArcCount();
        spi.setLastUpdateTime(reader.getLastUpdateTime());
        spi.update();
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
        return getSignature().substring(SIGNATURE_LENGTH);
    }

    /**
     * <p>setInfo.</p>
     *
     * @param info a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public void setInfo(String info) throws IOException {
        if (info != null && info.length() > 0) {
            spi.setSignature(SIGNATURE + info);
        }
        else {
            spi.setSignature(SIGNATURE);
        }
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
        writer.writeTag("version", RRDTOOL_VERSION3);
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
        //header.signature.set(signature.get());
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
        if(version < 0) {
            for(int i=0; i < VERSIONS.length; i++) {
                if(spi.getSignature().endsWith(VERSIONS[i])) {
                    version = i + 1;
                    break;
                }
            }
        }
        return version;
    }

    boolean isRrd4jHeader() throws IOException {
        return spi.getSignature().startsWith(SIGNATURE) || spi.getSignature().startsWith("JR"); // backwards compatible with JRobin
    }

    void validateHeader() throws IOException {
        if (!isRrd4jHeader()) {
            throw new IOException("Invalid file header. File [" + parentDb.getCanonicalPath() + "] is not a RRD4J RRD file");
        }
    }

}
