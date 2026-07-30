/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.health;

import java.util.Calendar;
import java.util.Date;
import java.time.Duration;
import java.time.ZoneId;

/// The bucket width of an [AggregateQuery] -- "per hour", "per calendar
/// day", "per calendar month".
///
/// #### Fixed durations and calendar periods are not the same thing
///
/// A calendar day is 23 or 25 hours across a daylight-saving transition,
/// and a month is 28 to 31 days. Bucketing by a fixed `86400000` therefore
/// drifts against the dates a user sees in your UI, silently, twice a
/// year. Both platforms model the distinction natively -- Health Connect
/// splits `aggregateGroupByDuration` from `aggregateGroupByPeriod`, and
/// HealthKit takes `DateComponents` -- so this API keeps it too.
///
/// Calendar-based intervals require an explicit [TimeZone]. Nothing here
/// reads the JVM default, because a server-side default of UTC would put a
/// user's evening walk into the wrong day.
///
/// The zone is captured by reference and must not be modified afterwards
/// -- see [#getTimeZone()], which explains why a copy is not taken.
public final class HealthInterval {

    private final long fixedMillis;
    private final int calendarField;
    private final int calendarAmount;
    private final ZoneId zone;
    private final int firstDayOfWeek;

    private HealthInterval(long fixedMillis, int calendarField,
            int calendarAmount, ZoneId zone, int firstDayOfWeek) {
        this.fixedMillis = fixedMillis;
        this.calendarField = calendarField;
        this.calendarAmount = calendarAmount;
        this.zone = zone;
        this.firstDayOfWeek = firstDayOfWeek;
    }

    private static void requirePositive(long amount, String what) {
        if (amount < 1) {
            throw new IllegalArgumentException(
                    what + " must be positive, got " + amount);
        }
    }

    /// A fixed number of milliseconds.
    public static HealthInterval millis(long millis) {
        requirePositive(millis, "interval");
        return new HealthInterval(millis, -1, 0, null, 0);
    }

    /// A fixed number of minutes.
    public static HealthInterval minutes(int minutes) {
        requirePositive(minutes, "minutes");
        return millis(minutes * 60000L);
    }

    /// A fixed number of hours.
    public static HealthInterval hours(int hours) {
        requirePositive(hours, "hours");
        return millis(hours * 3600000L);
    }

    /// Calendar days in `zone`, aligned to local midnight. Correct across
    /// daylight-saving transitions.
    public static HealthInterval calendarDays(int days, ZoneId zone) {
        requirePositive(days, "days");
        requireZone(zone);
        return new HealthInterval(0, Calendar.DAY_OF_MONTH, days, zone, 0);
    }

    /// Calendar weeks in `zone`, aligned to `firstDayOfWeek` (a
    /// `java.util.Calendar` day constant such as `Calendar.MONDAY`).
    /// The first day of the week is explicit because it differs by locale
    /// and silently guessing it shifts every bucket boundary.
    public static HealthInterval calendarWeeks(int weeks, ZoneId zone,
            int firstDayOfWeek) {
        requirePositive(weeks, "weeks");
        requireZone(zone);
        if (firstDayOfWeek < Calendar.SUNDAY
                || firstDayOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException(
                    "firstDayOfWeek must be a java.util.Calendar day"
                            + " constant, got " + firstDayOfWeek);
        }
        return new HealthInterval(0, Calendar.WEEK_OF_YEAR, weeks, zone,
                firstDayOfWeek);
    }

    /// Calendar months in `zone`, aligned to local midnight on the first of
    /// the month.
    public static HealthInterval calendarMonths(int months, ZoneId zone) {
        requirePositive(months, "months");
        requireZone(zone);
        return new HealthInterval(0, Calendar.MONTH, months, zone, 0);
    }

    private static void requireZone(ZoneId zone) {
        if (zone == null) {
            throw new IllegalArgumentException(
                    "a calendar-based interval requires an explicit ZoneId");
        }
    }

    /// `true` when this interval follows the calendar rather than a fixed
    /// number of milliseconds.
    public boolean isCalendarBased() {
        return calendarField >= 0;
    }

