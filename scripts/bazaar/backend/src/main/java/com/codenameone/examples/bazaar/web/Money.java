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
package com.codenameone.examples.bazaar.web;

import com.codenameone.examples.bazaar.ops.Amounts;

/// Prices as a page shows them. Amounts are minor units of their currency
/// ([Amounts]): cents, yen or fils.
public final class Money {
    private Money() {
    }

    /// `1250, USD` -> `$12.50`, `1250, JPY` -> yen sign and `1250`; a currency
    /// without a symbol is spelled out in front: `CHF 12.50`, `BHD 1.250`.
    public static String format(long minor, String currency) {
        return Amounts.format(minor, currency);
    }

    /// `1250, USD` -> `12.50`, `1250, JPY` -> `1250`: what structured data
    /// and exports want for a price.
    public static String plain(long minor, String currency) {
        return Amounts.plain(minor, currency);
    }

    /// Basis points as a percentage: 1000 is "10", 1250 is "12.5".
    public static String percent(int bps) {
        String whole = Integer.toString(bps / 100);
        int rest = bps % 100;
        if (rest == 0) {
            return whole;
        }
        return whole + "." + (rest % 10 == 0 ? Integer.toString(rest / 10)
                : (rest < 10 ? "0" : "") + rest);
    }
}
