package org.rrd4j.graph;

import java.io.IOException;

import org.rrd4j.core.Util;
import org.rrd4j.data.DataProcessor;

public abstract class DummyGraph {

    protected ImageWorker imageWorker;
    protected RrdGraphDef graphDef;
    protected ImageParameters imageParameters;
    protected Mapper graphMapper;
    protected DataProcessor dataProcessor;

    protected void buildGraph() throws IOException {
        //For the test only; when creating the mock object above, if we include ".withConstructor(100,100)"
        // easymock calls the constructor but the call to resize doesn't happen properly.
        // But it's imperative to setup internal state in the ImageWorker.  So, call it here.  
        imageWorker.resize(100,100);

        //The imageParameters setup code is duplicated and simplified from that in similarly named functions
        // in RrdGraph.  It would be nice if this could be re-used somehow, but that would require
        // being able to pass in an ImageWorker to RrdGraph.  But currently the imageworker is created 
        // and used immediately in the constructor of RrdGraph.
        // And we need some existing unit tests to be able to prove that it's doing the right thing first
        // before we can refactor it so that we can write better unit tests.  Mr Chicken, meet Mr Egg.
        // So, this is about the best we can do at this stage.  
        // The called functions are named the same as in RrdGraph, but are generally simplified for the purposes of this test
        // eliminating code that we don't use
        imageParameters = new ImageParameters();
        dataProcessor = fetchData(imageParameters, graphDef);
        calculatePlotValues(graphDef, dataProcessor);
        findMinMaxValues(imageParameters, graphDef);
        identifySiUnit(imageParameters, graphDef);
        expandValueRange(imageParameters, graphDef);
        initializeLimits(imageParameters, graphDef);
        imageWorker.resize(imageParameters.xgif, imageParameters.ygif);

        graphMapper = new Mapper(graphDef, imageParameters);
    }

    protected double getSmallFontCharWidth() {
        return imageWorker.getStringWidth("a", graphDef.getFont(RrdGraphDef.FONTTAG_LEGEND));
    }

    protected double getSmallFontHeight() {
        return imageWorker.getFontHeight(graphDef.getFont(RrdGraphDef.FONTTAG_LEGEND));
    }

    protected double getLargeFontHeight() {
        return imageWorker.getFontHeight(graphDef.getFont(RrdGraphDef.FONTTAG_TITLE));
    }

    protected void initializeLimits(ImageParameters imageParameters, RrdGraphDef gdef) {
        imageParameters.xsize = gdef.width;
        imageParameters.ysize = gdef.height;
        imageParameters.unitslength = gdef.unitsLength;
        if (gdef.onlyGraph) {
            if (imageParameters.ysize > 64) {
                throw new RuntimeException("Cannot create graph only, height too big");
            }
            imageParameters.xorigin = 0;
        }
        else {
            imageParameters.xorigin = (int) (RrdGraphConstants.PADDING_LEFT + imageParameters.unitslength * getSmallFontCharWidth());
        }
        if (gdef.verticalLabel != null) {
            imageParameters.xorigin += getSmallFontHeight();
        }
        if (gdef.onlyGraph) {
            imageParameters.yorigin = imageParameters.ysize;
        }
        else {
            imageParameters.yorigin = RrdGraphConstants.PADDING_TOP + imageParameters.ysize;
        }
        if (gdef.title != null) {
            imageParameters.yorigin += getLargeFontHeight() + RrdGraphConstants.PADDING_TITLE;
        }
        if (gdef.onlyGraph) {
            imageParameters.xgif = imageParameters.xsize;
            imageParameters.ygif = imageParameters.yorigin;
        }
        else {
            imageParameters.xgif = RrdGraphConstants.PADDING_RIGHT + imageParameters.xsize + imageParameters.xorigin;
            imageParameters.ygif = imageParameters.yorigin + (int) (RrdGraphConstants.PADDING_PLOT * getSmallFontHeight());
        }
    }

