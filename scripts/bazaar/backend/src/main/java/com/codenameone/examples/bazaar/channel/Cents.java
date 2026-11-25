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
package com.codenameone.examples.bazaar.channel;

import com.codenameone.examples.bazaar.ops.Amounts;

/// Decimal money strings (`"12.3"`, `"12.34"`, `"1250"` for yen) to minor
/// units of their currency and back, without floating point: marketplaces
/// answer prices as decimal strings. The decimals are the currency's own
/// ([Amounts#digits]).
public final class Cents {
    private Cents() {
    }

    /// Minor units of a decimal string in `currency`; decimals past the
    /// currency's own are cut. 0 when it is not a number or would not fit.
    static long parse(Object value, String currency) {
        return Amounts.parse(value == null ? "" : String.valueOf(value), currency);
    }

    /// `1234, "USD"` as `"12.34"`, `1234, "JPY"` as `"1234"`.
    public static String format(long minor, String currency) {
        return Amounts.plain(minor, currency);
    }
}
