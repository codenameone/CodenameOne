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
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.analytics.Analytics;
import com.codenameone.examples.bazaar.analytics.Views;
import com.codenameone.examples.bazaar.api.BreakdownDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.FunnelStepDto;
import com.codenameone.examples.bazaar.api.InsightsDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PipelineCardDto;
import com.codenameone.examples.bazaar.api.PipelineColumnDto;
import com.codenameone.examples.bazaar.api.PipelineMoveDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.ops.SalesPipeline;
import com.codenameone.examples.bazaar.pipeline.Pipeline;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.CartRepository;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The sales pipeline: columns worked out from carts and orders, moves that
/// do the real thing and refuse the rest, the intent score, and the
/// database-counted channel mix and funnel.
@BackendTest
class PipelineTest {
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private Inventory inventory;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;
    @Autowired
    private Pipeline pipeline;
    @Autowired
    private Views views;
    @Autowired
    private Analytics analytics;
    @Autowired
    private CartRepository cartRows;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 20, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static PipelineCardDto card(List<PipelineColumnDto> board, String id) {
        for (PipelineColumnDto c : board) {
            for (PipelineCardDto card : c.cards()) {
                if (card.id().equals(id)) {
                    return card;
                }
            }
        }
        return null;
    }

    private static String dump(List<PipelineColumnDto> board) {
        StringBuilder out = new StringBuilder();
        for (PipelineColumnDto c : board) {
            out.append(c.id()).append('=');
            for (PipelineCardDto card : c.cards()) {
                out.append(card.id()).append('/').append(card.customer()).append(' ');
            }
            out.append("; ");
        }
        return out.toString();
    }

    private void refused(int status, Runnable r) {
        try {
            r.run();
            fail("expected " + status);
        } catch (ResponseStatusException e) {
            assertEquals(status, e.getStatus(), e.getReason());
        }
    }

