package org.rrd4j.backend.spi;

import java.io.IOException;

public interface Updater {
    public void update() throws IOException;
    public void flush() throws IOException;
}
