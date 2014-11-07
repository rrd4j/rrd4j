package org.rrd4j.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
        volatile RrdDb rrdDb = null;
        final AtomicInteger count = new AtomicInteger(0);
        //A flag that indicate the rrdDb is in use (count is non zero)
        final Semaphore ulock = new Semaphore(1, true);
        //A flag that indicate that update operation are active for this rrdDb
        final Semaphore rlock = new Semaphore(1, true);
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
    private final ReadWriteLock poolLock = new ReentrantReadWriteLock();

    /**
     * <p>Constructor for RrdDbPool.</p>
     */
    private RrdDbPool() {
        if (!(RrdBackendFactory.getDefaultFactory() instanceof RrdFileBackendFactory)) {
            throw new RuntimeException("Cannot create instance of " + getClass().getName() + " with " +
                    "a default backend factory not derived from RrdFileBackendFactory");
        }
        capacity = new Semaphore(maxCapacity, true) {
            @Override
            public String toString() {
                return "Capacity semaphore: " + super.toString();
            }            
        };
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
        Set<String> files = new HashSet<String>(pool.keySet().size());
        for(Map.Entry<String,RrdEntry> e: pool.entrySet()) {
            RrdEntry re = e.getValue();
            if(re != null && re.count.get() > 0) {
                files.add(e.getKey());
            }
        }
        return files.toArray(new String[files.size()]);
    }

    private RrdEntry getUnlockedEntry(String path) throws IOException, InterruptedException {
        String canonicalPath = Util.getCanonicalPath(path);
        RrdEntry ref = pool.putIfAbsent(canonicalPath, new RrdEntry());
        if(ref == null) {
            ref = pool.get(canonicalPath);
        }
        return ref;
    }
    private RrdEntry getEntry(String path) throws IOException, InterruptedException  {
        poolLock.readLock().lockInterruptibly();
        RrdEntry ref = getUnlockedEntry(path);
        boolean acquired = ref.ulock.tryAcquire();
        try {
            ref.rlock.acquire();
            return ref;
        } catch (InterruptedException e) {
            if(acquired) {
                ref.ulock.release();                
            }
            throw e;
        }
        finally {
            poolLock.readLock().unlock();            
        }
    }

    private RrdEntry getUnusedEntry(String path) throws IOException, InterruptedException {
        poolLock.readLock().lockInterruptibly();
        RrdEntry ref = getUnlockedEntry(path);
        boolean acquired = false;
        try {
            ref.ulock.acquire();
            acquired = true;
            ref.rlock.acquire();
            return ref;
        } catch (InterruptedException e) {
            if(acquired) {
                ref.ulock.release();                
            }
            throw e;
        }
        finally {
            poolLock.readLock().unlock();            
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

        String canonicalPath = rrdDb.getCanonicalPath();
        RrdEntry ref;
        try {
            ref = getEntry(canonicalPath);
        } catch (InterruptedException e) {
            throw new RuntimeException("release interrupted for " + rrdDb, e);
        }

        if (ref.count.get() <= 0) {
            throw new IllegalStateException("Could not release [" + canonicalPath + "], the file was never requested");
        }
        if (ref.count.decrementAndGet() <= 0  && ref.rrdDb != null) {
            ref.rrdDb.close();
            ref.rrdDb = null;
            capacity.release();
            ref.count.set(0);
            ref.rlock.release();
            ref.ulock.release();
        }

        if(ref.count.get() == 0) {
            try {
                //Got exclusive access to the pool
                poolLock.writeLock().lockInterruptibly();
                ref = pool.get(canonicalPath);
                //It if failed, some one is working on it, so that's up to him to manage the cleaning
                if(ref != null && ref.rlock.tryAcquire()) {
                    if(ref.count.get() == 0)
                        pool.remove(canonicalPath);
                    ref.rlock.release();
                }
                poolLock.writeLock().unlock();
            } catch (InterruptedException e) {
                throw new RuntimeException("release interrupted for " + rrdDb, e);
            }
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
            ref = getEntry(path);
        } catch (InterruptedException e) {
            throw new RuntimeException("request interrupted for " + path, e);
        }

        try {
            if(ref.count.get() > 0) {
                ref.count.incrementAndGet();
            }
            //Not opened or in inconsistent state, try to recover
            else {
                try {
                    capacity.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException("RrdDb acquire interrupted", e);
                }
                try {
                    ref.rrdDb = new RrdDb(path);
                } catch (IOException e) {
                    capacity.release();
                    throw e;
                }
                ref.count.set(1);
            }
        } finally {
            ref.rlock.release();
        }
        return ref.rrdDb;
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
        RrdEntry ref;
        try {
            ref = getUnusedEntry(rrdDef.getPath());
        } catch (InterruptedException e) {
            throw new RuntimeException("request interrupted for new rrdDef " + rrdDef.getPath(), e);
        }
        try {
            ref.rrdDb = new RrdDb(rrdDef);
            ref.count.set(1);
        } catch (IOException e) {
            capacity.release();
            throw e;
        } catch (IllegalArgumentException e) { //new RrdDb(rrdDef) can also throw IllegalArgumentException
            capacity.release();
            throw e;
        } finally {
            ref.rlock.release();
        }
        return ref.rrdDb;
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
        RrdEntry ref;
        try {
            ref = getUnusedEntry(path);
        } catch (InterruptedException e) {
            throw new RuntimeException("request interrupted for new rrd " + path, e);
        }
        try {
            ref.rrdDb = new RrdDb(path, sourcePath);
            ref.count.set(1);
        } catch (IOException e) {
            capacity.release();
            throw e;
        } finally {
            ref.rlock.release();
        }
        return ref.rrdDb;
    }

    /**
     * Sets the maximum number of simultaneously open RRD files.
     *
     * @param newCapacity Maximum number of simultaneously open RRD files.
     */
    public void setCapacity(int newCapacity) {
        int available = capacity.drainPermits();
        if (available != maxCapacity) {
            capacity.release(available);
            throw new RuntimeException("Can only be done on a empty pool");
        }
        else {
            capacity = new Semaphore(newCapacity, true) {
                @Override
                public String toString() {
                    return "Capacity semaphore: " + super.toString();
                }            
            };
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
        String canonicalPath = rrdDb.getCanonicalPath();
        RrdEntry ref = pool.get(canonicalPath);
        if(ref == null)
            return 0;
        return ref.count.get();
    }

    /**
     * Returns the number of usage for a RRD.
     *
     * @param path RRD file for which informations is needed.
     * @return the number of request for this file
     * @throws java.io.IOException if any.
     */
    public int getOpenCount(String path) throws IOException {
        String canonicalPath = Util.getCanonicalPath(path);
        RrdEntry ref = pool.get(canonicalPath);
        if(ref == null)
            return 0;
        return ref.count.get();
    }
}
