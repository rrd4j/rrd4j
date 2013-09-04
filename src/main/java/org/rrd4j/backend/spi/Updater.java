package org.rrd4j.backend.spi;

import java.io.IOException;

public interface Updater {
    public void save() throws IOException;
    public void load() throws IOException;
}
