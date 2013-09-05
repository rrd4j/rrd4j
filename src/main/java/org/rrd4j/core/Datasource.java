package org.rrd4j.core;

import java.io.IOException;

import org.rrd4j.DsType;
import org.rrd4j.backend.RrdBackend;

/**
 * Class to represent single datasource within RRD. Each datasource object holds the
 * following information: datasource definition (once set, never changed) and
 * datasource state variables (changed whenever RRD gets updated).<p>
 * <p/>
 * Normally, you don't need to manipulate Datasource objects directly, it's up to
 * Rrd4j framework to do it for you.
 *
 * @author Sasa Markovic
 */
public class Datasource implements RrdUpdater {
    private static final double MAX_32_BIT = Math.pow(2, 32);
    private static final double MAX_64_BIT = Math.pow(2, 64);

    private final RrdDb parentDb;

    private final org.rrd4j.backend.spi.Datasource spi;
    
    Datasource(RrdDb parentDb, DsDef dsDef, int dsIndex) throws IOException {
        spi = parentDb.getRrdBackend().getDatasource(dsIndex);
        boolean shouldInitialize = dsDef != null;
        this.parentDb = parentDb;
        if (shouldInitialize) {
            spi.dsName = dsDef.getDsName();
            spi.dsType = dsDef.getDsType();
            spi.heartbeat = dsDef.getHeartbeat();
            spi.minValue = dsDef.getMinValue();
            spi.maxValue = dsDef.getMaxValue();
            spi.setLastValue(Double.NaN);
            spi.setAccumValue(0.0);
            Header header = parentDb.getHeader();
            spi.setNanSeconds(header.getLastUpdateTime() % header.getStep());
            spi.save();
        }
    }

    Datasource(RrdDb parentDb, DataImporter reader, int dsIndex) throws IOException {
        this(parentDb, (DsDef) null, dsIndex);
        spi.dsName = reader.getDsName(dsIndex);
        spi.dsType = reader.getDsType(dsIndex);
        spi.heartbeat = reader.getHeartbeat(dsIndex);
        spi.minValue = reader.getMinValue(dsIndex);
        spi.maxValue = reader.getMaxValue(dsIndex);
        spi.setLastValue(reader.getLastValue(dsIndex));
        spi.setAccumValue(reader.getAccumValue(dsIndex));
        spi.setNanSeconds(reader.getNanSeconds(dsIndex));
        spi.save();
    }

    String dump() throws IOException {
        return "== DATASOURCE ==\n" +
                "DS:" + spi.dsName + ":" + spi.dsType + ":" +
                spi.heartbeat + ":" + spi.minValue + ":" +
                spi.maxValue + "\nlastValue:" + spi.getLastValue() +
                " nanSeconds:" + spi.getNanSeconds() +
                " accumValue:" + spi.getAccumValue() + "\n";
    }

