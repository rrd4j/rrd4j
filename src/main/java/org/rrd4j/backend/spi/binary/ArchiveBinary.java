package org.rrd4j.backend.spi.binary;

import org.rrd4j.ConsolFun;
import org.rrd4j.backend.spi.Archive;
import org.rrd4j.core.ArcDef;
import org.rrd4j.core.BackendException;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.Robin;
import org.rrd4j.core.RrdBackend;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdUpdater;
import org.rrd4j.core.Util;
import org.rrd4j.core.XmlWriter;

import java.io.IOException;

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
    private final RrdDb parentDb;

    protected final RrdAllocator allocator;
    protected final RrdBinaryBackend backend;

    // definition
    protected final RrdString consolFun;
    protected final RrdDouble xff;
    protected final RrdInt steps;
    protected final RrdInt rows;

    // state
    private final Robin[] robins;

    ArchiveBinary(RrdAllocator allocator, RrdBinaryBackend backend) throws BackendException {
        this.allocator = allocator;
        this.backend = backend;
        consolFun = new RrdString(this, true).get();
        xff = new RrdDouble(this);
        steps = new RrdInt(this, true).get();
        rows = new RrdInt(this, true).get();
        boolean shouldInitialize = arcDef != null;
        if (shouldInitialize) {
            consolFun = ConsolFun.valueOf(arcDef.getConsolFun().name());
            xff.set(arcDef.getXff());
            steps = arcDef.getSteps();
            rows = arcDef.getRows();
        }
        int n = parentDb.getHeader().getDsCount();
        int version = parentDb.getHeader().getVersion();
        if (version == 1) {
            robins = new RobinArray[n];
            for (int i = 0; i < n; i++) {
                robins[i] = new RobinArray(this, rows, shouldInitialize);
            }
        } else {
            RrdInt[] pointers = new RrdInt[n];
            robins = new RobinMatrix[n];
            for (int i = 0; i < n; i++) {
                pointers[i] = new RrdInt(this);
            }
            RrdDoubleMatrix values = new RrdDoubleMatrix(this, rows, n, shouldInitialize);
            for (int i = 0; i < n; i++) {
                robins[i] = new RobinMatrix(this, values, pointers[i], i);
            }
        }
    }

    /**
     * Returns the underlying archive state object. Each datasource has its
     * corresponding ArcState object (archive states are managed independently
     * for each RRD datasource).
     *
     * @param dsIndex Datasource index
     * @return Underlying archive state object
     */
    public ArcState getArcState(int dsIndex) {
        return states[dsIndex];
    }

    /**
     * Returns the underlying round robin archive. Robins are used to store actual
     * archive values on a per-datasource basis.
     *
     * @param dsIndex Index of the datasource in the RRD.
     * @return Underlying round robin archive for the given datasource.
     */
    public Robin getRobin(int dsIndex) {
        return robins[dsIndex];
    }

    @Override
    public Robin robin(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Robin> robins() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getXff() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setXff(double xff) {
        // TODO Auto-generated method stub

    }
    
    /**
     * <p>getRrdAllocator.</p>
     *
     * @return a {@link org.rrd4j.core.RrdAllocator} object.
     */
    RrdAllocator getRrdAllocator() {
        return allocator;
    }

    /**
     * <p>getRrdBackend.</p>
     *
     * @return a {@link org.rrd4j.core.RrdBackend} object.
     */
    RrdBinaryBackend getRrdBackend() {
        return backend;
    }

}
