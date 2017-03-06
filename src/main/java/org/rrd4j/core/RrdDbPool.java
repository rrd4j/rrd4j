package org.rrd4j.core;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class should be used to synchronize access to RRD files
 * in a multithreaded environment. This class should be also used to prevent opening of
 * too many RRD files at the same time (thus avoiding operating system limits).
 * <p>
 * It's much more scalable than the previous pool
 */
public class RrdDbPool {
    private static class RrdDbPoolSingletonHolder {
        static final RrdDbPool instance = new RrdDbPool();

        private RrdDbPoolSingletonHolder() {}
    }

    /**
     * Initial capacity of the pool i.e. maximum number of simultaneously open RRD files. The pool will
     * never open too many RRD files at the same time.
     */
    public static final int INITIAL_CAPACITY = 200;

    private static class RrdEntry {
        RrdDb rrdDb = null;
        int count = 0;
        final CountDownLatch waitempty;
        final CountDownLatch inuse;
        final boolean placeholder;
        final URI uri;
        RrdEntry(boolean placeholder, URI canonicalPath) {
            this.placeholder = placeholder;
            this.uri = canonicalPath;
            if( placeholder) {
                inuse = new CountDownLatch(1);
                waitempty = null;
            } else {
                inuse = null;
                waitempty = new CountDownLatch(1);
            }
        }
    }

    /**
     * Creates a single instance of the class on the first call,
     * or returns already existing one. Uses Initialization On Demand Holder idiom.
     *
     * @return Single instance of this class
     * @throws java.lang.RuntimeException Thrown if the default RRD backend is not derived from the {@link org.rrd4j.core.RrdFileBackendFactory}
     */
    public static RrdDbPool getInstance() {
        return RrdDbPoolSingletonHolder.instance;
    }

    private final AtomicInteger usage = new AtomicInteger(0);
    private final ReentrantLock countLock = new ReentrantLock();
    private final Condition full = countLock.newCondition();
    private int maxCapacity = INITIAL_CAPACITY;

    private final ConcurrentMap<URI, RrdEntry> pool = new ConcurrentHashMap<URI, RrdEntry>(INITIAL_CAPACITY);

    private final RrdBackendFactory defaultFactory;

    /**
     * Constructor for RrdDbPool.
     * 
     * Not private, used by junit tests
     */
    RrdDbPool() {
        if (!(RrdBackendFactory.getDefaultFactory() instanceof RrdFileBackendFactory)) {
            throw new RuntimeException("Cannot create instance of " + getClass().getName() + " with " +
                    "a default backend factory not derived from RrdFileBackendFactory");
        }
        defaultFactory = RrdBackendFactory.getDefaultFactory();
    }

    /**
     * Returns the number of open RRD files.
     *
     * @return Number of currently open RRD files held in the pool.
     */
    public int getOpenFileCount() {
        return usage.get();
    }

    /**
     * Returns an array of open file URI.
     *
     * @return Array with {@link URI} to open RRD files held in the pool.
     */
    public URI[] getOpenUri() {
        //Direct toarray from keySet can fail
        Set<URI> files = new HashSet<>();
        files.addAll(pool.keySet());
        return files.toArray(new URI[0]);
    }

    /**
     * Returns an array of open file path.
     *
     * @return Array with canonical path to open RRD files held in the pool.
     */
    public String[] getOpenFiles() {
        //Direct toarray from keySet can fail
        Set<String> files = new HashSet<>();
        for (RrdEntry i: pool.values()) {
            files.add(i.rrdDb.getPath());
        }
        return files.toArray(new String[0]);
    }

    private RrdEntry getEntry(URI uri, boolean cancreate) throws IOException, InterruptedException {
        RrdEntry ref;
        do {
            ref = pool.get(uri);
            if(ref == null) {
                //Slot empty
                //If still absent put a place holder, and create the entry to return
                try {
                    countLock.lockInterruptibly();
                    while(ref == null && usage.get() >= maxCapacity && cancreate) {
                        full.await();
                        ref = pool.get(uri);
                    }
                    if(ref == null && cancreate) {
                        ref = pool.putIfAbsent(uri, new RrdEntry(true, uri));
                        if(ref == null) {
                            ref = new RrdEntry(false, uri);
                            usage.incrementAndGet();
                        }
                    }
                } finally {
                    if(countLock.isHeldByCurrentThread()) {
                        countLock.unlock();
                    }
                }
            } else if(! ref.placeholder) {
                // Real entry, try to put a place holder if some one didn't get it meanwhile
                if( ! pool.replace(uri, ref, new RrdEntry(true, uri))) {
                    //Dummy ref, a new iteration is needed
                    ref = new RrdEntry(true, uri);
                }
            } else {
                // a place holder, wait for the using task to finish
                ref.inuse.await();
            }
        } while(ref != null && ref.placeholder);
        return ref;
    }

    private enum ACTION {
        SWAP, DROP;
    };

