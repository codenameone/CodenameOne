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
package com.codenameone.examples.dash.rules;

/// Integer helpers for the simulation.
///
/// The rules use no `float` or `double` at all. Floating point is where platforms
/// disagree (fused multiply-add, x87 precision, constant folding), and one ulp of
/// difference in a position is a desync two minutes later. Every length is an `int`
/// in millimetres ([Track#UNIT] per metre) and every product goes through `long`.
public final class FixedMath {
    private FixedMath() {
    }

    /// Floor of the square root of a non-negative value, by Newton iteration.
    public static long isqrt(long v) {
        if (v <= 0) {
            return 0;
        }
        long x = v;
        long y = (x + 1) >>> 1;
        while (y < x) {
            x = y;
            y = (x + v / x) >>> 1;
        }
        return x;
    }

    public static long distSq(int x1, int y1, int x2, int y2) {
        long dx = (long) x2 - x1;
        long dy = (long) y2 - y1;
        return dx * dx + dy * dy;
    }

    /// True when the two points are no further apart than `range`.
    public static boolean within(int x1, int y1, int x2, int y2, int range) {
        long r = range;
        return distSq(x1, y1, x2, y2) <= r * r;
    }

    public static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /// `a * num / den` without overflowing, rounding toward zero. A zero `den`
    /// answers 0 explicitly: ParparVM's integer division does not throw on zero,
    /// so the rules never divide by a value they have not checked.
    public static int muldiv(int a, int num, int den) {
        if (den == 0) {
            return 0;
        }
        return (int) ((long) a * num / den);
    }
}
