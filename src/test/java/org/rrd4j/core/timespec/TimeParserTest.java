package org.rrd4j.core.timespec;

import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

/**
 * Mostly about testing TimeParser; relies on TimeSpec to be working as well (and to some degree exercises it implicitly; 
 * if you break TimeSpec, these tests may fail when doing relative times (e.g start = X, end = start+100 and the like)
 *  
 * @author cmiskell
 *
 */
public class TimeParserTest {

    @Before
    public void setup() throws InterruptedException {
        //Yeah, this looks weird, but there's method to my madness.  
        // Many of these tests create Calendars based on the current time, then use the 
        // parsing code which will assume something about when 'now' is, often the current day/week
        // If we're running around midnight and the Calendar gets created on one day and the parsing
        // happens on the next, you'll get spurious failures, which would be really annoying and hard
        // to replicate.  Initial tests show that most tests take ~100ms, so if it's within 11 seconds
        // of midnight, wait for 30 seconds (and print a message saying why)
        Calendar now = Calendar.getInstance(Locale.ENGLISH);
        if(now.get(Calendar.HOUR_OF_DAY) == 23 && now.get(Calendar.MINUTE) > 59 && now.get(Calendar.SECOND) > 50) {
            Thread.sleep(11000);
        }
    }

    private Calendar[] parseTimes(String startTime, String endTime) {
        TimeParser startParser = new TimeParser(startTime);
        TimeParser endParser = new TimeParser(endTime);
        TimeSpec specStart = startParser.parse();
        TimeSpec specEnd = endParser.parse();
        return TimeSpec.getTimes(specStart, specEnd);
    }

