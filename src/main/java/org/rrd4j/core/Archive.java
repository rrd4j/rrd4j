package org.rrd4j.core;

import java.io.IOException;

import org.rrd4j.ConsolFun;

/**
 * Class to represent single RRD archive in a RRD with its internal state.
 * Normally, you don't need methods to manipulate archive objects directly
 * because Rrd4j framework does it automatically for you.<p>
 * <p/>
 * Each archive object consists of three parts: archive definition, archive state objects
 * (one state object for each datasource) and round robin archives (one round robin for
 * each datasource). API (read-only) is provided to access each of these parts.<p>
 *
 * @author Sasa Markovic
 */
public class Archive implements RrdUpdater {
    private final RrdDb parentDb;

    // state
    private ArcState[] states;
    
    //SPI
    private org.rrd4j.backend.spi.Archive spi;

    Archive(RrdDb parentDb, ArcDef arcDef) throws IOException {
        this.parentDb = parentDb;
        spi = parentDb.getRrdBackend().getArchive();

        boolean shouldInitialize = arcDef != null;
        if (shouldInitialize) {
            spi.consolFun = arcDef.getConsolFun();
            spi.setXff(arcDef.getXff());
            spi.steps = arcDef.getSteps();
            spi.rows = arcDef.getRows();
            
            int n = parentDb.getHeader().getDsCount();
            states = new ArcState[n];
            for (int i = 0; i < n; i++) {
                states[i] = new ArcState(this, shouldInitialize);
            }
        }
    }

    // read from XML
    Archive(RrdDb parentDb, DataImporter reader, int arcIndex) throws IOException {
        this(parentDb, new ArcDef(
                reader.getConsolFun(arcIndex), reader.getXff(arcIndex),
                reader.getSteps(arcIndex), reader.getRows(arcIndex)));
        int n = parentDb.getHeader().getDsCount();
        for (int i = 0; i < n; i++) {
            // restore state
            states[i].setAccumValue(reader.getStateAccumValue(arcIndex, i));
            states[i].setNanSteps(reader.getStateNanSteps(arcIndex, i));
            // restore robins
            double[] values = reader.getValues(arcIndex, i);
            spi.robin(i).update(values);
        }
    }

