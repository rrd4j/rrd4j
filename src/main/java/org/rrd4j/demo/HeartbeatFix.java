package org.rrd4j.demo;

import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdToolkit;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

public class HeartbeatFix {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("SYNTAX: HeartbeatFix <rrd directory> <heartbeat>");
            System.exit(-1);
        }
        File directory = new File(args[0]);
        long heartbeat = Long.parseLong(args[1]);
        File[] files = directory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".rrd.jrb");
            }
        });
        System.out.println(files.length + " files found");
        for (int i = 0; i < files.length; i++) {
            String path = files[i].getAbsolutePath();
            System.out.print((i + 1) + ": " + path + ": ");
            // fix heartbeat
            RrdToolkit.setDsHeartbeat(files[i].getAbsolutePath(), 0, heartbeat);
            RrdToolkit.setDsHeartbeat(files[i].getAbsolutePath(), 1, heartbeat);
            System.out.print("fixed");
            // check consistency of the file
            RrdDb rrd = new RrdDb(path);
            if (rrd.getRrdDef().getEstimatedSize() == files[i].length() &&
                    rrd.getDatasource(0).getHeartbeat() == heartbeat &&
                    rrd.getDatasource(1).getHeartbeat() == heartbeat) {
                System.out.println(", verified");
            }
            else {
                System.out.println(", ********** ERROR **********");
            }
            rrd.close();
        }
        System.out.println("FINISHED!");
    }
}
