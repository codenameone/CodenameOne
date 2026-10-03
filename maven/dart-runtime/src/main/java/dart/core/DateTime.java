/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Dart's {@code dart:core} DateTime: an instant on the timeline, stored as
 * microseconds since the Unix epoch plus a UTC/local flag, as the Dart VM keeps
 * it. Fields are computed on demand in the proleptic Gregorian calendar, as Dart's
 * are; a local value takes its zone offset from {@link java.util.Calendar}.
 *
 * <p>It used to keep milliseconds and discard the microsecond argument, so two
 * instants a microsecond apart compared equal, differed by zero, and a
 * sub-millisecond Duration added nothing.</p>
 */
public final class DateTime {

    private final long epochMicros;
    private final boolean utc;

    private DateTime(long epochMicros, boolean utc) {
        this.epochMicros = epochMicros;
        this.utc = utc;
    }

    /** Local-time constructor mirroring {@code DateTime(year, [month, day, ...])}. */
    public DateTime(long year, long month, long day, long hour, long minute,
                    long second, long millisecond, long microsecond) {
        this(build(year, month, day, hour, minute, second, millisecond, false) * 1000L + microsecond, false);
    }

    public static DateTime now() {
        return new DateTime(System.currentTimeMillis() * 1000L, false);
    }

    /** UTC constructor mirroring {@code DateTime.utc(year, [month, day, ...])}. */
    public static DateTime utc(long year, long month, long day, long hour, long minute,
                               long second, long millisecond, long microsecond) {
        return new DateTime(build(year, month, day, hour, minute, second, millisecond, true) * 1000L
                + microsecond, true);
    }

    public static DateTime fromMillisecondsSinceEpoch(long millisecondsSinceEpoch, boolean isUtc) {
        return new DateTime(millisecondsSinceEpoch * 1000L, isUtc);
    }

    public static DateTime fromMicrosecondsSinceEpoch(long microsecondsSinceEpoch, boolean isUtc) {
        return new DateTime(microsecondsSinceEpoch, isUtc);
    }

    /** Milliseconds since the epoch, rounded toward negative infinity as Dart does. */
    private long epochMillis() {
        long q = epochMicros / 1000L;
        return (epochMicros % 1000L != 0 && epochMicros < 0) ? q - 1 : q;
    }

    /** Milliseconds in a day. */
    private static final long DAY_MS = 86400000L;

    /**
     * 1583-01-01T00:00Z. From here on java.util.Calendar's default Gregorian rules and
     * Dart's agree; before it Calendar switches to the Julian calendar at its 1582
     * cutover while Dart stays proleptic Gregorian, so DateTime.utc(1582, 10, 10)
     * came out as October 20 -- every field, the epoch value and every comparison
     * ten days off.
     */
    private static final long GREGORIAN_AGREES_MS = daysFromCivil(1583, 1, 1) * DAY_MS;

    private static long build(long year, long month, long day, long hour, long minute,
                              long second, long millisecond, boolean utc) {
        // Out-of-range components are how Dart spells calendar arithmetic:
        // DateTime(y, m + 1, 0) is the last day of month m and month 0 is the
        // previous December. They are normalized here arithmetically. An omitted
        // month or day arrives as 1, not 0, because the dart:core stub declares
        // Dart's own defaults.
        long y = year + floorDiv(month - 1, 12);
        long m = floorMod(month - 1, 12) + 1;
        long wall = (daysFromCivil(y, m, 1) + day - 1) * DAY_MS
                + hour * 3600000L + minute * 60000L + second * 1000L + millisecond;
        if (utc) {
            return wall;
        }
        if (wall - 2 * DAY_MS < GREGORIAN_AGREES_MS) {
            // No zone kept daylight time this early, so the raw offset is the offset.
            return wall - TimeZone.getDefault().getRawOffset();
        }
        return calendarBuild(year, month, day, hour, minute, second, millisecond);
    }

