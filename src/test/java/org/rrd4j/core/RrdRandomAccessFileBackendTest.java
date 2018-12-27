package org.rrd4j.core;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RrdRandomAccessFileBackendTest extends BackendTester {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testBackendFactory() throws IOException {
        File rrdfile = testFolder.newFile("testfile");
        try(RrdRandomAccessFileBackendFactory factory = new RrdRandomAccessFileBackendFactory()) {
            super.testBackendFactory(factory,rrdfile.getCanonicalPath());
        }
    }

    @Test
    public void testRead1() throws IOException {
        super.testRead1(new RrdRandomAccessFileBackendFactory());
    }

    @Test
    public void testRead2() throws IOException {
        super.testRead2(new RrdRandomAccessFileBackendFactory());
    }

    @Test(expected=InvalidRrdException.class)
    public void testReadCorruptSignature() throws Exception {
        super.testReadCorruptSignature(new RrdRandomAccessFileBackendFactory());
    }

    @Test(expected=InvalidRrdException.class)
    public void testReadEmpty() throws Exception {
        super.testReadEmpty(new RrdRandomAccessFileBackendFactory());
    }

}
