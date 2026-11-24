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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.bazaar.account.DemoAccounts;
import com.codenameone.examples.bazaar.api.RewardsDto;
import com.codenameone.examples.bazaar.domain.Fulfillment;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.live.LiveHub;
import com.codenameone.examples.bazaar.repository.FulfillmentRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.DemoStores;
import com.codenameone.examples.bazaar.store.RewardsEndpoint;
import com.codenameone.examples.bazaar.store.ShopperDemo;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The walkthrough's shopper side: off on a test server, a history that the
/// rewards endpoint counts, and an order taken step by step to Delivered with
/// every step published on the live socket. A user of its own, so no other
/// test's demo shopper is touched.
@BackendTest
class ShopperDemoTest {
    private static final String WHO = "rewards-walkthrough@bazaar.example";

    @Autowired
    private ShopperDemo demo;
    @Autowired
    private DemoStores demoStores;
    @Autowired
    private RewardsEndpoint rewards;
    @Autowired
    private StoreOrderRepository orderRows;
    @Autowired
    private FulfillmentRepository fulfillmentRows;
    @Autowired
    private LiveHub hub;
    @Autowired
    private com.codenameone.examples.bazaar.repository.CustomerAddressRepository addressRows;
    @Autowired
    private com.codenameone.examples.bazaar.repository.StoreRepository storeRows;

    @Test
    void seedsAHistoryTheRewardsCountAndFollowsAnOrderToTheDoor() throws Exception {
        assertFalse(demo.enabled(), "never on unless bazaar.demo.shopper asks");
        demoStores.seed(DemoAccounts.MERCHANT, DemoAccounts.CASHIER);
        RewardsDto none = rewards.rewards(WHO, "");
        assertEquals(0, none.orders());
        assertTrue(demo.seed(WHO));
        assertFalse(demo.seed(WHO), "seeds once");

        RewardsDto r = rewards.rewards(WHO, "");
        assertEquals(5, r.orders(), "six seeded, one cancelled");
        assertEquals(2, r.stores());
        assertTrue(r.delivered() >= 3, () -> "delivered " + r.delivered());
        assertEquals(5, r.wishlist());
        assertEquals(2, addressRows.findByUsername(WHO).size(), "home and work are saved");
        assertTrue(storeRows.findByHandle(DemoStores.FENNEL_FIG) != null,
                "the third store, for the Explorer badge");
        assertEquals(0, rewards.rewards(null, "").orders(), "a guest has nothing");

        // The seeded orders are history: nothing moves them.
        assertEquals(ShopperDemo.IDLE, demo.advance(WHO, 0));

        // Make the packed one look like an order placed in the tour.
        StoreOrder live = null;
        for (StoreOrder o : orderRows.findByUsernameOrderByCreatedAtDesc(WHO)) {
            List<Fulfillment> fs = fulfillmentRows.findByOrderId(o.getId());
            if (!fs.isEmpty() && Fulfillments.PACKED.equals(fs.get(0).getStatus())) {
                live = o;
            }
        }
        assertTrue(live != null, "a packed order was seeded");
        live.setClientId("tour-" + live.getId());
        orderRows.save(live);
        String[] expected = {ShopperDemo.LABELLED, ShopperDemo.SHIPPED, ShopperDemo.SCANNED,
            ShopperDemo.SCANNED, ShopperDemo.SCANNED};
        for (String kind : expected) {
            long heard = hub.changes();
            assertEquals(kind, demo.advance(WHO, 0));
            assertTrue(hub.changes() > heard, () -> kind + " was not published");
        }
        Fulfillment f = fulfillmentRows.findByOrderId(live.getId()).get(0);
        assertEquals(Fulfillments.DELIVERED, f.getStatus());
        assertEquals(ShopperDemo.IDLE, demo.advance(WHO, 0), "delivered is the end");
    }
}
