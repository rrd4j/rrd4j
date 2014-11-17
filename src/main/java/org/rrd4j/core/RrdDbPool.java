package org.rrd4j.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

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
    }

    /**
     * Initial capacity of the pool i.e. maximum number of simultaneously open RRD files. The pool will
     * never open too many RRD files at the same time.
     */
    public static final int INITIAL_CAPACITY = 200;

    static private class RrdEntry {
        RrdDb rrdDb = null;
        int count = 0;
        final CountDownLatch waitempty;
        final CountDownLatch inuse;
        final boolean placeholder;
        final String canonicalPath;
        RrdEntry(boolean placeholder, String canonicalPath) {
            this.placeholder = placeholder;
            this.canonicalPath = canonicalPath;
            if( placeholder) {
                inuse = new CountDownLatch(1);
                waitempty = null;
            } else {
                inuse = null;
                waitempty = new CountDownLatch(1);
            }
        }
        @Override
        public String toString() {
            return placeholder + "@" + canonicalPath;
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

    private Semaphore capacity;
    private int maxCapacity = INITIAL_CAPACITY;

    private final ConcurrentMap<String, RrdEntry> pool = new ConcurrentHashMap<String, RrdEntry>(INITIAL_CAPACITY);

    /**
     * <p>Constructor for RrdDbPool.</p>
     * 
     * Not private, used by junit tests
     */
    RrdDbPool() {
        if (!(RrdBackendFactory.getDefaultFactory() instanceof RrdFileBackendFactory)) {
            throw new RuntimeException("Cannot create instance of " + getClass().getName() + " with " +
                    "a default backend factory not derived from RrdFileBackendFactory");
        }
        capacity = new Semaphore(maxCapacity, true);
    }

    /**
     * Returns the number of open RRD files.
     *
     * @return Number of currently open RRD files held in the pool.
     */
    public int getOpenFileCount() {
        return maxCapacity - capacity.availablePermits();
    }

    /**
     * Returns an array of open file names.
     *
     * @return Array with canonical paths to open RRD files held in the pool.
     */
    public String[] getOpenFiles() {
        //Direct toarray from keySet can fail
        Set<String> files = new HashSet<String>();
        files.addAll(pool.keySet());
        return files.toArray(new String[0]);
    }

    private RrdEntry getEntry(String path, boolean cancreate) throws IOException, InterruptedException {
        String canonicalPath = Util.getCanonicalPath(path);
        RrdEntry ref = null;
        while(ref == null || ref.placeholder) {
            ref = pool.get(canonicalPath);
            if(ref == null) {
                //Slot empty
                //If still absent put a place holder, and create the entry
                ref = pool.putIfAbsent(canonicalPath, new RrdEntry(true, canonicalPath));
                if(ref == null) {
                    return cancreate ? new RrdEntry(false, canonicalPath) : null;                    
                }               
            } else if(! ref.placeholder) {
                // Real entry, try to put a place holder if some one did'nt get it meanwhile
                if(pool.replace(canonicalPath, ref, new RrdEntry(true, canonicalPath))) {
                    return ref;
                }
            }
            // Reached only if a place holder and not null
            // Wait for other tasks to be finished
            ref.inuse.await();
        }
        return ref;
    }

    private enum ACTION {
        SWAP, DROP;
    };

    private void passNext(ACTION a, RrdEntry e) {
        RrdEntry o = null;
        switch (a) {
        case SWAP:
            o = pool.put(e.canonicalPath, e);
            break;
        case DROP:
            o = pool.remove(e.canonicalPath);
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

        RrdEntry ref;
        try {
            ref = getEntry(rrdDb.getPath(), false);
        } catch (InterruptedException e) {
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
            capacity.release();
            passNext(ACTION.DROP, ref);
            //If someone is waiting for an empty entry, signal it
            ref.waitempty.countDown();
        } else {
            passNext(ACTION.SWAP, ref);
        }
    }

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
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public RrdDb requestRrdDb(String path) throws IOException {
        RrdEntry ref;
        try {
            ref = getEntry(path, true);
        } catch (InterruptedException e) {
            throw new RuntimeException("request interrupted for " + path, e);
        }

        try {
            //Loop if count was 0 (need open) and acquire failed
            while(ref.count == 0 && ! capacity.tryAcquire()) {
                boolean acquired = false;
                try {
                    //Don't hold the ref, wait until acquire succeeded
                    passNext(ACTION.SWAP, ref);
                    capacity.acquire();
                    acquired = true;
                    ref = getEntry(path, true);    
                    //oups, this was not the real acquire, release for the while test 
                    capacity.release();
                } catch (InterruptedException e) {
                    if(acquired) {
                        capacity.release();                       
                    }
                    throw new RuntimeException("RrdDb acquire interrupted", e);
                }                
            }
            //lock was successfully acquired with an empty ref
            if(ref.count == 0) {
                try {
                    ref.rrdDb = new RrdDb(path);
                } catch (IOException e) {
                    capacity.release();
                    throw e;
                }
            }
            ref.count++;
            return ref.rrdDb;
        } finally {
            passNext(ACTION.SWAP, ref);
        }
    }

    private RrdEntry waitEmpty(String path) throws IOException, InterruptedException {
        RrdEntry ref = null;
        try {
            do {
                ref = getEntry(path, true);
                if(ref.count != 0) {
                    //Not empty, give it back, but wait for signal
                    passNext(ACTION.SWAP, ref);                
                    ref.waitempty.await();
                    // ref is not a real reference (not replaced by a placeholder
                    // So must try a get entry before carry on
                    ref = null; //might be checked by the catch
                    continue;
                }
            } while(ref == null || ref.count != 0);
            return ref;
        } catch (InterruptedException e) {
            if(ref != null) {
                passNext(ACTION.SWAP, ref);                
            }
            throw e;
        }
    }

    private RrdEntry requestEmpty(String path) throws InterruptedException, IOException {
        RrdEntry ref = waitEmpty(path);

        //No slot, release the lock and wait
        boolean acquired = capacity.tryAcquire();
        while(! acquired) {
            try {
                passNext(ACTION.SWAP, ref);
                capacity.acquire();
                acquired = true;
                ref = waitEmpty(path);    
            } catch (InterruptedException e) {
                if(acquired) {
                    capacity.release();                       
                }
                throw e;
            }
        }
        ref.count = 1;
        return ref;
    }

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
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public RrdDb requestRrdDb(RrdDef rrdDef) throws IOException {
        RrdEntry ref = null;
        try {
            ref = requestEmpty(rrdDef.getPath());
            ref.rrdDb = new RrdDb(rrdDef);
            return ref.rrdDb;
        } catch (InterruptedException e) {
            throw new RuntimeException("request interrupted for new rrdDef " + rrdDef.getPath(), e);
        } finally {
            if(ref != null) {
                passNext(ACTION.SWAP, ref);                                            
            }
        }
    }

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
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public RrdDb requestRrdDb(String path, String sourcePath)
            throws IOException {

        RrdEntry ref = null;
        try {
            ref = requestEmpty(path);
            ref.rrdDb = new RrdDb(path, sourcePath);
            return ref.rrdDb;
        } catch (InterruptedException e) {
            throw new RuntimeException("request interrupted for new rrd " + path, e);
        } finally {
            if(ref != null) {
                passNext(ACTION.SWAP, ref);                                            
            }
        }

    }

    /**
     * Sets the maximum number of simultaneously open RRD files.
     *
     * @param capacity Maximum number of simultaneously open RRD files.
     */
    public void setCapacity(int newCapacity) {
        int available = capacity.drainPermits();
        if (available != maxCapacity) {
            capacity.release(available);
            throw new RuntimeException("Can only be done on a empty pool");
        }
        else {
            capacity = new Semaphore(newCapacity, true);
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
        return getOpenCount(rrdDb.getPath());
    }

    /**
     * Returns the number of usage for a RRD.
     *
     * @param path RRD file for which informations is needed.
     * @return the number of request for this file
     * @throws java.io.IOException if any.
     */
    public int getOpenCount(String path) throws IOException {
        RrdEntry ref = null;
        try {
            ref = getEntry(path, false);
            if(ref == null)
                return 0;
            else {
                return ref.count;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("getOpenCount interrupted", e);
        } finally {
            if(ref != null) {
                passNext(ACTION.SWAP, ref);
            }
        }
    }
}
