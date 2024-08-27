package org.rrd4j.graph;

import org.junit.Assert;
import org.junit.Test;

public class FindUnitTest {

    private void check(double base, double value, char expected) {
        int digits = (int) (Math.floor(Math.log(Math.abs(value)) / Math.log(base)));
        // Comparison done using String, otherwise error message are unreadable
        Assert.assertEquals(String.valueOf(expected), String.valueOf(FindUnit.resolveSymbol(digits)));
    }

    @Test
    public void check() {
        double[] bases = {1000, 1024};
        char[] expected = {'?', 'y', 'z', 'a', 'f', 'p', 'n', 'µ', 'm', ' ', 'k', 'M', 'G', 'T', 'P', 'E', 'Z', 'Y', '?'};
        for (double base: bases) {
            for (int i = -9 ; i <= 9; i++) {
                check(base, Math.pow(base, i), expected[i + 9]);
                check(base, Math.pow(base, i + (base - 1)/base), expected[i + 9]);
            }
        }
    }

}
