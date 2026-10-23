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
        this(checkedMicros(millisToMicros(build(year, month, day, hour, minute, second, millisecond, false)),
                microsecond), false);
    }

    public static DateTime now() {
        return new DateTime(System.currentTimeMillis() * 1000L, false);
    }

    /** UTC constructor mirroring {@code DateTime.utc(year, [month, day, ...])}. */
    public static DateTime utc(long year, long month, long day, long hour, long minute,
                               long second, long millisecond, long microsecond) {
        return new DateTime(checkedMicros(millisToMicros(build(year, month, day, hour, minute, second,
                millisecond, true)), microsecond), true);
    }

    /// Dart's own grammar for DateTime.parse: an ISO 8601 date, an optional time
    /// after `T` or a space, an optional fraction, and an optional `Z` or offset.
    private static final com.codename1.util.regex.Pattern ISO = com.codename1.util.regex.Pattern.compile(
            "^([+-]?\\d{4,6})-?(\\d\\d)-?(\\d\\d)(?:[ T](\\d\\d)(?::?(\\d\\d)(?::?(\\d\\d)(?:[.,](\\d+))?)?)?"
            + "( ?[zZ]| ?([-+])(\\d\\d)(?::?(\\d\\d))?)?)?$");

    /// `DateTime.parse`: a value with a `Z` or an offset is UTC, anything else local.
    public static DateTime parse(String formattedString) {
        DateTime d = tryParse(formattedString);
        if (d == null) {
            throw new FormatException("Invalid date format\n" + formattedString);
        }
        return d;
    }

    public static DateTime tryParse(String formattedString) {
        if (formattedString == null) {
            return null;
        }
        com.codename1.util.regex.Matcher m = ISO.matcher(formattedString);
        if (!m.matches()) {
            return null;
        }
        long year = Long.parseLong(m.group(1).startsWith("+") ? m.group(1).substring(1) : m.group(1));
        long month = Long.parseLong(m.group(2));
        long day = Long.parseLong(m.group(3));
        long hour = num(m.group(4));
        long minute = num(m.group(5));
        long second = num(m.group(6));
        long milli = 0;
        long micro = 0;
        String frac = m.group(7);
        if (frac != null) {
            String six = (frac + "000000").substring(0, 6);
            long all = Long.parseLong(six);
            // Digits past the sixth are dropped, not rounded, as the Dart VM does:
            // .9999999 stays in the same second.
            milli = all / 1000;
            micro = all % 1000;
        }
        // A well-formed date outside DateTime's range is still unparseable: tryParse
        // answers null (and parse a FormatException), never the constructor's error.
        try {
            if (m.group(8) == null) {
                return new DateTime(year, month, day, hour, minute, second, milli, micro);
            }
            DateTime u = utc(year, month, day, hour, minute, second, milli, micro);
            if (m.group(9) != null) {
                long offset = num(m.group(10)) * 60 + num(m.group(11));
                long sign = m.group(9).equals("-") ? -1 : 1;
                long at = u.epochMicros - sign * offset * 60000000L;
                checkMicrosRange(at);
                return new DateTime(at, true);
            }
            return u;
        } catch (ArgumentError outOfRange) {
            return null;
        }
    }

    private static long num(String s) {
        return s == null ? 0 : Long.parseLong(s);
    }

    /// `toIso8601String`: `2024-01-15T10:30:00.000`, `Z` added for a UTC value.
    public String toIso8601String() {
        long y = year();
        String ys = y >= -9999 && y <= 9999 ? fourDigits(y) : (y < 0 ? "-" : "+") + sixDigits(Math.abs(y));
        long micros = microsecond();
        String text = ys + "-" + twoDigits(month()) + "-" + twoDigits(day())
                + "T" + twoDigits(hour()) + ":" + twoDigits(minute()) + ":" + twoDigits(second())
                + "." + threeDigits(millisecond()) + (micros == 0 ? "" : threeDigits(micros));
        return utc ? text + "Z" : text;
    }

    private static String sixDigits(long n) {
        String s = Long.toString(n);
        while (s.length() < 6) {
            s = "0" + s;
        }
        return s;
    }

    public static DateTime fromMillisecondsSinceEpoch(long millisecondsSinceEpoch, boolean isUtc) {
        return new DateTime(millisToMicros(millisecondsSinceEpoch), isUtc);
    }

    public static DateTime fromMicrosecondsSinceEpoch(long microsecondsSinceEpoch, boolean isUtc) {
        checkMicrosRange(microsecondsSinceEpoch);
        return new DateTime(microsecondsSinceEpoch, isUtc);
    }

    /**
     * Dart's DateTime is valid for +/-100,000,000 days from the epoch, i.e.
     * +/-8,640,000,000,000,000 milliseconds (the same bound the dart:core VM
     * enforces, named {@code _maxMillisecondsSinceEpoch} there). This has to be
     * checked BEFORE multiplying by 1000 to get microseconds: multiplying first
     * and checking after lets a huge millisecond value (Long.MAX_VALUE, say)
     * silently wrap through long overflow into a small, in-range microsecond
     * value, so the resulting instant lands near the epoch instead of failing --
     * every caller of this conversion goes through here rather than writing the
     * multiply inline.
     */
    private static final long MAX_MILLISECONDS_SINCE_EPOCH = 8640000000000000L;

    private static long millisToMicros(long milliseconds) {
        if (milliseconds > MAX_MILLISECONDS_SINCE_EPOCH || milliseconds < -MAX_MILLISECONDS_SINCE_EPOCH) {
            throw new ArgumentError("DateTime is outside valid range: " + milliseconds);
        }
        return milliseconds * 1000L;
    }

    /** Same bound as {@link #millisToMicros}, expressed in microseconds, for the
     * constructor ({@link #fromMicrosecondsSinceEpoch}) that never multiplies and so
     * cannot overflow, but still must not silently accept a value Dart itself rejects.
     */
    private static final long MAX_MICROSECONDS_SINCE_EPOCH = MAX_MILLISECONDS_SINCE_EPOCH * 1000L;

    private static void checkMicrosRange(long microseconds) {
        if (microseconds > MAX_MICROSECONDS_SINCE_EPOCH || microseconds < -MAX_MICROSECONDS_SINCE_EPOCH) {
            throw new ArgumentError("DateTime is outside valid range: " + microseconds / 1000L);
        }
    }

    /**
     * {@code base + delta} microseconds, validated the way Dart's VM validates every
     * DateTime it builds (add, subtract and the field constructors' microsecond argument
     * all go through its checking constructor). {@code base} is already in range
     * (+/-8.64e18), so the sum overflows a long only when {@code delta} is huge; that is
     * detected from the signs (both operands share a sign the result lacks) rather than
     * with Math.addExact, which the device class libraries do not all provide. An
     * overflowed sum is out of Dart's range by definition.
     */
    private static long checkedMicros(long base, long delta) {
        long sum = base + delta;
        if (((base ^ sum) & (delta ^ sum)) < 0) {
            throw new ArgumentError("DateTime is outside valid range");
        }
        checkMicrosRange(sum);
        return sum;
    }

    /**
     * {@code a + b}, checked for overflow the same way {@link #checkedMicros} checks
     * its sum (both operands share a sign the result lacks -- no Math.addExact, which
     * the device class libraries do not all provide). Used by {@link #build}, whose
     * inputs are raw constructor arguments -- not yet known to be anywhere near Dart's
     * valid range -- rather than an already-checked instant plus a bounded delta.
     */
    private static long checkedAdd(long a, long b) {
        long sum = a + b;
        if (((a ^ sum) & (b ^ sum)) < 0) {
            throw new ArgumentError("DateTime is outside valid range");
        }
        return sum;
    }

    /**
     * {@code a * b}, checked for overflow by hand (no Math.multiplyExact, for the same
     * reason as {@link #checkedAdd}): a nonzero {@code a} whose product divides back
     * to {@code b} did not overflow, except for the one case division cannot catch --
     * {@code Long.MIN_VALUE * -1}, which "divides back" cleanly only because
     * {@code Long.MIN_VALUE / -1} overflows the identical way -- so that pair is
     * rejected outright, in either argument order.
     */
    private static long checkedMultiply(long a, long b) {
        if ((a == -1L && b == Long.MIN_VALUE) || (a == Long.MIN_VALUE && b == -1L)) {
            throw new ArgumentError("DateTime is outside valid range");
        }
        long product = a * b;
        if (a != 0 && product / a != b) {
            throw new ArgumentError("DateTime is outside valid range");
        }
        return product;
    }

    /**
     * A ceiling on the year {@link #build} will hand to {@link #daysFromCivil}, well
     * short of where that method's own arithmetic (era * 146097) would overflow a long.
     * Every OTHER caller of {@code daysFromCivil} already passes a year inside Dart's
     * real valid range (+/-100,000,000 days, about +/-273,785 years) and so never needs
     * this -- {@code build} is the one call site fed raw, unvalidated constructor
     * arguments, where a huge year (say {@code DateTime.utc(Long.MAX_VALUE, 1, 1, 0, 0,
     * 0, 0, 0)}) used to overflow silently through that multiply and wrap into a value
     * that looked like a plausible date instead of failing. This bound is still an order
     * of magnitude looser than Dart's real range, so no legitimate date trips it.
     */
    private static final long MAX_BUILD_YEAR = 3_000_000L;

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
        long y = checkedAdd(year, floorDiv(month - 1, 12));
        long m = floorMod(month - 1, 12) + 1;
        if (y > MAX_BUILD_YEAR || y < -MAX_BUILD_YEAR) {
            throw new ArgumentError("DateTime is outside valid range: year " + year);
        }
        // Every operation from here on is checked: a huge day, hour, minute,
        // second or millisecond either combines into the true (and then
        // out-of-range) result, which the caller's own range check rejects,
        // or overflows a long outright, which these throw on directly --
        // either way, never a silent wraparound into something plausible.
        long days = checkedAdd(daysFromCivil(y, m, 1), checkedAdd(day, -1L));
        long wall = checkedMultiply(days, DAY_MS);
        wall = checkedAdd(wall, checkedMultiply(hour, 3600000L));
        wall = checkedAdd(wall, checkedMultiply(minute, 60000L));
        wall = checkedAdd(wall, checkedMultiply(second, 1000L));
        wall = checkedAdd(wall, millisecond);
        if (utc) {
            return wall;
        }
        if (wall - 2 * DAY_MS < GREGORIAN_AGREES_MS) {
            // No zone kept daylight time this early, so the raw offset is the offset.
            return wall - TimeZone.getDefault().getRawOffset();
        }
        return calendarBuild(wall);
    }

    /**
     * A local time through the device's Calendar, which knows its daylight-time rules.
     * Takes the already fully-normalized wall-clock value ({@code wall}, built above by
     * checked long arithmetic that never narrows) rather than the caller's raw
     * year/month/day/hour/minute/second/millisecond. Those are Dart's own out-of-range
     * calendar-arithmetic idiom and can be arbitrarily large -- DateTime(1970, 1, 1,
     * 2_200_000_000) is a real example, about 91.7 million days past the epoch -- and this
     * used to hand them to {@code Calendar.set(int, int)} with a plain {@code (int)} cast.
     * A hugely out-of-range {@code long} narrows by truncating to its low 32 bits rather
     * than normalizing, so the cast silently produced a small, wrong value (often negative)
     * instead of the carried-over day/month/year Dart's semantics require.
     *
     * <p>{@code wall} decomposes here into a day count and a time-of-day ({@code
     * floorDiv}/{@code floorMod}) and then into year/month/day ({@link #civilFromDays}, long
     * arithmetic throughout), so narrowing happens only once every component is already
     * correct. The year is the sole component that could in principle still be too big for
     * an {@code int}, and every overflow check upstream ({@code checkedAdd}/{@code
     * checkedMultiply} in {@link #build}, plus {@code MAX_BUILD_YEAR}) bounds the day count
     * {@code wall} can carry to well under {@code Integer.MAX_VALUE} years -- and any
     * negative (BC) year takes the earlier, Calendar-free "ancient" branch in {@link
     * #build}, never reaching here -- so the narrowing below is always safe once it happens
     * this late.</p>
     */
    private static long calendarBuild(long wall) {
        long days = floorDiv(wall, DAY_MS);
        long timeOfDay = floorMod(wall, DAY_MS);
        long[] ymd = civilFromDays(days);
        // CN1's Calendar has no clear(); every time-carrying field is set
        // explicitly so no residual "now" component leaks in.
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, (int) ymd[0]);
        c.set(Calendar.MONTH, (int) ymd[1] - 1);
        c.set(Calendar.DAY_OF_MONTH, (int) ymd[2]);
        c.set(Calendar.HOUR_OF_DAY, (int) (timeOfDay / 3600000L));
        c.set(Calendar.MINUTE, (int) (timeOfDay / 60000L % 60L));
        c.set(Calendar.SECOND, (int) (timeOfDay / 1000L % 60L));
        c.set(Calendar.MILLISECOND, (int) (timeOfDay % 1000L));
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
        return new DateTime(checkedMicros(epochMicros, duration.inMicroseconds()), utc);
    }

    public DateTime subtract(Duration duration) {
        long micros = duration.inMicroseconds();
        // Negating Long.MIN_VALUE wraps to itself; no in-range DateTime survives
        // subtracting it anyway.
        if (micros == Long.MIN_VALUE) {
            throw new ArgumentError("DateTime is outside valid range");
        }
        return new DateTime(checkedMicros(epochMicros, -micros), utc);
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
