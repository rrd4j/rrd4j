package org.rrd4j.core;

import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;

import java.io.IOException;

public abstract class DataImporter {

    // header
    abstract public String getVersion() throws IOException;

    abstract public long getLastUpdateTime() throws IOException;

    abstract public long getStep() throws IOException;

    abstract public int getDsCount() throws IOException;

    abstract public int getArcCount() throws IOException;

    // datasource
    abstract public String getDsName(int dsIndex) throws IOException;

    abstract public DsType getDsType(int dsIndex) throws IOException;

    abstract public long getHeartbeat(int dsIndex) throws IOException;

    abstract public double getMinValue(int dsIndex) throws IOException;

    abstract public double getMaxValue(int dsIndex) throws IOException;

    // datasource state
    abstract public double getLastValue(int dsIndex) throws IOException;

    abstract public double getAccumValue(int dsIndex) throws IOException;

    abstract public long getNanSeconds(int dsIndex) throws IOException;

    // archive
    abstract public ConsolFun getConsolFun(int arcIndex) throws IOException;

    abstract public double getXff(int arcIndex) throws IOException;

    abstract public int getSteps(int arcIndex) throws IOException;

    abstract public int getRows(int arcIndex) throws IOException;

    // archive state
    abstract public double getStateAccumValue(int arcIndex, int dsIndex) throws IOException;

    abstract public int getStateNanSteps(int arcIndex, int dsIndex) throws IOException;

    abstract public double[] getValues(int arcIndex, int dsIndex) throws IOException;

    public void release() throws IOException {
        // NOP
    }

}
