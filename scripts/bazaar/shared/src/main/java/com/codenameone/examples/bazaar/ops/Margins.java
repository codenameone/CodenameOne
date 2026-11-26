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
package com.codenameone.examples.bazaar.ops;

/// Margin and markup of a variant from its price and its cost per item, in
/// integer arithmetic. Both answer basis points (10000 = 100%) and
/// [#UNKNOWN] when there is no cost to measure against.
///
/// - **Margin** is what is left of the price once the cost is paid:
///   `(price - cost) / price`.
/// - **Markup** is what was added on top of the cost: `(price - cost) / cost`.
public final class Margins {
    /// No cost per item is recorded, or the price is zero.
    public static final int UNKNOWN = Integer.MIN_VALUE;

    private Margins() {
    }

    /// Margin in basis points, rounded half away from zero.
    public static int marginBps(long priceCents, long costCents) {
        if (costCents <= 0 || priceCents <= 0) {
            return UNKNOWN;
        }
        return ratio(priceCents - costCents, priceCents);
    }

    /// Markup in basis points, rounded half away from zero.
    public static int markupBps(long priceCents, long costCents) {
        if (costCents <= 0 || priceCents <= 0) {
            return UNKNOWN;
        }
        return ratio(priceCents - costCents, costCents);
    }

    /// `bps` as a percentage with one decimal, such as `37.5%` or `-12.0%`;
    /// a hyphen for [#UNKNOWN].
    public static String percent(int bps) {
        if (bps == UNKNOWN) {
            return "-";
        }
        long tenths = bps >= 0 ? (bps + 5L) / 10L : -((-(long) bps + 5L) / 10L);
        StringBuilder b = new StringBuilder();
        if (tenths < 0) {
            b.append('-');
        }
        long abs = Math.abs(tenths);
        b.append(abs / 10L).append('.').append(abs % 10L).append('%');
        return b.toString();
    }

    private static int ratio(long top, long bottom) {
        long scaled = top * 10000L;
        long half = bottom / 2L;
        long q = scaled >= 0 ? (scaled + half) / bottom : -((-scaled + half) / bottom);
        if (q > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (q < -Integer.MAX_VALUE) {
            return -Integer.MAX_VALUE;
        }
        return (int) q;
    }
}