    /**
     * Returns archive time step in seconds. Archive step is equal to RRD step
     * multiplied with the number of archive steps.
     *
     * @return Archive time step in seconds
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    public long getArcStep() throws IOException {
        return parentDb.getHeader().getStep() * spi.steps;
    }

    String dump() throws IOException {
        StringBuilder sb = new StringBuilder("== ARCHIVE ==\n");
        sb.append("RRA:").append(spi.consolFun).append(":").append(spi.getXff()).append(":").append(spi.steps).append(":").append(spi.rows).append("\n");
        sb.append("interval [").append(getStartTime()).append(", ").append(getEndTime()).append("]" + "\n");
        for (int i = 0; i < states.length; i++) {
            sb.append(states[i].dump());
            sb.append(spi.robin(i).dump());
        }
        return sb.toString();
    }

    RrdDb getParentDb() {
        return parentDb;
    }

    void archive(int dsIndex, double value, long numUpdates) throws IOException {
        Robin robin = spi.robin(dsIndex);
        ArcState state = states[dsIndex];
        long step = parentDb.getHeader().getStep();
        long lastUpdateTime = parentDb.getHeader().getLastUpdateTime();
        long updateTime = Util.normalize(lastUpdateTime, step) + step;
        long arcStep = getArcStep();
        // finish current step
        while (numUpdates > 0) {
            accumulate(state, value);
            numUpdates--;
            if (updateTime % arcStep == 0) {
                finalizeStep(state, robin);
                break;
            } else {
                updateTime += step;
            }
        }
        // update robin in bulk
        int bulkUpdateCount = (int) Math.min(numUpdates / spi.steps, (long) spi.rows);
        robin.bulkStore(value, bulkUpdateCount);
        // update remaining steps
        long remainingUpdates = numUpdates % spi.steps;
        for (long i = 0; i < remainingUpdates; i++) {
            accumulate(state, value);
        }
    }

    private void accumulate(ArcState state, double value) throws IOException {
        if (Double.isNaN(value)) {
            state.setNanSteps(state.getNanSteps() + 1);
        } else {
            switch (spi.consolFun) {
                case MIN:
                    state.setAccumValue(Util.min(state.getAccumValue(), value));
                    break;
                case MAX:
                    state.setAccumValue(Util.max(state.getAccumValue(), value));
                    break;
                case FIRST:
                    if (Double.isNaN(state.getAccumValue())) {
                        state.setAccumValue(value);
                    }
                    break;
                case LAST:
                    state.setAccumValue(value);
                    break;
                case AVERAGE:
                case TOTAL:
                    state.setAccumValue(Util.sum(state.getAccumValue(), value));
                    break;
            }
        }
    }

    private void finalizeStep(ArcState state, Robin robin) throws IOException {
        // should store
        long arcSteps = spi.steps;
        long nanSteps = state.getNanSteps();
        //double nanPct = (double) nanSteps / (double) arcSteps;
        double accumValue = state.getAccumValue();
        if (nanSteps <= spi.getXff() * arcSteps && !Double.isNaN(accumValue)) {
            if (getConsolFun() == ConsolFun.AVERAGE) {
                accumValue /= (arcSteps - nanSteps);
            }
            robin.store(accumValue);
        } else {
            robin.store(Double.NaN);
        }
        state.setAccumValue(Double.NaN);
        state.setNanSteps(0);
    }

    /**
     * Returns archive consolidation function ("AVERAGE", "MIN", "MAX", "FIRST", "LAST" or "TOTAL").
     *
     * @return Archive consolidation function.
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    public ConsolFun getConsolFun() throws IOException {
        return spi.consolFun;
    }

    /**
     * Returns archive X-files factor.
     *
     * @return Archive X-files factor (between 0 and 1).
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    public double getXff() throws IOException {
        return spi.getXff();
    }

    /**
     * Returns the number of archive steps.
     *
     * @return Number of archive steps.
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    public int getSteps() throws IOException {
        return spi.steps;
    }

    /**
     * Returns the number of archive rows.
     *
     * @return Number of archive rows.
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    public int getRows() throws IOException {
        return spi.rows;
    }

    /**
     * Returns current starting timestamp. This value is not constant.
     *
     * @return Timestamp corresponding to the first archive row
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    public long getStartTime() throws IOException {
        long endTime = getEndTime();
        long arcStep = getArcStep();
        return endTime - (spi.rows - 1) * arcStep;
    }

    /**
     * Returns current ending timestamp. This value is not constant.
     *
     * @return Timestamp corresponding to the last archive row
     * @throws java.io.IOException Thrown in case of I/O error.
     */
    public long getEndTime() throws IOException {
        long arcStep = getArcStep();
        long lastUpdateTime = parentDb.getHeader().getLastUpdateTime();
        return Util.normalize(lastUpdateTime, arcStep);
    }

    /**
     * Returns the underlying archive state object. Each datasource has its
     * corresponding ArcState object (archive states are managed independently
     * for each RRD datasource).
     *
     * @param dsIndex Datasource index
     * @return Underlying archive state object
     */
    public ArcState getArcState(int dsIndex) {
        return states[dsIndex];
    }

    /**
     * Returns the underlying round robin archive. Robins are used to store actual
     * archive values on a per-datasource basis.
     *
     * @param dsIndex Index of the datasource in the RRD.
     * @return Underlying round robin archive for the given datasource.
     */
    public Robin getRobin(int dsIndex) {
        return spi.robin(dsIndex);
    }

