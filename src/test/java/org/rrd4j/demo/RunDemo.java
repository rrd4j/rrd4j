package org.rrd4j.demo;

import java.io.IOException;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;

public class RunDemo {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void runDemo() throws IOException {
        System.setProperty("rrd4j.demopath", testFolder.getRoot().getAbsolutePath());
        Demo.main(new String[] {});
        DataProcessorDemo.main(new String[] {});
        MinMax.main(new String[] {});
        RrdTutorial.main(new String[] {});
        HeartbeatFix.main(new String[] {testFolder.getRoot().getAbsolutePath(), "700"});
    }
}