    /**
     * Returns datasource name.
     *
     * @return Datasource name
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public String getName() throws IOException {
        return spi.dsName;
    }

    /**
     * Returns datasource type (GAUGE, COUNTER, DERIVE, ABSOLUTE).
     *
     * @return Datasource type.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public DsType getType() throws IOException {
        return spi.dsType;
    }

    /**
     * Returns datasource heartbeat
     *
     * @return Datasource heartbeat
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public long getHeartbeat() throws IOException {
        return spi.heartbeat;
    }

    /**
     * Returns minimal allowed value for this datasource.
     *
     * @return Minimal value allowed.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public double getMinValue() throws IOException {
        return spi.minValue;
    }

    /**
     * Returns maximal allowed value for this datasource.
     *
     * @return Maximal value allowed.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public double getMaxValue() throws IOException {
        return spi.maxValue;
    }

    /**
     * Returns last known value of the datasource.
     *
     * @return Last datasource value.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public double getLastValue() throws IOException {
        return spi.getLastValue();
    }

    /**
     * Returns value this datasource accumulated so far.
     *
     * @return Accumulated datasource value.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public double getAccumValue() throws IOException {
        return spi.getAccumValue();
    }

    /**
     * Returns the number of accumulated NaN seconds.
     *
     * @return Accumulated NaN seconds.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public long getNanSeconds() throws IOException {
        return spi.getNanSeconds();
    }

    final void process(long newTime, double newValue) throws IOException {
        Header header = parentDb.getHeader();
        long step = header.getStep();
        long oldTime = header.getLastUpdateTime();
        long startTime = Util.normalize(oldTime, step);
        long endTime = startTime + step;
        double oldValue = spi.getLastValue();
        double updateValue = calculateUpdateValue(oldTime, oldValue, newTime, newValue);
        if (newTime < endTime) {
            accumulate(oldTime, newTime, updateValue);
        }
        else {
            // should store something
            long boundaryTime = Util.normalize(newTime, step);
            accumulate(oldTime, boundaryTime, updateValue);
            double value = calculateTotal(startTime, boundaryTime);

            // how many updates?
            long numSteps = (boundaryTime - endTime) / step + 1L;

            // ACTION!
            parentDb.archive(this, value, numSteps);

            // cleanup
            spi.setNanSeconds(0);
            spi.setAccumValue(0.0);

            accumulate(boundaryTime, newTime, updateValue);
        }
    }

    private double calculateUpdateValue(long oldTime, double oldValue,
                                        long newTime, double newValue) throws IOException {
        double updateValue = Double.NaN;
        if (newTime - oldTime <= spi.heartbeat) {


            if (spi.dsType == DsType.GAUGE) {
                updateValue = newValue;
            }
            else if (spi.dsType == DsType.COUNTER) {
                if (!Double.isNaN(newValue) && !Double.isNaN(oldValue)) {
                    double diff = newValue - oldValue;
                    if (diff < 0) {
                        diff += MAX_32_BIT;
                    }
                    if (diff < 0) {
                        diff += MAX_64_BIT - MAX_32_BIT;
                    }
                    if (diff >= 0) {
                        updateValue = diff / (newTime - oldTime);
                    }
                }
            }
            else if (spi.dsType == DsType.ABSOLUTE) {
                if (!Double.isNaN(newValue)) {
                    updateValue = newValue / (newTime - oldTime);
                }
            }
            else if (spi.dsType == DsType.DERIVE) {
                if (!Double.isNaN(newValue) && !Double.isNaN(oldValue)) {
                    updateValue = (newValue - oldValue) / (newTime - oldTime);
                }
            }

            if (!Double.isNaN(updateValue)) {
                double minVal = spi.minValue;
                double maxVal = spi.maxValue;
                if (!Double.isNaN(minVal) && updateValue < minVal) {
                    updateValue = Double.NaN;
                }
                if (!Double.isNaN(maxVal) && updateValue > maxVal) {
                    updateValue = Double.NaN;
                }
            }
        }
        spi.setLastValue(newValue);
        return updateValue;
    }

    private void accumulate(long oldTime, long newTime, double updateValue) throws IOException {
        if (Double.isNaN(updateValue)) {
            spi.setNanSeconds(spi.getNanSeconds()  + (newTime - oldTime));
        }
        else {
            spi.setAccumValue(spi.getAccumValue() + updateValue * (newTime - oldTime));
        }
    }

    private double calculateTotal(long startTime, long boundaryTime) throws IOException {
        double totalValue = Double.NaN;
        long validSeconds = boundaryTime - startTime - spi.getNanSeconds();
        if (spi.getNanSeconds() <= spi.heartbeat && validSeconds > 0) {
            totalValue = spi.getAccumValue() / validSeconds;
        }
        // IMPORTANT:
        // if datasource name ends with "!", we'll send zeros instead of NaNs
        // this might be handy from time to time
        if (Double.isNaN(totalValue) && spi.dsName.endsWith(DsDef.FORCE_ZEROS_FOR_NANS_SUFFIX)) {
            totalValue = 0D;
        }
        return totalValue;
    }

    void appendXml(XmlWriter writer) throws IOException {
        writer.startTag("ds");
        writer.writeTag("name", spi.dsName);
        writer.writeTag("type", spi.dsType);
        writer.writeTag("minimal_heartbeat", spi.heartbeat);
        writer.writeTag("min", spi.minValue);
        writer.writeTag("max", spi.maxValue);
        writer.writeComment("PDP Status");
        writer.writeTag("last_ds", spi.getLastValue(), "UNKN");
        writer.writeTag("value", spi.getAccumValue());
        writer.writeTag("unknown_sec", spi.getNanSeconds());
        writer.closeTag();  // ds
    }

    /**
     * {@inheritDoc}
     *
     * Copies object's internal state to another Datasource object.
     */
    public void copyStateTo(RrdUpdater other) throws IOException {
        if (!(other instanceof Datasource)) {
            throw new IllegalArgumentException(
                    "Cannot copy Datasource object to " + other.getClass().getName());
        }
        Datasource datasource = (Datasource) other;
        if (!datasource.spi.dsName.equals(spi.dsName)) {
            throw new IllegalArgumentException("Incompatible datasource names");
        }
        if (!datasource.spi.dsType.equals(spi.dsType)) {
            throw new IllegalArgumentException("Incompatible datasource types");
        }
        datasource.spi.setLastValue(spi.getLastValue());
        datasource.spi.setNanSeconds(spi.getNanSeconds());
        datasource.spi.setAccumValue(spi.getAccumValue());
    }