    FetchData fetchData(FetchRequest request) throws IOException {
        long arcStep = getArcStep();
        long fetchStart = Util.normalize(request.getFetchStart(), arcStep);
        long fetchEnd = Util.normalize(request.getFetchEnd(), arcStep);
        if (fetchEnd < request.getFetchEnd()) {
            fetchEnd += arcStep;
        }
        long startTime = getStartTime();
        long endTime = getEndTime();
        String[] dsToFetch = request.getFilter();
        if (dsToFetch == null) {
            dsToFetch = parentDb.getDsNames();
        }
        int dsCount = dsToFetch.length;
        int ptsCount = (int) ((fetchEnd - fetchStart) / arcStep + 1);
        long[] timestamps = new long[ptsCount];
        double[][] values = new double[dsCount][ptsCount];
        long matchStartTime = Math.max(fetchStart, startTime);
        long matchEndTime = Math.min(fetchEnd, endTime);
        double[][] robinValues = null;
        if (matchStartTime <= matchEndTime) {
            // preload robin values
            int matchCount = (int) ((matchEndTime - matchStartTime) / arcStep + 1);
            int matchStartIndex = (int) ((matchStartTime - startTime) / arcStep);
            robinValues = new double[dsCount][];
            for (int i = 0; i < dsCount; i++) {
                int dsIndex = parentDb.getDsIndex(dsToFetch[i]);
                robinValues[i] = spi.robin(dsIndex).getValues(matchStartIndex, matchCount);
            }
        }
        for (int ptIndex = 0; ptIndex < ptsCount; ptIndex++) {
            long time = fetchStart + ptIndex * arcStep;
            timestamps[ptIndex] = time;
            for (int i = 0; i < dsCount; i++) {
                double value = Double.NaN;
                if (time >= matchStartTime && time <= matchEndTime) {
                    // inbound time
                    int robinValueIndex = (int) ((time - matchStartTime) / arcStep);
                    assert robinValues != null;
                    value = robinValues[i][robinValueIndex];
                }
                values[i][ptIndex] = value;
            }
        }
        FetchData fetchData = new FetchData(this, request);
        fetchData.setTimestamps(timestamps);
        fetchData.setValues(values);
        return fetchData;
    }

    void appendXml(XmlWriter writer) throws IOException {
        writer.startTag("rra");
        writer.writeTag("cf", spi.consolFun);
        writer.writeComment(getArcStep() + " seconds");
        writer.writeTag("pdp_per_row", spi.steps);
        writer.startTag("params");
        writer.writeTag("xff", spi.getXff());
        writer.closeTag(); // params
        writer.startTag("cdp_prep");
        for (ArcState state : states) {
            state.appendXml(writer);
        }
        writer.closeTag(); // cdp_prep
        writer.startTag("database");
        long startTime = getStartTime();
        for (int i = 0; i < spi.rows; i++) {
            long time = startTime + i * getArcStep();
            writer.writeComment(Util.getDate(time) + " / " + time);
            writer.startTag("row");
            for (Robin robin : spi.robins()) {
                writer.writeTag("v", robin.getValue(i));
            }
            writer.closeTag(); // row
        }
        writer.closeTag(); // database
        writer.closeTag(); // rra
    }

    /**
     * {@inheritDoc}
     *
     * Copies object's internal state to another Archive object.
     */
    public void copyStateTo(RrdUpdater other) throws IOException {
        if (!(other instanceof Archive)) {
            throw new IllegalArgumentException(
                    "Cannot copy Archive object to " + other.getClass().getName());
        }
        Archive arc = (Archive) other;
        if (!arc.spi.consolFun.equals(spi.consolFun)) {
            throw new IllegalArgumentException("Incompatible consolidation functions");
        }
        if (arc.spi.steps != spi.steps) {
            throw new IllegalArgumentException("Incompatible number of steps");
        }
        int count = parentDb.getHeader().getDsCount();
        for (int i = 0; i < count; i++) {
            int j = Util.getMatchingDatasourceIndex(parentDb, i, arc.parentDb);
            if (j >= 0) {
                states[i].copyStateTo(arc.states[j]);
                spi.robin(i).copyStateTo(arc.spi.robin(j));
            }
        }
    }

    /**
     * Sets X-files factor to a new value.
     *
     * @param xff New X-files factor value. Must be >= 0 and < 1.
     * @throws java.io.IOException Thrown in case of I/O error
     */
    public void setXff(double xff) throws IOException {
        if (xff < 0D || xff >= 1D) {
            throw new IllegalArgumentException("Invalid xff supplied (" + xff + "), must be >= 0 and < 1");
        }
        spi.setXff(xff);
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
