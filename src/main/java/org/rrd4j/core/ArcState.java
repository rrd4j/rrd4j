package org.rrd4j.core;

import java.io.IOException;

/**
 * Class to represent internal RRD archive state for a single datasource. Objects of this
 * class are never manipulated directly, it's up to Rrd4j to manage internal archive states.
 *
 * @author Sasa Markovic
 */
public class ArcState implements RrdUpdater {
    private Archive parentArc;

    org.rrd4j.backend.spi.ArcState spi;

    ArcState(Archive parentArc, boolean shouldInitialize) throws IOException {
        this.parentArc = parentArc;
        spi = parentArc.getParentDb().getRrdBackend().ArcState();

        if (shouldInitialize) {
            Header header = parentArc.getParentDb().getHeader();
            long step = header.getStep();
            long lastUpdateTime = header.getLastUpdateTime();
            long arcStep = parentArc.getArcStep();
            long initNanSteps = (Util.normalize(lastUpdateTime, step) -
                    Util.normalize(lastUpdateTime, arcStep)) / step;
            spi.setAccumValue(Double.NaN);
            spi.setNanSteps(initNanSteps);
        }
    }

    String dump() throws IOException {
        return "accumValue:" + spi.getAccumValue() + " nanSteps:" + spi.getNanSteps() + "\n";
    }

    void setNanSteps(long value) throws IOException {
        spi.setNanSteps(value);
    }

    /**
     * Returns the number of currently accumulated NaN steps.
     *
     * @return Number of currently accumulated NaN steps.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public long getNanSteps() throws IOException {
        return spi.getNanSteps();
    }

    void setAccumValue(double value) throws IOException {
        spi.setAccumValue(value);
    }

    /**
     * Returns the value accumulated so far.
     *
     * @return Accumulated value
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public double getAccumValue() throws IOException {
        return spi.getAccumValue();
    }

    /**
     * Returns the Archive object to which this ArcState object belongs.
     *
     * @return Parent Archive object.
     */
    public Archive getParent() {
        return parentArc;
    }

    void appendXml(XmlWriter writer) throws IOException {
        writer.startTag("ds");
        writer.writeTag("value", spi.getAccumValue());
        writer.writeTag("unknown_datapoints", spi.getNanSteps());
        writer.closeTag(); // ds
    }

    /**
     * {@inheritDoc}
     *
     * Copies object's internal state to another ArcState object.
     */
    public void copyStateTo(RrdUpdater other) throws IOException {
        if (!(other instanceof ArcState)) {
            throw new IllegalArgumentException("Cannot copy ArcState object to " + other.getClass().getName());
        }
        ArcState arcState = (ArcState) other;
        arcState.spi.setAccumValue(spi.getAccumValue());
        arcState.spi.setNanSteps(spi.getNanSteps());
    }

    /**
     * Returns the underlying storage (backend) object which actually performs all
     * I/O operations.
     *
     * @return I/O backend object
     */
    public RrdBackend getRrdBackend() {
        return parentArc.getRrdBackend();
    }

}
