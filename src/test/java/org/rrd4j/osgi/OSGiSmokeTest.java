package org.rrd4j.osgi;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.ConsolFun.MAX;

import java.io.IOException;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdRandomAccessFileBackendFactory;
import org.rrd4j.core.Sample;

/**
 * This test validates that the rrd4j bundle resolves in a minimal OSGi container
 * 
 * <p>The bundle is picked up from the <tt>target/classes</tt> folder, so the classes <i>and</i>
 * the MANIFEST.MF file should be present there. This is usually the case when using Eclipse with
 * m2e, but in some scenarios there might be a need to run <tt>mvn compile</tt>.</p>
 * 
 * <p>This is intentionally not a full-fledged test of rrd4j, it just ensures that the bundle
 * works in an OSGi container.</p>
 *
 */
@RunWith(PaxExam.class)
public class OSGiSmokeTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Configuration
    public Option[] config() {
        return options(bundle("reference:file:target/classes"), junitBundles());
    }

    @Test
    public void basicUsage() throws IOException {
        RrdDef rrdDef = new RrdDef(temp.newFile().getAbsolutePath(), 300);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600); // 1 step, 600 rows
        rrdDef.addArchive(AVERAGE, 0.5, 6, 700); // 6 steps, 700 rows
        rrdDef.addDatasource("inbytes", DsType.GAUGE, 600, Double.NaN, Double.NaN);
        rrdDef.addArchive(MAX, 0.5, 1, 600);

        Random rnd = new Random();

        // due to sun.misc usually not being exported, default to the FILE backend
        try (RrdDb rrdDb = RrdDb.getBuilder().setBackendFactory(new RrdRandomAccessFileBackendFactory()).setRrdDef(rrdDef).build()
        ) {
            long time = System.currentTimeMillis() / 1000;
            Sample sample = rrdDb.createSample();
            for ( int i = 0 ; i < 10; i++ ) {
                sample.setTime(time + i * 1000);
                sample.setValue("inbytes", rnd.nextDouble());
                sample.update();
            }
        }
    }

}
