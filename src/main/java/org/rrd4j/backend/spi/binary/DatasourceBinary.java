package org.rrd4j.backend.spi.binary;

import java.io.IOException;

import org.rrd4j.backend.spi.Datasource;

/**
 * Class to represent single datasource within RRD. Each datasource object holds the
 * following information: datasource definition (once set, never changed) and
 * datasource state variables (changed whenever RRD gets updated).<p>
 * <p/>
 * Normally, you don't need to manipulate Datasource objects directly, it's up to
 * Rrd4j framework to do it for you.
 *
 * @author Sasa Markovic
 */
public class DatasourceBinary extends Datasource implements Allocated {
    protected final RrdAllocator allocator;
    protected final RrdBinaryBackend backend;

    // definition
    private final RrdString dsName, dsType;
    private final RrdLong heartbeat;
    private final RrdDouble minValue, maxValue;

    // state variables
    private RrdDouble lastValue;
    private RrdLong nanSeconds;
    private RrdDouble accumValue;

    DatasourceBinary(RrdAllocator allocator, RrdBinaryBackend backend) throws IOException {
        this.allocator = allocator;
        this.backend = backend;

        dsName = new RrdString(this);
        dsType = new RrdString(this);
        heartbeat = new RrdLong(this);
        minValue = new RrdDouble(this);
        maxValue = new RrdDouble(this);
        lastValue = new RrdDouble(this);
        accumValue = new RrdDouble(this);
        nanSeconds = new RrdLong(this);
    }

    /**
     * Returns last known value of the datasource.
     *
     * @return Last datasource value.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    @Override
    public double getLastValue() throws IOException {
        return lastValue.get();
    }

    /**
     * Returns value this datasource accumulated so far.
     *
     * @return Accumulated datasource value.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    @Override
    public double getAccumValue() throws IOException {
        return accumValue.get();
    }

    /**
     * Returns the number of accumulated NaN seconds.
     *
     * @return Accumulated NaN seconds.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    @Override
    public long getNanSeconds() throws IOException {
        return nanSeconds.get();
    }

     /**
     * Returns the underlying storage (backend) object which actually performs all
     * I/O operations.
     *
     * @return I/O backend object
     */
    @Override
    public RrdBinaryBackend getRrdBackend() {
        return backend;
    }

    /**
     * Required to implement RrdUpdater interface. You should never call this method directly.
     *
     * @return Allocator object
     */
    public RrdAllocator getRrdAllocator() {
        return allocator;
    }

    @Override
    public void setLastValue(double lastValue) throws IOException {
        this.lastValue.set(lastValue);
    }

    @Override
    public void setNanSeconds(long nanSeconds) throws IOException {
        this.nanSeconds.set(nanSeconds);
    }

    @Override
    public void setAccumValue(double accumValue) throws IOException {
        this.setAccumValue(accumValue);
    }

    @Override
    public void update() throws IOException {
        this.dsName.set(super.dsName);
        this.dsType.set(super.dsType.toString());
        this.heartbeat.set(super.heartbeat);
        this.minValue.set(super.minValue);
        this.maxValue.set(super.maxValue);
    }

    @Override
    public void flush() throws IOException {
    }
}

