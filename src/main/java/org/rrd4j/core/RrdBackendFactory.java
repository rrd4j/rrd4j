package org.rrd4j.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base (abstract) backend factory class which holds references to all concrete
 * backend factories and defines abstract methods which must be implemented in
 * all concrete factory implementations.
 * <p>
 *
 * Factory classes are used to create concrete {@link org.rrd4j.core.RrdBackend} implementations.
 * Each factory creates unlimited number of specific backend objects.
 *
 * Rrd4j supports four different backend types (backend factories) out of the box:
 * <ul>
 * <li>{@link org.rrd4j.core.RrdRandomAccessFileBackend}: objects of this class are created from the
 * {@link org.rrd4j.core.RrdRandomAccessFileBackendFactory} class. This was the default backend used in all
 * Rrd4j releases before 1.4.0 release. It uses java.io.* package and RandomAccessFile class to store
 * RRD data in files on the disk.
 *
 * <li>{@link org.rrd4j.core.RrdSafeFileBackend}: objects of this class are created from the
 * {@link org.rrd4j.core.RrdSafeFileBackendFactory} class. It uses java.io.* package and RandomAccessFile class to store
 * RRD data in files on the disk. This backend is SAFE:
 * it locks the underlying RRD file during update/fetch operations, and caches only static
 * parts of a RRD file in memory. Therefore, this backend is safe to be used when RRD files should
 * be shared <b>between several JVMs</b> at the same time. However, this backend is *slow* since it does
 * not use fast java.nio.* package (it's still based on the RandomAccessFile class).
 *
 * <li>{@link org.rrd4j.core.RrdNioBackend}: objects of this class are created from the
 * {@link org.rrd4j.core.RrdNioBackendFactory} class. The backend uses java.io.* and java.nio.*
 * classes (mapped ByteBuffer) to store RRD data in files on the disk. This is the default backend
 * since 1.4.0 release.
 *
 * <li>{@link org.rrd4j.core.RrdMemoryBackend}: objects of this class are created from the
 * {@link org.rrd4j.core.RrdMemoryBackendFactory} class. This backend stores all data in memory. Once
 * JVM exits, all data gets lost. The backend is extremely fast and memory hungry.
 * </ul>
 *
 * Each backend factory is identified by its {@link #getName() name}. Constructors
 * are provided in the {@link org.rrd4j.core.RrdDb} class to create RrdDb objects (RRD databases)
 * backed with a specific backend.
 * <p>
 *
 * See javadoc for {@link org.rrd4j.core.RrdBackend} to find out how to create your custom backends.
 *
 */
public abstract class RrdBackendFactory {

    /**
     * The default factory type. It will also put in the active factories list.
     * 
     */
    public static final String DEFAULTFACTORY = "NIO";

    private static final Map<String, RrdBackendFactory> factories = new ConcurrentHashMap<String, RrdBackendFactory>();
    private static RrdBackendFactory defaultFactory;
    private static final List<RrdBackendFactory> activeFactories = new ArrayList<>();

    static {
        RrdRandomAccessFileBackendFactory fileFactory = new RrdRandomAccessFileBackendFactory();
        registerFactory(fileFactory);
        RrdMemoryBackendFactory memoryFactory = new RrdMemoryBackendFactory();
        registerFactory(memoryFactory);
        RrdNioBackendFactory nioFactory = new RrdNioBackendFactory();
        registerFactory(nioFactory);
        RrdSafeFileBackendFactory safeFactory = new RrdSafeFileBackendFactory();
        registerFactory(safeFactory);
        setDefaultFactory(DEFAULTFACTORY);
        setActiveFactories(RrdBackendFactory.getFactory(DEFAULTFACTORY));
    }

    /**
     * Returns backend factory for the given backend factory name.
     *
     * @param name Backend factory name. Initially supported names are:
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
     * {@link org.rrd4j.core.RrdDb} objects if no factory is specified in the RrdDb constructor.
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
     * Set the list of active factories, i.e. the factory used to resolve URI.
     * 
     * @param newFactories the new active factories.
     */
    public static void setActiveFactories(RrdBackendFactory... newFactories) {
        activeFactories.clear();
        activeFactories.addAll(Arrays.asList(newFactories));
    }

    /**
     * Add factories to the list of active factories, i.e. the factory used to resolve URI.
     * 
     * @param newFactories active factories to add.
     */
    public static void addFactories(RrdBackendFactory... newFactories) {
        activeFactories.addAll(Arrays.asList(newFactories));
    }

    /**
     * For a given URI, try to find a factory that can manage it.
     * 
     * @param uri URI to try.
     * @return a {@link RrdBackendFactory} that can manage that URI.
     * @throws IllegalArgumentException when no matching factory is found.
     */
    public static RrdBackendFactory findFactory(URI uri) {
        for (RrdBackendFactory tryfactory: activeFactories) {
            if (tryfactory.canStore(uri)) {
                return tryfactory;
            }
        }
        throw new IllegalArgumentException("no matching backend factory for " + uri);
    }

    /**
     * @return the scheme name for URI, default to getName().toLowerCase()
     */
    public String getScheme() {
        return getName().toLowerCase();
    }

    protected URI getRootUri() {
        try {
            return new URI(getScheme(), null, "/", null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid scheme " + getScheme());
        }
    }

    public boolean canStore(URI uri) {
        return false;
    }

    /**
     * Try to match an URI against a root URI using a few rules:
     * <ul>
     * <li>scheme must match if they are given
     * <li>if uri is opaque (scheme:nonabsolute), the scheme specific part is resolve as a relative path
     * <li>
     * </ul>
     * 
     * @param rootUri
     * @param uri
     * @return a calculate normalized absolute URI or null if the tried URL don't match against the root.
     */
    protected URI resolve(URI rootUri, URI uri, boolean relative) {
        String scheme = uri.getScheme();
        if (scheme != null && ! scheme.equals(rootUri.getScheme())) {
            return null;
        } else if (scheme == null) {
            scheme = rootUri.getScheme();
        }
        String authority = uri.getAuthority();
        if (authority != null && ! authority.equals(rootUri.getAuthority())) {
            return null;
        } else if (authority == null) {
            authority = rootUri.getAuthority();
        }
        String path;
        if (uri.isOpaque()) {
            // try to resolve an opaque uri as scheme:relativepath
            path = uri.getSchemeSpecificPart();
        } else if (! uri.isAbsolute()) {
            // A relative URI, resolve it against the root
            path = rootUri.resolve(uri).normalize().getPath();
        } else {
            path = uri.normalize().getPath();
        }
        if (! path.startsWith(rootUri.getPath())) {
            return null;
        }
        String query = uri.getQuery();
        String fragment = uri.getFragment();
        String newUriString = String.format("%s://%s%s%s%s", scheme, authority, path , query != null ? "?" + query : "", fragment != null ? "#" + fragment : "");
        URI newURI = URI.create(newUriString);
        if (relative) {
            return rootUri.relativize(newURI);
        } else {
            return newURI;
        }
    }

    public URI getCanonicalUri(URI uri) {
        return resolve(getRootUri(), uri, false);
    }

    public URI getUri(String path) {
        URI rootUri = getRootUri();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try {
            return new URI(getScheme(), rootUri.getAuthority(), rootUri.getPath() + path, null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    public String getPath(URI uri) {
        URI rootUri = getRootUri();
        uri = resolve(rootUri, uri, true);
        if (uri == null) {
            return null;
        }
        return "/" + uri.getPath();
    }

    /**
     * Creates RrdBackend object for the given storage path.
     *
     * @param path     Storage path
     * @param readOnly True, if the storage should be accessed in read/only mode.
     *                 False otherwise.
     * @return Backend object which handles all I/O operations for the given storage path
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    protected abstract RrdBackend open(String path, boolean readOnly) throws IOException;

    /**
     * Creates RrdBackend object for the given storage path.
     *
     * @param uri     Storage uri
     * @param readOnly True, if the storage should be accessed in read/only mode.
     *                 False otherwise.
     * @return Backend object which handles all I/O operations for the given storage path
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    protected RrdBackend open(URI uri, boolean readOnly) throws IOException {
        return open(getPath(uri), readOnly);
    }

    /**
     * Determines if a storage with the given path already exists.
     *
     * @param path Storage path
     * @throws java.io.IOException in case of I/O error.
     * @return a boolean.
     */
    protected abstract boolean exists(String path) throws IOException;

    /**
     * Determines if a storage with the given URI already exists.
     *
     * @param uri Storage URI.
     * @throws java.io.IOException in case of I/O error.
     * @return a boolean.
     */
    protected boolean exists(URI uri) throws IOException {
        return exists(getPath(uri));
    }

    /**
     * Determines if the header should be validated.
     *
     * @param path Storage path
     * @throws java.io.IOException if header validation fails
     * @return a boolean.
     */
    protected abstract boolean shouldValidateHeader(String path) throws IOException;

    /**
     * Determines if the header should be validated.
     *
     * @param uri Storage URI
     * @throws java.io.IOException if header validation fails
     * @return a boolean.
     */
    protected boolean shouldValidateHeader(URI uri) throws IOException {
        return shouldValidateHeader(getPath(uri));
    }

    /**
     * Returns the name (primary ID) for the factory.
     *
     * @return Name of the factory.
     */
    public abstract String getName();

}
