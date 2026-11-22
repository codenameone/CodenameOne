/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.splat.rules;

/// Integer trigonometry over binary angles.
///
/// An angle is an `int` where [#FULL] is one full turn, so yaw wraps with a mask and
/// never needs `Math.PI`. Sines are fixed point with [#ONE] as 1.0. The table is
/// built at class load from Bhaskara I's rational approximation, in integers only,
/// so it is the same table on every runtime; its error (under 0.2%) is far below
/// anything a player can see, and agreeing is what matters, not accuracy.
public final class Trig {
    /// One full turn.
    public static final int FULL = 4096;
    public static final int HALF = FULL / 2;
    public static final int QUARTER = FULL / 4;
    /// Fixed point 1.0 for sines and cosines.
    public static final int ONE = 1 << 14;

    private static final int[] SIN = new int[FULL];

    static {
        long p = HALF;
        for (int a = 0; a < FULL; a++) {
            long x = a % HALF;
            long t = x * (p - x);
            long v = 16L * t * ONE / (5L * p * p - 4L * t);
            SIN[a] = a < HALF ? (int) v : (int) -v;
        }
    }

    private Trig() {
    }

    public static int wrap(int a) {
        return a & (FULL - 1);
    }

    public static int sin(int a) {
        return SIN[a & (FULL - 1)];
    }

    public static int cos(int a) {
        return SIN[(a + QUARTER) & (FULL - 1)];
    }

    /// The angle of the vector `(x, z)` measured from +z toward +x, which is how yaw
    /// is measured: yaw 0 looks down +z. Found by binary search on the sine table,
    /// so it is exact to the table's resolution.
    public static int atan2(long x, long z) {
        if (x == 0 && z == 0) {
            return 0;
        }
        long ax = x < 0 ? -x : x;
        long az = z < 0 ? -z : z;
        // angle in [0, QUARTER] whose tangent is ax/az
        int lo = 0;
        int hi = QUARTER;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            // compare sin(mid)/cos(mid) with ax/az without dividing
            if ((long) sin(mid) * az < (long) cos(mid) * ax) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        int a = lo;
        if (z >= 0) {
            return x >= 0 ? a : wrap(-a);
        }
        return x >= 0 ? HALF - a : wrap(HALF + a);
    }

    /// The signed shortest turn from `from` to `to`, in `(-HALF, HALF]`.
    public static int delta(int from, int to) {
        int d = wrap(to - from);
        return d > HALF ? d - FULL : d;
    }
}
