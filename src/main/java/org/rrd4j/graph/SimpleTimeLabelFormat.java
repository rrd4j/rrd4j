package org.rrd4j.graph;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class SimpleTimeLabelFormat implements TimeLabelFormat {

    private final String format;

    public SimpleTimeLabelFormat(String format) {
        // escape strftime like format string
        this.format = format.replaceAll("([^%]|^)%([^%t])", "$1%t$2");
    }

    public String format(Calendar calendar, Locale locale, Date timestamp) {
        Calendar c = (Calendar) calendar.clone();
        c.setTime(timestamp);
        if (format.contains("%")) {
            // strftime like format string
            return String.format(locale, format, c);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.setCalendar(c);
            return sdf.format(timestamp);
        }
    }
}
