package org.rrd4j.data;

class PDef extends Source {
    private final Plottable plottable;

    PDef(String name, Plottable plottable) {
        super(name);
        this.plottable = plottable;
    }

    void calculateValues() {
        long[] times = getTimestamps();
        double[] vals = new double[times.length];
        for (int i = 0; i < times.length; i++) {
            vals[i] = plottable.getValue(times[i]);
        }
        setValues(vals);
    }
}