    protected void expandValueRange(ImageParameters imageParameters, RrdGraphDef gdef) {
        imageParameters.ygridstep = (gdef.valueAxisSetting != null) ? gdef.valueAxisSetting.gridStep : Double.NaN;
        imageParameters.ylabfact = (gdef.valueAxisSetting != null) ? gdef.valueAxisSetting.labelFactor : 0;
        if (!gdef.rigid && !gdef.logarithmic) {
            double sensiblevalues[] = {
                    1000.0, 900.0, 800.0, 750.0, 700.0, 600.0, 500.0, 400.0, 300.0, 250.0, 200.0, 125.0, 100.0,
                    90.0, 80.0, 75.0, 70.0, 60.0, 50.0, 40.0, 30.0, 25.0, 20.0, 10.0,
                    9.0, 8.0, 7.0, 6.0, 5.0, 4.0, 3.5, 3.0, 2.5, 2.0, 1.8, 1.5, 1.2, 1.0,
                    0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0.0, -1
            };
            double scaled_min, scaled_max, adj;
            if (Double.isNaN(imageParameters.ygridstep)) {
                if (gdef.altYMrtg) { /* mrtg */
                    imageParameters.decimals = Math.ceil(Math.log10(Math.max(Math.abs(imageParameters.maxval), Math.abs(imageParameters.minval))));
                    imageParameters.quadrant = 0;
                    if (imageParameters.minval < 0) {
                        imageParameters.quadrant = 2;
                        if (imageParameters.maxval <= 0) {
                            imageParameters.quadrant = 4;
                        }
                    }
                    switch (imageParameters.quadrant) {
                    case 2:
                        imageParameters.scaledstep = Math.ceil(50 * Math.pow(10, -(imageParameters.decimals)) * Math.max(Math.abs(imageParameters.maxval),
                                Math.abs(imageParameters.minval))) * Math.pow(10, imageParameters.decimals - 2);
                        scaled_min = -2 * imageParameters.scaledstep;
                        scaled_max = 2 * imageParameters.scaledstep;
                        break;
                    case 4:
                        imageParameters.scaledstep = Math.ceil(25 * Math.pow(10,
                                -(imageParameters.decimals)) * Math.abs(imageParameters.minval)) * Math.pow(10, imageParameters.decimals - 2);
                        scaled_min = -4 * imageParameters.scaledstep;
                        scaled_max = 0;
                        break;
                    default: /* quadrant 0 */
                        imageParameters.scaledstep = Math.ceil(25 * Math.pow(10, -(imageParameters.decimals)) * imageParameters.maxval) *
                        Math.pow(10, imageParameters.decimals - 2);
                        scaled_min = 0;
                        scaled_max = 4 * imageParameters.scaledstep;
                        break;
                    }
                    imageParameters.minval = scaled_min;
                    imageParameters.maxval = scaled_max;
                }
                else if (gdef.altAutoscale) {
                    /* measure the amplitude of the function. Make sure that
                                           graph boundaries are slightly higher then max/min vals
                                           so we can see amplitude on the graph */
                    double delt, fact;

                    delt = imageParameters.maxval - imageParameters.minval;
                    adj = delt * 0.1;
                    fact = 2.0 * Math.pow(10.0,
                            Math.floor(Math.log10(Math.max(Math.abs(imageParameters.minval), Math.abs(imageParameters.maxval)))) - 2);
                    if (delt < fact) {
                        adj = (fact - delt) * 0.55;
                    }
                    imageParameters.minval -= adj;
                    imageParameters.maxval += adj;
                }
                else if (gdef.altAutoscaleMax) {
                    /* measure the amplitude of the function. Make sure that
                                           graph boundaries are slightly higher than max vals
                                           so we can see amplitude on the graph */
                    adj = (imageParameters.maxval - imageParameters.minval) * 0.1;
                    imageParameters.maxval += adj;
                }
                else {
                    scaled_min = imageParameters.minval / imageParameters.magfact;
                    scaled_max = imageParameters.maxval / imageParameters.magfact;
                    for (int i = 1; sensiblevalues[i] > 0; i++) {
                        if (sensiblevalues[i - 1] >= scaled_min && sensiblevalues[i] <= scaled_min) {
                            imageParameters.minval = sensiblevalues[i] * imageParameters.magfact;
                        }
                        if (-sensiblevalues[i - 1] <= scaled_min && -sensiblevalues[i] >= scaled_min) {
                            imageParameters.minval = -sensiblevalues[i - 1] * imageParameters.magfact;
                        }
                        if (sensiblevalues[i - 1] >= scaled_max && sensiblevalues[i] <= scaled_max) {
                            imageParameters.maxval = sensiblevalues[i - 1] * imageParameters.magfact;
                        }
                        if (-sensiblevalues[i - 1] <= scaled_max && -sensiblevalues[i] >= scaled_max) {
                            imageParameters.maxval = -sensiblevalues[i] * imageParameters.magfact;
                        }
                    }
                }
            }
            else {
                imageParameters.minval = (double) imageParameters.ylabfact * imageParameters.ygridstep *
                        Math.floor(imageParameters.minval / ((double) imageParameters.ylabfact * imageParameters.ygridstep));
                imageParameters.maxval = (double) imageParameters.ylabfact * imageParameters.ygridstep *
                        Math.ceil(imageParameters.maxval / ((double) imageParameters.ylabfact * imageParameters.ygridstep));
            }

        }
    }

