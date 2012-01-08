package org.rrd4j.converter;

import org.rrd4j.core.RrdDb;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Date;

/**
 * Simple utility class to convert RRD files created with RRDTool 1.0.x to
 * Rrd4j's native RRD format. Conversion process is quite fast.
 */
public class Converter {
    private static final String FACTORY_NAME = "FILE";
    private static final String SUFFIX = ".jrb";
    private static final DecimalFormat secondsFormatter = new DecimalFormat("##0.000");
    private static final DecimalFormat countFormatter = new DecimalFormat("0000");

    private String[] files;
    private int totalCount, badCount, goodCount;

    private Converter(String[] files) {
        try {
            RrdDb.setDefaultFactory(FACTORY_NAME);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        this.files = files;
    }

    private void convertAll() {
        Date t1 = new Date();
        final String ruler = "=======================================================================";
        println(ruler);
        println("Converting RRDTool files to Rrd4j native format.");
        println("Original RRDTool files will not be modified in any way");
        println("RRD4J files created during the process will have a " + SUFFIX + " suffix");
        println(ruler);
        for (String file : files) {
            convertFile(file);
        }
        println(ruler);
        println("Finished: " + totalCount + " total, " +
                goodCount + " OK, " + badCount + " failed");
        Date t2 = new Date();
        double secs = (t2.getTime() - t1.getTime()) / 1000.0;
        println("Conversion took " + secondsFormatter.format(secs) + " sec");
        if (totalCount > 0) {
            double avgSec = secs / totalCount;
            println("Average per-file conversion time: " + secondsFormatter.format(avgSec) + " sec");
        }
    }

    private void convertFile(String path) {
        long start = System.currentTimeMillis();
        totalCount++;
        try {
            File rrdFile = new File(path);
            print(countFormatter.format(totalCount) + "/" + countFormatter.format(files.length) +
                    " " + rrdFile.getName() + " ");
            String sourcePath = rrdFile.getCanonicalPath();
            String destPath = sourcePath + SUFFIX;
            RrdDb rrd = new RrdDb(destPath, RrdDb.PREFIX_RRDTool + sourcePath);
            rrd.close();
            goodCount++;
            double seconds = (System.currentTimeMillis() - start) / 1000.0;
            println("[OK, " + secondsFormatter.format(seconds) + " sec]");
        }
        catch (Exception e) {
            badCount++;
            println("[" + e + "]");
        }
    }

    private static void println(String msg) {
        System.out.println(msg);
    }

    private static void print(String msg) {
        System.out.print(msg);
    }

    /**
     * <p>To convert RRD files created with RRDTool use the following syntax:</p>
     * <pre>
     * java -cp rrd4j-{version} org.rrd4j.convertor.Convert [path to RRD file(s)]
     * </pre>
     * <p>For example:</p>
     * <pre>
     * java -cp rrd4j-{version} org.rrd4j.convertor.Convert rrdtool/files/*.rrd
     * </pre>
     * <p>...and enjoy the show.</p>
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            println("Usage  : java -jar converter.jar <RRD file pattern> ...");
            println("Example: java -jar converter.jar files/*.rrd");
            System.exit(1);
        }
        Converter c = new Converter(args);
		c.convertAll();
	}
}
