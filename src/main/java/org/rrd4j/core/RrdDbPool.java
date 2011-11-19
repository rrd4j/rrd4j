package org.rrd4j.core;

import java.io.IOException;

/**
 * This class should be used to synchronize access to RRD files
 * in a multithreaded environment. This class should be also used to prevent opening of
 * too many RRD files at the same time (thus avoiding operating system limits).
 * <p>
 * The abstract wrapper for different implementations of the pools.
 */
public abstract class RrdDbPool {
    private static class RrdDbPoolSingletonHolder {
        static final RrdDbPool instance = new RrdDbPoolNew();
    }

    /**
     * Creates a single instance of the class on the first call,
     * or returns already existing one. Uses Initialization On Demand Holder idiom.
     *
     * @return Single instance of this class
     * @throws RuntimeException Thrown if the default RRD backend is not derived from the {@link RrdFileBackendFactory}
     */
    public static RrdDbPool getInstance() {
        return RrdDbPoolSingletonHolder.instance;
    }

    /**
     * Initial capacity of the pool i.e. maximum number of simultaneously open RRD files. The pool will
     * never open too many RRD files at the same time.
     */
    public static final int INITIAL_CAPACITY = 200;

    /**
     * Requests a RrdDb reference for the given RRD file path.<p>
     * <ul>
     * <li>If the file is already open, previously returned RrdDb reference will be returned. Its usage count
     * will be incremented by one.
     * <li>If the file is not already open and the number of already open RRD files is less than
     * {@link #INITIAL_CAPACITY}, the file will be open and a new RrdDb reference will be returned.
     * If the file is not already open and the number of already open RRD files is equal to
     * {@link #INITIAL_CAPACITY}, the method blocks until some RRD file is closed.
     * </ul>
     *
     * @param path Path to existing RRD file
     * @return reference for the give RRD file
     * @throws IOException Thrown in case of I/O error
     */
    public abstract RrdDb requestRrdDb(String path) throws IOException;

    /**
     * Requests a RrdDb reference for the given RRD file definition object.<p>
     * <ul>
     * <li>If the file with the path specified in the RrdDef object is already open,
     * the method blocks until the file is closed.
     * <li>If the file is not already open and the number of already open RRD files is less than
     * {@link #INITIAL_CAPACITY}, a new RRD file will be created and a its RrdDb reference will be returned.
     * If the file is not already open and the number of already open RRD files is equal to
     * {@link #INITIAL_CAPACITY}, the method blocks until some RRD file is closed.
     * </ul>
     *
     * @param rrdDef Definition of the RRD file to be created
     * @return Reference to the newly created RRD file
     * @throws IOException Thrown in case of I/O error
     */
    public abstract RrdDb requestRrdDb(RrdDef rrdDef) throws IOException;

    /**
     * Requests a RrdDb reference for the given path. The file will be created from
     * external data (from XML dump, RRD file or RRDTool's binary RRD file).<p>
     * <ul>
     * <li>If the file with the path specified is already open,
     * the method blocks until the file is closed.
     * <li>If the file is not already open and the number of already open RRD files is less than
     * {@link #INITIAL_CAPACITY}, a new RRD file will be created and a its RrdDb reference will be returned.
     * If the file is not already open and the number of already open RRD files is equal to
     * {@link #INITIAL_CAPACITY}, the method blocks until some RRD file is closed.
     * </ul>
     *
     * @param path       Path to RRD file which should be created
     * @param sourcePath Path to external data which is to be converted to Rrd4j's native RRD file format
     * @return Reference to the newly created RRD file
     * @throws IOException Thrown in case of I/O error
     */
    public abstract RrdDb requestRrdDb(String path, String sourcePath) throws IOException;

    /**
     * Releases RrdDb reference previously obtained from the pool. When a reference is released, its usage
     * count is decremented by one. If usage count drops to zero, the underlying RRD file will be closed.
     *
     * @param rrdDb RrdDb reference to be returned to the pool
     * @throws IOException Thrown in case of I/O error
     */
    public abstract void release(RrdDb rrdDb) throws IOException;

    /**
     * Returns the maximum number of simultaneously open RRD files.
     *
     * @return maximum number of simultaneously open RRD files
     */
    public abstract int getCapacity();

    /**
     * Sets the maximum number of simultaneously open RRD files.
     *
     * @param capacity Maximum number of simultaneously open RRD files.
     */
    public abstract void setCapacity(int capacity);

    /**
     * Returns an array of open file names.
     *
     * @return Array with canonical paths to open RRD files held in the pool.
     */
    public abstract String[] getOpenFiles();

    /**
     * Returns the number of open RRD files.
	 *
	 * @return Number of currently open RRD files held in the pool.
	 */
	public abstract int getOpenFileCount();

    /**
     * Returns the number of usage for a RRD.
     *
     * @param rrdDb RrdDb reference for which informations is needed.
     * @return the number of request for this rrd
     * @throws IOException
     */
    public abstract int getOpenCount(RrdDb rrdDb) throws IOException;

    /**
     * Returns the number of usage for a RRD.
     *
     * @param path RRD file for which informations is needed.
     * @return the number of request for this file
     * @throws IOException
     */
    public abstract int getOpenCount(String path) throws IOException;

}