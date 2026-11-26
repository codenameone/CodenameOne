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
package com.codenameone.examples.bazaar.carrier;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// Live carrier rates for checkout, cached briefly per cart and address.
///
/// [com.codenameone.examples.bazaar.cart.Quoter] asks this for every CARRIER
/// shipping rate a cart's address gets. The same cart, weight, origin and
/// address within [#TTL_MILLIS] reuse one answer, so typing through the
/// checkout form does not call the carrier per keystroke, and placing the
/// order re-prices from the server's own answer (cached or fresh) -- the
/// client only ever names a rate, never an amount.
///
/// A carrier that throws or answers nothing is remembered as a failure for
/// the same short while and answers null: the shipping calculator then
/// offers the rate's fallback price or the zone's other rates, so a carrier
/// outage never blocks checkout.
@Component
public class CarrierRates {
    /// How long one carrier answer is reused.
    public static final long TTL_MILLIS = 10 * 60 * 1000L;
    /// Above this many entries the expired ones are dropped, and if that is
    /// not enough the cache starts again.
    private static final int MAX_ENTRIES = 2000;

    private final CarrierProvider carrier;
    private final Map<String, Entry> cache = new HashMap<String, Entry>();
    private int calls;

    public CarrierRates(CarrierProvider carrier) {
        this.carrier = carrier;
    }

    private static final class Entry {
        final long expires;
        /// Null for a carrier that failed.
        final List<RateQuoteDto> quotes;

        Entry(long expires, List<RateQuoteDto> quotes) {
            this.expires = expires;
            this.quotes = quotes;
        }
    }

    /// The carrier's name, as a rate's `carrier` column records it.
    public String carrierName() {
        return carrier.name();
    }

    /// How many times the carrier itself was asked; for tests.
    public synchronized int calls() {
        return calls;
    }

    /// The carrier's quotes for `weightGrams` from `from` to `to`, or null
    /// when it cannot answer. `cartKey` scopes the cache to one cart.
    public List<RateQuoteDto> quotes(String cartKey, int weightGrams, CarrierProvider.Place from,
            CarrierProvider.Place to) {
        String key = key(cartKey, weightGrams, from, to);
        long now = System.currentTimeMillis();
        synchronized (this) {
            Entry e = cache.get(key);
            if (e != null && e.expires > now) {
                return e.quotes;
            }
            calls++;
        }
        List<RateQuoteDto> got;
        try {
            got = carrier.rates(new CarrierProvider.Parcel(Math.max(1, weightGrams)), from, to);
            if (got != null && got.isEmpty()) {
                got = null;
            }
        } catch (IOException | RuntimeException failed) {
            // An outage, a refusal or a carrier that is not configured: the
            // checkout falls back, it does not fail.
            got = null;
        }
        List<RateQuoteDto> kept = got == null ? null : new ArrayList<RateQuoteDto>(got);
        synchronized (this) {
            if (cache.size() >= MAX_ENTRIES) {
                Iterator<Map.Entry<String, Entry>> it = cache.entrySet().iterator();
                while (it.hasNext()) {
                    if (it.next().getValue().expires <= now) {
                        it.remove();
                    }
                }
                if (cache.size() >= MAX_ENTRIES) {
                    cache.clear();
                }
            }
            cache.put(key, new Entry(now + TTL_MILLIS, kept));
        }
        return kept;
    }

    /// Forgets every cached answer; for tests.
    public synchronized void clear() {
        cache.clear();
    }

    private static String key(String cartKey, int weightGrams, CarrierProvider.Place from,
            CarrierProvider.Place to) {
        StringBuilder b = new StringBuilder();
        b.append(cartKey == null ? "" : cartKey).append('|').append(weightGrams);
        place(b, from);
        place(b, to);
        return b.toString();
    }

    private static void place(StringBuilder b, CarrierProvider.Place p) {
        if (p == null) {
            b.append("|-");
            return;
        }
        b.append('|').append(p.country).append('|').append(p.postal).append('|')
                .append(p.region).append('|').append(p.city).append('|').append(p.address)
                .append('|').append(p.lat).append('|').append(p.lng);
    }
}