    private void move(Shop s, String id, String to) {
        try {
            pipeline.move(s.owner, s.storeId, new PipelineMoveDto(id, to));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void cartsAndOrdersSitWhereTheirStateSaysAndMoveOnlyByDoingIt() throws Exception {
        Shop s = shop("pipe-shop");
        // A guest who put something in a cart and left nothing to reach them by.
        // A new cart gets a token of its own; later calls name that one.
        String guestToken = carts.add(s.storeId, null, null,
                new CartLineChangeDto(s.variantId, 1)).token();
        Cart guest = carts.cart(s.storeId, null, guestToken);
        // A signed-in shopper who looked around twice before adding two.
        String shopper = People.customer(accounts, "pipe-shopper");
        long now = System.currentTimeMillis();
        long hour = 3600000L;
        views.record(s.storeId, s.productId, shopper, Instant.ofEpochMilli(now - 5 * hour));
        views.record(s.storeId, s.productId, shopper, Instant.ofEpochMilli(now - 5 * hour + 240000));
        views.record(s.storeId, s.productId, shopper, Instant.ofEpochMilli(now - hour));
        views.record(s.storeId, s.productId, shopper, Instant.ofEpochMilli(now - hour + 360000));
        carts.add(s.storeId, shopper, null, new CartLineChangeDto(s.variantId, 2));
        Cart signedIn = carts.cart(s.storeId, shopper, null);
        // A buyer whose order is paid and waiting.
        OrderDto o = s.buy(People.customer(accounts, "pipe-buyer"), 1);

        List<PipelineColumnDto> b = pipeline.board(s.owner, s.storeId, null, 0);
        assertEquals(8, b.size());
        final List<PipelineColumnDto> first = b;
        assertNotNull(card(first, guest.getId()), () -> guest.getId() + " not on " + dump(first));
        assertEquals(SalesPipeline.BROWSING, card(b, guest.getId()).column());
        PipelineCardDto hot = card(b, signedIn.getId());
        assertEquals(SalesPipeline.CHECKOUT, hot.column());
        assertEquals(SalesPipeline.PAID, card(b, o.id()).column());
        assertEquals(-1, card(b, o.id()).intent());
        // 4 views (12), a second visit (5), 10 minutes (10), value > 0.
        assertTrue(hot.valueCents() > 0);
        SalesPipeline.Signals expect = new SalesPipeline.Signals();
        expect.views = 4;
        expect.visits = 2;
        expect.minutes = 10;
        expect.valueCents = hot.valueCents();
        assertEquals(SalesPipeline.score(expect), hot.intent());
        assertEquals("views:4", hot.reasons().get(0));
        assertTrue(hot.reasons().contains("minutes:10"), hot.reasons().toString());
        assertTrue(hot.reasons().contains("visits:2"), hot.reasons().toString());

        // Nobody to write to, a skipped step, a move backwards: all refused.
        refused(409, () -> move(s, guest.getId(), SalesPipeline.REMINDED));
        refused(409, () -> move(s, o.id(), SalesPipeline.SHIPPED));
        refused(409, () -> move(s, o.id(), SalesPipeline.CHECKOUT));
        refused(404, () -> move(s, "no-such-card", SalesPipeline.REMINDED));

        // The recovery mail goes once; the card is then in Reminded and stays.
        move(s, signedIn.getId(), SalesPipeline.REMINDED);
        assertFalse(Instant.EPOCH.equals(cartRows.findById(signedIn.getId()).getReminderSentAt()));
        b = pipeline.board(s.owner, s.storeId, null, 0);
        assertEquals(SalesPipeline.REMINDED, card(b, signedIn.getId()).column());
        refused(409, () -> move(s, signedIn.getId(), SalesPipeline.REMINDED));

        // Fulfilment, one step at a time, each the real action.
        move(s, o.id(), SalesPipeline.FULFILLING);
        assertEquals(SalesPipeline.FULFILLING, card(pipeline.board(s.owner, s.storeId, null, 0),
                o.id()).column());
        move(s, o.id(), SalesPipeline.SHIPPED);
        move(s, o.id(), SalesPipeline.DELIVERED);
        assertEquals(SalesPipeline.DELIVERED, card(pipeline.board(s.owner, s.storeId, null, 0),
                o.id()).column());

        // One column, paged; and the route answers the owner.
        List<PipelineColumnDto> one = pipeline.board(s.owner, s.storeId, SalesPipeline.BROWSING, 0);
        assertEquals(1, one.size());
        assertEquals(1, one.get(0).total());
        assertFalse(one.get(0).more());
        refused(400, () -> {
            try {
                pipeline.board(s.owner, s.storeId, "NOPE", 0);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        });
        assertEquals(200, Rest.statusOf(mvc, People.asMerchant(get("/api/merchant/stores/"
                + s.storeId + "/pipeline?column=PAID&page=0"), s.owner)));
    }

    @Test
    void insightsAreCountedByTheDatabase() throws Exception {
        Shop s = shop("insight-shop");
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < 3; iter++) {
            views.record(s.storeId, s.productId, "visitor-" + iter,
                    Instant.ofEpochMilli(now - 60000L * (iter + 1)));
        }
        // Long before the period: not counted.
        views.record(s.storeId, s.productId, "visitor-old", Instant.ofEpochMilli(now
                - 90L * 86400000L));
        carts.add(s.storeId, null, "insight-guest", new CartLineChangeDto(s.variantId, 1));
        OrderDto o = s.buy(People.customer(accounts, "insight-buyer"), 2);

        InsightsDto in = analytics.insights(s.owner, s.storeId, 0, 0);
        List<FunnelStepDto> f = in.funnel();
        assertEquals("views", f.get(0).step());
        assertEquals(3, f.get(0).count());
        assertEquals("orders", f.get(3).step());
        assertEquals(1, f.get(3).count());
        int cartsMade = f.get(1).count();
        assertTrue(cartsMade >= 2, "the guest's cart and the buyer's: " + cartsMade);
        assertEquals(cartsMade - 1, f.get(2).count(), "only the guest never reached checkout");
        BreakdownDto app = null;
        BreakdownDto web = null;
        for (BreakdownDto c : in.channels()) {
            if ("app".equals(c.key())) {
                app = c;
            } else if ("web".equals(c.key())) {
                web = c;
            }
        }
        assertNotNull(app);
        assertEquals(5, in.channels().size());
        assertEquals(1, app.orders());
        assertEquals(o.subtotalCents() - o.discountCents(), app.salesCents());
        assertEquals(0, web.orders());
    }
}
