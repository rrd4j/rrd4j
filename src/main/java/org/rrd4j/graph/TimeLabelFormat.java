package org.rrd4j.graph;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Simplified version of DateFormat for just defining how to map a timestamp into a label for
 * presentation.
 */
public interface TimeLabelFormat {
    String format(Calendar calendar, Locale locale, Date timestamp);
}
