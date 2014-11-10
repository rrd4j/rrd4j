package org.rrd4j.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
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
        final ReentrantLock inuse = new ReentrantLock();
        final Condition empty = inuse.newCondition();
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
    private final ReentrantReadWriteLock poolLock = new ReentrantReadWriteLock();

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

    private RrdEntry getUnlockedEntry(String path) throws IOException {
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
        try {
            ref.inuse.lockInterruptibly();
            return ref;
        }
        finally {
            poolLock.readLock().unlock();            
        }
    }

    private RrdEntry getUnusedEntry(String path) throws IOException, InterruptedException {
        poolLock.readLock().lockInterruptibly();
        RrdEntry ref = getUnlockedEntry(path);

        // Now wait until the condition empty is OK
        try {
            ref.inuse.lockInterruptibly();
            while(ref.count.intValue() != 0) {
                ref.empty.await();
            }
            return ref;
        } catch (InterruptedException e) {
            if(ref.inuse.isHeldByCurrentThread()) {
                ref.inuse.unlock();                
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

        try {
            if (ref.count.get() <= 0) {
                throw new IllegalStateException("Could not release [" + canonicalPath + "], the file was never requested");
            }
            if (ref.count.decrementAndGet() <= 0  && ref.rrdDb != null) {
                ref.rrdDb.close();
                ref.rrdDb = null;
                capacity.release();
                ref.count.set(0);
                ref.empty.signal();
            }
        } finally {
            ref.inuse.unlock();
        }

        //Ok, the last referenced was removed
        //try to avoid the Map to grow and remove the reference
        if(ref.count.get() == 0) {
            try {
                //Got exclusive access to the pool
                poolLock.writeLock().lockInterruptibly();
                ref = pool.get(canonicalPath);
                // Already removed
                if(ref == null) {
                    return;
                }
                ref.inuse.lockInterruptibly();                
                //No one started to wait on it, still no use, remove from the map
                if(! ref.inuse.hasWaiters(ref.empty) && ref.count.get() == 0) {
                    pool.remove(canonicalPath);
                }
                ref.inuse.unlock();
            } catch (InterruptedException e) {
                throw new RuntimeException("release interrupted for " + rrdDb, e);
            } finally {
                if (poolLock.isWriteLocked()) {
                    poolLock.writeLock().unlock();
                }
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
                ref.rrdDb = new RrdDb(path);
                ref.count.set(1);
                try {
                    capacity.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException("RrdDb acquire interrupted", e);
                }
            }
            return ref.rrdDb;
        } finally {
            ref.inuse.unlock();;
        }
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
            capacity.acquire();
            ref.rrdDb = new RrdDb(rrdDef);
            ref.count.set(1);
        } catch (IOException e) {
            capacity.release();
            throw e;
        } catch (IllegalArgumentException e) { //new RrdDb(rrdDef) can also throw IllegalArgumentException
            capacity.release();
            throw e;
        } catch (InterruptedException e) {
            throw new RuntimeException("request interrupted for new rrdDef " + rrdDef.getPath(), e);
        } finally {
            ref.inuse.unlock();;
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
            capacity.acquire();
            ref.rrdDb = new RrdDb(path, sourcePath);
            ref.count.set(1);
        } catch (IOException e) {
            capacity.release();
            throw e;
        } catch (InterruptedException e) {
            throw new RuntimeException("request interrupted for new rrd " + path, e);
        } finally {
            ref.inuse.unlock();;
        }
        return ref.rrdDb;
    }

    /**
     * Sets the maximum number of simultaneously open RRD files.
     *
     * @param newCapacity Maximum number of simultaneously open RRD files.
     */
    public void setCapacity(int newCapacity) {
        try {
            poolLock.writeLock().lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException("interrupted while changing capacity", e);
        }
        try {
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
        } finally {
            if (poolLock.isWriteLocked()) {
                poolLock.writeLock().unlock();
            }
        }
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
