package org.rrd4j.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.rrd4j.backend.spi.binary.RrdBerkeleyDbBackendFactory;
import org.rrd4j.backend.spi.binary.RrdMemoryBackend;
import org.rrd4j.backend.spi.binary.RrdMemoryBackendFactory;
import org.rrd4j.backend.spi.binary.RrdMongoDBBackendFactory;
import org.rrd4j.backend.spi.binary.RrdNioBackend;
import org.rrd4j.backend.spi.binary.RrdNioBackendFactory;
import org.rrd4j.backend.spi.binary.RrdRandomAccessFileBackend;
import org.rrd4j.backend.spi.binary.RrdRandomAccessFileBackendFactory;
import org.rrd4j.backend.spi.binary.RrdSafeFileBackend;
import org.rrd4j.backend.spi.binary.RrdSafeFileBackendFactory;

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
    public enum State {
        /**
         * A service in this state is inactive. It does minimal work and consumes
         * minimal resources.
         */
        NEW,
        /**
         * A service in this state is transitioning to {@link #RUNNING}.
         */
        STARTING,
        /**
         * A service in this state is operational.
         */
        RUNNING,
        /**
         * A service in this state is transitioning to {@link #TERMINATED}.
         */
        STOPPING,
        /**
         * A service in this state has completed execution normally. It does minimal
         * work and consumes minimal resources.
         */
        TERMINATED,
        /**
         * A service in this state has encountered a problem and may not be
         * operational. It cannot be started nor stopped.
         */
        FAILED
    }

    private static final class FactoryState {
        private final Class<? extends RrdBackendFactory> clazz;
        volatile private RrdBackendFactory instance;
        FactoryState(Class<? extends RrdBackendFactory> clazz) {
            this.clazz = clazz;
        }
    }
    private static final ConcurrentMap<String, FactoryState> factories = new ConcurrentHashMap<String, FactoryState>();
    private static RrdBackendFactory defaultFactory = null;
    private static String defaultFactoryName = RrdNioBackendFactory.class.getAnnotation(RrdBackendMeta.class).value();
    static {
        registerFactory(RrdRandomAccessFileBackendFactory.class);
        registerFactory(RrdMemoryBackendFactory.class);
        registerFactory(RrdNioBackendFactory.class);
        registerFactory(RrdSafeFileBackendFactory.class);
        registerFactory(RrdBerkeleyDbBackendFactory.class);
        registerFactory(RrdMongoDBBackendFactory.class);
    }

    /**
     * Returns backend factory for the given backend factory name. <p>
     * It will not start nor configure the factory.
     *
     * @param factoryName Backend factory name. Initially supported names are:<p>
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
    public static RrdBackendFactory getFactory(String factoryName) {
        FactoryState fs = factories.get(factoryName);
        if(fs == null) {
            throw new IllegalArgumentException("No backend factory found with the name specified [" + factoryName + "]");           
        }
        synchronized(fs) {
            Class<? extends RrdBackendFactory> factoryClass = fs.clazz;
            try {
                if(fs.instance == null) {
                    fs.instance = factoryClass.getConstructor().newInstance();
                }
                return fs.instance;
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("The backend " +  factoryName + " can't be used", e);
            } catch (SecurityException e) {
                throw new IllegalStateException("The backend " +  factoryName + " can't be used", e);
            } catch (InstantiationException e) {
                throw new IllegalStateException("The backend " +  factoryName + " can't be used", e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("The backend " +  factoryName + " can't be used", e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("The backend " +  factoryName + " can't be used", e);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("The backend " +  factoryName + " can't be used", e);
            }
        }
    }

    /**
     * Registers new (custom) backend factory within the Rrd4j framework.
     *
     * @param factory Factory to be registered
     */
    public static String registerFactory(Class<? extends RrdBackendFactory> factoryClass) {
        RrdBackendMeta nameAnnotation = factoryClass.getAnnotation(RrdBackendMeta.class);
        if(nameAnnotation != null) {
            String name = nameAnnotation.value();
            FactoryState fs = new FactoryState(factoryClass);
            factories.putIfAbsent(name, fs);
            return name;
        }
        else {
            throw new IllegalArgumentException("Backend factory don't have the name anotation");
        }
    }

    /**
     * Registers new (custom) backend factory within the Rrd4j framework and sets this
     * factory as the default.<p>
     * It will not start nor configure the factory.
     * 
     * @param factory Factory to be registered and set as default
     */
    public static void registerAndSetAsDefaultFactory(Class<? extends RrdBackendFactory> factoryClass) {
        String name = registerFactory(factoryClass);
        setDefaultFactory(name);
    }

    /**
     * Returns the default backend factory. This factory is used to construct
     * {@link RrdDb} objects if no factory is specified in the RrdDb constructor.<p>
     * It will not start nor configure the factory.
     *
     * @return Default backend factory.
     */
    public static synchronized RrdBackendFactory getDefaultFactory() {
        if(defaultFactory == null) {
            defaultFactory = getFactory(defaultFactoryName);
        }
        return defaultFactory;
    }

    /**
     * Replaces the default backend factory with a new one. This method must be called before
     * the first RRD gets created. <p>
     * It will not start nor configure the factory.
     *
     * @param factoryName Name of the default factory. Out of the box, Rrd4j supports four
     *                    different RRD backends: "FILE" (java.io.* based), "SAFE" (java.io.* based - use this
     *                    backend if RRD files may be accessed from several JVMs at the same time),
     *                    "NIO" (java.nio.* based) and "MEMORY" (byte[] based).
     */
    public static synchronized void setDefaultFactory(String factoryName) {
        if (RrdBackend.isInstanceCreated() && ! defaultFactoryName.equals(factoryName)) {
            throw new IllegalStateException("Could not change the default backend factory. " +
                    "This method must be called before the first RRD gets created");
        }
        defaultFactoryName = factoryName;
        defaultFactory = null;
    }

    private final String name;
    private volatile State state = State.NEW;

    protected RrdBackendFactory() {
        name = getClass().getAnnotation(RrdBackendMeta.class).value();
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
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
        if(state != State.RUNNING)
            throw new IllegalStateException("backend not started");
        RrdBackend backend = doOpen(path, readOnly);
        backend.setFactory(this);
        return backend;
    }

    protected abstract RrdBackend doOpen(String path, boolean readOnly) throws IOException;

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
    public final String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see com.google.common.util.concurrent.AbstractService#doStart()
     */
    public synchronized final State start() {
        if(state != State.NEW)
            return state;
        state = State.STARTING;
        try {
            if(startBackend()) {
                state = State.RUNNING;
            }
            else {
                state = State.FAILED;
                throw new IllegalStateException("backend failed to start");
            }
        } catch (Exception e) {
            state = State.FAILED;  
            throw new IllegalStateException("backend failed to start with exception", e);
        }
        return state;
    }

    /* (non-Javadoc)
     * @see com.google.common.util.concurrent.AbstractService#doStop()
     */
    public synchronized final State stop() {
        if(state != State.RUNNING)
            return state;
        state = State.STOPPING;
        try {
            if(stopBackend()) {
                state = State.TERMINATED;
            }
            else {
                state = State.FAILED;
                throw new IllegalStateException("backend failed to stop");
            }
        } catch (Exception e) {
            state = State.FAILED;  
            throw new IllegalStateException("backend failed to stop with exception", e);

        }
        return state;
    }

    /**
     * A class that can be called to force an commit to permanent storage of date
     */
    public final void sync() {
        if(state == State.RUNNING)
            doSync();
    }

    /**
     * A empty method. Subclass should overwrite it to force a sync of data.
     */
    protected void doSync() {
    }

    /**
     * A method can be be overriden to provide back-end staticics
     * @return an empty map
     */
    public Map<String, Number> getStats() {
        return Collections.emptyMap();
    }

    abstract protected boolean startBackend();
    abstract protected boolean stopBackend();

    /**
     * @return the state
     */
    State getState() {
        return state;
    }
    
    public abstract String resolveUniqId(Object id) throws IOException;

}