    /**
     * Returns index of this Datasource object in the RRD.
     *
     * @return Datasource index in the RRD.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public int getDsIndex() throws IOException {
        try {
            return parentDb.getDsIndex(spi.dsName);
        }
        catch (IllegalArgumentException e) {
            return -1;
        }
    }

    /**
     * Sets datasource heartbeat to a new value.
     *
     * @param heartbeat New heartbeat value
     * @throws java.io.IOException              Thrown in case of I/O error
     * @throws java.lang.IllegalArgumentException Thrown if invalid (non-positive) heartbeat value is specified.
     */
    public void setHeartbeat(long heartbeat) throws IOException {
        if (heartbeat < 1L) {
            throw new IllegalArgumentException("Invalid heartbeat specified: " + heartbeat);
        }
        spi.heartbeat = heartbeat;
    }

    /**
     * Sets datasource name to a new value
     *
     * @param newDsName New datasource name
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public void setDsName(String newDsName) throws IOException {
        if (parentDb.containsDs(newDsName)) {
            throw new IllegalArgumentException("Datasource already defined in this RRD: " + newDsName);
        }

        spi.dsName = newDsName;
    }

    /**
     * <p>Setter for the field <code>dsType</code>.</p>
     *
     * @param newDsType a {@link org.rrd4j.DsType} object.
     * @throws java.io.IOException if any.
     */
    public void setDsType(DsType newDsType) throws IOException {
        // set datasource type
        spi.dsType = newDsType;
        // reset datasource status
        spi.setLastValue(Double.NaN);
        spi.setAccumValue(0.0);
        // reset archive status
        int dsIndex = parentDb.getDsIndex(spi.dsName);
        Archive[] archives = parentDb.getArchives();
        for (Archive archive : archives) {
            archive.getArcState(dsIndex).setAccumValue(Double.NaN);
        }
    }

