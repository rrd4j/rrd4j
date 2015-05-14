package org.rrd4j.graph;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

/**
 * Simple time label using a format similar to {@code strftime}. For more details on the
 * supported conversions see the Date/Time Conversions section for {@link java.util.Formatter}.
 * Examples:
 *
 * <ul>
 *   <li>strftime pattern: {@code %Y-%m-%dT%H:%M:%S}</li>
 *   <li>simple date format pattern: {@code yyyy'-'MM'-'dd'T'HH':'mm':'ss}</li>
 * </ul>
 */
public class SimpleTimeLabelFormat implements TimeLabelFormat {

    private final String format;

    /**
     * Create a new instance using a format string that is either an strftime patter or a simple
     * date format pattern.
     */
    public SimpleTimeLabelFormat(String format) {
        // escape strftime like format string
        this.format = format.replaceAll("%([^%])", "%1\\$t$1");
        System.err.println(this.format);
    }

    @Override
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
