/*******************************************************************************
 * Copyright (c) 2011 The OpenNMS Group, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *******************************************************************************/
package org.rrd4j.graph;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.IPlottable;
import org.rrd4j.data.Variable;

public class PrintTextTest {

    static class ConstantStaticDef implements IPlottable {
        private double m_startTime = Double.NEGATIVE_INFINITY;
        private double m_endTime = Double.POSITIVE_INFINITY;

        ConstantStaticDef(long startTime, long endTime, double value) {
            m_startTime = startTime;
            m_endTime = endTime;
        }

        @Override
        public double getValue(long timestamp) {
            if (m_startTime <= timestamp && m_endTime >= timestamp) {
                return Math.log(timestamp * 1.0);
            } else {
                return Double.NaN;
            }
        }
    }

    @Test
    public void testTrim() throws java.io.IOException {
        Date endDate = new Date();
        Date startDate = new Date(endDate.getTime() - TimeUnit.HOURS.toMillis(4));
        ConstantStaticDef sdef = new ConstantStaticDef(TimeUnit.MILLISECONDS.toSeconds(startDate.getTime()), TimeUnit.MILLISECONDS.toSeconds(endDate.getTime()), 123456.0);

        PrintText ct1 = new PrintText("avg", "%10.0lf\\g", true, false);
        PrintText ct2 = new PrintText("min", "%10.0lf\\g", true, false);
        PrintText ct3 = new PrintText("max", "%10.0lf\\g", true, false);
        DataProcessor dproc = new DataProcessor(startDate, endDate);
        ValueScaler valueScaler = new ValueScaler(1000);
        dproc.datasource("test", sdef);
        dproc.datasource("avg", "test", new Variable.AVERAGE());
        dproc.datasource("max", "test", new Variable.MAX());
        dproc.datasource("min", "test", new Variable.MIN());
        dproc.processData();
        ct1.resolveText(Locale.ENGLISH, dproc, valueScaler);
        ct2.resolveText(Locale.ENGLISH, dproc, valueScaler);
        ct3.resolveText(Locale.ENGLISH, dproc, valueScaler);
        Assert.assertEquals("        21", ct1.resolvedText);
        Assert.assertEquals("        21", ct2.resolvedText);
        Assert.assertEquals("        21", ct3.resolvedText);
    }
}
