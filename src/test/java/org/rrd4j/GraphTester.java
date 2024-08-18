package org.rrd4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.graph.RrdGraphDef;

public class GraphTester {

    @BeforeClass
    public static void prepare() {
        System.getProperties().setProperty("java.awt.headless", "true");
    }

    protected Path saveGraph(RrdGraphDef gDef, TemporaryFolder tf, String testClass, String testName, String format) throws IOException {
        Path destinationDirectory = resolveTestsPath(tf, testClass);
        Path destinationPath = destinationDirectory.resolve(String.format("%s.%s", testName, format));
        gDef.setImageFormat(format);
        gDef.setFilename(destinationPath.toString());
        return destinationPath;
    }

    protected Path resolveTestsPath(TemporaryFolder tf, String testClass) throws IOException {
        String testsPathProperty = System.getProperty("rrd4j.graphTestsPath");
        Path testsPath = Optional.of(testsPathProperty == null ? tf.getRoot().toPath() : Paths.get(testsPathProperty))
                                 .map(p -> p.resolve(testClass))
                                 .get();
        if (!Files.exists(testsPath)) {
            Files.createDirectory(testsPath);
        } else if (!Files.isDirectory(testsPath)) {
            throw new IllegalStateException(String.format("Destination is not a directory \"%s\"", testsPath));
        }
        return testsPath;
    }

    protected void saveGraph(RrdGraphDef gDef, TemporaryFolder tf, String testClass, String testName)
            throws IOException {
        saveGraph(gDef, tf, testClass, testName, "png");
    }

}
