package org.rrd4j.core.timespec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.rrd4j.core.timespec.TimeParser;
import org.rrd4j.core.timespec.TimeSpec;

/**
 * Mostly about testing TimeParser; relies on TimeSpec to be working as well (and to some degree exercises it implicitly; 
 * if you break TimeSpec, these tests may fail when doing relative times (e.g start = X, end = start+100 and the like)
 *  
 * @author cmiskell
 *
 */
public class TimeParserTest {

    private final static int ONE_HOUR_IN_MILLIS=60*60*1000;
    private final static int ONE_DAY_IN_MILLIS=24 * ONE_HOUR_IN_MILLIS;
    //private final static int ONE_NIGHT_IN_PARIS = 0x7a57e1e55;


    @Before
    public void setup() throws InterruptedException {
        //Yeah, this looks weird, but there's method to my madness.  
        // Many of these tests create Calendars based on the current time, then use the 
        // parsing code which will assume something about when 'now' is, often the current day/week
        // If we're running around midnight and the Calendar gets created on one day and the parsing
        // happens on the next, you'll get spurious failures, which would be really annoying and hard
        // to replicate.  Initial tests show that most tests take ~100ms, so if it's within 10 seconds
        // of midnight, wait for 30 seconds (and print a message saing why)
        Calendar now = new GregorianCalendar();
        if(now.get(Calendar.HOUR_OF_DAY) == 23 && now.get(Calendar.MINUTE) > 59 && now.get(Calendar.SECOND) > 50) {
            Thread.sleep(30000);
        }
    }

    private long[] parseTimes(String startTime, String endTime) {
        TimeParser startParser = new TimeParser(startTime);
        TimeParser endParser = new TimeParser(endTime);
        TimeSpec specStart = startParser.parse();
        TimeSpec specEnd = endParser.parse();
        return TimeSpec.getTimestamps(specStart, specEnd);
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
        assertTrue("Expected a "+desc+" time within "+epsilon+"ms of "+ expected
                + " but got " + actual,
                Math.abs(actual - expected) < epsilon);
    }

