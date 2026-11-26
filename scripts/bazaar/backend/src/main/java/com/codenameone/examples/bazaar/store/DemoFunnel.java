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
package com.codenameone.examples.bazaar.store;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.analytics.Views;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

/// The browsing behind the demo stores' online orders, so the analytics
/// funnel reads like a shop's: many product views, fewer carts, fewer that
/// reach checkout, fewer orders. Without it the seeded orders arrived from
/// no views and no carts, and the funnel widened at the bottom.
///
/// For each seeded online order it writes, around the order's time:
///
/// - the buyer's cart (with the order's e-mail) and their views of what
///   they bought plus [#BROWSED] other products;
/// - [#CHECKOUT_ONLY] cart that reached checkout and was left;
/// - [#ANONYMOUS] carts that never got an e-mail, each after two views;
/// - [#LOOKERS] visitors who viewed two products and carted nothing.
///
/// Every seeded cart is empty -- bought or emptied -- so none is on the
/// pipeline board or due an abandoned-cart mail: both read only carts with
/// lines ([com.codenameone.examples.bazaar.pipeline.Pipeline],
/// [com.codenameone.examples.bazaar.cart.AbandonedCarts]).
@Component
public class DemoFunnel {
    static final int BROWSED = 2;
    static final int CHECKOUT_ONLY = 1;
    static final int ANONYMOUS = 2;
    static final int LOOKERS = 3;
    private static final long MINUTE = 60000L;

    private final DataSource db;
    private final Views views;

    public DemoFunnel(DataSource db, Views views) {
        this.db = db;
        this.views = views;
    }

    /// Seeds the browsing behind one online order, `lines` its lines and
    /// `productIds` the store's products to browse among.
    public void behind(StoreOrder o, List<OrderLine> lines, List<String> productIds, int seq)
            throws IOException {
        if (productIds.isEmpty()) {
            return;
        }
        String store = o.getStoreId();
        long at = o.getCreatedAt().toEpochMilli();
        String buyer = cart(store, o.getEmail(), at - 15 * MINUTE, at);
        for (int iter = 0; iter < lines.size(); iter++) {
            views.record(store, lines.get(iter).getProductId(), buyer,
                    Instant.ofEpochMilli(at - 20 * MINUTE));
        }
        int next = seq * 7;
        for (int iter = 0; iter < BROWSED; iter++) {
            views.record(store, productIds.get(next++ % productIds.size()), buyer,
                    Instant.ofEpochMilli(at - 25 * MINUTE));
        }
        for (int iter = 0; iter < CHECKOUT_ONLY; iter++) {
            long when = at - (2 + iter) * 60 * MINUTE;
            String v = cart(store, "browser" + (seq % 7) + "@example.com", when, when);
            twoViews(store, productIds, v, when, next);
            next += 2;
        }
        for (int iter = 0; iter < ANONYMOUS; iter++) {
            long when = at - (4 + iter) * 60 * MINUTE;
            String v = cart(store, "", when, when);
            twoViews(store, productIds, v, when, next);
            next += 2;
        }
        for (int iter = 0; iter < LOOKERS; iter++) {
            long when = at - (6 + iter) * 60 * MINUTE;
            twoViews(store, productIds, "looker-" + Ids.next(), when, next);
            next += 2;
        }
    }

    private void twoViews(String store, List<String> productIds, String visitor, long when,
            int from) throws IOException {
        for (int iter = 0; iter < 2; iter++) {
            views.record(store, productIds.get((from + iter) % productIds.size()), visitor,
                    Instant.ofEpochMilli(when - MINUTE));
        }
    }

    /// An empty cart, as the visitor key its views carry (the cart token).
    private String cart(String storeId, String email, long created, long updated)
            throws IOException {
        String token = Ids.next() + Ids.next();
        db.execute("INSERT INTO cart (id, store_id, username, token, email, discount_code, "
                + "gift_card_code, reminder_sent_at, created_at, updated_at, language, "
                + "use_store_credit) VALUES (?, ?, '', ?, ?, '', '', 0, ?, ?, '', 0)",
                new Object[] {Ids.next(), storeId, token, email == null ? "" : email,
                    Long.valueOf(created), Long.valueOf(updated)});
        return token;
    }
}
