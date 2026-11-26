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
import com.codename1.backend.Web;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.domain.FxRate;
import com.codenameone.examples.bazaar.domain.Market;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.FxRateRepository;
import com.codenameone.examples.bazaar.repository.MarketRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The European Central Bank's daily reference rates, for the markets that
/// choose them ([Markets#ECB]).
///
/// Every six hours the server reads the bank's daily file
/// (`bazaar.fx.ecb.url`, by default [#ECB_URL]), which gives each currency per
/// euro, and writes the cross rate from each store's currency into that
/// store's [FxRate] row as its *feed* rate, with the day the bank published it.
/// The merchant's own rate on the same row is never touched, so a market
/// switched back to manual finds it as it was. An order keeps the rate it was
/// charged at and which of the two that was.
///
/// Cross rates are integer arithmetic on millionths -- `(per euro of target) /
/// (per euro of store)`, rounded half up -- with no floating point anywhere. A
/// currency the bank does not publish is left as it was, and a market in it
/// shows the store's own prices, as one whose rate is gone does.
///
/// The fetch is off under the `test` profile and with `bazaar.fx.feed=off`;
/// tests hand [#refresh] the XML through [Source]. Like the Stripe provider,
/// the client is held to canned answers only and has not been run against the
/// live file in CI.
@Component
public class FxFeed {
    /// The bank's daily file.
    public static final String ECB_URL =
            "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
    static final long MICROS = 1000000L;

    private final MarketRepository markets;
    private final FxRateRepository rates;
    private final StoreRepository stores;
    private final Source network;
    private final boolean enabled;

    public FxFeed(MarketRepository markets, FxRateRepository rates, StoreRepository stores,
            Config config) throws IOException {
        this.markets = markets;
        this.rates = rates;
        this.stores = stores;
        this.network = new WebSource(config.get("bazaar.fx.ecb.url", ECB_URL));
        this.enabled = !"test".equals(config.getProfile())
                && !"off".equalsIgnoreCase(config.get("bazaar.fx.feed", "on").trim());
    }

    /// Where the XML comes from: the network, or a test's string.
    public interface Source {
        String fetch() throws IOException;
    }

    /// One day's rates: each currency per euro in millionths (EUR itself is
    /// 1000000), and the day they were published, in epoch milliseconds.
    public static final class Daily {
        public final Map<String, Long> perEuro = new HashMap<String, Long>();
        public long publishedAt;
    }

    @Scheduled(fixedDelay = 21600000, initialDelay = 90000, lock = "bazaar-fx-feed")
    public void refreshJob() {
        if (!enabled) {
            return;
        }
        try {
            refresh(network);
        } catch (IOException | RuntimeException failed) {
            // The rates already stored stay; the next run tries again.
            System.err.println("[fx] " + failed.getMessage());
        }
    }

    /// Reads the feed and updates every store with a market on it; answers how
    /// many rates were written.
    public int refresh(Source from) throws IOException {
        return apply(parse(from.fetch()));
    }

    /// Parses the bank's file without an XML library: the `time` attribute of
    /// the day's `Cube`, then every `currency`/`rate` pair, in either quote
    /// style. A file with no rates is an error, so a broken answer never reads
    /// as "every currency vanished".
    public static Daily parse(String xml) throws IOException {
        if (xml == null || xml.length() > 1000000) {
            throw new IOException("The rate feed's answer is not usable");
        }
        Daily d = new Daily();
        d.perEuro.put("EUR", Long.valueOf(MICROS));
        String time = attribute(xml, 0, xml.length(), "time");
        if (time != null && time.length() == 10) {
            try {
                d.publishedAt = LocalDate.parse(time).toEpochDay() * Days.MILLIS;
            } catch (RuntimeException badDate) {
                d.publishedAt = 0;
            }
        }
        int at = 0;
        while (true) {
            int c = find(xml, at, xml.length(), "currency");
            if (c < 0) {
                break;
            }
            int end = xml.indexOf('>', c);
            if (end < 0) {
                break;
            }
            String currency = attribute(xml, c, end, "currency");
            String rate = attribute(xml, c - 64 < at ? at : c - 64, end, "rate");
            long micros = rate == null ? -1 : micros(rate);
            if (currency != null && currency.length() == 3 && micros > 0) {
                d.perEuro.put(currency, Long.valueOf(micros));
            }
            at = end;
        }
        if (d.perEuro.size() < 2) {
            throw new IOException("The rate feed has no rates");
        }
        return d;
    }

