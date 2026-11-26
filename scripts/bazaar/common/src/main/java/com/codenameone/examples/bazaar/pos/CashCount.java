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
package com.codenameone.examples.bazaar.pos;

/// Counting a drawer note by note and coin by coin. Pure: no display.
public final class CashCount {
    /// Notes and coins, largest first, in cents.
    public static final long[] DENOMINATIONS = {10000, 5000, 2000, 1000, 500, 100, 25, 10, 5, 1};

    private CashCount() {
    }

    /// What `counts` (one per denomination, missing ones zero) add up to.
    public static long total(int[] counts) {
        long sum = 0;
        for (int iter = 0; counts != null && iter < counts.length
                && iter < DENOMINATIONS.length; iter++) {
            sum += DENOMINATIONS[iter] * Math.max(0, counts[iter]);
        }
        return sum;
    }

    /// Counted minus expected: positive over, negative short.
    public static long variance(long counted, long expected) {
        return counted - expected;
    }

    /// What a closed shift should have in the drawer before it is counted,
    /// as the server works it out: the float, net cash sales and cash moves.
    public static long expected(long opening, long cashSales, long cashMoves) {
        return opening + cashSales + cashMoves;
    }
}
