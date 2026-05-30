package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for
 * https://github.com/codenameone/CodenameOne/issues/1515
 * -- the Calendar UI component must preserve the hour/minute/second/millis
 * of the Date passed into setDate, setCurrentDate and setSelectedDate. The
 * 2015 reporter found that MonthView.setSelectedDay and setCurrentDay were
 * forcibly normalising the time-of-day, so round-tripping through getDate
 * / getCurrentDate silently lost the time component.
 */
class CalendarDatePreservationTest extends UITestBase {

    private static Date local(int year, int month, int dayOfMonth, int hour, int minute, int second, int millis) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.set(year, month, dayOfMonth, hour, minute, second);
        cal.set(java.util.Calendar.MILLISECOND, millis);
        return cal.getTime();
    }

    private static void assertSameInstant(Date expected, Date actual) {
        assertEquals(expected.getTime(), actual.getTime(),
                "Date instant must round-trip exactly; got " + actual + ", expected " + expected);
    }

    @FormTest
    void setDateRoundTripsHourMinuteSecondMillis() {
        Calendar c = new Calendar();
        Date in = local(2026, java.util.Calendar.MARCH, 15, 13, 45, 30, 500);
        c.setDate(in);
        assertSameInstant(in, c.getDate());
    }

    @FormTest
    void setSelectedDateRoundTripsHourMinuteSecondMillis() {
        Calendar c = new Calendar();
        Date in = local(2026, java.util.Calendar.AUGUST, 1, 23, 59, 59, 999);
        c.setSelectedDate(in);
        assertSameInstant(in, c.getDate());
    }

    @FormTest
    void setCurrentDateRoundTripsHourMinuteSecondMillis() {
        Calendar c = new Calendar();
        Date in = local(2026, java.util.Calendar.JANUARY, 1, 9, 30, 15, 250);
        c.setCurrentDate(in);
        assertSameInstant(in, c.getCurrentDate());
    }

    @FormTest
    void setDateMidnightStillReturnsMidnight() {
        // Pre-fix the time was forcibly set to 01:00, so midnight became 01:00.
        Calendar c = new Calendar();
        Date midnight = local(2026, java.util.Calendar.JULY, 4, 0, 0, 0, 0);
        c.setDate(midnight);
        assertSameInstant(midnight, c.getDate());
    }

    @FormTest
    void dayOfMonthStillReportsCorrectlyAfterPreservingTimeOfDay() {
        // The fix preserves the time-of-day on read but day-of-month math must
        // still reflect the user-supplied day.
        Calendar c = new Calendar();
        Date in = local(2026, java.util.Calendar.MARCH, 15, 13, 45, 30, 500);
        c.setDate(in);

        GregorianCalendar verifier = new GregorianCalendar(TimeZone.getDefault());
        verifier.setTime(c.getDate());
        assertEquals(2026, verifier.get(java.util.Calendar.YEAR));
        assertEquals(java.util.Calendar.MARCH, verifier.get(java.util.Calendar.MONTH));
        assertEquals(15, verifier.get(java.util.Calendar.DAY_OF_MONTH));
    }
}
