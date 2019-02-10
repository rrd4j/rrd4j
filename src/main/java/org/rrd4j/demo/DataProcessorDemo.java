package org.rrd4j.demo;

import java.io.IOException;

import org.rrd4j.ConsolFun;
import org.rrd4j.core.Util;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.Variable;

public class DataProcessorDemo {

    /**
     * Cute little demo. Uses demo.rrd file previously created by basic Rrd4j demo.
     *
     * @param args Not used
     * @throws java.io.IOException if any.
     */
    public static void main(String[] args) throws IOException {
        // time span
        long t1 = Demo.START;
        long t2 = Demo.END;
        System.out.println("t1 = " + t1);
        System.out.println("t2 = " + t2);

        // RRD file to use
        String rrdPath = Util.getRrd4jDemoPath("demo.rrd");

        // constructor
        DataProcessor dp = new DataProcessor(t1, t2);

        // uncomment and run again
        //dp.setFetchRequestResolution(86400);

        // uncomment and run again
        //dp.setStep(86500);

        // datasource definitions
        dp.addDatasource("X", rrdPath, "sun", ConsolFun.AVERAGE);
        dp.addDatasource("Y", rrdPath, "shade", ConsolFun.AVERAGE);
        dp.addDatasource("Z", "X,Y,+,2,/");
        dp.addDatasource("DERIVE[Z]", "Z,PREV(Z),-,STEP,/");
        dp.addDatasource("TREND[Z]", "DERIVE[Z],SIGN");
        dp.addDatasource("AVG[Z]", "Z", new Variable.AVERAGE());
        dp.addDatasource("DELTA", "Z,AVG[Z],-");

        // action
        long laptime = System.currentTimeMillis();
        //dp.setStep(86400);
        dp.processData();
        System.out.println("Data processed in " + (System.currentTimeMillis() - laptime) + " milliseconds\n---");
        System.out.println(dp.dump());

        // aggregates
        System.out.println("\nAggregates for X");
        double xAverage = dp.getVariable("X", new Variable.AVERAGE()).value;
        double xFirst = dp.getVariable("X", new Variable.FIRST()).value;
        double xLast = dp.getVariable("X", new Variable.LAST()).value;
        double xMax = dp.getVariable("X", new Variable.MAX()).value;
        double xMin = dp.getVariable("X", new Variable.MIN()).value;
        double xTotal = dp.getVariable("X", new Variable.TOTAL()).value;
        System.out.println(dumpValues(xAverage, xFirst, xLast, xMax, xMin, xTotal));
        System.out.println("\nAggregates for Y");
        double yAverage = dp.getVariable("Y", new Variable.AVERAGE()).value;
        double yFirst = dp.getVariable("Y", new Variable.FIRST()).value;
        double yLast = dp.getVariable("Y", new Variable.LAST()).value;
        double yMax = dp.getVariable("Y", new Variable.MAX()).value;
        double yMin = dp.getVariable("Y", new Variable.MIN()).value;
        double yTotal = dp.getVariable("Y", new Variable.TOTAL()).value;
        System.out.println(dumpValues(yAverage, yFirst, yLast, yMax, yMin, yTotal));

        // 95-percentile

        System.out.println("\n95-percentile for X: " + Util.formatDouble(dp.getVariable("X", new Variable.PERCENTILE(95)).value));
        System.out.println("95-percentile for Y: " + Util.formatDouble(dp.getVariable("Y", new Variable.PERCENTILE(95)).value));

        // lastArchiveUpdateTime
        System.out.println("\nLast archive update time was: " + dp.getLastRrdArchiveUpdateTime());
    }

    private static String dumpValues(double average, double first, double last, double max, double min, double total) {
        StringBuilder bl = new StringBuilder();
        bl.append(ConsolFun.AVERAGE.name() + '=' + Util.formatDouble(average));
        bl.append(", " + ConsolFun.FIRST.name() + '=' + Util.formatDouble(first));
        bl.append(", " + ConsolFun.LAST.name() + '=' + Util.formatDouble(last));
        bl.append(", " + ConsolFun.MAX.name() + '=' + Util.formatDouble(max));
        bl.append(", " + ConsolFun.MIN.name() + '=' + Util.formatDouble(min));
        bl.append(", " + ConsolFun.TOTAL.name() + '=' + Util.formatDouble(total));
        return bl.toString();
    }

}
