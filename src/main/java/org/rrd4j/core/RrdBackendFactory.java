package org.rrd4j.core;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base (abstract) backend factory class which holds references to all concrete
 * backend factories and defines abstract methods which must be implemented in
 * all concrete factory implementations.<p>
 *
 * Factory classes are used to create concrete {@link RrdBackend} implementations.
 * Each factory creates unlimited number of specific backend objects.
 *
 * Rrd4j supports four different backend types (backend factories) out of the box:<p>
 * <ul>
 * <li>{@link RrdRandomAccessFileBackend}: objects of this class are created from the
 * {@link RrdRandomAccessFileBackendFactory} class. This was the default backend used in all
 * Rrd4j releases before 1.4.0 release. It uses java.io.* package and RandomAccessFile class to store
 * RRD data in files on the disk.
 *
 * <li>{@link RrdSafeFileBackend}: objects of this class are created from the
 * {@link RrdSafeFileBackendFactory} class. It uses java.io.* package and RandomAccessFile class to store
 * RRD data in files on the disk. This backend is SAFE:
 * it locks the underlying RRD file during update/fetch operations, and caches only static
 * parts of a RRD file in memory. Therefore, this backend is safe to be used when RRD files should
 * be shared <b>between several JVMs</b> at the same time. However, this backend is *slow* since it does
 * not use fast java.nio.* package (it's still based on the RandomAccessFile class).
 *
 * <li>{@link RrdNioBackend}: objects of this class are created from the
 * {@link RrdNioBackendFactory} class. The backend uses java.io.* and java.nio.*
 * classes (mapped ByteBuffer) to store RRD data in files on the disk. This is the default backend
 * since 1.4.0 release.
 *
 * <li>{@link RrdMemoryBackend}: objects of this class are created from the
 * {@link RrdMemoryBackendFactory} class. This backend stores all data in memory. Once
 * JVM exits, all data gets lost. The backend is extremely fast and memory hungry.
 * </ul>
 *
 * Each backend factory is identified by its {@link #getName() name}. Constructors
 * are provided in the {@link RrdDb} class to create RrdDb objects (RRD databases)
 * backed with a specific backend.<p>
 *
 * See javadoc for {@link RrdBackend} to find out how to create your custom backends.
 */
public abstract class RrdBackendFactory {
    private static final Map<String, RrdBackendFactory> factories = new ConcurrentHashMap<String, RrdBackendFactory>();
    private static RrdBackendFactory defaultFactory;

    static {
        RrdRandomAccessFileBackendFactory fileFactory = new RrdRandomAccessFileBackendFactory();
        registerFactory(fileFactory);
        RrdMemoryBackendFactory memoryFactory = new RrdMemoryBackendFactory();
        registerFactory(memoryFactory);
        RrdNioBackendFactory nioFactory = new RrdNioBackendFactory();
        registerFactory(nioFactory);
        RrdSafeFileBackendFactory safeFactory = new RrdSafeFileBackendFactory();
        registerFactory(safeFactory);
        selectDefaultFactory();
    }

    private static void selectDefaultFactory() {
        setDefaultFactory("NIO");
    }

    /**
     * Returns backend factory for the given backend factory name.
     *
     * @param name Backend factory name. Initially supported names are:<p>
     *             <ul>
     *             <li><b>FILE</b>: Default factory which creates backends based on the
     *             java.io.* package. RRD data is stored in files on the disk
     *             <li><b>SAFE</b>: Default factory which creates backends based on the
     *             java.io.* package. RRD data is stored in files on the disk. This backend
     *             is "safe". Being safe means that RRD files can be safely shared between
     *             several JVM's.
     *             <li><b>NIO</b>: Factory which creates backends based on the
     *             java.nio.* package. RRD data is stored in files on the disk
     *             <li><b>MEMORY</b>: Factory which creates memory-oriented backends.
     *             RRD data is stored in memory, it gets lost as soon as JVM exits.
     *             </ul>
     * @return Backend factory for the given factory name
     */
    public static RrdBackendFactory getFactory(String name) {
        RrdBackendFactory factory = factories.get(name);
        if (factory != null) {
            return factory;
        }
        else {
            throw new IllegalArgumentException("No backend factory found with the name specified [" + name + "]");
        }
    }

    /**
     * Registers new (custom) backend factory within the Rrd4j framework.
     *
     * @param factory Factory to be registered
     */
    public static void registerFactory(RrdBackendFactory factory) {
        String name = factory.getName();
        if (!factories.containsKey(name)) {
            factories.put(name, factory);
        }
        else {
            throw new IllegalArgumentException("Backend factory '" + name + "' cannot be registered twice");
        }
    }

    /**
     * Registers new (custom) backend factory within the Rrd4j framework and sets this
     * factory as the default.
     *
     * @param factory Factory to be registered and set as default
     */
    public static void registerAndSetAsDefaultFactory(RrdBackendFactory factory) {
        registerFactory(factory);
        setDefaultFactory(factory.getName());
    }

    /**
     * Returns the default backend factory. This factory is used to construct
     * {@link RrdDb} objects if no factory is specified in the RrdDb constructor.
     *
     * @return Default backend factory.
     */
    public static RrdBackendFactory getDefaultFactory() {
        return defaultFactory;
    }

    /**
     * Replaces the default backend factory with a new one. This method must be called before
     * the first RRD gets created. <p>
     *
     * @param factoryName Name of the default factory. Out of the box, Rrd4j supports four
     *                    different RRD backends: "FILE" (java.io.* based), "SAFE" (java.io.* based - use this
     *                    backend if RRD files may be accessed from several JVMs at the same time),
     *                    "NIO" (java.nio.* based) and "MEMORY" (byte[] based).
     */
    public static void setDefaultFactory(String factoryName) {
        // We will allow this only if no RRDs are created
        if (!RrdBackend.isInstanceCreated()) {
            defaultFactory = getFactory(factoryName);
        }
        else {
            throw new IllegalStateException("Could not change the default backend factory. " +
                    "This method must be called before the first RRD gets created");
        }
    }

    /**
     * Creates RrdBackend object for the given storage path.
     *
     * @param path     Storage path
     * @param readOnly True, if the storage should be accessed in read/only mode.
     *                 False otherwise.
     * @return Backend object which handles all I/O operations for the given storage path
     * @throws IOException Thrown in case of I/O error.
     */
    protected abstract RrdBackend open(String path, boolean readOnly) throws IOException;

    /**
     * Determines if a storage with the given path already exists.
     *
     * @param path Storage path
     * @return True, if such storage exists, false otherwise.
     */
    protected abstract boolean exists(String path) throws IOException;

    /**
     * Determines if the header should be validated.
     *
     * @param path Storage path
     * @return True, if the header should be validated for this factory
     * @throws IOException if header validation fails
     */
    protected abstract boolean shouldValidateHeader(String path) throws IOException;

    /**
     * Returns the name (primary ID) for the factory.
     *
     * @return Name of the factory.
     */
	public abstract String getName();
}
