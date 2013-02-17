package org.rrd4j.core;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import sun.nio.ch.DirectBuffer;

/**
 * Backend which is used to store RRD data to ordinary disk files
 * using java.nio.* package. This is the default backend engine.
 */
public class RrdNioBackend extends RrdRandomAccessFileBackend {
    private MappedByteBuffer byteBuffer;

    private final Runnable syncRunnable = new Runnable() {
        public void run() {
            sync();
        }
    };

    private ScheduledFuture<?> syncRunnableHandle = null;

    /**
     * Creates RrdFileBackend object for the given file path, backed by java.nio.* classes.
     *
     * @param path       Path to a file
     * @param readOnly   True, if file should be open in a read-only mode. False otherwise
     * @param syncPeriod See {@link RrdNioBackendFactory#setSyncPeriod(int)} for explanation
     * @throws IOException Thrown in case of I/O error
     */
    protected RrdNioBackend(String path, boolean readOnly, RrdSyncThreadPool threadPool, int syncPeriod) throws IOException {
        super(path, readOnly);
        try {
            mapFile();
        }
        catch (IOException ioe) {
            super.close();
            throw ioe;
        }
        catch (RuntimeException rte) {
            super.close();
            throw rte;
        }
        try {
            if (!readOnly) {
                syncRunnableHandle = threadPool.scheduleWithFixedDelay(syncRunnable, syncPeriod, syncPeriod, TimeUnit.SECONDS);
            }
        } catch (RuntimeException rte) {
            unmapFile();
            super.close();
            throw rte;
        }
    }

    private void mapFile() throws IOException {
        long length = getLength();
        if (length > 0) {
            FileChannel.MapMode mapMode =
                    readOnly ? FileChannel.MapMode.READ_ONLY : FileChannel.MapMode.READ_WRITE;
            byteBuffer = rafile.getChannel().map(mapMode, 0, length);
        }
    }

    private void unmapFile() {
        if (byteBuffer != null) {
            if (byteBuffer instanceof DirectBuffer) {
                ((DirectBuffer) byteBuffer).cleaner().clean();
            }
            byteBuffer = null;
        }
    }

    /**
     * Sets length of the underlying RRD file. This method is called only once, immediately
     * after a new RRD file gets created.
     *
     * @param newLength Length of the RRD file
     * @throws IOException Thrown in case of I/O error.
     */
    protected synchronized void setLength(long newLength) throws IOException {
        unmapFile();
        super.setLength(newLength);
        mapFile();
    }

    /**
     * Writes bytes to the underlying RRD file on the disk
     *
     * @param offset Starting file offset
     * @param b      Bytes to be written.
     */
    protected synchronized void write(long offset, byte[] b) throws IOException {
        if (byteBuffer != null) {
            byteBuffer.position((int) offset);
            byteBuffer.put(b);
        }
        else {
            throw new IOException("Write failed, file " + getPath() + " not mapped for I/O");
        }
    }

    /**
     * Reads a number of bytes from the RRD file on the disk
     *
     * @param offset Starting file offset
     * @param b      Buffer which receives bytes read from the file.
     */
    protected synchronized void read(long offset, byte[] b) throws IOException {
        if (byteBuffer != null) {
            byteBuffer.position((int) offset);
            byteBuffer.get(b);
        }
        else {
            throw new IOException("Read failed, file " + getPath() + " not mapped for I/O");
        }
    }

    /**
     * Closes the underlying RRD file.
     *
     * @throws IOException Thrown in case of I/O error
     */
    public synchronized void close() throws IOException {
        // cancel synchronization
        try {
            if (!readOnly) {
                syncRunnableHandle.cancel(false);
                sync();
            }
            unmapFile();
        }
        finally {
            super.close();
        }
    }

    /**
     * This method forces all data cached in memory but not yet stored in the file,
     * to be stored in it.
     */
    protected synchronized void sync() {
        if (byteBuffer != null) {
            byteBuffer.force();
        }
    }
}
