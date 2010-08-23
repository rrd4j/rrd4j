package org.rrd4j.core;

import java.io.IOException;

class RrdAllocator {
    private long allocationPointer = 0L;

    long allocate(long byteCount) throws IOException {
        long pointer = allocationPointer;
        allocationPointer += byteCount;
        return pointer;
    }
}