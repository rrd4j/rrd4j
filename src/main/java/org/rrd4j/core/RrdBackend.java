package org.rrd4j.core;

import java.io.IOException;

import org.rrd4j.backend.spi.ArcState;
import org.rrd4j.backend.spi.Archive;
import org.rrd4j.backend.spi.Datasource;
import org.rrd4j.backend.spi.Header;
import org.rrd4j.backend.spi.Robin;

/**
 * Base implementation class for all backend classes. Each Round Robin Database object
 * ({@link org.rrd4j.core.RrdDb} object) is backed with a single RrdBackend object which performs
 * actual I/O operations on the underlying storage. Rrd4j supports
 * multiple backends out of the box. E.g.:</p>
 * <ul>
 * <li>{@link org.rrd4j.backend.spi.binary.RrdRandomAccessFileBackend}: objects of this class are created from the
 * {@link org.rrd4j.backend.spi.binary.RrdRandomAccessFileBackendFactory} class. This was the default backend used in all
 * Rrd4j releases prior to 1.4.0. It uses java.io.* package and
 * RandomAccessFile class to store RRD data in files on the disk.
 *
 * <li>{@link org.rrd4j.backend.spi.binary.RrdNioBackend}: objects of this class are created from the
 * {@link org.rrd4j.backend.spi.binary.RrdNioBackendFactory} class. The backend uses java.io.* and java.nio.*
 * classes (mapped ByteBuffer) to store RRD data in files on the disk. This backend is fast, very fast,
 * but consumes a lot of memory (borrowed not from the JVM but from the underlying operating system
 * directly). <b>This is the default backend used in Rrd4j since 1.4.0 release.</b>
 *
 * <li>{@link org.rrd4j.backend.spi.binary.RrdMemoryBackend}: objects of this class are created from the
 * {@link org.rrd4j.backend.spi.binary.RrdMemoryBackendFactory} class. This backend stores all data in memory. Once
 * JVM exits, all data gets lost. The backend is extremely fast and memory hungry.
 * </ul>
 *
 * To create your own backend in order to provide some custom type of RRD storage,
 * you should do the following:</p>
 *
 * <ul>
 * <li>Create your custom RrdBackend class (RrdCustomBackend, for example)
 * by extending RrdBackend class. You have to implement all abstract methods defined
 * in the base class.
 *
 * <li>Create your custom RrdBackendFactory class (RrdCustomBackendFactory,
 * for example) by extending RrdBackendFactory class. You have to implement all
 * abstract methods defined in the base class. Your custom factory class will actually
 * create custom backend objects when necessary.
 *
 * <li>Create instance of your custom RrdBackendFactory and register it as a regular
 * factory available to Rrd4j framework. See javadoc for {@link org.rrd4j.core.RrdBackendFactory} to
 * find out how to do this.
 * </ul>
 *
 * @author Sasa Markovic
 */
public abstract class RrdBackend {
    private static boolean instanceCreated = false;
    private final String path;
    private RrdBackendFactory factory;

    /**
     * Creates backend for a RRD storage with the given path.
     *
     * @param path String identifying RRD storage. For files on the disk, this
     *             argument should represent file path. Other storage types might interpret
     *             this argument differently.
     */
    protected RrdBackend(String path) {
        this.path = path;
        instanceCreated = true;
    }

    /**
     * Returns path to the storage.
     *
     * @return Storage path
     */
    public String getPath() {
        return path;
    }

    /**
     * Closes the underlying backend.
     *
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public void close() throws IOException {
    }

    /**
     * This method suggests the caching policy to the Rrd4j frontend (high-level) classes. If <code>true</code>
     * is returned, frontend classes will cache frequently used parts of a RRD file in memory to improve
     * performance. If </code>false</code> is returned, high level classes will never cache RRD file sections
     * in memory.
     *
     * @return <code>true</code> if file caching is enabled, <code>false</code> otherwise. By default, the
     *         method returns <code>true</code> but it can be overriden in subclasses.
     */
    protected boolean isCachingAllowed() {
        return true;
    }

	static boolean isInstanceCreated() {
		return instanceCreated;
	}

    /**
     * @return the factory
     */
    public RrdBackendFactory getFactory() {
        return factory;
    }

    /**
     * @param factory the factory to set
     */
    public void setFactory(RrdBackendFactory factory) {
        this.factory = factory;
    }

    public abstract String getUniqId() throws IOException;
    
    public abstract Header getHeader() throws IOException;

    public abstract Datasource getDatasource(int index) throws IOException;
    
    public abstract Archive getArchive(int index) throws IOException;
        
    public abstract ArcState getArcState(int dsIndex, int arcIndex) throws IOException;

    public abstract Robin getRobin(int dsIndex, int arcIndex) throws IOException;

    public abstract void create(RrdDb rrdDb, RrdDef rrdDef) throws IOException;
    
    public abstract void load(RrdDb rrdDb) throws IOException;

    public abstract void load(RrdDb rrdDb, DataImporter reader) throws IOException;

}