    /// The fixed width in milliseconds, or 0 when [#isCalendarBased()].
    public long getFixedMillis() {
        return fixedMillis;
    }

    /// The time zone calendar boundaries are computed in, or null for a
    /// fixed-duration interval.
    ///
    /// The caller's own instance, not a copy. `java.util.TimeZone` is
    /// mutable in principle -- `SimpleTimeZone` has setters -- so an
    /// interval built from one and then reconfigured moves its bucket
    /// boundaries, and the same samples fall into different days. The
    /// framework cannot defend against that here: the compile target is
    /// the CLDC 1.1 subset, where `TimeZone` does not expose a public
    /// `clone()` and `getTimeZone(getID())` answers GMT for an id it does
    /// not recognise -- which would silently discard the rules of the
    /// very zone that needs defending.
    ///
    /// So it is a contract rather than a guarantee: pass a zone you do
    /// not go on to modify. `TimeZone.getTimeZone("Europe/Berlin")` and
    /// `TimeZone.getDefault()` are both fine; a `SimpleTimeZone` you keep
    /// a reference to and reconfigure is not.
    public ZoneId getZone() {
        return zone;
    }

    /// This interval as a [Duration], or **null** when it is calendar-based.
    ///
    /// A calendar day is 23, 24 or 25 hours depending on the transition it
    /// spans, so a calendar interval has no fixed length and saying otherwise
    /// would be the drift this API takes an explicit zone to avoid.
    public Duration getDuration() {
        return isCalendarBased() ? null : Duration.ofMillis(fixedMillis);
    }

    /// An interval of `length`.
    public static HealthInterval of(Duration length) {
        if (length == null) {
            throw new IllegalArgumentException("a length is required");
        }
        return millis(length.toMillis());
    }

    /// The start of the bucket that contains `millis`. For a fixed
    /// interval this is anchored on `anchorMillis`; for a calendar
    /// interval it snaps to the local period boundary and `anchorMillis`
    /// is ignored.
    public long bucketStart(long millis, long anchorMillis) {
        if (!isCalendarBased()) {
            long delta = millis - anchorMillis;
            long floored = delta - floorMod(delta, fixedMillis);
            return anchorMillis + floored;
        }
        Calendar c = HealthTimeRange.calendarIn(zone);
        c.setTime(new Date(millis));
        if (calendarField == Calendar.MONTH) {
            c.set(Calendar.DAY_OF_MONTH, 1);
        } else if (calendarField == Calendar.WEEK_OF_YEAR) {
            // Walk back to the configured first day. This is done by hand
            // rather than with setFirstDayOfWeek + set(DAY_OF_WEEK, ...):
            // the CLDC Calendar has no setFirstDayOfWeek, and set(DAY_OF_WEEK)
            // can jump forward rather than back.
            while (c.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
                c.add(Calendar.DAY_OF_MONTH, -1);
            }
        }
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime().getTime();
    }

    /// The start of the bucket after the one starting at
    /// `bucketStartMillis`. Bucket `n`'s exclusive end is bucket `n+1`'s
    /// inclusive start, so buckets tile with no gap and no overlap even
    /// when their widths differ.
    public long nextBoundary(long bucketStartMillis) {
        if (!isCalendarBased()) {
            return bucketStartMillis + fixedMillis;
        }
        Calendar c = HealthTimeRange.calendarIn(zone);
        c.setTime(new Date(bucketStartMillis));
        if (calendarField == Calendar.WEEK_OF_YEAR) {
            c.add(Calendar.DAY_OF_MONTH, 7 * calendarAmount);
        } else {
            c.add(calendarField, calendarAmount);
        }
        return c.getTime().getTime();
    }

    private static long floorMod(long x, long y) {
        long r = x % y;
        return r < 0 ? r + y : r;
    }

    @Override
    public String toString() {
        if (!isCalendarBased()) {
            return fixedMillis + "ms";
        }
        String field = calendarField == Calendar.MONTH ? "month"
                : calendarField == Calendar.WEEK_OF_YEAR ? "week" : "day";
        return calendarAmount + " " + field + "(s) in " + zone.getId();
    }
}
