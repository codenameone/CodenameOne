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
package com.codenameone.examples.bazaar.checkout;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.FieldAnswerDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/// The life of a checkout's idempotency key.
///
/// A key is remembered for [#LIFETIME_MILLIS] (24 hours, as long as Stripe
/// keeps its own) with the fingerprint of the request that first used it:
///
/// - the same key with the same request answers the order it placed;
/// - the same key with a different request is refused with 422, since a key
///   names one order and a changed address or note is not that order;
/// - the same key while its first request is still running is refused with
///   409; a request that has run longer than [#IN_FLIGHT_MILLIS] is taken to
///   have died and its key may be used again;
/// - a request that fails gives its key back, so a retry after a decline
///   starts clean.
///
/// [#sweep] forgets keys past their lifetime. A forgotten key that placed an
/// order still answers that order, since the order itself carries it
/// (`store_order.client_id`); only the fingerprint check and the in-flight
/// lock end with the lifetime.
@Component
public class IdempotencyKeys {
    public static final long LIFETIME_MILLIS = 24L * 3600000L;
    public static final long IN_FLIGHT_MILLIS = 10L * 60000L;
    static final String IN_FLIGHT = "IN_FLIGHT";
    static final String DONE = "DONE";

    private final DataSource db;

    public IdempotencyKeys(DataSource db) {
        this.db = db;
    }

    /// Claims `clientId` for a request with `fingerprint` at `now`. Answers
    /// true for a first use (the caller places the order and calls
    /// [#finish] or [#forget]) and false for a repeat of a request that
    /// finished, which the caller answers with the order it placed.
    ///
    /// - `ResponseStatusException` 422: the key was used with another request;
    ///   409 while the first request with it is still running
    public boolean begin(String clientId, String fingerprint, long now) throws IOException {
        for (int attempt = 0; attempt < 2; attempt++) {
            Map r = db.queryOne("SELECT fingerprint, state, created_at, expires_at FROM "
                    + "idempotency_key WHERE client_id = ?", new Object[] {clientId});
            if (r != null && Rows.number(r.get("expires_at")) <= now) {
                db.execute("DELETE FROM idempotency_key WHERE client_id = ? AND expires_at <= ?",
                        new Object[] {clientId, Long.valueOf(now)});
                r = null;
            }
            if (r == null) {
                try {
                    db.execute("INSERT INTO idempotency_key (client_id, fingerprint, state, "
                            + "order_id, created_at, expires_at) VALUES (?, ?, ?, '', ?, ?)",
                            new Object[] {clientId, fingerprint, IN_FLIGHT, Long.valueOf(now),
                                Long.valueOf(now + LIFETIME_MILLIS)});
                    return true;
                } catch (IOException taken) {
                    // Another request inserted it first: read what it left.
                    continue;
                }
            }
            if (!fingerprint.equals(Rows.text(r.get("fingerprint")))) {
                throw new ResponseStatusException(422,
                        "That idempotency key was used for a different order");
            }
            if (DONE.equals(Rows.text(r.get("state")))) {
                return false;
            }
            long started = Rows.number(r.get("created_at"));
            if (now - started < IN_FLIGHT_MILLIS) {
                throw new ResponseStatusException(409,
                        "The order with that idempotency key is still being placed");
            }
            // The first request died without a word: take the key over.
            if (db.execute("UPDATE idempotency_key SET created_at = ?, expires_at = ? WHERE "
                    + "client_id = ? AND state = ? AND created_at = ?",
                    new Object[] {Long.valueOf(now), Long.valueOf(now + LIFETIME_MILLIS), clientId,
                        IN_FLIGHT, Long.valueOf(started)}) == 1) {
                return true;
            }
        }
        throw new ResponseStatusException(409,
                "The order with that idempotency key is still being placed");
    }

    /// The request that claimed `clientId` placed `orderId`.
    public void finish(String clientId, String orderId) throws IOException {
        db.execute("UPDATE idempotency_key SET state = ?, order_id = ? WHERE client_id = ?",
                new Object[] {DONE, orderId == null ? "" : orderId, clientId});
    }

    /// The request that claimed `clientId` failed; the key may be used again.
    public void forget(String clientId) throws IOException {
        db.execute("DELETE FROM idempotency_key WHERE client_id = ? AND state = ?",
                new Object[] {clientId, IN_FLIGHT});
    }

    /// Forgets every key past its lifetime at `now`; answers how many.
    public int sweep(long now) throws IOException {
        return db.execute("DELETE FROM idempotency_key WHERE expires_at <= ?",
                new Object[] {Long.valueOf(now)});
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 90000, lock = "bazaar-idempotency-sweep")
    public void sweepExpired() throws IOException {
        sweep(System.currentTimeMillis());
    }

    /// What makes two checkout requests the same request: where the order
    /// goes, how it ships and is paid, and what the shopper wrote. A card
    /// typed into the web checkout is checked again on every submit and gets
    /// a new token, so it is left out.
    public static String fingerprint(String storeId, PlaceOrderDto req) throws IOException {
        StringBuilder b = new StringBuilder();
        field(b, storeId);
        AddressDto a = req.address();
        if (a != null) {
            field(b, a.name());
            field(b, a.line1());
            field(b, a.line2());
            field(b, a.city());
            field(b, a.region());
            field(b, a.postal());
            field(b, a.country());
            field(b, a.phone());
            b.append(a.lat()).append('|').append(a.lng()).append('|');
        } else {
            b.append("-|");
        }
        field(b, req.shippingRateId());
        field(b, req.paymentMethodId());
        field(b, req.email());
        field(b, req.note());
        b.append(req.tip()).append('|');
        List<FieldAnswerDto> fields = req.fields();
        if (fields != null) {
            for (int iter = 0; iter < fields.size(); iter++) {
                FieldAnswerDto f = fields.get(iter);
                if (f != null) {
                    field(b, f.fieldId());
                    field(b, f.value());
                }
            }
        }
        return Ids.sha256(b.toString()).substring(0, 64);
    }

    private static void field(StringBuilder b, String value) {
        String v = value == null ? "" : value.trim();
        b.append(v.length()).append(':').append(v).append('|');
    }
}
