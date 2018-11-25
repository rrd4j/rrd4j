package org.rrd4j.core;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base (abstract) backend factory class which holds references to all concrete
 * backend factories and defines abstract methods which must be implemented in
 * all concrete factory implementations.
 * <p>
 *
 * Factory classes are used to create concrete {@link org.rrd4j.core.RrdBackend} implementations.
 * Each factory creates unlimited number of specific backend objects.
 *
 * Rrd4j supports six different backend types (backend factories) out of the box:
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
 * 
 * <li>{@link org.rrd4j.core.RrdBerkeleyDbBackend}: objects of this class are created from the 
 * {@link org.rrd4j.core.RrdBerkeleyDbBackendFactory} class. It stores RRD data to ordinary disk files 
 * using <a href="http://www.oracle.com/technetwork/database/berkeleydb/overview/index-093405.html">Oracle Berkeley DB</a> Java Edition.
 * 
 * <li>{@link org.rrd4j.core.RrdMongoDBBackend}: objects of this class are created from the {@link org.rrd4j.core.RrdMongoDBBackendFactory} class.
 * It stores data in a {@link com.mongodb.DBCollection} from <a href="http://www.mongodb.org/">MongoDB</a>.
 * </ul>
 *
 * Each backend factory used to be identified by its {@link #getName() name}. Constructors
 * are provided in the {@link org.rrd4j.core.RrdDb} class to create RrdDb objects (RRD databases)
 * backed with a specific backend.
 * <p>
 * A more generic management was added in version 3.2 that allows multiple instance of a backend to be used. Each backend can
 * manage custom URL. They are tried in the declared order by the {@link #setActiveFactories(RrdBackendFactory...)} or
 * {@link #addFactories(RrdBackendFactory...)} and the method {@link #canStore(URI)} return true when it can manage the given
 * URI. Using {@link #setActiveFactories(RrdBackendFactory...)} with new created instance is the preferred way to manage factories, as
 * it provides a much precise control of creation and end of life of factories.
 * <p>
 * For default implementation, the path is separated in a root URI prefix and the path components. The root URI can be
 * used to identify different name spaces or just be ```/```.
 * <p>
 * See javadoc for {@link org.rrd4j.core.RrdBackend} to find out how to create your custom backends.
 *
 */
public abstract class RrdBackendFactory implements Closeable {

    /**
     * The default factory type. It will also put in the active factories list.
     * 
     */
    public static final String DEFAULTFACTORY = "NIO";

    private static final Map<String, RrdBackendFactory> factories = new ConcurrentHashMap<String, RrdBackendFactory>();
    private static final List<RrdBackendFactory> activeFactories = new ArrayList<>();
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    static {
        RrdRandomAccessFileBackendFactory fileFactory = new RrdRandomAccessFileBackendFactory();
        registerFactory(fileFactory);
        RrdMemoryBackendFactory memoryFactory = new RrdMemoryBackendFactory();
        registerFactory(memoryFactory);
        RrdNioBackendFactory nioFactory = new RrdNioBackendFactory();
        registerFactory(nioFactory);
        RrdSafeFileBackendFactory safeFactory = new RrdSafeFileBackendFactory();
        registerFactory(safeFactory);
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
     *             <li><b>BERKELEY</b>: a memory-oriented backend that ensure persistens
     *             in a <a href="http://www.oracle.com/technetwork/database/berkeleydb/overview/index-093405.html">Berkeley Db</a> storage.
     *             <li><b>MONGODB</b>: a memory-oriented backend that ensure persistens
     *             in a <a href="http://www.mongodb.org/">MongoDB</a> storage.
     *             </ul>
     * @return Backend factory for the given factory name
     */
    public static RrdBackendFactory getFactory(String name) {
        lock.readLock().lock();
        try {
            RrdBackendFactory factory = factories.get(name);
            if (factory != null) {
                return factory;
            } else {
                throw new IllegalArgumentException(
                        "No backend factory found with the name specified ["
                                + name + "]");
            } 
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Registers new (custom) backend factory within the Rrd4j framework.
     *
     * @param factory Factory to be registered
     */
    public static void registerFactory(RrdBackendFactory factory) {
        lock.writeLock().lock();
        try {
            String name = factory.getName();
            if (!factories.containsKey(name)) {
                factories.put(name, factory);
            }
            else {
                throw new IllegalArgumentException("Backend factory '" + name + "' cannot be registered twice");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Registers new (custom) backend factory within the Rrd4j framework and sets this
     * factory as the default.
     *
     * @param factory Factory to be registered and set as default
     */
    public static void registerAndSetAsDefaultFactory(RrdBackendFactory factory) {
        lock.writeLock().lock();
        try {
            registerFactory(factory);
            setDefaultFactory(factory.getName());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the default backend factory. This factory is used to construct
     * {@link org.rrd4j.core.RrdDb} objects if no factory is specified in the RrdDb constructor.
     *
     * @return Default backend factory.
     */
    public static RrdBackendFactory getDefaultFactory() {
        lock.readLock().lock();
        try {
            return activeFactories.get(0);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Replaces the default backend factory with a new one. This method must be called before
     * the first RRD gets created.
     * <p>
     * It also clear the list of actives factories and set it to the default factory.
     * <p>
     *
     * @param factoryName Name of the default factory..
     */
    public static void setDefaultFactory(String factoryName) {
        lock.writeLock().lock();
        try {
            // We will allow this only if no RRDs are created
            if (!RrdBackend.isInstanceCreated()) {
                activeFactories.clear();
                activeFactories.add(getFactory(factoryName));
            } else {
                throw new IllegalStateException(
                        "Could not change the default backend factory. "
                                + "This method must be called before the first RRD gets created");
            } 
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Set the list of active factories, i.e. the factory used to resolve URI.
     * 
     * @param newFactories the new active factories.
     */
    public static void setActiveFactories(RrdBackendFactory... newFactories) {
        lock.writeLock().lock();
        try {
            activeFactories.clear();
            activeFactories.addAll(Arrays.asList(newFactories));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Add factories to the list of active factories, i.e. the factory used to resolve URI.
     * 
     * @param newFactories active factories to add.
     */
    public static void addFactories(RrdBackendFactory... newFactories) {
        lock.writeLock().lock();
        try {
            activeFactories.addAll(Arrays.asList(newFactories));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * For a given URI, try to find a factory that can manage it.
     * 
     * @param uri URI to try.
     * @return a {@link RrdBackendFactory} that can manage that URI.
     * @throws IllegalArgumentException when no matching factory is found.
     */
    public static RrdBackendFactory findFactory(URI uri) {
        lock.readLock().lock();
        try {
            for (RrdBackendFactory tryfactory : activeFactories) {
                if (tryfactory.canStore(uri)) {
                    return tryfactory;
                }
            }
            throw new IllegalArgumentException(
                    "no matching backend factory for " + uri);
        } finally {
            lock.readLock().unlock();
        }
    }

    private static final Pattern URIPATTERN = Pattern.compile("^(?:(?<scheme>[a-zA-Z][a-zA-Z0-9+-\\.]*):)?(?://(?<authority>[^/\\?#]*))?(?<path>[^\\?#]*)(?:\\?(?<query>[^#]*))?(?:#(?<fragment>.*))?$");

    /**
     * Try to detect an URI from a path. It's needed because of windows path that look's like an URI
     * and to URL-encode the path.
     * 
     * @param rrdpath
     * @return an URI
     */
    public static URI buildGenericUri(String rrdpath) {
        Matcher urimatcher = URIPATTERN.matcher(rrdpath);
        if (urimatcher.matches()) {
            String scheme = urimatcher.group("scheme");
            String authority = urimatcher.group("authority");
            String path = urimatcher.group("path");
            String query = urimatcher.group("query");
            String fragment = urimatcher.group("fragment");
            try {
                // If scheme is a single letter, it's not a scheme, but a windows path
                if (scheme != null && scheme.length() == 1) {
                    return new File(rrdpath).toURI();
                }
                // A scheme and a not absolute path, it's an opaque URI
                if (scheme != null && path.charAt(0) != '/') {
                    return new URI(scheme, path, query);
                }
                // A relative file was given, ensure that it's OK if it was on a non-unix plateform
                if (File.separatorChar != '/' && scheme == null) {
                    path = path.replace(File.separatorChar, '/');
                }
                return new URI(scheme, authority, path, query, fragment);
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }
        throw new IllegalArgumentException("Not an URI pattern");
    }

    private static class ClosingReference extends PhantomReference<RrdDb> {
        private RrdBackend backend;
        public ClosingReference(RrdDb db, RrdBackend backend,
                ReferenceQueue<? super RrdDb> q) {
            super(db, q);
            this.backend = backend;
        }
        @Override
        public void clear() {
            try {
                backend.close();
            } catch (IOException e) {
            }
            backend = null;
            super.clear();
        }
    };

    private final ReferenceQueue<RrdDb> refQueue = new ReferenceQueue<>();

    protected final String name;
    protected final boolean cachingAllowed;
    protected final String scheme;
    protected final boolean shouldValidateHeader;

    protected RrdBackendFactory() {
        RrdBackendAnnotation annotation = getClass().getAnnotation(RrdBackendAnnotation.class);
        if (annotation != null) {
            name = annotation.name();
            cachingAllowed = annotation.cachingAllowed();
            if (annotation.scheme() != null && ! annotation.scheme().isEmpty()) {
                scheme = annotation.scheme();
            } else {
                scheme = name.toLowerCase(Locale.ENGLISH);
            }
            shouldValidateHeader = annotation.shouldValidateHeader();
        } else {
            name = getName();
            cachingAllowed = RrdBackendAnnotation.DEFAULT_CACHING_ALLOWED;
            scheme = getName().toLowerCase(Locale.ENGLISH);
            shouldValidateHeader = true;
        }
    }

    /**
     * Check that all phantom reference are indeed safely closed.
     */
    public void checkClosing() {
        while(true) {
            ClosingReference ref = (ClosingReference) refQueue.poll();
            if (ref == null) {
                break;
            } else if (ref.backend != null) {
                try {
                    ref.backend.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * @return the scheme name for URI, default to getName().toLowerCase()
     */
    public String getScheme() {
        return scheme;
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
     * <li>scheme must match if they are given.
     * <li>authority must match if they are given.
     * <li>if uri is opaque (scheme:nonabsolute), the scheme specific part is resolve as a relative path.
     * <li>query and fragment is kept as is.
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

    /**
     * Ensure that an URI is returned in a non-ambiguous way.
     * 
     * @param uri a valid URI for this backend.
     * @return the canonized URI.
     */
    public URI getCanonicalUri(URI uri) {
        return resolve(getRootUri(), uri, false);
    }

    /**
     * Transform an path in a valid URI for ths backend.
     * 
     * @param path
     * @return
     */
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

    /**
     * Extract the local path from an URI.
     * 
     * @param uri The URI to parse.
     * @return the local path from the URI.
     */
    public String getPath(URI uri) {
        URI rootUri = getRootUri();
        uri = resolve(rootUri, uri, true);
        if (uri == null) {
            return null;
        }
        return "/" + uri.getPath();
    }

    protected abstract RrdBackend open(String path, boolean readOnly) throws IOException;

    /**
     * Creates RrdBackend object for the given storage path.
     *
     * @param path     Storage path
     * @param readOnly True, if the storage should be accessed in read/only mode.
     *                 False otherwise.
     * @return Backend object which handles all I/O operations for the given storage path
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    RrdBackend getBackend(RrdDb rrdDb, String path, boolean readOnly) throws IOException {
        checkClosing();
        RrdBackend backend = open(path, readOnly);
        backend.done(this, new ClosingReference(rrdDb, backend, refQueue));
        return backend;
    }

    /**
     * Creates RrdBackend object for the given storage path.
     * @param rrdDb 
     *
     * @param uri     Storage uri
     * @param readOnly True, if the storage should be accessed in read/only mode.
     *                 False otherwise.
     * @return Backend object which handles all I/O operations for the given storage path
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    RrdBackend getBackend(RrdDb rrdDb, URI uri, boolean readOnly) throws IOException {
        checkClosing();
        RrdBackend backend =  open(getPath(uri), readOnly);
        backend.done(this, new ClosingReference(rrdDb, backend, refQueue));
        return backend;
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
    protected boolean shouldValidateHeader(String path) throws IOException {
        return shouldValidateHeader;
    }

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
    public String getName() {
        return name;
    }
    
    /**
     * A generic close handle, default do nothing.
     * @since 3.4
     * @throws IOException
     */
    public void close() throws IOException {
        
    }

}
