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

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.ops.Amounts;
import java.io.IOException;

/// The platform's one reporting currency, and the rate each order is
/// recorded at so the platform's totals add like with like.
///
/// Stores sell in their own currencies. Every order records, when it is
/// placed ([#stamp]), the rate from its store's currency into the reporting
/// currency (`bazaar.fx.reporting.currency`, US dollars by default), and the
/// platform's GMV converts each order at that recorded rate ([#toReporting]):
/// a later move in the market does not rewrite last month's figure.
///
/// The rates come from an [ExchangeRates] provider, chosen by
/// `bazaar.fx.reporting.provider`: `fixed` (the default, [FixedExchangeRates])
/// or `ecb` ([EcbExchangeRates], the bank's daily file at `bazaar.fx.ecb.url`,
/// with the fixed table behind it). Under the `test` profile the provider is
/// always the fixed table, so no test reaches the network.
@Component
public class ReportingRates {
    private final String currency;
    private final ExchangeRates rates;

    @com.codename1.backend.annotations.Autowired
    public ReportingRates(Config config) throws IOException {
        this(config.get("bazaar.fx.reporting.currency", "USD"), provider(config));
    }

    /// A fixed currency and provider, for tests and tools.
    public ReportingRates(String currency, ExchangeRates rates) {
        String c = currency == null ? "" : currency.trim();
        this.currency = c.length() == 3 ? c : "USD";
        this.rates = rates == null ? new FixedExchangeRates() : rates;
    }

    private static ExchangeRates provider(Config config) throws IOException {
        ExchangeRates fixed = new FixedExchangeRates();
        String wanted = config.get("bazaar.fx.reporting.provider", "fixed").trim();
        if ("ecb".equalsIgnoreCase(wanted) && !"test".equals(config.getProfile())) {
            return new EcbExchangeRates(EcbExchangeRates.web(
                    config.get("bazaar.fx.ecb.url", FxFeed.ECB_URL)), fixed);
        }
        return fixed;
    }

    /// The currency platform totals are reported in.
    public String currency() {
        return currency;
    }

    /// The provider rates come from.
    public ExchangeRates rates() {
        return rates;
    }

    /// Records on `order` the current rate from its currency into the
    /// reporting currency. An order already stamped keeps its rate.
    public void stamp(StoreOrder order) {
        if (order == null || order.getReportingRateMicros() > 0) {
            return;
        }
        order.setReportingCurrency(currency);
        order.setReportingRateMicros(rates.rateMicros(order.getCurrency(), currency));
    }

    /// `minor` units of `order`'s currency in minor units of the reporting
    /// currency: at the rate recorded on the order when it was recorded
    /// against this reporting currency, else at today's rate (an order from
    /// before rates were recorded, or after the reporting currency changed).
    /// Half to even; 0 for a currency no rate is known for.
    public long toReporting(StoreOrder order, long minor) {
        long rate = order.getReportingRateMicros();
        if (rate <= 0 || !currency.equalsIgnoreCase(order.getReportingCurrency())) {
            rate = rates.rateMicros(order.getCurrency(), currency);
        }
        return convert(minor, order.getCurrency(), currency, rate);
    }

    /// `minor` units of `from` in `to` at `rateMicros`, half to even; 0 for
    /// no rate or a product too large to hold.
    public static long convert(long minor, String from, String to, long rateMicros) {
        if (rateMicros <= 0) {
            return 0;
        }
        try {
            return Amounts.convert(minor, from, to, rateMicros);
        } catch (ArithmeticException overflow) {
            return 0;
        }
    }
}
