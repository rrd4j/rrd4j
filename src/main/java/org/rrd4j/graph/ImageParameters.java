package org.rrd4j.graph;

import java.util.function.DoubleUnaryOperator;

class ImageParameters {
    long start, end;
    double minval, maxval;
    int unitsexponent;
    double base;
    double magfact;
    char symbol;
    double ygridstep;
    int ylabfact;
    double decimals;
    int quadrant;
    double scaledstep;
    int xsize;
    int ysize;
    int xorigin;
    int yorigin;
    int unitslength;
    int xgif, ygif;
    DoubleUnaryOperator log;
}
