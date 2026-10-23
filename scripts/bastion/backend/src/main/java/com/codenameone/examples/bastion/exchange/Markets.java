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
package com.codenameone.examples.bastion.exchange;

import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bastion.domain.Asset;
import com.codenameone.examples.bastion.domain.Market;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.repository.MarketRepository;

import java.io.IOException;
import java.util.List;

/// The markets, their prices, and what a quantity of an asset is worth.
///
/// Markets come from `bastion.markets`, a comma-separated list of
/// `id:base:quote:lot:indexPrice`, and are written to the `market` table the
/// first time the server sees them; a later change to the lot or the price of
/// an existing market is ignored, because orders already on the book were
/// placed against them.
///
/// **Integer arithmetic only.** A price is quote ledger units per one whole
/// base unit (cents per bitcoin); a quantity is base ledger units (satoshi).
/// The value of a quantity is `price * quantity / 10^decimals(base)`, computed
/// with [#multiplyExact(long,long)], so a product too large for a `long` is refused
/// rather than wrapped into a small, plausible number.
@Component
public class Markets {
    static final String DEFAULTS = "BTC-USD:BTC:USD:1000:6000000,ETH-USD:ETH:USD:100000:300000";

    private final MarketRepository markets;
    private final Ledger ledger;
    private final String configured;

    public Markets(MarketRepository markets, Ledger ledger,
            @Value("${bastion.markets:}") String configured) {
        this.markets = markets;
        this.ledger = ledger;
        this.configured = configured.trim().length() == 0 ? DEFAULTS : configured.trim();
    }

    @PostConstruct
    public void seed() throws IOException {
        for (String one : split(configured, ',')) {
            String[] f = split(one.trim(), ':');
            if (f.length != 5) {
                throw new IllegalArgumentException("bastion.markets: '" + one
                        + "' is not id:base:quote:lot:indexPrice");
            }
            try {
                create(f[0], f[1], f[2], Long.parseLong(f[3]), Long.parseLong(f[4]));
            } catch (IOException | RuntimeException failed) {
                // Two servers started together both find no row and both
                // insert it; the loser's insert fails on the key and changed
                // nothing.
                if (find(f[0]) == null) {
                    throw failed;
                }
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private void create(String id, String base, String quote, long lot, long index)
            throws IOException {
        if (markets.findById(id) != null) {
            return;
        }
        if (lot <= 0 || index <= 0) {
            throw new IllegalArgumentException("bastion.markets: " + id
                    + " needs a positive lot and price");
        }
        ledger.asset(base);
        ledger.asset(quote);
        Market m = new Market();
        m.setId(id);
        m.setBase(base);
        m.setQuote(quote);
        m.setLot(lot);
        m.setIndexPrice(index);
        markets.insert(m);
    }

    @Transactional(readOnly = true)
    public Market find(String id) throws IOException {
        return id == null ? null : markets.findById(id);
    }

    /// Market `id`, or 404.
    @Transactional(readOnly = true)
    public Market get(String id) throws IOException {
        Market m = find(id);
        if (m == null) {
            throw Refused.with(404, "no-such-market", "No such market");
        }
        return m;
    }

    @Transactional(readOnly = true)
    public List<Market> all() throws IOException {
        return markets.findAll();
    }

    /// Moves a market's index price; the simulated feed.
    @Transactional(rollbackFor = Exception.class)
    public void index(String id, long price) throws IOException {
        if (price > 0) {
            markets.index(id, price);
        }
    }

    /// What `amount` ledger units of `asset` are worth in `quote` ledger
    /// units at the index price, **rounded up**: a limit valued here may be
    /// reached a cent early, never passed a cent late. An asset with no
    /// market in `quote` is refused, since its value is unknown.
    @Transactional(readOnly = true)
    public long value(String asset, long amount, String quote) throws IOException {
        if (asset.equals(quote) || amount == 0) {
            return amount;
        }
        List<Market> found = markets.findByBaseAndQuoteOrderByIdAsc(asset, quote);
        if (found.isEmpty()) {
            throw Refused.with(409, "there-no-price", "There is no price for {0}", asset);
        }
        // The shortest id is the configured "BTC-USD" rather than any other
        // market on the same pair.
        Market m = found.get(0);
        for (Market other : found) {
            if (other.getId().length() < m.getId().length()) {
                m = other;
            }
        }
        Asset base = ledger.asset(asset);
        try {
            return ceilDiv(multiplyExact(m.getIndexPrice(), amount), scale(base));
        } catch (ArithmeticException tooLarge) {
            return Long.MAX_VALUE;
        }
    }

    /// 10 to the asset's decimals: the ledger units in one whole unit.
    public static long scale(Asset asset) {
        long s = 1;
        for (int i = 0; i < asset.getDecimals(); i++) {
            s = multiplyExact(s, 10L);
        }
        return s;
    }

    /// `a + b`, refused when it does not fit a `long`. Written out, as is
    /// [#multiplyExact(long,long)], because the class library a compiled
    /// server links has no `Math.addExact`.
    static long addExact(long a, long b) {
        long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    /// `a * b`, refused when it does not fit a `long`.
    static long multiplyExact(long a, long b) {
        long r = a * b;
        long ax = Math.abs(a);
        long ay = Math.abs(b);
        if (((ax | ay) >>> 31) != 0) {
            if ((b != 0 && r / b != a) || (a == Long.MIN_VALUE && b == -1)) {
                throw new ArithmeticException("long overflow");
            }
        }
        return r;
    }

    /// `text` cut at every `separator`, empty pieces kept (String.split is
    /// not in a compiled server's class library either).
    static String[] split(String text, char separator) {
        java.util.List<String> parts = new java.util.ArrayList<String>();
        int from = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == separator) {
                parts.add(text.substring(from, i));
                from = i + 1;
            }
        }
        parts.add(text.substring(from));
        return parts.toArray(new String[parts.size()]);
    }

    /// `a / b` rounded up, for `a >= 0` and `b > 0`.
    static long ceilDiv(long a, long b) {
        long q = a / b;
        return q * b == a ? q : q + 1;
    }
}
