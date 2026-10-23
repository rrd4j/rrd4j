package org.rrd4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RrdDefStepTest {

    @Parameterized.Parameters(name = "step={0}, valid={1}")
    public static Collection<Object[]> steps() {
        return Arrays.asList(new Object[][] {
                {0L, false}, {-1L, false}, {Long.MIN_VALUE, false},
                {1L, true}, {Long.MAX_VALUE, true}
        });
    }

    private final long step;
    private final boolean valid;

    public RrdDefStepTest(long step, boolean valid) {
        this.step = step;
        this.valid = valid;
    }

    @Test
    public void testSetterStepValidation() {
        RrdDef definition = new RrdDef("test", 300);
        if (valid) {
            definition.setStep(step);
            assertEquals(step, definition.getStep());
        } else {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> definition.setStep(step));
            assertEquals("Invalid RRD step specified: " + step, exception.getMessage());
            assertEquals(300, definition.getStep());
        }
    }

    @Test
    public void testConstructorStepValidation() {
        URI uri = URI.create("memory:/test");
        if (valid) {
            assertEquals(step, new RrdDef("test", step).getStep());
            assertEquals(step, new RrdDef(uri, step).getStep());
        } else {
            assertThrows(IllegalArgumentException.class, () -> new RrdDef("test", step));
            assertThrows(IllegalArgumentException.class, () -> new RrdDef(uri, step));
        }
    }
}
