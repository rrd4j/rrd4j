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
    protected final RrdAllocator allocator;
    protected final RrdBinaryBackend backend;

    private RrdString signature;
    private RrdLong step;
    private RrdInt dsCount, arcCount;
    private RrdLong lastUpdateTime;
   
    HeaderBinary(RrdAllocator allocator, RrdBinaryBackend backend) throws IOException {
        this.allocator = allocator;
        this.backend = backend;

        this.signature = new RrdString(this);
        this.step = new RrdLong(this, true);
        super.step = this.step.get();
        this.dsCount = new RrdInt(this, true);
        super.dsCount = this.dsCount.get();
        this.arcCount = new RrdInt(this, true);
        super.arcCount = this.arcCount.get();
        this.lastUpdateTime = new RrdLong(this);
    }

    /**
     * Returns RRD signature. Initially, the returned string will be
     * of the form <b><i>Rrd4j, version x.x</i></b>.
     *
     * @return RRD signature
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public String getSignature() throws IOException {
        return signature.get();
    }

    /**
     * Returns the last update time of the RRD.
     *
     * @return Timestamp (Unix epoch, no milliseconds) corresponding to the last update time.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public long getLastUpdateTime() throws IOException {
        return lastUpdateTime.get();
    }

    public void setLastUpdateTime(long lastUpdateTime) throws IOException {
        this.lastUpdateTime.set(lastUpdateTime);
    }

    @Override
    public void update() throws IOException {
        this.step.set(super.step);
        this.dsCount.set(super.dsCount);
        this.arcCount.set(super.arcCount);
    }

    @Override
    public void flush() {
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
        this.signature.set(signature);
    }

}