    /** A local time through the device's Calendar, which knows its daylight-time rules. */
    private static long calendarBuild(long year, long month, long day, long hour, long minute,
                                      long second, long millisecond) {
        // CN1's Calendar has no clear(); every time-carrying field is set
        // explicitly so no residual "now" component leaks in. The lenient
        // Calendar normalizes out-of-range components as Dart does.
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, (int) year);
        c.set(Calendar.MONTH, (int) month - 1);
        c.set(Calendar.DAY_OF_MONTH, (int) day);
        c.set(Calendar.HOUR_OF_DAY, (int) hour);
        c.set(Calendar.MINUTE, (int) minute);
        c.set(Calendar.SECOND, (int) second);
        c.set(Calendar.MILLISECOND, (int) millisecond);
        return c.getTime().getTime();
    }

    /**
     * This instant's wall-clock milliseconds: the instant itself in UTC, shifted by
     * the zone's offset for a local value. The offset of a modern local instant is
     * read back from the Calendar that applies the zone's daylight-time rules.
     */
    private long wallMillis() {
        long ms = epochMillis();
        if (utc) {
            return ms;
        }
        if (ms - 2 * DAY_MS < GREGORIAN_AGREES_MS) {
            return ms + TimeZone.getDefault().getRawOffset();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(ms));
        long localAsUtc = daysFromCivil(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH)) * DAY_MS
                + c.get(Calendar.HOUR_OF_DAY) * 3600000L + c.get(Calendar.MINUTE) * 60000L
                + c.get(Calendar.SECOND) * 1000L + c.get(Calendar.MILLISECOND);
        return localAsUtc;
    }

    /** {year, month, day} of the wall-clock day, proleptic Gregorian. */
    private long[] date() {
        return civilFromDays(floorDiv(wallMillis(), DAY_MS));
    }

    private long timeOfDay() {
        return floorMod(wallMillis(), DAY_MS);
    }

    /**
     * Days since 1970-01-01 of a proleptic Gregorian date, month 1..12 (Howard
     * Hinnant's days_from_civil).
     */
    static long daysFromCivil(long y, long m, long d) {
        y -= m <= 2 ? 1 : 0;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    /** The inverse of {@link #daysFromCivil}: {year, month, day}. */
    static long[] civilFromDays(long z) {
        z += 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        return new long[] {yoe + era * 400 + (m <= 2 ? 1 : 0), m, d};
    }

    private static long floorDiv(long a, long b) {
        long q = a / b;
        return (a % b != 0 && (a < 0) != (b < 0)) ? q - 1 : q;
    }

    private static long floorMod(long a, long b) {
        return a - floorDiv(a, b) * b;
    }

    public long year() {
        return date()[0];
    }

    public long month() {
        return date()[1];
    }

    public long day() {
        return date()[2];
    }

    public long hour() {
        return timeOfDay() / 3600000L;
    }

    public long minute() {
        return timeOfDay() / 60000L % 60;
    }

    public long second() {
        return timeOfDay() / 1000L % 60;
    }

    public long millisecond() {
        return timeOfDay() % 1000L;
    }

    public long microsecond() {
        return epochMicros - epochMillis() * 1000L;
    }

    /** Dart weekday: Monday == 1 .. Sunday == 7. */
    public long weekday() {
        // 1970-01-01 was a Thursday, weekday 4.
        return floorMod(floorDiv(wallMillis(), DAY_MS) + 3, 7) + 1;
    }

    public long millisecondsSinceEpoch() {
        return epochMillis();
    }

    public long microsecondsSinceEpoch() {
        return epochMicros;
    }

    public DateTime add(Duration duration) {
        return new DateTime(epochMicros + duration.inMicroseconds(), utc);
    }

    public DateTime subtract(Duration duration) {
        return new DateTime(epochMicros - duration.inMicroseconds(), utc);
    }

    public Duration difference(DateTime other) {
        return Duration.ofMicroseconds(epochMicros - other.epochMicros);
    }

    public boolean isBefore(DateTime other) {
        return epochMicros < other.epochMicros;
    }

    public boolean isAfter(DateTime other) {
        return epochMicros > other.epochMicros;
    }

    public boolean isAtSameMomentAs(DateTime other) {
        return epochMicros == other.epochMicros;
    }

    public DateTime toLocal() {
        return utc ? new DateTime(epochMicros, false) : this;
    }

    public DateTime toUtc() {
        return utc ? this : new DateTime(epochMicros, true);
    }

    public long compareTo(DateTime other) {
        return epochMicros < other.epochMicros ? -1 : (epochMicros > other.epochMicros ? 1 : 0);
    }

    /** The underlying instant as a {@link java.util.Date} (used by DateFormat). */
    public Date toJavaDate() {
        return new Date(epochMillis());
    }

    public boolean isUtc() {
        return utc;
    }

    @Override
    public boolean equals(Object o) {
        // The instant AND the time-zone mode, as Dart's operator == compares:
        // a local value and its toUtc() are the same moment but not equal.
        // isAtSameMomentAs is the instant-only comparison. Comparing instants
        // alone made them equal here, so a local and a UTC value collapsed into
        // one map or set key.
        return o instanceof DateTime && ((DateTime) o).epochMicros == epochMicros
                && ((DateTime) o).utc == utc;
    }

    @Override
    public int hashCode() {
        return (int) (epochMicros ^ (epochMicros >>> 32));
    }

    @Override
    /// Dart's own format: {@code 2024-01-15 10:30:00.000}, and for a UTC value the
    /// same with a trailing {@code Z}. This returned java.util.Date's locale text in
    /// the host's time zone, so one transpiled value printed differently on each
    /// device, a UTC value lost its Z, and nothing expecting a Dart date string could
    /// parse it back. Microseconds print as three more digits when they are not zero,
    /// as Dart prints them.
    public String toString() {
        long micros = microsecond();
        String text = fourDigits(year()) + "-" + twoDigits(month()) + "-" + twoDigits(day())
                + " " + twoDigits(hour()) + ":" + twoDigits(minute()) + ":" + twoDigits(second())
                + "." + threeDigits(millisecond()) + (micros == 0 ? "" : threeDigits(micros));
        return utc ? text + "Z" : text;
    }

    private static String fourDigits(long n) {
        long abs = Math.abs(n);
        String sign = n < 0 ? "-" : "";
        if (abs >= 1000) {
            return "" + n;
        }
        if (abs >= 100) {
            return sign + "0" + abs;
        }
        if (abs >= 10) {
            return sign + "00" + abs;
        }
        return sign + "000" + abs;
    }

    private static String threeDigits(long n) {
        if (n >= 100) {
            return "" + n;
        }
        if (n >= 10) {
            return "0" + n;
        }
        return "00" + n;
    }

    private static String twoDigits(long n) {
        return n >= 10 ? "" + n : "0" + n;
    }
}
