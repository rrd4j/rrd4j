package org.rrd4j.backend.spi;

import java.io.IOException;

import org.rrd4j.ConsolFun;

public abstract class Archive implements Updater {
    public ConsolFun consolFun;
    public int steps;
    public int rows;

    abstract public double getXff() throws IOException;
    abstract public void setXff(double xff) throws IOException;

}