    /**
     * Test the specification of just an "hour" (assumed today) for
     * both start and end, using the 24hour clock
     */
    @Test
    public void test24HourClockHourTodayStartEndTime() {
        Calendar now = new GregorianCalendar();
        now.set(Calendar.HOUR_OF_DAY, 8); //8am
        setSubHourFieldsZero(now);

        Date startDate = now.getTime();
        now.set(Calendar.HOUR_OF_DAY, 16); //4pm
        setSubHourFieldsZero(now);
        Date endDate = now.getTime();

        long[] result = this.parseTimes("08", "16");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);

    }

    /**
     * Test the specification of just an "hour" (assumed today) start using "midnight", end = now
     */
    @Test
    public void testMidnightToday() {
        Calendar now = new GregorianCalendar();
        now.set(Calendar.HOUR_OF_DAY, 0); //midnight
        setSubHourFieldsZero(now);

        Date startDate = now.getTime();

        long[] result = this.parseTimes("midnight", "16");
        long start = result[0] * 1000;

        assertEquals(startDate.getTime(), start);

    }

    /**
     * Test the specification of just an "hour" (assumed today) start using "noon", end = now
     */
    @Test
    public void testNoonToday() {
        Calendar now = new GregorianCalendar();
        now.set(Calendar.HOUR_OF_DAY, 12); //noon
        setSubHourFieldsZero(now);

        Date startDate = now.getTime();

        long[] result = this.parseTimes("noon", "16");
        long start = result[0] * 1000;

        assertEquals(startDate.getTime(), start);

    }

    /**
     * Test the specification of just an "hour" (assumed today) for
     * both start and end, using am/pm designators
     */
    @Test
    public void testAMPMClockHourTodayStartEndTime() {
        Calendar now = new GregorianCalendar();
        now.set(Calendar.HOUR_OF_DAY, 8); //8am
        setSubHourFieldsZero(now);
        Date startDate = now.getTime();
        now.set(Calendar.HOUR_OF_DAY, 16); //4pm
        setSubHourFieldsZero(now);
        Date endDate = now.getTime();

        long[] result = this.parseTimes("8am", "4pm");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);

    }

    /**
     * Test that we can explicitly use the term "today", e.g. "today 9am"
     * NB: This was failing in 1.5.12, so well worth testing :)
     */
    @Test
    public void testTodayTime() {
        Calendar now = new GregorianCalendar();
        now.set(Calendar.HOUR_OF_DAY, 9); //9am
        setSubHourFieldsZero(now);
        Date startDate = now.getTime();
        now.set(Calendar.HOUR_OF_DAY, 17); //5pm
        setSubHourFieldsZero(now);
        Date endDate = now.getTime();

        long[] result = this.parseTimes("9am today", "5pm today");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);
    }

    /**
     * Test that we can explicitly use the term "yesterday", e.g. "yesterday 9am"
     * NB: This was failing in 1.5.12, so well worth testing :)
     */
    @Test
    public void testYesterdayTime() {
        Calendar now = new GregorianCalendar();
        now.set(Calendar.HOUR_OF_DAY, 9); //9am
        setSubHourFieldsZero(now);
        Date startDate = new Date(now.getTimeInMillis()-ONE_DAY_IN_MILLIS);
        now.set(Calendar.HOUR_OF_DAY, 17); //5pm
        setSubHourFieldsZero(now);
        Date endDate = new Date(now.getTimeInMillis()-ONE_DAY_IN_MILLIS);

        long[] result = this.parseTimes("9am yesterday", "5pm yesterday");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);
    }

    /**
     * Test that we can explicitly use the term "tomorrow", e.g. "tomorrow 9am"
     * NB: This was failing in 1.5.12, so well worth testing :)
     */
    @Test
    public void testTomorrowTime() {
        Calendar now = new GregorianCalendar();
        now.set(Calendar.HOUR_OF_DAY, 9); //9am
        setSubHourFieldsZero(now);
        Date startDate = new Date(now.getTimeInMillis()+ONE_DAY_IN_MILLIS);
        now.set(Calendar.HOUR_OF_DAY, 17); //5pm
        setSubHourFieldsZero(now);
        Date endDate = new Date(now.getTimeInMillis()+ONE_DAY_IN_MILLIS);

        long[] result = this.parseTimes("9am tomorrow", "5pm tomorrow");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);
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
        Calendar now = new GregorianCalendar();
        Date endDate = now.getTime();
        Date startDate = new Date(now.getTimeInMillis()-ONE_HOUR_IN_MILLIS);

        long[] result = this.parseTimes("-1h", "now");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertTimestampsEqualWithEpsilon(endDate.getTime(), end, 1000, "end");
        assertTimestampsEqualWithEpsilon(startDate.getTime(), start, 1000, "start");

    }

    /**
     * Test a start relative to an end that isn't now
     */
    @Test
    public void testRelativeStartOffsetEnd() {
        Calendar now = new GregorianCalendar();
        Date endDate = new Date(now.getTimeInMillis()-ONE_HOUR_IN_MILLIS);
        Date startDate = new Date(now.getTimeInMillis()-(3*ONE_HOUR_IN_MILLIS));

        //End is 1 hour ago; start is 2 hours before that
        long[] result = this.parseTimes("end-2h", "-1h");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertTimestampsEqualWithEpsilon(endDate.getTime(), end, 1000, "end");
        assertTimestampsEqualWithEpsilon(startDate.getTime(), start, 1000, "start");

    }

    /**
     * Test a start relative to an end that isn't now
     */
    @Test
    public void testRelativeStartOffsetEndAbbreviatedEnd() {
        Calendar now = new GregorianCalendar();
        Date endDate = new Date(now.getTimeInMillis()-ONE_HOUR_IN_MILLIS);
        Date startDate = new Date(now.getTimeInMillis()-(3*ONE_HOUR_IN_MILLIS));

        //End is 1 hour ago; start is 2 hours before that
        long[] result = this.parseTimes("e-2h", "-1h");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertTimestampsEqualWithEpsilon(endDate.getTime(), end, 1000, "end");
        assertTimestampsEqualWithEpsilon(startDate.getTime(), start, 1000, "start");

    }

    /**
     * Test an end relative to a start that isn't now
     */
    @Test
    public void testRelativeEndOffsetStart() {
        Calendar now = new GregorianCalendar();
        Date endDate = new Date(now.getTimeInMillis()-(2*ONE_HOUR_IN_MILLIS));
        Date startDate = new Date(now.getTimeInMillis()-(4*ONE_HOUR_IN_MILLIS));

        long[] result = this.parseTimes("-4h", "start+2h");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertTimestampsEqualWithEpsilon(endDate.getTime(), end, 1000, "end");
        assertTimestampsEqualWithEpsilon(startDate.getTime(), start, 1000, "start");

    }

    /**
     * Test an end relative to a start that isn't now - abbreviated start (s)
     */
    @Test
    public void testRelativeEndOffsetStartAbbreviatedStart() {
        Calendar now = new GregorianCalendar();
        Date endDate = new Date(now.getTimeInMillis()-(2*ONE_HOUR_IN_MILLIS));
        Date startDate = new Date(now.getTimeInMillis()-(4*ONE_HOUR_IN_MILLIS));

        long[] result = this.parseTimes("-4h", "s+2h");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertTimestampsEqualWithEpsilon(endDate.getTime(), end, 1000, "end");
        assertTimestampsEqualWithEpsilon(startDate.getTime(), start, 1000, "start");

    }

    /**
     * Test hour:min, and hour.min syntaxes
     */
    @Test
    public void testHourMinuteSyntax() {
        Calendar now = new GregorianCalendar();
        setSubHourFieldsZero(now);
        now.set(Calendar.HOUR_OF_DAY, 8); 
        now.set(Calendar.MINUTE, 30);

        Date startDate = now.getTime();
        now.set(Calendar.HOUR_OF_DAY, 16); 
        now.set(Calendar.MINUTE, 45);
        Date endDate = now.getTime();

        //Mixed syntaxes FTW; two tests in one
        //This also exercises the test of the order of parsing (time then day).  If
        // that order is wrong, 8.30 could be (and was at one point) interpreted as a day.month 
        long[] result = this.parseTimes("8.30", "16:45");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);

    }

    /**
     * Test a plain date specified as DD.MM.YYYY
     */
    @Test
    public void testDateWithDots() {
        //Remember: 0-based month
        Calendar cal = new GregorianCalendar(1980,0,1);
        Date startDate = cal.getTime();

        cal = new GregorianCalendar(1980,11,15);
        Date endDate = cal.getTime();

        //Start is a simple one; end ensures we have our days/months around the right way.
        long[] result = this.parseTimes("00:00 01.01.1980", "00:00 15.12.1980");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);

    }

    /**
     * Test a plain date specified as DD/MM/YYYY
     */
    @Test
    public void testDateWithSlashes() {
        //Remember: 0-based month
        Calendar cal = new GregorianCalendar(1980,0,1);
        Date startDate = cal.getTime();

        cal = new GregorianCalendar(1980,11,15);
        Date endDate = cal.getTime();

        //Start is a simple one; end ensures we have our days/months around the right way.
        long[] result = this.parseTimes("00:00 01/01/1980", "00:00 12/15/1980");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);

    }

    /**
     * Test a plain date specified as YYYYMMDD
     */
    @Test
    public void testDateWithNoDelimiters() {
        //Remember: 0-based month
        Calendar cal = new GregorianCalendar(1980,0,1);
        Date startDate = cal.getTime();

        cal = new GregorianCalendar(1980,11,15);
        Date endDate = cal.getTime();

        //Start is a simple one; end ensures we have our days/months around the right way.
        long[] result = this.parseTimes("00:00 19800101", "00:00 19801215");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);

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
        //Remember: 0-based month -- 2 = March
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.MONTH, 2); 
        cal.set(Calendar.DAY_OF_MONTH, 1);
        this.setSubDayFieldsZero(cal);
        Date startDate = cal.getTime();

        //10 = November 
        cal = new GregorianCalendar();
        cal.set(Calendar.MONTH, 10); 
        cal.set(Calendar.DAY_OF_MONTH, 15);
        this.setSubDayFieldsZero(cal);
        Date endDate = cal.getTime();

        //one short, one long month name
        long[] result = this.parseTimes("00:00 Mar 1 ", "00:00 November 15");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);

    }

    /**
     * Test named month dates with 2 digit years
     * 
     * NB: Seems silly to test all, just test that an arbitrary one works, in both short and long form
     * If we find actual problems with specific months, we can add more tests
     */
    @Test
    public void testNamedMonthsTwoDigitYear() {
        //Remember: 0-based month -- 1 = Feb
        Calendar cal = new GregorianCalendar(1980,1,2);
        Date startDate = cal.getTime();

        //9 = October 
        cal = new GregorianCalendar(1980,9,16);
        Date endDate = cal.getTime();

        long[] result = this.parseTimes("00:00 Feb 2 80", "00:00 October 16 80");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);

    }

    /**
     * Test named month dates with 4 digit years
     * 
     * NB: Seems silly to test all, just test that an arbitrary one works, in both short and long form
     * 
     * If we find actual problems with specific months, we can add more tests
     */
    @Test
    public void testNamedMonthsFourDigitYear() {
        //Remember: 0-based month -- 3 = April
        Calendar cal = new GregorianCalendar(1980,3,6);
        Date startDate = cal.getTime();

        //8 = Sept 
        cal = new GregorianCalendar(1980,8,17);
        Date endDate = cal.getTime();

        long[] result = this.parseTimes("00:00 Apr 6 1980", "00:00 September 17 1980");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(startDate.getTime(), start);
        assertEquals(endDate.getTime(), end);

    }

    /**
     * Test day of week specification.  The expected behaviour is annoyingly murky; if these tests start failing
     * give serious consideration to fixing the tests rather than the underlying code.
     * 
     */
    @Test
    public void testDayOfWeekTimeSpec() {
        Calendar cal = new GregorianCalendar(Locale.US);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        this.setSubHourFieldsZero(cal);
        Date startDate = cal.getTime();

        cal = new GregorianCalendar(Locale.US);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        cal.set(Calendar.HOUR_OF_DAY, 18);
        this.setSubHourFieldsZero(cal);
        Date endDate = cal.getTime();


        long[] result = this.parseTimes("noon Thursday", "6pm Friday");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertEquals(start, startDate.getTime());
        assertEquals(end, endDate.getTime());

    }

    /**
     * Test some basic time offsets
     */
    @Test
    public void testTimeOffsets1() {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(cal.getTimeInMillis()-60000);
        Date startDate = cal.getTime();

        cal = new GregorianCalendar();
        cal.setTimeInMillis(cal.getTimeInMillis()-10000);
        Date endDate = cal.getTime();

        long[] result = this.parseTimes("now - 1minute", "now-10 seconds");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertTimestampsEqualWithEpsilon(startDate.getTime(), start, 1000, "start");
        assertTimestampsEqualWithEpsilon(endDate.getTime(), end, 1000, "end");

    }

    /**
     * Test some basic time offsets
     * NB: Due to it's use of a "day" offset, this may fail around daylight savings time.
     * Maybe (Depends how the parsing code constructs it's dates)
     */
    @Test
    public void testTimeOffsets2() {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(cal.getTimeInMillis()-ONE_DAY_IN_MILLIS);
        Date startDate = cal.getTime();

        cal = new GregorianCalendar();
        cal.setTimeInMillis(cal.getTimeInMillis()-(3*ONE_HOUR_IN_MILLIS));
        Date endDate = cal.getTime();

        long[] result = this.parseTimes("now - 1 day", "now-3 hours");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertTimestampsEqualWithEpsilon(startDate.getTime(), start, 1000, "start");
        assertTimestampsEqualWithEpsilon(endDate.getTime(), end, 1000, "end");

    }

    /**
     * Test some basic time offsets
     */
    @Test
    public void testTimeOffsets3() {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.MONTH, 6);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.add(Calendar.MONTH, -1);
        Date startDate = cal.getTime();

        cal = new GregorianCalendar();
        cal.set(Calendar.MONTH, 6);
        cal.set(Calendar.DAY_OF_MONTH, 12);
        cal.add(Calendar.WEEK_OF_YEAR, -3);
        Date endDate = cal.getTime();

        long[] result = this.parseTimes("Jul 12 - 1 month", "Jul 12 - 3 weeks");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertTimestampsEqualWithEpsilon(startDate.getTime(), start, 1000, "start");
        assertTimestampsEqualWithEpsilon(endDate.getTime(), end, 1000, "end");

    }

    /**
     * Test another basic time offset
     */
    @Test
    public void testTimeOffsets4() {
        //Month 6 = July (0 offset)
        Calendar cal = new GregorianCalendar(1980, 6, 12);
        Date endDate = cal.getTime();

        cal = new GregorianCalendar(1979, 6, 12);
        Date startDate = cal.getTime();

        long[] result = this.parseTimes("end - 1 year", "00:00 12.07.1980");
        long start = result[0] * 1000;
        long end = result[1] * 1000;

        assertTimestampsEqualWithEpsilon(startDate.getTime(), start, 1000, "start");
        assertTimestampsEqualWithEpsilon(endDate.getTime(), end, 1000, "end");
    }

    /**
     * Test some complex offset examples (per the rrdfetch man page)
     */
    @Test
    public void complexTest1() {

        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        setSubHourFieldsZero(cal);
        Date startDate = cal.getTime();


        long[] result = this.parseTimes("noon yesterday -3hours", "now");
        long start = result[0] * 1000;

        assertEquals(startDate.getTime(), start);
    }

    /**
     * Test some more complex offset examples
     */
    @Test
    public void complexTest2() {

        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.HOUR, -5);
        cal.add(Calendar.MINUTE, -45);
        Date startDate = cal.getTime();

        long[] result = this.parseTimes("-5h45min", "now");
        long start = result[0] * 1000;

        assertTimestampsEqualWithEpsilon(startDate.getTime(), start, 1000, "start");
    }

    /**
     * Test some more complex offset examples
     */
    @Test
    public void complexTest3() {

        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.MONTH, -5);
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        cal.add(Calendar.DAY_OF_YEAR, -2);
        Date startDate = cal.getTime();

        long[] result = this.parseTimes("-5mon1w2d", "now");
        long start = result[0] * 1000;

        assertTimestampsEqualWithEpsilon(startDate.getTime(), start, 1000, "start");
    }


}
