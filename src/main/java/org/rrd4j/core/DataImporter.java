package org.rrd4j.core;

import java.io.Closeable;
import java.io.IOException;

import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;

/**
 * <p>An abstract class to import data from external source.</p>
 * @author Fabrice Bacchella
 * @since 3.5
 */
public abstract class DataImporter implements Closeable {

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

    protected long getEstimatedSize() throws IOException {
        int dsCount = getDsCount();
        int arcCount = getArcCount();
        int rowCount = 0;
        for (int i = 0; i < arcCount; i++) {
            rowCount += getRows(i);
        }
        String[] dsNames = new String[getDsCount()];
        for (int i = 0 ; i < dsNames.length; i++) {
            dsNames[i] = getDsName(i);
        }
        return RrdDef.calculateSize(dsCount, arcCount, rowCount, dsNames);
    }

    void release() throws IOException {
        // NOP
    }

    @Override
    public void close() throws IOException {
        release();
    }

}
