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

/// Where the platform's exchange rates come from, for adding sales made in
/// different currencies into one figure ([ReportingRates]).
///
/// A rate is how many units of `to` one unit of `from` buys, in millionths:
/// US dollars to yen at 150 is 150000000. Units are major units; the
/// difference in decimals between the two currencies is the converter's
/// business ([com.codenameone.examples.bazaar.ops.Amounts#convert]).
///
/// [FixedExchangeRates] is the demo table, and the default;
/// [EcbExchangeRates] reads the European Central Bank's daily reference rates.
public interface ExchangeRates {
    /// Units of `to` per unit of `from`, in millionths; 1000000 for the same
    /// currency and 0 when either is unknown to this provider.
    long rateMicros(String from, String to);

    /// A short name for where the rates come from, such as `fixed` or `ecb`.
    String source();
}
