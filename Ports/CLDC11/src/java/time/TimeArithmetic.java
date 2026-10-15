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
package java.time;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;

/// Exact arithmetic and unit tables the date and time classes share. The
/// exact operations throw `ArithmeticException("long overflow")` where the
/// JDK's `Math.*Exact` do, rather than wrapping around.
final class TimeArithmetic {
    static final long NANOS_PER_SECOND = 1000000000L;
    static final long SECONDS_PER_DAY = 86400L;
    static final long NANOS_PER_DAY = SECONDS_PER_DAY * NANOS_PER_SECOND;

    /// The whole seconds of each `ChronoUnit`, by ordinal; 0 below a second.
    private static final long[] UNIT_SECONDS = {0L, 0L, 0L, 1L, 60L, 3600L, 43200L, 86400L, 604800L,
        2629746L, 31556952L, 315569520L, 3155695200L, 31556952000L, 31556952000000000L, Long.MAX_VALUE};

    /// The nanoseconds in one unit below a second, by ordinal; 0 above.
    private static final long[] UNIT_NANOS = {1L, 1000L, 1000000L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
        0L, 0L, 0L};

    private TimeArithmetic() {
    }

    static long floorDiv(long a, long b) {
        long q = a / b;
        return (a % b != 0 && ((a < 0) != (b < 0))) ? q - 1 : q;
    }

    static long floorMod(long a, long b) {
        return a - floorDiv(a, b) * b;
    }

    static long multiplyExact(long a, long b) {
        long r = a * b;
        long absA = a < 0 ? -a : a;
        long absB = b < 0 ? -b : b;
        // MIN_VALUE * -1 is tested before the division: ParparVM divides in
        // C, where MIN_VALUE / -1 is undefined and traps (SIGFPE) on x86.
        if (((absA | absB) >>> 31) != 0
                && (a == Long.MIN_VALUE && b == -1 || b != 0 && r / b != a)) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    static long addExact(long a, long b) {
        long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    static long subtractExact(long a, long b) {
        long r = a - b;
        if (((a ^ b) & (a ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    static int toIntExact(long value) {
        if ((int) value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) value;
    }

    /// `unit` as a `ChronoUnit`, or the JDK's refusal of a unit it cannot add.
    static ChronoUnit chrono(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return (ChronoUnit) unit;
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        throw unsupported(unit);
    }

    static UnsupportedTemporalTypeException unsupported(Object unit) {
        return new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    /// The whole seconds in one `unit`, 0 for the units shorter than a second.
    static long seconds(ChronoUnit unit) {
        return UNIT_SECONDS[unit.ordinal()];
    }

    /// The nanoseconds in one `unit` shorter than a second, 0 for the others.
    static long nanos(ChronoUnit unit) {
        return UNIT_NANOS[unit.ordinal()];
    }

    /// The whole units between two moments given as seconds and nanoseconds,
    /// the fraction dropped towards zero.
    static long timeUntil(long s1, long n1, long s2, long n2, ChronoUnit unit) {
        long seconds = s2 - s1;
        long nanos = n2 - n1;
        if (seconds > 0 && nanos < 0) {
            seconds--;
            nanos += NANOS_PER_SECOND;
        } else if (seconds < 0 && nanos > 0) {
            seconds++;
            nanos -= NANOS_PER_SECOND;
        }
        long perNanos = nanos(unit);
        if (perNanos != 0) {
            return addExact(multiplyExact(seconds, NANOS_PER_SECOND / perNanos), nanos / perNanos);
        }
        return seconds / seconds(unit);
    }

    /// The nanosecond of the second of a value that has a time, 0 for one
    /// that has none.
    static int nanoOfSecond(Object value) {
        if (value instanceof Instant) {
            return ((Instant) value).getNano();
        }
        if (value instanceof LocalTime) {
            return ((LocalTime) value).getNano();
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).getNano();
        }
        if (value instanceof ZonedDateTime) {
            return ((ZonedDateTime) value).getNano();
        }
        if (value instanceof OffsetDateTime) {
            return ((OffsetDateTime) value).toLocalDateTime().getNano();
        }
        return 0;
    }

    /// `value`, or the JDK's `DateTimeException` for a field outside
    /// `min` to `max`.
    static long check(String field, long min, long max, long value) {
        if (value < min || value > max) {
            throw new DateTimeException("Invalid value for " + field + " (valid values " + min + " - " + max
                    + "): " + value);
        }
        return value;
    }

    static DateTimeException instantOutOfRange() {
        return new DateTimeException("Instant exceeds minimum or maximum instant");
    }

    /// What `parse(text, formatter)` throws when the text was read but does
    /// not make the value asked for.
    static java.time.format.DateTimeParseException unparsable(CharSequence text, DateTimeException cause) {
        return new java.time.format.DateTimeParseException("Text '" + text + "' could not be parsed: "
                + cause.getMessage(), text, 0, cause);
    }
}
