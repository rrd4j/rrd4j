package org.rrd4j.backend.spi;

import java.io.IOException;

public abstract class Header implements Updater {
    
    public long step;                  // constant, may be cached
    public int dsCount, arcCount;      // constant, may be cached

    abstract public String getSignature() throws IOException;
    abstract public void setSignature(String signature) throws IOException;
    
    abstract public void setLastUpdateTime(long lastUpdateTime) throws IOException;
    abstract public long getLastUpdateTime() throws IOException;
    
}
