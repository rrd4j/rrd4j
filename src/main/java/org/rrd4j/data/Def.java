package org.rrd4j.data;

import java.io.IOException;
import java.net.URI;

import org.rrd4j.ConsolFun;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;

class Def extends Source {
    private final String path;
    private final String dsName;
    private final RrdBackendFactory backend;
    private final ConsolFun consolFun;
    private FetchData fetchData;

    Def(String name, FetchData fetchData) {
        this(name, name, fetchData);
    }

    Def(String name, String dsName, FetchData fetchData) {
        this(name,
                fetchData.getRequest().getParentDb().getPath(),
                dsName, fetchData.getRequest().getConsolFun(),
                fetchData.getRequest().getParentDb().getRrdBackend().getFactory()
                );
        this.fetchData = fetchData;
    }

    Def(String name, String path, String dsName, ConsolFun consolFunc) {
        this(name, path, dsName, consolFunc, RrdBackendFactory.getDefaultFactory());
    }

    Def(String name, String path, String dsName, ConsolFun consolFunc, RrdBackendFactory backend) {
        super(name);
        this.path = path;
        this.dsName = dsName;
        this.consolFun = consolFunc;
        this.backend = backend;
    }

    String getPath() {
        return path;
    }

    String getCanonicalPath() throws IOException {
        return backend.getCanonicalUri(backend.getUri(path)).toString();
    }

    URI getCanonicalUri() throws IOException {
        return backend.getCanonicalUri(backend.getUri(path));
    }

     String getDsName() {
        return dsName;
    }

    ConsolFun getConsolFun() {
        return consolFun;
    }

    RrdBackendFactory getBackend() {
        return backend;
    }

    boolean isCompatibleWith(Def def) throws IOException {
        return getCanonicalPath().equals(def.getCanonicalPath()) &&
                getConsolFun() == def.consolFun &&
                ((backend == null && def.backend == null) ||
                        (backend != null && def.backend != null && backend.equals(def.backend)));
    }

    RrdDb getRrdDb() {
        return fetchData.getRequest().getParentDb();
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

    /* (non-Javadoc)
     * @see org.rrd4j.data.Source#getAggregates(long, long)
     */
    @Override
    @Deprecated
    Aggregates getAggregates(long tStart, long tEnd) {
        long[] t = getRrdTimestamps();
        double[] v = getRrdValues();
        return new Aggregator(t, v).getAggregates(tStart, tEnd);
    }

    /* (non-Javadoc)
     * @see org.rrd4j.data.Source#getPercentile(long, long, double)
     */
    @Override
    @Deprecated
    double getPercentile(long tStart, long tEnd, double percentile) {
        long[] t = getRrdTimestamps();
        double[] v = getRrdValues();
        return new Aggregator(t, v).getPercentile(tStart, tEnd, percentile);
    }

    boolean isLoaded() {
        return fetchData != null;
    }
}
