package org.rrd4j.backend.spi.binary;

import java.io.IOException;

import org.rrd4j.backend.spi.Header;

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
public class HeaderBinary extends Header implements Allocated {
    static final int SIGNATURE_LENGTH = 5;
    static final String SIGNATURE = "RRD4J";

    static final String DEFAULT_SIGNATURE = "RRD4J, version 0.2";
    static final private String VERSIONS[] = {"version 0.1", "version 0.2"};

    protected final RrdAllocator allocator;
    protected final RrdBinaryBackend backend;

    private RrdString signatureP;
    private RrdLong stepP;
    private RrdInt dsCountP, arcCountP;
    private RrdLong lastUpdateTimeP;
   
    HeaderBinary(RrdAllocator allocator, RrdBinaryBackend backend) throws IOException {
        this.allocator = allocator;
        this.backend = backend;

        this.signatureP = new RrdString(this);
        this.stepP = new RrdLong(this, true);
        super.step = this.stepP.get();
        this.dsCountP = new RrdInt(this, true);
        super.dsCount = this.dsCountP.get();
        this.arcCountP = new RrdInt(this, true);
        super.arcCount = this.arcCountP.get();
        this.lastUpdateTimeP = new RrdLong(this);
    }

    /**
     * Returns RRD signature. Initially, the returned string will be
     * of the form <b><i>Rrd4j, version x.x</i></b>.
     *
     * @return RRD signature
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public String getSignature() throws IOException {
        return signatureP.get();
    }

    /**
     * Returns the last update time of the RRD.
     *
     * @return Timestamp (Unix epoch, no milliseconds) corresponding to the last update time.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public long getLastUpdateTime() throws IOException {
        return lastUpdateTimeP.get();
    }

    public void setLastUpdateTime(long lastUpdateTime) throws IOException {
        this.lastUpdateTimeP.set(lastUpdateTime);
    }

    @Override
    public void save() throws IOException {
        String initSignature = null; 
        if(version != -1) {
            initSignature = SIGNATURE + ", " + VERSIONS[ version - 1];           
        }
        else {
            initSignature = DEFAULT_SIGNATURE;
        }
        signatureP.set(initSignature);
        stepP.set(step);
        dsCountP.set(dsCount);
        arcCountP.set(arcCount);
    }

    @Override
    public void load() throws IOException {
        step = stepP.get();
        dsCount = dsCountP.get();
        arcCount = arcCountP.get();
        for(int i=0; i < VERSIONS.length; i++) {
            if(signatureP.get().endsWith(VERSIONS[i])) {
                version = i + 1;
                break;
            }
        }
    }

    @Override
    public RrdAllocator getRrdAllocator() {
        return allocator;
    }

    @Override
    public RrdBinaryBackend getRrdBackend() {
        return backend;
    }

    @Override
    public void setSignature(String signature) throws IOException {
        this.signatureP.set(signature);
    }

    boolean isRrd4jHeader() throws IOException {
        return getSignature().startsWith(SIGNATURE) || getSignature().startsWith("JR"); // backwards compatible with JRobin
    }

    public void validateHeader() throws IOException {
        if (!isRrd4jHeader()) {
            throw new IOException("Invalid file header. File [" + backend.getUniqId() + "] is not a RRD4J RRD file");
        }
    }

    /**
     * Return the RRD version.
     *
     * @return RRD version
     * @throws java.io.IOException if any.
     */
    public int getVersion() throws IOException {
        if(version < 0) {
        }
        return version;
    }

    @Override
    public String getInfo() throws IOException {
        return signatureP.get().substring(SIGNATURE_LENGTH);
    }

}
