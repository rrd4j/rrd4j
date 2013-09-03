package org.rrd4j.backend.spi.binary;

import java.io.IOException;

import org.rrd4j.backend.spi.ArcState;

public class ArcStateBinary extends ArcState implements Allocated {
    protected final RrdAllocator allocator;
    protected final RrdBinaryBackend backend;

    private RrdDouble accumValue;
    private RrdLong nanSteps;

    public ArcStateBinary(RrdAllocator allocator, RrdBinaryBackend backend) throws IOException {
        super();
        this.allocator = allocator;
        this.backend = backend;

        accumValue = new RrdDouble(this);
        nanSteps = new RrdLong(this);
    }

    @Override
    public void update() throws IOException {        
    }

    @Override
    public void flush() throws IOException {
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
    public double getAccumValue() throws IOException {
        return accumValue.get();
    }

    @Override
    public void setAccumValue(double accumValue) throws IOException {
        this.accumValue.set(accumValue);
    }

    @Override
    public void setNanSteps(long nanSteps) throws IOException {
        this.nanSteps.set(nanSteps);
    }

    @Override
    public long getNanSteps() throws IOException {
        return nanSteps.get();
    }

}