    /// The position of attribute `name=` in `[from, to)`, preceded by white
    /// space, or -1.
    private static int find(String xml, int from, int to, String name) {
        int at = from;
        while (true) {
            int i = xml.indexOf(name + "=", at);
            if (i < 0 || i >= to) {
                return -1;
            }
            char before = i == 0 ? ' ' : xml.charAt(i - 1);
            if (before == ' ' || before == '\t' || before == '\n' || before == '\r') {
                return i;
            }
            at = i + 1;
        }
    }

    /// The value of the first attribute `name` in `[from, to)`, or null.
    static String attribute(String xml, int from, int to, String name) {
        int i = find(xml, from, to, name);
        if (i < 0 || i + name.length() + 1 >= xml.length()) {
            return null;
        }
        int q = i + name.length() + 1;
        char quote = xml.charAt(q);
        if (quote != '\'' && quote != '"') {
            return null;
        }
        int close = xml.indexOf(quote, q + 1);
        return close < 0 || close - q > 40 ? null : xml.substring(q + 1, close);
    }

    /// A decimal such as `1.0842` in millionths, rounded half up past the sixth
    /// place; -1 for anything that is not a plain positive decimal.
    static long micros(String text) {
        if (text.length() == 0 || text.length() > 24) {
            return -1;
        }
        long whole = 0;
        long frac = 0;
        int places = 0;
        boolean dot = false;
        boolean roundUp = false;
        for (int iter = 0; iter < text.length(); iter++) {
            char ch = text.charAt(iter);
            if (ch == '.' && !dot) {
                dot = true;
            } else if (ch >= '0' && ch <= '9') {
                if (!dot) {
                    whole = whole * 10 + (ch - '0');
                    if (whole > MICROS) {
                        return -1;
                    }
                } else if (places < 6) {
                    frac = frac * 10 + (ch - '0');
                    places++;
                } else if (places == 6) {
                    roundUp = ch >= '5';
                    places++;
                }
            } else {
                return -1;
            }
        }
        while (places < 6) {
            frac *= 10;
            places++;
        }
        return whole * MICROS + frac + (roundUp ? 1 : 0);
    }

    /// Units of `to` per unit of `from` in millionths, from rates per euro;
    /// 0 when either is missing.
    public static long cross(Daily d, String from, String to) {
        Long f = d.perEuro.get(from);
        Long t = d.perEuro.get(to);
        if (f == null || t == null || f.longValue() <= 0) {
            return 0;
        }
        try {
            long scaled = Math.multiplyExact(t.longValue(), MICROS);
            return Math.addExact(scaled, f.longValue() / 2) / f.longValue();
        } catch (ArithmeticException overflow) {
            return 0;
        }
    }

    /// Writes `d` as the feed rate of every store and currency a market on the
    /// feed uses; answers how many rows were written.
    @Transactional(rollbackFor = IOException.class)
    public int apply(Daily d) throws IOException {
        List<Market> on = markets.findByFxSource(Markets.ECB);
        Set<String> done = new HashSet<String>();
        int written = 0;
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < on.size(); iter++) {
            Market m = on.get(iter);
            if (!done.add(m.getStoreId() + ":" + m.getCurrency())) {
                continue;
            }
            Store store = stores.findById(m.getStoreId());
            if (store == null || store.getCurrency().equals(m.getCurrency())) {
                continue;
            }
            long rate = cross(d, store.getCurrency(), m.getCurrency());
            if (rate <= 0 || rate > Markets.MAX_RATE) {
                continue;
            }
            FxRate r = rates.findByStoreIdAndCurrency(store.getId(), m.getCurrency());
            boolean fresh = r == null;
            if (fresh) {
                // No rate of the merchant's: the manual rate stays 0, which no
                // manual market is ever priced at.
                r = new FxRate();
                r.setId(Ids.next());
                r.setStoreId(store.getId());
                r.setCurrency(m.getCurrency());
                r.setUpdatedAt(Instant.ofEpochMilli(now));
            }
            r.setFeedMicros(rate);
            r.setFeedSource(Markets.ECB);
            r.setFeedAt(d.publishedAt > 0 ? d.publishedAt : now);
            if (fresh) {
                rates.insert(r);
            } else {
                rates.save(r);
            }
            written++;
        }
        return written;
    }

    /// The real network, as [Web] makes it.
    static final class WebSource implements Source {
        private final String url;

        WebSource(String url) {
            this.url = url;
        }

        @Override
        public String fetch() throws IOException {
            Web.Result r = Web.get(url);
            if (r.getStatus() != 200) {
                throw new IOException("The rate feed answered " + r.getStatus());
            }
            return r.getBodyAsString();
        }
    }
}
