package org.rrd4j.demo;

import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdToolkit;

import java.io.File;
import java.io.IOException;

/**
 * <p>HeartbeatFix class.</p>
 */
public class HeartbeatFix {
    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     * @throws java.io.IOException if any.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("SYNTAX: HeartbeatFix <rrd directory> <heartbeat>");
            System.exit(-1);
        }
        File directory = new File(args[0]);
        long heartbeat = Long.parseLong(args[1]);
        File[] files = directory.listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".rrd"));
        System.out.println(files.length + " files found");
        for (int i = 0; i < files.length; i++) {
            String path = files[i].getAbsolutePath();
            System.out.print((i + 1) + ": " + path + ": ");
            // fix heartbeat
            RrdToolkit.setDsHeartbeat(files[i].getAbsolutePath(), 0, heartbeat);
            System.out.print("fixed");
            // check consistency of the file
            try (RrdDb rrd = RrdDb.getBuilder().setPath(path).build()) {
                if (rrd.getRrdDef().getEstimatedSize() == files[i].length() &&
                        rrd.getDatasource(0).getHeartbeat() == heartbeat) {
                    System.out.println(", verified");
                }
                else {
                    System.out.println(", ********** ERROR **********");
                }
            }
        }
        System.out.println("FINISHED!");
    }
}