    /**
     * Set all fields smaller than an hour to 0.
     * @param now
     */
    private void setSubHourFieldsZero(Calendar cal) {
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Set all fields smaller than a day to 0 (rounding down to midnight effectively)
     * @param now
     */
    private void setSubDayFieldsZero(Calendar cal) {
        setSubHourFieldsZero(cal);
        cal.set(Calendar.HOUR_OF_DAY,0);
    }

    /**
     * Kinda like the JUnit asserts for doubles, which allows an "epsilon"
     * But this is for integers, and with a specific description in the assert
     * just for timestamps.
     * 
     * All times are expected in milliseconds
     * 
     * @param expected - expected value
     * @param actual - actual value
     * @param epsilon - the maximum difference
     * @param desc - some description of the time.  Usually "start" or "end", could be others
     */
    private void assertTimestampsEqualWithEpsilon(long expected, long actual, int epsilon, String desc) {
        assertTrue("Expected a " + desc + " time within " + epsilon + "ms of "+ expected
                   + " but got " + actual,
                   Math.abs(actual - expected) < epsilon);
    }

    private void check(Consumer<Calendar> startSet, String startStr) {
        Calendar startCal = Calendar.getInstance(Locale.ENGLISH);
        startSet.accept(startCal);
        Date startDate = startCal.getTime();

        TimeParser startParser = new TimeParser(startStr);
        TimeSpec specStart = startParser.parse();

        Instant start = Instant.ofEpochSecond(specStart.getTimestamp());
        assertTimestampsEqualWithEpsilon(startDate.getTime(), start.toEpochMilli(), 1000, "start");
    }

    private void check(Consumer<Calendar> startSet, Consumer<Calendar> endSet, String startStr, String endStr) {
        Calendar now = Calendar.getInstance(Locale.ENGLISH);
        now.setLenient(true);

        Calendar startCal = (Calendar) now.clone();
        startSet.accept(startCal);
        Date startDate = startCal.getTime();

        Calendar endCal = (Calendar) now.clone();
        endSet.accept(endCal);
        Date endDate = endCal.getTime();

        Calendar[] result = parseTimes(startStr, endStr);

        assertTimestampsEqualWithEpsilon(startDate.getTime(), result[0].toInstant().toEpochMilli(), 1000, "start");
        assertTimestampsEqualWithEpsilon(endDate.getTime(), result[1].toInstant().toEpochMilli(), 1000, "end");
    }

    /**
     * Test the specification of just an "hour" (assumed today) for
     * both start and end, using the 24hour clock
     */
    @Test
    public void test24HourClockHourTodayStartEndTime() {
        check(c -> {
            c.set(Calendar.HOUR_OF_DAY, 8); //8am
            setSubHourFieldsZero(c);
        }, c -> {
            c.set(Calendar.HOUR_OF_DAY, 16); //4pm
            setSubHourFieldsZero(c);
        },
        "08", "16");
    }

    /**
     * Test the specification of just an "hour" (assumed today) start using "midnight", end = now
     */
    @Test
    public void testMidnight() {
        check(c -> {
            c.set(Calendar.HOUR_OF_DAY, 0); //midnight
            setSubHourFieldsZero(c);
        }, "midnight");
    }

    /**
     * Test the specification of just an "hour" (assumed today) start using "noon", end = now
     */
    @Test
    public void testNoon() {
        check(c -> {
            c.set(Calendar.HOUR_OF_DAY, 12); //noon
            setSubHourFieldsZero(c);
        }, "noon");
    }

    /**
     * Test the specification of just an "hour" (assumed today) start using "noon", end = now
     */
    @Test
    public void testTeaTime() {
        check(c -> {
            c.set(Calendar.HOUR_OF_DAY, 16); //teatime
            setSubHourFieldsZero(c);
        }, "teatime");
    }

    /**
     * Test the specification of just an "hour" (assumed today) for
     * both start and end, using am/pm designators
     */
    @Test
    public void testAMPMClockHourTodayStartEndTime() {
        for (int i = 0 ; i < 12 ; i++) {
            int cur = i;
            check(c -> {
                c.set(Calendar.HOUR_OF_DAY, cur);
                setSubHourFieldsZero(c);
            }, c -> {
                c.set(Calendar.HOUR_OF_DAY, 12 + cur);
                setSubHourFieldsZero(c);
            },
            i + "am", i + "pm");
        }
    }

    @Test
    public void test12AMPM() {
        check(c -> {
            /* 12:xx AM is 00:xx, not 12:xx */
            c.set(Calendar.HOUR_OF_DAY, 0);
            setSubHourFieldsZero(c);
        }, c -> {
            /* 12:xx PM is 12:xx, not 24:xx */
            c.set(Calendar.HOUR_OF_DAY, 12);
            setSubHourFieldsZero(c);
        },
        "12am", "12pm");
    }

    /**
     * Test that we can explicitly use the term "today", e.g. "today 9am"
     * NB: This was failing in 1.5.12, so well worth testing :)
     */
    @Test
    public void testTodayTime() {
        check(c -> {
            c.set(Calendar.HOUR_OF_DAY, 9); //9am
            setSubHourFieldsZero(c);
        }, c -> {
            c.set(Calendar.HOUR_OF_DAY, 17); //5pm
            setSubHourFieldsZero(c);
        },
        "9am today", "5pm today");
    }

    /**
     * Test that we can explicitly use the term "yesterday", e.g. "yesterday 9am"
     * NB: This was failing in 1.5.12, so well worth testing :)
     */
    @Test
    public void testYesterdayTime() {
        check(c -> {
            c.set(Calendar.HOUR_OF_DAY, 9); //9am
            c.add(Calendar.DAY_OF_YEAR, -1);
            setSubHourFieldsZero(c);
        }, c -> {
            c.set(Calendar.HOUR_OF_DAY, 17); //5pm
            c.add(Calendar.DAY_OF_YEAR, -1);
            setSubHourFieldsZero(c);
        },
        "9am yesterday", "5pm yesterday");
    }

    /**
     * Test that we can explicitly use the term "tomorrow", e.g. "tomorrow 9am"
     * NB: This was failing in 1.5.12, so well worth testing :)
     */
    @Test
    public void testTomorrowTime() {
        check(c -> {
            c.set(Calendar.HOUR_OF_DAY, 9); //9am
            c.add(Calendar.DAY_OF_YEAR, +1);
            setSubHourFieldsZero(c);
        }, c -> {
            c.set(Calendar.HOUR_OF_DAY, 17); //5pm
            c.add(Calendar.DAY_OF_YEAR, +1);
            setSubHourFieldsZero(c);
        },
        "9am tomorrow", "5pm tomorrow");
    }

    /**
     * Tests a simple negative hour offset
     * 
     * Test the simple start=-1h example from rrdfetch man page.  
     * End is "now" (implied if missing in most user interfaces), 
     * start should be 1 hour prior to now 
     */
    @Test
    public void testSimpleNegativeOffset() {
        check(c -> {
            c.add(Calendar.HOUR_OF_DAY, -1);
        }, c -> {
        },
        "-1hour", "now");
    }

    /**
     * Test a start relative to an end that isn't now
     */
    @Test
    public void testRelativeStartOffsetEnd() {
        check(c -> {
            c.add(Calendar.HOUR_OF_DAY, -3);
        }, c -> {
            c.add(Calendar.HOUR_OF_DAY, -1);
        },
        //End is 1 hour ago; start is 2 hours before that
        "end-2hours", "-1h");
    }

    /**
     * Test a start relative to an end that isn't now
     */
    @Test
    public void testRelativeStartOffsetEndAbbreviatedEnd() {
        check(c -> {
            c.add(Calendar.HOUR_OF_DAY, -3);
        }, c -> {
            c.add(Calendar.HOUR_OF_DAY, -1);
        },
        //End is 1 hour ago; start is 2 hours before that
        "e-2h", "-1h");
    }

    /**
     * Test an end relative to a start that isn't now
     */
    @Test
    public void testRelativeEndOffsetStart() {
        check(c -> {
            c.add(Calendar.HOUR_OF_DAY, -4);
        }, c -> {
            c.add(Calendar.HOUR_OF_DAY, -2);
        },
        "-4h", "start+2h");
    }

    /**
     * Test an end relative to a start that isn't now - abbreviated start (s)
     */
    @Test
    public void testRelativeEndOffsetStartAbbreviatedStart() {
        check(c -> {
            c.add(Calendar.HOUR_OF_DAY, -4);
        }, c -> {
            c.add(Calendar.HOUR_OF_DAY, -2);
        },
        "-4h", "s+2h");
    }

    /**
     * Test hour:min, and hour.min syntaxes
     */
    @Test
    public void testHourMinuteSyntax() {
        check(c -> {
            setSubHourFieldsZero(c);
            c.set(Calendar.HOUR_OF_DAY, 8); 
            c.set(Calendar.MINUTE, 30);
        }, c -> {
            setSubHourFieldsZero(c);
            c.set(Calendar.HOUR_OF_DAY, 16); 
            c.set(Calendar.MINUTE, 45);
        },
        //Mixed syntaxes FTW; two tests in one
        //This also exercises the test of the order of parsing (time then day).  If
        // that order is wrong, 8.30 could be (and was at one point) interpreted as a day.month 
        "8.30", "16:45");
    }

    /**
     * Test a plain date specified as DD.MM.YYYY
     */
    @Test
    public void testDateWithDots() {
        check(c -> {
            setSubDayFieldsZero(c);
            c.set(1980, 0, 1);
        }, c -> {
            setSubDayFieldsZero(c);
            c.set(1980, 11, 15);
        },
        //Start is a simple one; end ensures we have our days/months around the right way.
        "00:00 01.01.1980", "00:00 15.12.1980");
    }

    /**
     * Test a plain date specified as DD/MM/YYYY
     */
    @Test
    public void testDateWithSlashes() {
        check(c -> {
            setSubDayFieldsZero(c);
            setSubHourFieldsZero(c);
            c.set(1980, 0, 1);
        }, c -> {
            setSubDayFieldsZero(c);
            c.set(1980, 11, 15);
        },
        //Start is a simple one; end ensures we have our days/months around the right way.
        "00:00 01/01/1980", "00:00 12/15/1980");
    }

    /**
     * Test a plain date specified as YYYYMMDD
     */
    @Test
    public void testDateWithNoDelimiters() {
        check(c -> {
            setSubDayFieldsZero(c);
            setSubHourFieldsZero(c);
            c.set(1980, 0, 1);
        }, c -> {
            setSubDayFieldsZero(c);
            c.set(1980, 11, 15);
        },
        //Start is a simple one; end ensures we have our days/months around the right way.
        "00:00 19800101", "00:00 19801215");
    }

    /**
     * Test a plain date specified as YYYYMMDD, using the order given in rrdfetch man page
     */
    @Test
    public void testDateWithNoDelimitersReversed() {
        check(c -> {
            c.set(Calendar.MONTH, 6);
            c.set(Calendar.DAY_OF_MONTH, 3);
            c.set(Calendar.YEAR, 1997);
            c.set(Calendar.HOUR_OF_DAY, 12);
            c.set(Calendar.MINUTE, 45);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        },
        "19970703 12:45");
    }

    /**
     * Test named month dates with no year
     * 
     * NB: Seems silly to test all, just test that an arbitrary one works, in both short and long form  
     * 
     * If we find actual problems with specific months, we can add more tests
     */
    @Test
    public void testNamedMonthsNoYear() {
        check(c -> {
            c.set(Calendar.MONTH, 2); 
            c.set(Calendar.DAY_OF_MONTH, 1);
            setSubDayFieldsZero(c);
        }, c -> {
            c.set(Calendar.MONTH, 10); 
            c.set(Calendar.DAY_OF_MONTH, 15);
            setSubDayFieldsZero(c);
        },
        // one short, one long month name
        "00:00 Mar 1", "00:00 November 15");
    }

    /**
     * Test named month dates with 2 digit years
     * 
     * NB: Seems silly to test all, just test that an arbitrary one works, in both short and long form
     * If we find actual problems with specific months, we can add more tests
     */
    @Test
    public void testNamedMonthsTwoDigitYearTwentiethCentury() {
        for (int i = 38; i <= 99; i++) {
            int cur = i;
            check(c -> {
                setSubDayFieldsZero(c);
                c.set(1900 + cur, 1, 2);
            }, c -> {
                setSubDayFieldsZero(c);
                c.set(1900 + cur, 9, 16);
            }, "00:00 Feb 2 " + i, "00:00 October 16 " + i);
        }
    }

    /**
     * Test named month dates with 2 digit years, in the range 00-37, means post 2000
     */
    @Test
    public void testNamedMonthsTwoDigitYear2000() {
        for (int i = 0; i <= 37; i++) {
            int cur = i;
            String year = String.format("%02d", i);
            check(c -> {
                setSubDayFieldsZero(c);
                c.set(2000 + cur, 1, 2);
            }, c -> {
                setSubDayFieldsZero(c);
                c.set(2000 + cur, 9, 16);
            }, "00:00 Feb 2 " + year, "00:00 October 16 " + year);
        }
    }

    /**
     * Test named month dates with 4 digit years
     */
    @Test
    public void testNamedMonthsFourDigitYear() {
        for (int i = 1000; i <= 3000; i += 1000) {
            int cur = i;
            String year = String.format("%04d", i);
            check(c -> {
                setSubDayFieldsZero(c);
                c.set(cur, 1, 2);
            }, c -> {
                setSubDayFieldsZero(c);
                c.set(cur, 9, 16);
            }, "00:00 Feb 2 " + year, "00:00 October 16 " + year);
        }
    }

    /**
     * Test day of week specification.  The expected behaviour is annoyingly murky; if these tests start failing
     * give serious consideration to fixing the tests rather than the underlying code.
     * 
     */
    @Test
    public void testDayOfWeekTimeSpec() {
        check(c -> {
            c.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
            c.set(Calendar.HOUR_OF_DAY, 12);
            setSubHourFieldsZero(c);
        }, c -> {
            c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
            c.set(Calendar.HOUR_OF_DAY, 18);
            setSubHourFieldsZero(c);
        },
        "noon Thursday", "6pm Friday");
    }

    /**
     * Test some basic time offsets
     */
    @Test
    public void testTimeOffsets1() {
        check(c -> {
            c.add(Calendar.MINUTE, -1);
        }, c -> {
            c.add(Calendar.SECOND, -10);
        },
        "now - 1minute", "now-10 second");
    }

    /**
     * Test some basic time offsets
     * NB: Due to it's use of a "day" offset, this may fail around daylight savings time.
     * Maybe (Depends how the parsing code constructs it's dates)
     */
    @Test
    public void testTimeOffsets2() {
        check(c -> {
            c.add(Calendar.DAY_OF_YEAR, -1);
        }, c -> {
            c.add(Calendar.HOUR, -3);
        },
        "now - 1 day", "now-3 hours");
    }

    /**
     * Test some basic time offsets
     */
    @Test
    public void testTimeOffsets3() {
        check(c -> {
            c.set(Calendar.MONTH, 6);
            c.set(Calendar.DAY_OF_MONTH, 12);
            c.add(Calendar.MONTH, -1);
        }, c -> {
            c.set(Calendar.MONTH, 6);
            c.set(Calendar.DAY_OF_MONTH, 12);
            c.add(Calendar.WEEK_OF_YEAR, -3);
        },
        "Jul 12 - 1 month", "Jul 12 - 3 weeks");
    }

    /**
     * Test another basic time offset
     */
    @Test
    public void testTimeOffsets4() {
        check(c -> {
            setSubDayFieldsZero(c);
            c.set(1979, 6, 12);
        }, c -> {
            setSubDayFieldsZero(c);
            c.set(1980, 6, 12);
        },
        "end - 1 year", "00:00 12.07.1980");
    }

    /**
     * Test some complex offset examples (per the rrdfetch man page)
     */
    @Test
    public void complexTest1() {
        check(c -> {
            c.set(Calendar.HOUR_OF_DAY, 9);
            c.add(Calendar.DAY_OF_YEAR, -1);
            setSubHourFieldsZero(c);
        },
        "noon yesterday -3hours");
    }

    /**
     * Test some more complex offset examples
     */
    @Test
    public void complexTest2() {
        check(c -> {
            c.add(Calendar.HOUR, -5);
            c.add(Calendar.MINUTE, -45);
        },
        "-5h45min");
    }

    /**
     * Test some more complex offset examples
     */
    @Test
    public void complexTest3() {
        check(c -> {
            c.add(Calendar.MONTH, -5);
            c.add(Calendar.WEEK_OF_YEAR, -1);
            c.add(Calendar.DAY_OF_YEAR, -2);
        },
        "-5mon1w2d");
    }

    /**
     * Test some more complex offset examples
     */
    @Test
    public void testGuessMinute() {
        check(c -> {
            c.add(Calendar.HOUR, -5);
            c.add(Calendar.MINUTE, -45);
        },
        "-5h45m");
    }

    /**
     * Test some more complex offset examples
     */
    @Test
    public void testGuessMonth() {
        check(c -> {
            c.add(Calendar.YEAR, -1);
            c.add(Calendar.MONTH, -2);
        },
        "-1y2m");
    }

    /**
     * Test time as seconds
     */
    @Test
    public void rrdfetchManSeconds() {
        check(c -> {
            c.setTimeZone(TimeZone.getTimeZone("UTC"));
            c.set(Calendar.MONTH, 6);
            c.set(Calendar.DAY_OF_MONTH, 5);
            c.set(Calendar.YEAR, 1999);
            c.set(Calendar.HOUR_OF_DAY, 18);
            c.set(Calendar.MINUTE, 45);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        },
        "931200300");
    }

}
