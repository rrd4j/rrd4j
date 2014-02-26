package org.rrd4j.data;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.core.Util;

public class RpnCalculatorTest {
    class Myplottable extends Plottable {
        double[] values;
        Myplottable(double... values) {
            this.values = values;
        }
        @Override
        public double getValue(long timestamp) {
            return values[(int) timestamp - 1];
        }
    }

    private final static long testedTimeSeconds = Util.getCalendar("2001-02-03 04:05:06").getTime().getTime() / 1000;

    private void expected(DataProcessor dp, String rpn, double... values) throws IOException {
        dp.processData();
        RpnCalculator calc = new RpnCalculator(rpn, "rpn name", dp);
        double[] rpnValues = calc.calculateValues();
        System.out.println(Arrays.toString(rpnValues));
        for(int i=0; i < values.length; i++) {
            String message = String.format("for '%s', at %d", rpn, i);
            Assert.assertEquals(message, values[i], rpnValues[i], 1e-10);
        }
    }

    @Test
    public void testLT() throws IOException {
        DataProcessor dp = new DataProcessor(1, 3);
        dp.addDatasource("source1", new Myplottable(2.0, 2.0, 4.0));
        dp.addDatasource("source2", new Myplottable(3.0, 2.0, 1.0));
        expected(dp, "source1,source2,LT", 1.0, 0.0, 0.0);
    }

    @Test
    public void testLE() throws IOException {
        DataProcessor dp = new DataProcessor(1, 3);
        dp.addDatasource("source1", new Myplottable(2.0, 2.0, 4.0));
        dp.addDatasource("source2", new Myplottable(3.0, 2.0, 1.0));
        expected(dp, "source1,source2,LE", 1.0, 1.0, 0.0);
    }

    @Test
    public void testGT() throws IOException {
        DataProcessor dp = new DataProcessor(1, 3);
        dp.addDatasource("source1", new Myplottable(2.0, 2.0, 4.0));
        dp.addDatasource("source2", new Myplottable(3.0, 2.0, 1.0));
        expected(dp, "source1,source2,GT", 0.0, 0.0, 1.0);
    }

    @Test
    public void testGE() throws IOException {
        DataProcessor dp = new DataProcessor(1, 3);
        dp.addDatasource("source1", new Myplottable(2.0, 2.0, 4.0));
        dp.addDatasource("source2", new Myplottable(3.0, 2.0, 1.0));
        expected(dp, "source1,source2,GE", 0.0, 1.0, 1.0);
    }

    @Test
    public void testEQ() throws IOException {
        DataProcessor dp = new DataProcessor(1, 3);
        dp.addDatasource("source1", new Myplottable(2.0, 2.0, 4.0));
        dp.addDatasource("source2", new Myplottable(3.0, 2.0, 1.0));
        expected(dp, "source1,source2,EQ", 0.0, 1.0, 0.0);
    }

    @Test
    public void testNE() throws IOException {
        DataProcessor dp = new DataProcessor(1, 3);
        dp.addDatasource("source1", new Myplottable(2.0, 2.0, 4.0));
        dp.addDatasource("source2", new Myplottable(3.0, 2.0, 1.0));
        expected(dp, "source1,source2,NE", 1.0, 0.0, 1.0);
    }

    @Test
    public void testUN() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        dp.addDatasource("source1", new Myplottable(Double.NaN, 2.0));
        expected(dp, "source1, UN", 1.0, 0.0);
    }

    @Test
    public void testAND1() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "1, 1, AND", 1);
    }

    @Test
    public void testAND2() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "1, 0, AND", 0);
    }

    @Test
    public void testAND3() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "0, 0, AND", 0);
    }

    @Test
    public void testOR1() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "1, 1, OR", 1);
    }

    @Test
    public void testOR2() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "1, 0, OR", 1);
    }

    @Test
    public void testOR3() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "0, 0, OR", 0);
    }

    @Test
    public void testXOR1() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "1, 1, XOR", 0);
    }

    @Test
    public void testXOR2() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "1, 0, XOR", 1);
    }

    @Test
    public void testXOR3() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "0, 0, XOR", 0);
    }

    @Test
    public void testIF() throws IOException {
        DataProcessor dp = new DataProcessor(1, 3);
        dp.addDatasource("source1", new Myplottable(0.0, 1.0, 2.0));
        dp.addDatasource("source2", new Myplottable(1.0, 1.0, 1.0));
        dp.addDatasource("source3", new Myplottable(2.0, 2.0, 2.0));
        expected(dp, "source1, source2, source3, IF", 2.0, 1.0, 1.0);
    }

    @Test
    public void testMAX() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "1, 2, MAX, 3 , MAX", 3);
    }

    @Test
    public void testMIN() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "1, 2, MIN, 3 , MIN", 1);
    }

    @Test
    public void testINF() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "INF", Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Test
    public void testUNKN() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "UNKN", Double.NaN, Double.NaN);
    }

    @Test
    public void testADD() throws IOException {
        DataProcessor dp = new DataProcessor(1, 4);
        dp.addDatasource("source1", new Myplottable(1.0, 1.0, Double.NaN, Double.NaN));
        dp.addDatasource("source2", new Myplottable(1.0, Double.NaN, 1.0, Double.NaN));
        expected(dp, "source1, source2, +", 2.0, Double.NaN, Double.NaN, Double.NaN);
    }

    @Test
    public void testADDNAN() throws IOException {
        DataProcessor dp = new DataProcessor(1, 4);
        dp.addDatasource("source1", new Myplottable(1.0, 1.0, Double.NaN, Double.NaN));
        dp.addDatasource("source2", new Myplottable(1.0, Double.NaN, 1.0, Double.NaN));
        expected(dp, "source1, source2, ADDNAN", 2.0, 1.0, 1.0, Double.NaN);
    }

    @Test
    public void testLIMIT() throws IOException {
        DataProcessor dp = new DataProcessor(1, 4);
        dp.addDatasource("source1", new Myplottable(1.0, 2.0, 3.0, 4.0));
        expected(dp, "source1, 2, 3, LIMIT", Double.NaN, 2.0, 3.0, Double.NaN);
    }

    @Test
    public void testCOUNT() throws IOException {
        DataProcessor dp = new DataProcessor(1, 4);
        expected(dp, "COUNT", 1.0, 2.0, 3.0, 4.0);
    }

    @Test
    public void testAVG() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "1,2,3,4,4,AVG", 2.5);
    }

    @Test
    public void testSORT() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "4,3,2,1,4,SORT,POP, 3, AVG", 2.0);
    }

    @Test
    public void testREV() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "4,3,2,1,4,REV,POP, 3, AVG", 2.0);
    }

    @Test
    public void testYEAR() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, testedTimeSeconds + ", YEAR", 2001);
    }

    @Test
    public void testMONTH() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, testedTimeSeconds + ", MONTH", 2);
    }

    @Test
    public void testDATE() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, testedTimeSeconds + ", DATE", 3);
    }

    @Test
    public void testHOUR() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, testedTimeSeconds + ", HOUR", 4);
    }

    @Test
    public void testMINUTE() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, testedTimeSeconds + ", MINUTE", 5);
    }

    @Test
    public void testSECOND() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, testedTimeSeconds + ", SECOND", 6);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalid() throws IOException {
        DataProcessor dp = new DataProcessor(1, 2);
        expected(dp, "nothing, 1, +");
    }

}
