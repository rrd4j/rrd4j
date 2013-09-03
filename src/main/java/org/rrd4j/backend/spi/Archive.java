package org.rrd4j.backend.spi;

import org.rrd4j.ConsolFun;
import org.rrd4j.core.Robin;

public abstract class Archive implements Updater  {
    public ConsolFun consolFun;   // constant, may be cached
    public int steps;          // constant, may be cached
    public int rows;           // constant, may be cached
    
    abstract public double getXff();
    abstract public void setXff(double xff);
    
    public abstract Robin robin(int index);
    
    public abstract Iterable<Robin> robins();

}
