package org.rrd4j.backend.spi;

import java.io.IOException;

public abstract class Header implements Updater {
    public long step;
    public int dsCount, arcCount;
    public int version = -1;

    abstract public String getSignature() throws IOException;
    abstract public void setSignature(String signature) throws IOException;

    abstract public void setLastUpdateTime(long lastUpdateTime) throws IOException;
    abstract public long getLastUpdateTime() throws IOException;

    abstract public void validateHeader() throws IOException;

    /**
     * <p>getInfo.</p>
     *
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    abstract public String getInfo() throws IOException;

}