    private void passNext(ACTION a, RrdEntry e) {
        RrdEntry o = null;
        switch (a) {
        case SWAP:
            o = pool.put(e.uri, e);
            break;
        case DROP:
            o = pool.remove(e.uri);
            if(usage.decrementAndGet() < maxCapacity) {
                try {
                    countLock.lockInterruptibly();
                    full.signalAll();
                    countLock.unlock();
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }
            break;
        }
        //task finished, waiting on a place holder can go on
        if(o != null) {
            o.inuse.countDown();
        }  
    }

    /**
     * Releases RrdDb reference previously obtained from the pool. When a reference is released, its usage
     * count is decremented by one. If usage count drops to zero, the underlying RRD file will be closed.
     *
     * @param rrdDb RrdDb reference to be returned to the pool
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public void release(RrdDb rrdDb) throws IOException {
        // null pointer should not kill the thread, just ignore it
        if (rrdDb == null) {
            return;
        }

        URI dburi = rrdDb.getUri();
        RrdEntry ref;
        try {
            ref = getEntry(dburi, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("release interrupted for " + rrdDb, e);
        }
        if(ref == null) {
            return;
        }

        if (ref.count <= 0) {
            passNext(ACTION.DROP, ref);
            throw new IllegalStateException("Could not release [" + rrdDb.getPath() + "], the file was never requested");
        }
        if (--ref.count == 0) {
            if(ref.rrdDb == null) {
                passNext(ACTION.DROP, ref);
                throw new IllegalStateException("Could not release [" + rrdDb.getPath() + "], pool corruption");
            }
            ref.rrdDb.close();
            passNext(ACTION.DROP, ref);
            //If someone is waiting for an empty entry, signal it
            ref.waitempty.countDown();
        } else {
            passNext(ACTION.SWAP, ref);
        }
    }

    /**
     * <p>Requests a RrdDb reference for the given RRD file path.</p>
     * <ul>
     * <li>If the file is already open, previously returned RrdDb reference will be returned. Its usage count
     * will be incremented by one.
     * <li>If the file is not already open and the number of already open RRD files is less than
     * {@link #INITIAL_CAPACITY}, the file will be open and a new RrdDb reference will be returned.
     * If the file is not already open and the number of already open RRD files is equal to
     * {@link #INITIAL_CAPACITY}, the method blocks until some RRD file is closed.
     * </ul>
     * <p>The path is transformed internally to URI using the default factory, that is the reference that will
     * be used elsewhere.</p>
     *
     * @param path Path to existing RRD file
     * @return reference for the give RRD file
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public RrdDb requestRrdDb(String path) throws IOException {
        return requestRrdDb(defaultFactory.getUri(path), defaultFactory);
    }

    /**
     * <p>Requests a RrdDb reference for the given RRD file path.</p>
     * <ul>
     * <li>If the file is already open, previously returned RrdDb reference will be returned. Its usage count
     * will be incremented by one.
     * <li>If the file is not already open and the number of already open RRD files is less than
     * {@link #INITIAL_CAPACITY}, the file will be open and a new RrdDb reference will be returned.
     * If the file is not already open and the number of already open RRD files is equal to
     * {@link #INITIAL_CAPACITY}, the method blocks until some RRD file is closed.
     * </ul>
     *
     * @param uri {@link URI} to existing RRD file
     * @return reference for the give RRD file
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public RrdDb requestRrdDb(URI uri) throws IOException {
        RrdBackendFactory factory = RrdBackendFactory.findFactory(uri);
        return requestRrdDb(uri, factory);
    }

    private RrdDb requestRrdDb(URI uri, RrdBackendFactory factory) throws IOException {
        RrdEntry ref = null;
        try {
            ref = getEntry(uri, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("request interrupted for " + uri, e);
        }

        //Someone might have already open it, rechecks
        if(ref.count == 0) {
            try {
                ref.rrdDb = new RrdDb(factory.getPath(uri), factory);
            } catch (IOException e) {
                passNext(ACTION.DROP, ref);
                throw e;
            }
        }
        ref.count++;
        passNext(ACTION.SWAP, ref);
        return ref.rrdDb;
    }

    /**
     * Wait for a empty reference with no usage
     * @param uri
     * @return an reference with no usage 
     * @throws IOException
     * @throws InterruptedException
     */
    private RrdEntry waitEmpty(URI uri) throws IOException, InterruptedException {
        RrdEntry ref = getEntry(uri, true);
        try {
            while(ref.count != 0) {
                //Not empty, give it back, but wait for signal
                passNext(ACTION.SWAP, ref);
                ref.waitempty.await();
                ref = getEntry(uri, true);
            }
            return ref;
        } catch (InterruptedException e) {
            passNext(ACTION.DROP, ref);
            throw e;
        }
    }

    /**
     * Got an empty reference, use it only if slots are available
     * But don't hold any lock waiting for it
     * @param uri
     * @return an reference with no usage 
     * @throws InterruptedException
     * @throws IOException
     */
    private RrdEntry requestEmpty(URI uri) throws InterruptedException, IOException {
        RrdEntry ref = waitEmpty(uri);
        ref.count = 1;
        return ref;
    }

    /**
     * <p>Requests a RrdDb reference for the given RRD file definition object.</p>
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
     * @throws java.io.IOException Thrown in case of I/O error
     */

    public RrdDb requestRrdDb(RrdDef rrdDef) throws IOException {
        RrdEntry ref = null;
        try {
            URI uri = RrdBackendFactory.findFactory(rrdDef.getUri()).getCanonicalUri(rrdDef.getUri());
            ref = requestEmpty(uri);
            ref.rrdDb = new RrdDb(rrdDef);
            return ref.rrdDb;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("request interrupted for new rrdDef " + rrdDef.getPath(), e);
        } finally {
            if(ref != null) {
                passNext(ACTION.SWAP, ref);
            }
        }
    }

    /**
     * <p>Requests a RrdDb reference for the given path. The file will be created from
     * external data (from XML dump or RRDTool's binary RRD file).</p>
     * <ul>
     * <li>If the file with the path specified is already open,
     * the method blocks until the file is closed.
     * <li>If the file is not already open and the number of already open RRD files is less than
     * {@link #INITIAL_CAPACITY}, a new RRD file will be created and a its RrdDb reference will be returned.
     * If the file is not already open and the number of already open RRD files is equal to
     * {@link #INITIAL_CAPACITY}, the method blocks until some RRD file is closed.
     * </ul>
     * <p>The path is transformed internally to URI using the default factory, that is the reference that will
     * be used elsewhere.</p>
     *
     * @param path       Path to RRD file which should be created
     * @param sourcePath Path to external data which is to be converted to Rrd4j's native RRD file format
     * @return Reference to the newly created RRD file
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public RrdDb requestRrdDb(String path, String sourcePath)
            throws IOException {

        RrdEntry ref = null;
        try {
            ref = requestEmpty(defaultFactory.getUri(path));
            ref.rrdDb = new RrdDb(path, sourcePath);
            return ref.rrdDb;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("request interrupted for new rrd " + path, e);
        } finally {
            if(ref != null) {
                passNext(ACTION.SWAP, ref);
            }
        }

    }

    /**
     * <p>Requests a RrdDb reference for the given path. The file will be created from
     * external data (from XML dump or RRDTool's binary RRD file).</p>
     * <ul>
     * <li>If the file with the path specified is already open,
     * the method blocks until the file is closed.
     * <li>If the file is not already open and the number of already open RRD files is less than
     * {@link #INITIAL_CAPACITY}, a new RRD file will be created and a its RrdDb reference will be returned.
     * If the file is not already open and the number of already open RRD files is equal to
     * {@link #INITIAL_CAPACITY}, the method blocks until some RRD file is closed.
     * </ul>
     * <p>The path is transformed internally to URI using the default factory, that is the reference that will
     * be used elsewhere.</p>
     *
     * @param uri       Path to RRD file which should be created
     * @param sourcePath Path to external data which is to be converted to Rrd4j's native RRD file format
     * @return Reference to the newly created RRD file
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public RrdDb requestRrdDb(URI uri, String sourcePath)
            throws IOException {

        RrdEntry ref = null;
        try {
            ref = requestEmpty(uri);
            ref.rrdDb = new RrdDb(uri, sourcePath);
            return ref.rrdDb;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("request interrupted for new rrd " + uri, e);
        } finally {
            if(ref != null) {
                passNext(ACTION.SWAP, ref);
            }
        }

    }

    /**
     * Sets the maximum number of simultaneously open RRD files.
     *
     * @param newCapacity Maximum number of simultaneously open RRD files.
     */
    public void setCapacity(int newCapacity) {
        int oldUsage = usage.getAndSet(maxCapacity);
        try {
            if (oldUsage != 0) {
                throw new RuntimeException("Can only be done on a empty pool");
            }
        } finally {
            usage.set(oldUsage);
        }
        maxCapacity = newCapacity;
    }

    /**
     * Returns the maximum number of simultaneously open RRD files.
     *
     * @return maximum number of simultaneously open RRD files
     */
    public int getCapacity() {
        return maxCapacity;
    }

    /**
     * Returns the number of usage for a RRD.
     *
     * @param rrdDb RrdDb reference for which informations is needed.
     * @return the number of request for this rrd
     * @throws java.io.IOException if any.
     */
    public int getOpenCount(RrdDb rrdDb) throws IOException {
        return getOpenCount(rrdDb.getUri());
    }

    /**
     * Returns the number of usage for a RRD.
     *
     * @param path RRD's path for which informations is needed.
     * @return the number of request for this file
     * @throws java.io.IOException if any.
     */
    public int getOpenCount(String path) throws IOException {
        return getOpenCount(defaultFactory.getUri(path));
    }

    /**
     * Returns the number of usage for a RRD.
     *
     * @param uri RRD's uri for which informations is needed.
     * @return the number of request for this file
     * @throws java.io.IOException if any.
     */
    public int getOpenCount(URI path) throws IOException {
        RrdEntry ref = null;
        try {
            ref = getEntry(path, false);
            if(ref == null)
                return 0;
            else {
                return ref.count;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("getOpenCount interrupted", e);
        } finally {
            if(ref != null) {
                passNext(ACTION.SWAP, ref);
            }
        }
    }
}
