package org.rrd4j.data;

interface NonRrdSource {
    void calculate(long tStart, long tEnd, DataProcessor dataProcessor);
}