    protected void identifySiUnit(ImageParameters imageParameters, RrdGraphDef gdef) {
        imageParameters.unitsexponent = gdef.unitsExponent;
        imageParameters.base = gdef.base;
        if (!gdef.logarithmic) {
            final char symbol[] = {'a', 'f', 'p', 'n', 'u', 'm', ' ', 'k', 'M', 'G', 'T', 'P', 'E'};
            int symbcenter = 6;
            double digits;
            if (imageParameters.unitsexponent != Integer.MAX_VALUE) {
                digits = Math.floor(imageParameters.unitsexponent / 3.0);
            }
            else {
                digits = Math.floor(Math.log(Math.max(Math.abs(imageParameters.minval), Math.abs(imageParameters.maxval))) / Math.log(imageParameters.base));
            }
            imageParameters.magfact = Math.pow(imageParameters.base, digits);
            if (((digits + symbcenter) < symbol.length) && ((digits + symbcenter) >= 0)) {
                imageParameters.symbol = symbol[(int) digits + symbcenter];
            }
            else {
                imageParameters.symbol = '?';
            }
        }
    }

    protected void findMinMaxValues(ImageParameters imageParameters, RrdGraphDef gdef) {
        double minval = Double.NaN, maxval = Double.NaN;
        for (PlotElement pe : gdef.plotElements) {
            if (pe instanceof SourcedPlotElement) {
                minval = Util.min(((SourcedPlotElement) pe).getMinValue(), minval);
                maxval = Util.max(((SourcedPlotElement) pe).getMaxValue(), maxval);
            }
        }
        if (Double.isNaN(minval)) {
            minval = 0D;
        }
        if (Double.isNaN(maxval)) {
            maxval = 1D;
        }
        imageParameters.minval = gdef.minValue;
        imageParameters.maxval = gdef.maxValue;
        /* adjust min and max values */
        if (Double.isNaN(imageParameters.minval) || ((!gdef.logarithmic && !gdef.rigid) && imageParameters.minval > minval)) {
            imageParameters.minval = minval;
        }
        if (Double.isNaN(imageParameters.maxval) || (!gdef.rigid && imageParameters.maxval < maxval)) {
            if (gdef.logarithmic) {
                imageParameters.maxval = maxval * 1.1;
            }
            else {
                imageParameters.maxval = maxval;
            }
        }
        /* make sure min is smaller than max */
        if (imageParameters.minval > imageParameters.maxval) {
            imageParameters.minval = 0.99 * imageParameters.maxval;
        }
        /* make sure min and max are not equal */
        if (Math.abs(imageParameters.minval - imageParameters.maxval) < .0000001) {
            imageParameters.maxval *= 1.01;
            if (!gdef.logarithmic) {
                imageParameters.minval *= 0.99;
            }
            /* make sure min and max are not both zero */
            if (imageParameters.maxval == 0.0) {
                imageParameters.maxval = 1.0;
            }
        }
    }

    protected void calculatePlotValues(RrdGraphDef gdef, DataProcessor dproc) {
        for (PlotElement pe : gdef.plotElements) {
            if (pe instanceof SourcedPlotElement) {
                ((SourcedPlotElement) pe).assignValues(dproc);
            }
        }
    }

    protected DataProcessor fetchData(ImageParameters imageParameters, RrdGraphDef gdef) throws IOException {
        DataProcessor dproc = new DataProcessor(gdef.startTime, gdef.endTime);
        dproc.setPoolUsed(gdef.poolUsed);
        if (gdef.step > 0) {
            dproc.setStep(gdef.step);
        }
        for (Source src : gdef.sources) {
            src.requestData(dproc);
        }
        dproc.processData();
        imageParameters.start = gdef.startTime;
        imageParameters.end = gdef.endTime;
        return dproc;
    }

}
