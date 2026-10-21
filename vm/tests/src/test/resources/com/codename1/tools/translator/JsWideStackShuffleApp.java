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

/// Category-2 stack shuffles used as expressions. A long or a double is two JVM
/// slots but one value, and javac moves it with `DUP2`, `DUP2_X1`, `DUP2_X2` and
/// `POP2` whenever a compound assignment or an increment is itself used as a
/// value, or a wide result is discarded.
public class JsWideStackShuffleApp {
    public static int result;
    static long staticTotal = 100L;

    long total = 1000L;
    double ratio = 1.5d;

    static long wide(long v) {
        return v * 3L;
    }

    static double wideDouble(double v) {
        return v * 2d;
    }

    // Every statement here needs one of the wide shuffles. The guard is what
    // matters to the test: a method with no branch at all is emitted as straight
    // line code and never reaches the pc-switch emitter, whatever the test asks.
    long shuffle(long[] longs, double[] doubles, int i, long n) {
        if (i < 0) {
            return -1L;
        }
        // DUP2 (array and index), LALOAD, LADD, DUP2_X2, LASTORE
        long a = (longs[i] += n);
        // DUP2 (array and index), DALOAD, DADD, DUP2_X2, DASTORE
        double b = (doubles[i] += 0.25d);
        // DUP, GETFIELD, LADD, DUP2_X1, PUTFIELD
        long c = (this.total += n);
        double d = (this.ratio *= 4d);
        // GETSTATIC, LADD, DUP2, PUTSTATIC
        long e = (staticTotal += n);
        // DUP2, LALOAD, DUP2_X2, LCONST_1, LADD, LASTORE
        long f = longs[i]++;
        double g = doubles[i]--;
        // DUP, GETFIELD, DUP2_X1, LCONST_1, LADD, PUTFIELD
        long h = this.total++;
        // POP2
        wide(n);
        wideDouble(b);
        return a + c * 10L + e * 100L + f * 1000L + h * 10000L + (long) (b * 8d) + (long) (d * 16d) + (long) (g * 32d);
    }

    public static void main(String[] args) {
        int mask = 0;
        JsWideStackShuffleApp app = new JsWideStackShuffleApp();
        long[] longs = new long[] {7L, 0x100000000L, 9L};
        double[] doubles = new double[] {0.5d, 2.5d, 4.5d};
        long sum = app.shuffle(longs, doubles, 1, 5L);

        // a = 2^32 + 5, c = 1005, e = 105, f = 2^32 + 5, h = 1005, b = 2.75, d = 6, g = 2.75
        long expected = 0x100000005L + 1005L * 10L + 105L * 100L + 0x100000005L * 1000L + 1005L * 10000L
                + 22L + 96L + 88L;
        if (sum == expected) {
            mask |= 1;
        }
        if (longs[0] == 7L && longs[1] == 0x100000006L && longs[2] == 9L) {
            mask |= 2;
        }
        if (doubles[0] == 0.5d && doubles[1] == 1.75d && doubles[2] == 4.5d) {
            mask |= 4;
        }
        if (app.total == 1006L && app.ratio == 6d) {
            mask |= 8;
        }
        if (staticTotal == 105L) {
            mask |= 16;
        }
        result = mask;
    }
}
