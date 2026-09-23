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
 * it. Field access is computed on demand through {@link java.util.Calendar}.
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

    private static long build(long year, long month, long day, long hour, long minute,
                              long second, long millisecond, boolean utc) {
        // CN1's Calendar has no clear(); every time-carrying field is set
        // explicitly so no residual "now" component leaks in.
        Calendar c = utc ? Calendar.getInstance(TimeZone.getTimeZone("UTC")) : Calendar.getInstance();
        c.set(Calendar.YEAR, (int) year);
        // Passed through unclamped: out-of-range components are how Dart spells
        // calendar arithmetic. DateTime(y, m + 1, 0) is the last day of month m and
        // month 0 is the previous December, and the lenient Calendar normalizes
        // them exactly as Dart does. An omitted month or day arrives as 1, not 0,
        // because the dart:core stub declares Dart's own defaults.
        c.set(Calendar.MONTH, (int) month - 1);
        c.set(Calendar.DAY_OF_MONTH, (int) day);
        c.set(Calendar.HOUR_OF_DAY, (int) hour);
        c.set(Calendar.MINUTE, (int) minute);
        c.set(Calendar.SECOND, (int) second);
        c.set(Calendar.MILLISECOND, (int) millisecond);
        return c.getTime().getTime();
    }

    private int field(int f) {
        Calendar c = utc ? Calendar.getInstance(TimeZone.getTimeZone("UTC")) : Calendar.getInstance();
        c.setTime(new Date(epochMillis()));
        return c.get(f);
    }

    public long year() {
        return field(Calendar.YEAR);
    }

    public long month() {
        return field(Calendar.MONTH) + 1;
    }

    public long day() {
        return field(Calendar.DAY_OF_MONTH);
    }

    public long hour() {
        return field(Calendar.HOUR_OF_DAY);
    }

    public long minute() {
        return field(Calendar.MINUTE);
    }

    public long second() {
        return field(Calendar.SECOND);
    }

    public long millisecond() {
        return field(Calendar.MILLISECOND);
    }

    public long microsecond() {
        return epochMicros - epochMillis() * 1000L;
    }

    /** Dart weekday: Monday == 1 .. Sunday == 7. */
    public long weekday() {
        int calDow = field(Calendar.DAY_OF_WEEK); // SUNDAY==1 .. SATURDAY==7
        return ((calDow + 5) % 7) + 1;
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
