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
import java.io.IOException;

/// The European Central Bank's daily reference rates as an [ExchangeRates]:
/// the same file and parser the market feed uses ([FxFeed#parse]), read
/// through the backend's own HTTP client.
///
/// The parsed day is cached for [#TTL_MILLIS]; the bank publishes once a
/// working day. A failed fetch keeps the last good day, and before the first
/// one -- or for a currency the bank does not publish -- the rate comes from
/// `fallback`, so a sale is never refused for want of a rate. Cross rates are
/// `(per euro of to) / (per euro of from)` in millionths, half to even.
public final class EcbExchangeRates implements ExchangeRates {
    /// How long one fetched day is trusted before asking again.
    public static final long TTL_MILLIS = 6L * 60 * 60 * 1000;

    private final FxFeed.Source source;
    private final ExchangeRates fallback;
    private FxFeed.Daily day;
    private long fetchedAt;

    public EcbExchangeRates(FxFeed.Source source, ExchangeRates fallback) {
        this.source = source;
        this.fallback = fallback;
    }

    /// The bank's file at `url`, through the backend's HTTP client.
    public static FxFeed.Source web(String url) {
        return new FxFeed.WebSource(url);
    }

    @Override
    public long rateMicros(String from, String to) {
        if (from != null && from.trim().equalsIgnoreCase(to == null ? "" : to.trim())) {
            return Amounts.MICROS;
        }
        FxFeed.Daily d = current();
        if (d != null) {
            Long f = d.perEuro.get(upper(from));
            Long t = d.perEuro.get(upper(to));
            if (f != null && t != null && f.longValue() > 0) {
                return Amounts.divideHalfEven(Math.multiplyExact(t.longValue(), Amounts.MICROS),
                        f.longValue());
            }
        }
        return fallback == null ? 0 : fallback.rateMicros(from, to);
    }

    @Override
    public String source() {
        return "ecb";
    }

    /// An ASCII upper case, never the locale's folding.
    private static String upper(String code) {
        String t = code == null ? "" : code.trim();
        StringBuilder b = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            b.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return b.toString();
    }

    private FxFeed.Daily current() {
        long now = System.currentTimeMillis();
        if (day != null && now - fetchedAt < TTL_MILLIS) {
            return day;
        }
        try {
            day = FxFeed.parse(source.fetch());
            fetchedAt = now;
        } catch (IOException | RuntimeException failed) {
            // The last good day stays; try again on a later call, not on every one.
            fetchedAt = now - TTL_MILLIS + 5L * 60 * 1000;
            System.err.println("[fx] reporting rates: " + failed.getMessage());
        }
        return day;
    }
}
