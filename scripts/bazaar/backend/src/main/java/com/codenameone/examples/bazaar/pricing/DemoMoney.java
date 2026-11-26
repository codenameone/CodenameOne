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

import com.codenameone.examples.bazaar.market.FixedExchangeRates;
import com.codenameone.examples.bazaar.ops.Amounts;

/// The demo's seed amounts, written once in US cents, in a store's own
/// currency: a store selling in yen gets yen prices, shipping rates and
/// thresholds of the same size, not 7500 yen for "$75". Converted at the
/// fixed demo table ([FixedExchangeRates]), half to even.
public final class DemoMoney {
    private DemoMoney() {
    }

    /// `usdCents` in minor units of `currency`; unchanged for US dollars and
    /// for a currency the demo table does not have.
    public static long local(long usdCents, String currency) {
        long rate = new FixedExchangeRates().rateMicros("USD", currency);
        if (rate <= 0 || "USD".equalsIgnoreCase(currency == null ? "" : currency.trim())) {
            return usdCents;
        }
        return Amounts.convert(usdCents, "USD", currency, rate);
    }
}
