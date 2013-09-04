package org.rrd4j.data;

import java.io.IOException;

import org.rrd4j.ConsolFun;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.RrdBackendFactory;

class Def extends Source {
    private String path, dsName, backend;
    private ConsolFun consolFun;
    private FetchData fetchData;

    Def(String name, FetchData fetchData) {
        this(name, name, fetchData);
    }

    Def(String name, String dsName, FetchData fetchData) {
        this(name, null, dsName, null, RrdBackendFactory.getDefaultFactory().getName());
        setFetchData(fetchData);
        setFetchData(fetchData);
        consolFun = fetchData.getRequest().getConsolFun();
        try {
            path = fetchData.getRequest().getParentDb().getUniqId();
            backend = fetchData.getRequest().getParentDb().getRrdBackend().getFactory().getName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Def(String name, String path, String dsName, ConsolFun consolFunc) {
        this(name, path, dsName, consolFunc, null);
    }

    Def(String name, String path, String dsName, ConsolFun consolFunc, String backend) {
        super(name);
        this.path = path;
        this.dsName = dsName;
        this.consolFun = consolFunc;
        if(backend == null)
            backend = RrdBackendFactory.getDefaultFactory().getName();
        this.backend = backend;
    }

    String getPath() {
        return path;
    }

    String getUniqId() throws IOException {
        return RrdBackendFactory.getFactory(backend).resolveUniqId(path);
    }

    String getDsName() {
        return dsName;
    }

    ConsolFun getConsolFun() {
        return consolFun;
    }

    String getBackend() {
        return backend;
    }

    boolean isCompatibleWith(Def def) throws IOException {
        return getUniqId().equals(def.getUniqId()) &&
                getConsolFun() == def.consolFun &&
                ((backend == null && def.backend == null) ||
                        (backend != null && def.backend != null && backend.equals(def.backend)));
    }

    void setFetchData(FetchData fetchData) {
        this.fetchData = fetchData;
    }

    long[] getRrdTimestamps() {
        return fetchData.getTimestamps();
    }

    double[] getRrdValues() {
        return fetchData.getValues(dsName);
    }

    long getArchiveEndTime() {
        return fetchData.getArcEndTime();
    }

    long getFetchStep() {
        return fetchData.getStep();
    }

    Aggregates getAggregates(long tStart, long tEnd) {
        long[] t = getRrdTimestamps();
        double[] v = getRrdValues();
        return new Aggregator(t, v).getAggregates(tStart, tEnd);
    }

    double getPercentile(long tStart, long tEnd, double percentile) {
        long[] t = getRrdTimestamps();
        double[] v = getRrdValues();
        return new Aggregator(t, v).getPercentile(tStart, tEnd, percentile);
    }

    boolean isLoaded() {
        return fetchData != null;
    }
}
