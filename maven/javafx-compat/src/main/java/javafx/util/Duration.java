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
package javafx.util;

import java.io.Serializable;

import javafx.beans.NamedArg;

/// An immutable length of time, kept in milliseconds. It may be negative,
/// [#INDEFINITE] (infinitely long) or [#UNKNOWN] (not a number).
public class Duration implements Comparable<Duration>, Serializable {

    private static final long serialVersionUID = 1L;

    /// No time at all.
    public static final Duration ZERO = new Duration(0);

    /// One millisecond.
    public static final Duration ONE = new Duration(1);

    /// An infinitely long time.
    public static final Duration INDEFINITE = new Duration(Double.POSITIVE_INFINITY);

    /// A time that is not known.
    public static final Duration UNKNOWN = new Duration(Double.NaN);

    private final double millis;

    /// Creates a duration of a number of milliseconds.
    public Duration(@NamedArg("millis") double millis) {
        this.millis = millis;
    }

    /// Parses a number followed directly by a unit: `ms`, `s`, `m` or `h`,
    /// in either case. `"1.5s"` is one and a half seconds.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the text is not such a value
    public static Duration valueOf(String time) {
        if (time == null) {
            throw new NullPointerException("time");
        }
        int split = -1;
        for (int i = 0; i < time.length(); i++) {
            char c = time.charAt(i);
            if (!(c >= '0' && c <= '9') && c != '.' && c != '-') {
                split = i;
                break;
            }
        }
        if (split == -1) {
            throw new IllegalArgumentException("The time parameter must have a suffix of [ms|s|m|h]");
        }
        double value;
        try {
            value = Double.parseDouble(time.substring(0, split));
        } catch (NumberFormatException malformed) {
            throw new IllegalArgumentException("The time parameter must be a number followed by [ms|s|m|h]");
        }
        // Units are ASCII by definition, so they are folded by hand: the
        // locale sensitive String.toLowerCase() would turn "S" into something
        // else on a Turkish device.
        String unit = asciiLower(time.substring(split));
        if ("ms".equals(unit)) {
            return millis(value);
        }
        if ("s".equals(unit)) {
            return seconds(value);
        }
        if ("m".equals(unit)) {
            return minutes(value);
        }
        if ("h".equals(unit)) {
            return hours(value);
        }
        throw new IllegalArgumentException("The time parameter must have a suffix of [ms|s|m|h]");
    }

    private static String asciiLower(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            result.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return result.toString();
    }

    /// Returns a duration of a number of milliseconds.
    public static Duration millis(double ms) {
        if (ms == 0) {
            return ZERO;
        }
        if (ms == 1) {
            return ONE;
        }
        if (ms == Double.POSITIVE_INFINITY) {
            return INDEFINITE;
        }
        if (Double.isNaN(ms)) {
            return UNKNOWN;
        }
        return new Duration(ms);
    }

    /// Returns a duration of a number of seconds.
    public static Duration seconds(double s) {
        return millis(s * 1000.0);
    }

    /// Returns a duration of a number of minutes.
    public static Duration minutes(double m) {
        return millis(m * (1000.0 * 60.0));
    }

    /// Returns a duration of a number of hours.
    public static Duration hours(double h) {
        return millis(h * (1000.0 * 60.0 * 60.0));
    }

    /// Returns the length in milliseconds.
    public double toMillis() {
        return millis;
    }

    /// Returns the length in seconds.
    public double toSeconds() {
        return millis / 1000.0;
    }

    /// Returns the length in minutes.
    public double toMinutes() {
        return millis / (60 * 1000.0);
    }

    /// Returns the length in hours.
    public double toHours() {
        return millis / (60 * 60 * 1000.0);
    }

    /// Returns the sum of this duration and another.
    public Duration add(Duration other) {
        return millis(millis + other.millis);
    }

    /// Returns this duration less another.
    public Duration subtract(Duration other) {
        return millis(millis - other.millis);
    }

    /// Returns the product of the two lengths in milliseconds.
    ///
    /// #### Deprecated
    ///
    /// Multiplying two durations has no meaning; use [#multiply(double)].
    @Deprecated
    public Duration multiply(Duration other) {
        return millis(millis * other.millis);
    }

    /// Returns this duration scaled by a factor.
    public Duration multiply(double n) {
        return millis(millis * n);
    }

    /// Returns this duration divided by a number.
    public Duration divide(double n) {
        return millis(millis / n);
    }

    /// Returns the quotient of the two lengths in milliseconds.
    ///
    /// #### Deprecated
    ///
    /// The result is not a duration; divide [#toMillis()] values instead.
    @Deprecated
    public Duration divide(Duration other) {
        return millis(millis / other.millis);
    }

    /// Returns the duration of the same length and opposite sign.
    public Duration negate() {
        return millis(-millis);
    }

    /// Returns whether this is infinitely long.
    public boolean isIndefinite() {
        return millis == Double.POSITIVE_INFINITY;
    }

    /// Returns whether the length is not a number.
    public boolean isUnknown() {
        return Double.isNaN(millis);
    }

    /// Returns whether this is shorter than another duration.
    public boolean lessThan(Duration other) {
        return millis < other.millis;
    }

    /// Returns whether this is not longer than another duration.
    public boolean lessThanOrEqualTo(Duration other) {
        return millis <= other.millis;
    }

    /// Returns whether this is longer than another duration.
    public boolean greaterThan(Duration other) {
        return millis > other.millis;
    }

    /// Returns whether this is not shorter than another duration.
    public boolean greaterThanOrEqualTo(Duration other) {
        return millis >= other.millis;
    }

    /// Returns `INDEFINITE`, `UNKNOWN` or the milliseconds followed by
    /// ` ms`.
    @Override
    public String toString() {
        return isIndefinite() ? "INDEFINITE" : (isUnknown() ? "UNKNOWN" : millis + " ms");
    }

    @Override
    public int compareTo(Duration d) {
        return Double.compare(millis, d.millis);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj instanceof Duration && Double.compare(((Duration) obj).millis, millis) == 0;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(millis);
        return (int) (bits ^ (bits >>> 32));
    }
}
