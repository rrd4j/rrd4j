package org.rrd4j.backend.spi.binary;

import java.io.IOException;

import org.rrd4j.ConsolFun;
import org.rrd4j.backend.spi.Archive;

/**
 * Class to represent single RRD archive in a RRD with its internal state.
 * Normally, you don't need methods to manipulate archive objects directly
 * because Rrd4j framework does it automatically for you.<p>
 * <p/>
 * Each archive object consists of three parts: archive definition, archive state objects
 * (one state object for each datasource) and round robin archives (one round robin for
 * each datasource). API (read-only) is provided to access each of these parts.<p>
 *
 * @author Sasa Markovic
 */
public class ArchiveBinary extends Archive implements Allocated {
    protected final RrdBinaryBackend backend;

    // definition
    final RrdString consolFunP;
    final RrdDouble xffP;
    final RrdInt stepsP;
    final RrdInt rowsP;

    ArchiveBinary(RrdBinaryBackend backend) throws IOException {
        this.backend = backend;
        
        consolFunP = new RrdString(this, true);
        xffP = new RrdDouble(this);
        this.stepsP = new RrdInt(this, true);
        this.rowsP = new RrdInt(this, true);
    }

    @Override
    public double getXff() throws IOException {
        return this.xffP.get();
    }

    @Override
    public void setXff(double xff) throws IOException {
        this.xffP.set(xff);
    }
    
    /**
     * <p>getRrdAllocator.</p>
     *
     * @return a {@link org.rrd4j.core.RrdAllocator} object.
     */
    public RrdAllocator getRrdAllocator() {
        return backend.getRrdAllocator();
    }

    /**
     * <p>getRrdBackend.</p>
     *
     * @return a {@link org.rrd4j.backend.RrdBackend} object.
     */
    public RrdBinaryBackend getRrdBackend() {
        return backend;
    }

    @Override
    public void save() throws IOException {
        this.consolFunP.set(super.consolFun.toString());
        this.stepsP.set(super.steps);
        this.rowsP.set(super.rows);
    }

    @Override
    public void load() throws IOException {
        super.consolFun = ConsolFun.valueOf(this.consolFunP.get());
        super.steps = this.stepsP.get();
        super.rows = this.rowsP.get();
    }

}
