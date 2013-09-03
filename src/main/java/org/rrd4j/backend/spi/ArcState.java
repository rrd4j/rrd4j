package org.rrd4j.backend.spi;

import java.io.IOException;

public abstract class ArcState implements Updater {
    public abstract double getAccumValue() throws IOException;
    public abstract void setAccumValue(double accumValue) throws IOException;

    public abstract void setNanSteps(long nanSteps) throws IOException;
    public abstract long getNanSteps() throws IOException;

}
