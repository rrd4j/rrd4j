package org.rrd4j.data;

interface NonRrdSource {
    public void calculate(long tStart, long tEnd, DataProcessor dataProcessor);
}
