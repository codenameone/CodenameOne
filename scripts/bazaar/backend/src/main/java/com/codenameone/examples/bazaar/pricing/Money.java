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
package com.codenameone.examples.bazaar.pricing;

/// Integer arithmetic on cents.
///
/// Every rounding in pricing goes through [#divideHalfEven]: banker's rounding,
/// so a long run of halves (15% of an odd number of cents, say) does not drift
/// up the way half-up does. Nothing here touches a `double`.
public final class Money {
    private Money() {
    }

    /// `numerator / denominator` rounded half to even. A zero or negative
    /// denominator answers 0 rather than dividing: the device VMs this code's
    /// relatives run on do not throw on division by zero.
    public static long divideHalfEven(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        boolean negative = numerator < 0;
        long n = negative ? -numerator : numerator;
        long q = n / denominator;
        long r = n - q * denominator;
        long twice = r * 2;
        if (twice > denominator || (twice == denominator && (q & 1) == 1)) {
            q++;
        }
        return negative ? -q : q;
    }

    /// `amount * basisPoints / 10000`, rounded half to even.
    public static long basisPoints(long amount, long basisPoints) {
        return divideHalfEven(amount * basisPoints, 10000L);
    }

    /// Whether a comma separated list holds `code`, ignoring ASCII case and
    /// the spaces around each entry.
    public static boolean listHas(String list, String code) {
        if (list == null || code == null) {
            return false;
        }
        int start = 0;
        int len = list.length();
        while (start <= len) {
            int end = list.indexOf(',', start);
            if (end < 0) {
                end = len;
            }
            String item = list.substring(start, end).trim();
            if (item.equalsIgnoreCase(code.trim())) {
                return true;
            }
            start = end + 1;
        }
        return false;
    }
}
