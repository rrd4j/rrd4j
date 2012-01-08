package org.rrd4j.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class should be used to synchronize access to RRD files
 * in a multithreaded environment. This class should be also used to prevent opening of
 * too many RRD files at the same time (thus avoiding operating system limits).
 * <p>
 * It's much more scalable than the previous pool
 */
class RrdDbPoolNew extends RrdDbPool {
    static private class RrdEntry {
        volatile RrdDb rrdDb = null;
        final AtomicInteger count = new AtomicInteger(0);
        final Semaphore ulock = new Semaphore(1, true);
        final Semaphore rlock = new Semaphore(1, true);
    }

    private Semaphore capacity;
    private int maxCapacity = INITIAL_CAPACITY;

    private final Map<String, RrdEntry> pool = new HashMap<String, RrdEntry>(INITIAL_CAPACITY);
    private final Semaphore poolLock = new Semaphore(1, true);

    protected RrdDbPoolNew() {
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

    public int getOpenFileCount() {
        return maxCapacity - capacity.availablePermits();
    }

    public String[] getOpenFiles() {
        try {
            poolLock.acquire();
            Set<String> files = new HashSet<String>(pool.keySet().size());
            for(Map.Entry<String,RrdEntry> e: pool.entrySet()) {
                RrdEntry re = e.getValue();
                if(re != null && re.count.get() > 0) {
                    files.add(e.getKey());
                }
            }
            poolLock.release();
            return files.toArray(new String[files.size()]);
        } catch (InterruptedException e) {
        }
        return new String[]{};
    }

    private RrdEntry getEntry(String path) throws IOException {
        String canonicalPath = Util.getCanonicalPath(path);
        RrdEntry ref;
        boolean poollocked = false;
        try {
            poolLock.acquire();
            poollocked = true;
            ref = pool.get(canonicalPath);
            if(ref == null) {
                ref = new RrdEntry();
                pool.put(canonicalPath, ref);
            }
            ref.rlock.acquire();
        } catch (InterruptedException e) {                
            throw new RuntimeException(e);
        }
        finally {
            if(poollocked)
                poolLock.release();
        }
        return ref;
    }

    private RrdEntry getUnusedEntry(String path) throws IOException {
        String canonicalPath = Util.getCanonicalPath(path);
        RrdEntry ref = getEntry(canonicalPath);
        ref.rlock.release();
        //wait until the entry is unused
        while(true) {
            try {
                ref.ulock.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException("RrdDb acquire interrupted", e);
            }
            ref = getEntry(canonicalPath);
            //Check if ref is still unreferenced
            if(ref.count.get() == 0) {
                try {
                    capacity.acquire();
                } catch (InterruptedException e) {
                    ref.rlock.release();
                    ref.ulock.release();
                    throw new RuntimeException("RrdDb acquire interrupted", e);
                }
                return ref;
            }
        }
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdDbPoolI#release(org.rrd4j.core.RrdDb)
     */
    public void release(RrdDb rrdDb) throws IOException {
        // null pointer should not kill the thread, just ignore it
        if (rrdDb == null) {
            return;
        }

        String canonicalPath = rrdDb.getCanonicalPath();
        RrdEntry ref = getEntry(canonicalPath);

        try {
            if (ref.count.get() <= 0) {
                throw new IllegalStateException("Could not release [" + canonicalPath + "], the file was never requested");
            }
            if (ref.count.decrementAndGet() <= 0  && ref.rrdDb != null) {
                ref.rrdDb.close();
                ref.rrdDb = null;
                capacity.release();
                ref.count.set(0);
                ref.ulock.release();
            }
        } finally {
            ref.rlock.release();
        }

        boolean poollocked = false;
        if(ref.count.get() == 0) {
            try {
                poolLock.acquire();
                poollocked = true;
                ref = pool.get(canonicalPath);
                //Only try to acquire
                //It if failed, some one is working on it, so that's up to him to manage the cleaning
                if(ref != null && ref.rlock.tryAcquire()) {
                    if(ref.count.get() == 0)
                        pool.remove(canonicalPath);
                    ref.rlock.release();
                }
                //No worry if the ref cannot be locked, that's not a big deal
            } catch (InterruptedException e) {
            }
            finally {
                if(poollocked)
                    poolLock.release();
            }
        }

    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdDbPoolI#requestRrdDb(java.lang.String)
     */
    public RrdDb requestRrdDb(String path) throws IOException {
        RrdEntry ref = getEntry(path);

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
                //Weak acquire; we just want to be sure someone got it
                //But don't hang on it
                ref.ulock.tryAcquire();
            }
        } finally {
            ref.rlock.release();
        }
        return ref.rrdDb;
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdDbPoolI#requestRrdDb(org.rrd4j.core.RrdDef)
     */
    public RrdDb requestRrdDb(RrdDef rrdDef) throws IOException {
        RrdEntry ref = null;
        try {
            ref = getUnusedEntry(rrdDef.getPath());
            ref.rrdDb = new RrdDb(rrdDef);
            ref.count.set(1);
        } catch (IOException e) {
            capacity.release();
            throw e;
        } finally {
            ref.rlock.release();
        }
        return ref.rrdDb;
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdDbPoolI#requestRrdDb(java.lang.String, java.lang.String)
     */
    public RrdDb requestRrdDb(String path, String sourcePath)
    throws IOException {
        RrdEntry ref = null;
        try {
            ref = getUnusedEntry(path);
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

    public int getCapacity() {
        return maxCapacity;
    }

    @Override
    public int getOpenCount(RrdDb rrdDb) throws IOException {
        String canonicalPath = rrdDb.getCanonicalPath();
        RrdEntry ref = pool.get(canonicalPath);
        if(ref == null)
            return 0;
        return ref.count.get();
    }

    @Override
    public int getOpenCount(String path) throws IOException {
        String canonicalPath = Util.getCanonicalPath(path);
        RrdEntry ref = pool.get(canonicalPath);
        if(ref == null)
            return 0;
        return ref.count.get();
    }
}
