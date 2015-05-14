package org.rrd4j.graph;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SimpleTimeLabelFormatTest {

    private static final Date INSTANT = new Date(1428550160000L);

    @Test
    public void strftime() throws Exception {
        Calendar calendar = Calendar.getInstance();
        TimeLabelFormat fmt = new SimpleTimeLabelFormat("%Y-%m-%dT%H:%M:%S");
        Assert.assertEquals("2015-04-08T20:29:20", fmt.format(calendar, Locale.US, INSTANT));
    }

    @Test
    public void simpleDateFormat() throws Exception {
        Calendar calendar = Calendar.getInstance();
        TimeLabelFormat fmt = new SimpleTimeLabelFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
        Assert.assertEquals("2015-04-08T20:29:20", fmt.format(calendar, Locale.US, INSTANT));
    }
}
