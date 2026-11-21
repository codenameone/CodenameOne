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
package com.codenameone.examples.bazaar;

import com.codename1.backend.DataSource;

/// A row of each of the markets, tax, risk and dispute tables for a shop,
/// so that a route naming one of them has a real object of that store to be
/// refused (MerchantIdorTest).
final class GlobalSeed {
    private GlobalSeed() {
    }

    static void seed(Shop s, String orderId, DataSource db) throws Exception {
        db.execute("INSERT INTO market (id, store_id, code, name, currency, countries, locale, "
                + "rounding, adjust_bps, active, position, created_at) VALUES "
                + "(?, ?, 'idor', 'Idor', 'USD', 'CA', '', 'NONE', 0, 1, 0, 0)",
                new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO duty_rate (id, store_id, country, hs_prefix, rate_bps) VALUES "
                + "(?, ?, 'CA', '', 500)", new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO tax_exemption (id, store_id, username, certificate_id, regions, "
                + "expires_at, created_at) VALUES (?, ?, 'x@example.com', 'C1', '', 0, 0)",
                new Object[] {Ids.next(), s.storeId});
        db.execute("INSERT INTO dispute (id, store_id, order_id, event_id, reason, amount_cents, "
                + "currency, status, due_at, created_at, updated_at) VALUES "
                + "(?, ?, ?, ?, 'fraudulent', 100, 'USD', 'NEEDS_RESPONSE', ?, 0, 0)",
                new Object[] {Ids.next(), s.storeId, orderId, "evt-" + Ids.next(),
                    Long.valueOf(System.currentTimeMillis() + 86400000L)});
        db.execute("UPDATE order_risk SET status = 'HELD' WHERE order_id = ?",
                new Object[] {orderId});
    }
}
