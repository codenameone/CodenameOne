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

/**
 * Static helpers for Dart's {@code Comparable}. Dart exposes
 * {@code Comparable.compare(a, b)} as a static combinator; it delegates to the
 * receivers' {@code compareTo}. Instances map onto {@link java.lang.Comparable}.
 */
public final class DartComparable {

    private DartComparable() {
    }

    /**
     * {@code Comparable.compare}: returns a negative value, zero, or a positive
     * value as {@code a} orders before, equal to, or after {@code b}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static long compare(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return compareNumbers((Number) a, (Number) b);
        }
        if (a instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        }
        throw new TypeError("type '" + (a == null ? "Null" : a.getClass().getName())
                + "' is not a subtype of type 'Comparable'");
    }

    /**
     * Dart's {@code num.compareTo} across int and double. Java's wrappers only
     * compare within their own class, so a List&lt;num&gt; holding 2 and 1.5
     * threw ClassCastException from Long.compareTo(Double) instead of sorting.
     * Double.compare gives Dart's order between two doubles: -0.0 before 0.0,
     * NaN after everything. An int against a double is compared exactly, as
     * the Dart VM does: widening the int first rounds 2^53 + 1 onto the double
     * 2^53 and reported them equal.
     */
    private static long compareNumbers(Number a, Number b) {
        boolean ai = isIntegral(a);
        boolean bi = isIntegral(b);
        if (ai && bi) {
            long x = a.longValue();
            long y = b.longValue();
            return x < y ? -1 : x > y ? 1 : 0;
        }
        if (!ai && !bi) {
            return Double.compare(a.doubleValue(), b.doubleValue());
        }
        long l = ai ? a.longValue() : b.longValue();
        double d = ai ? b.doubleValue() : a.doubleValue();
        int c;
        if (d != d) {
            c = -1;                             // every int orders before NaN
        } else {
            c = compareIntDouble(l, d);
            if (c == 0 && l == 0 && 1 / d < 0) {
                c = 1;                          // 0.compareTo(-0.0) is 1
            }
        }
        return ai ? c : -c;
    }

    /**
     * The numeric order of an int and a non-NaN double, without widening the
     * int: -1, 0 or 1 as {@code l} is below, equal to or above {@code d}. Zero
     * and negative zero are the same value here; callers that order them
     * (compareTo, but not the relational operators) do that themselves.
     */
    public static int compareIntDouble(long l, double d) {
        // [-2^63, 2^63) is the range of doubles a long can hold; outside it the
        // (long) cast below would saturate and compare wrongly.
        if (d >= 9.223372036854775808E18) {
            return -1;
        }
        if (d < -9.223372036854775808E18) {
            return 1;
        }
        double f = Math.floor(d);
        long fl = (long) f;
        if (l != fl) {
            return l < fl ? -1 : 1;
        }
        return d > f ? -1 : 0;                  // l == floor(d): below d iff d has a fraction
    }

    private static boolean isIntegral(Number n) {
        return n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte;
    }
}
