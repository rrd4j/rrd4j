package org.rrd4j.core;

import org.rrd4j.core.jrrd.RRDatabase;
import org.rrd4j.ConsolFun;

import java.io.IOException;

class RrdToolReader extends DataImporter {
    private RRDatabase rrd;

    RrdToolReader(String rrdPath) throws IOException {
        rrd = new RRDatabase(rrdPath);
    }

    String getVersion() {
        return rrd.getHeader().getVersion();
    }

    long getLastUpdateTime() {
        return Util.getTimestamp(rrd.getLastUpdate());
    }

    long getStep() {
        return rrd.getHeader().getPDPStep();
    }

    int getDsCount() {
        return rrd.getHeader().getDSCount();
    }

    int getArcCount() throws IOException {
        return rrd.getNumArchives();
    }

    String getDsName(int dsIndex) {
        return rrd.getDataSource(dsIndex).getName();
    }

    String getDsType(int dsIndex) {
        return rrd.getDataSource(dsIndex).getType().toString();
    }

    long getHeartbeat(int dsIndex) {
        return rrd.getDataSource(dsIndex).getMinimumHeartbeat();
    }

    double getMinValue(int dsIndex) {
        return rrd.getDataSource(dsIndex).getMinimum();
    }

    double getMaxValue(int dsIndex) {
        return rrd.getDataSource(dsIndex).getMaximum();
    }

    double getLastValue(int dsIndex) {
        String valueStr = rrd.getDataSource(dsIndex).getPDPStatusBlock().getLastReading();
        return Util.parseDouble(valueStr);
    }

    double getAccumValue(int dsIndex) {
        return rrd.getDataSource(dsIndex).getPDPStatusBlock().getValue();
    }

    long getNanSeconds(int dsIndex) {
        return rrd.getDataSource(dsIndex).getPDPStatusBlock().getUnknownSeconds();
    }

    ConsolFun getConsolFun(int arcIndex) {
        return ConsolFun.valueOf(rrd.getArchive(arcIndex).getType().toString());
    }

    double getXff(int arcIndex) {
        return rrd.getArchive(arcIndex).getXff();
    }

    int getSteps(int arcIndex) {
        return rrd.getArchive(arcIndex).getPdpCount();
    }

    int getRows(int arcIndex) throws IOException {
        return rrd.getArchive(arcIndex).getRowCount();
    }

    double getStateAccumValue(int arcIndex, int dsIndex) throws IOException {
        return rrd.getArchive(arcIndex).getCDPStatusBlock(dsIndex).getValue();
    }

    int getStateNanSteps(int arcIndex, int dsIndex) throws IOException {
        return rrd.getArchive(arcIndex).getCDPStatusBlock(dsIndex).getUnknownDatapoints();
    }

    double[] getValues(int arcIndex, int dsIndex) throws IOException {
        return rrd.getArchive(arcIndex).getValues()[dsIndex];
    }

    void release() throws IOException {
        if (rrd != null) {
            rrd.close();
            rrd = null;
        }
    }

    protected void finalize() throws Throwable {
        release();
    }
}