    /**
     * Sets minimum allowed value for this datasource. If <code>filterArchivedValues</code>
     * argument is set to true, all archived values less then <code>minValue</code> will
     * be fixed to NaN.
     *
     * @param minValue             New minimal value. Specify <code>Double.NaN</code> if no minimal
     *                             value should be set
     * @param filterArchivedValues true, if archived datasource values should be fixed;
     *                             false, otherwise.
     * @throws java.io.IOException              Thrown in case of I/O error
     * @throws java.lang.IllegalArgumentException Thrown if invalid minValue was supplied (not less then maxValue)
     */
    public void setMinValue(double minValue, boolean filterArchivedValues) throws IOException {
        double maxValue = spi.maxValue;
        if (!Double.isNaN(minValue) && !Double.isNaN(maxValue) && minValue >= maxValue) {
            throw new IllegalArgumentException("Invalid min/max values: " + minValue + "/" + maxValue);
        }

       spi.minValue = minValue;
       if (!Double.isNaN(spi.minValue) && filterArchivedValues) {
            int dsIndex = getDsIndex();
            Archive[] archives = parentDb.getArchives();
            for (Archive archive : archives) {
                archive.getRobin(dsIndex).filterValues(minValue, Double.NaN);
            }
        }
    }

    /**
     * Sets maximum allowed value for this datasource. If <code>filterArchivedValues</code>
     * argument is set to true, all archived values greater then <code>maxValue</code> will
     * be fixed to NaN.
     *
     * @param maxValue             New maximal value. Specify <code>Double.NaN</code> if no max
     *                             value should be set.
     * @param filterArchivedValues true, if archived datasource values should be fixed;
     *                             false, otherwise.
     * @throws java.io.IOException              Thrown in case of I/O error
     * @throws java.lang.IllegalArgumentException Thrown if invalid maxValue was supplied (not greater then minValue)
     */
    public void setMaxValue(double maxValue, boolean filterArchivedValues) throws IOException {
        if (!Double.isNaN(spi.minValue) && !Double.isNaN(maxValue) && spi.minValue >= maxValue) {
            throw new IllegalArgumentException("Invalid min/max values: " + spi.minValue + "/" + maxValue);
        }

        spi.maxValue = maxValue;
        if (!Double.isNaN(maxValue) && filterArchivedValues) {
            int dsIndex = getDsIndex();
            Archive[] archives = parentDb.getArchives();
            for (Archive archive : archives) {
                archive.getRobin(dsIndex).filterValues(Double.NaN, maxValue);
            }
        }
    }

    /**
     * Sets min/max values allowed for this datasource. If <code>filterArchivedValues</code>
     * argument is set to true, all archived values less then <code>minValue</code> or
     * greater then <code>maxValue</code> will be fixed to NaN.
     *
     * @param minValue             New minimal value. Specify <code>Double.NaN</code> if no min
     *                             value should be set.
     * @param maxValue             New maximal value. Specify <code>Double.NaN</code> if no max
     *                             value should be set.
     * @param filterArchivedValues true, if archived datasource values should be fixed;
     *                             false, otherwise.
     * @throws java.io.IOException              Thrown in case of I/O error
     * @throws java.lang.IllegalArgumentException Thrown if invalid min/max values were supplied
     */
    public void setMinMaxValue(double minValue, double maxValue, boolean filterArchivedValues) throws IOException {
        if (!Double.isNaN(minValue) && !Double.isNaN(maxValue) && minValue >= maxValue) {
            throw new IllegalArgumentException("Invalid min/max values: " + minValue + "/" + maxValue);
        }
        spi.minValue = minValue;
        spi.maxValue = maxValue;
        if (!(Double.isNaN(minValue) && Double.isNaN(maxValue)) && filterArchivedValues) {
            int dsIndex = getDsIndex();
            Archive[] archives = parentDb.getArchives();
            for (Archive archive : archives) {
                archive.getRobin(dsIndex).filterValues(minValue, maxValue);
            }
        }
    }

    /**
     * Returns the underlying storage (backend) object which actually performs all
     * I/O operations.
     *
     * @return I/O backend object
     */
    public RrdBackend getRrdBackend() {
        return parentDb.getRrdBackend();
    }

}

