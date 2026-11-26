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
package com.codenameone.examples.bazaar.market;

import com.codenameone.examples.bazaar.ops.Amounts;

/// A fixed table of demo rates, each currency per US dollar. Deterministic,
/// so tests and a fresh demo add up the same every time; not market data.
public final class FixedExchangeRates implements ExchangeRates {
    /// Code, then units per US dollar in millionths.
    private static final Object[][] PER_USD = {
        {"USD", Long.valueOf(1000000L)},
        {"EUR", Long.valueOf(920000L)},
        {"GBP", Long.valueOf(790000L)},
        {"CAD", Long.valueOf(1360000L)},
        {"AUD", Long.valueOf(1520000L)},
        {"NZD", Long.valueOf(1650000L)},
        {"CHF", Long.valueOf(880000L)},
        {"ILS", Long.valueOf(3700000L)},
        {"JPY", Long.valueOf(150000000L)},
        {"BHD", Long.valueOf(376000L)},
        {"KWD", Long.valueOf(307000L)},
        {"MXN", Long.valueOf(17000000L)},
    };

    /// The demo's rate from US dollars to `code`: units per dollar in
    /// millionths, or 0 when the table does not have it.
    public static long perUsd(String code) {
        if (code == null) {
            return 0;
        }
        String c = code.trim();
        for (int iter = 0; iter < PER_USD.length; iter++) {
            if (((String) PER_USD[iter][0]).equalsIgnoreCase(c)) {
                return ((Long) PER_USD[iter][1]).longValue();
            }
        }
        return 0;
    }

    @Override
    public long rateMicros(String from, String to) {
        if (from != null && from.trim().equalsIgnoreCase(to == null ? "" : to.trim())) {
            return Amounts.MICROS;
        }
        long f = perUsd(from);
        long t = perUsd(to);
        if (f <= 0 || t <= 0) {
            return 0;
        }
        return Amounts.divideHalfEven(Math.multiplyExact(t, Amounts.MICROS), f);
    }

    @Override
    public String source() {
        return "fixed";
    }
}
