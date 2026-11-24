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

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.FulfillmentDto;
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PackDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.bazaar.account.DemoAccounts;
import com.codenameone.examples.bazaar.api.PipelineCardDto;
import com.codenameone.examples.bazaar.api.PipelineColumnDto;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.live.LiveHub;
import com.codenameone.examples.bazaar.ops.SalesPipeline;
import com.codenameone.examples.bazaar.pipeline.Pipeline;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.DemoPipeline;
import com.codenameone.examples.bazaar.store.DemoStores;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// The walkthrough's self-moving pipeline: off on a test server, a seed that
/// puts cards in every column, and steps that are real transitions, each one
/// published on the live socket.
@BackendTest
class DemoPipelineTest {
    @Autowired
    private DemoPipeline demo;
    @Autowired
    private DemoStores demoStores;
    @Autowired
    private StoreRepository storeRows;
    @Autowired
    private Pipeline pipeline;
    @Autowired
    private LiveHub hub;
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
    private Fulfillments fulfillments;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;
    @Autowired
    private com.codenameone.examples.bazaar.repository.OrderLineRepository lines;

    /// A parcel another store shipped, still in the carrier's hands.
    private String shippedElsewhere() throws Exception {
        Shop s = new Shop("dp-other", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        OrderDto o = s.buy(People.customer(accounts, "dp-other-buyer"), 1);
        List<FulfilLineDto> ls = new ArrayList<FulfilLineDto>();
        ls.add(new FulfilLineDto(lines.findByOrderId(o.id()).get(0).getId(), null, null, 1));
        String id = fulfillments.create(s.owner, s.storeId, o.id(),
                new NewFulfillmentDto(s.locationId, "SHIP", ls)).id();
        fulfillments.pack(s.owner, s.storeId, id, new PackDto(0));
        fulfillments.buyLabel(s.owner, s.storeId, id, new LabelRequestDto(null));
        FulfillmentDto shipped = fulfillments.ship(s.owner, s.storeId, id);
        assertEquals(Fulfillments.SHIPPED, shipped.status());
        return id;
    }

    @Test
    void seedsEveryColumnAndEachStepMovesACardAndIsPublished() throws Exception {
        assertFalse(demo.enabled(), "never on unless bazaar.demo.pipeline asks");
        demoStores.seed(DemoAccounts.MERCHANT, DemoAccounts.CASHIER);
        Store store = storeRows.findByHandle(DemoStores.NORTHWIND);
        String elsewhere = shippedElsewhere();
        long before = hub.changes();
        assertTrue(demo.seed(store.getId()));
        assertEquals(Fulfillments.SHIPPED, fulfillments.detail(elsewhere).status(),
                "the seed runs its own parcels' clock, never another store's");
        assertFalse(demo.seed(store.getId()), "seeds once");
        assertTrue(hub.changes() > before, "the seeded orders were published");
        Map<String, Integer> counts = counts(board(store));
        String[] filled = {SalesPipeline.BROWSING, SalesPipeline.CHECKOUT, SalesPipeline.PAID,
            SalesPipeline.FULFILLING, SalesPipeline.SHIPPED, SalesPipeline.DELIVERED};
        for (String column : filled) {
            assertTrue(counts.get(column) != null && counts.get(column) > 0,
                    () -> column + " empty: " + counts);
        }
        Map<String, String> was = columns(board(store));
        int moved = 0;
        int acted = 0;
        for (int iter = 0; iter < 6; iter++) {
            long heard = hub.changes();
            String kind = demo.step(store.getId());
            assertTrue(DemoPipeline.IDLE.equals(kind) || hub.changes() > heard,
                    () -> kind + " was not published");
            if (!DemoPipeline.IDLE.equals(kind)) {
                acted++;
            }
            Map<String, String> now = columns(board(store));
            for (Map.Entry<String, String> e : now.entrySet()) {
                String old = was.get(e.getKey());
                if (old == null || !old.equals(e.getValue())) {
                    moved++;
                }
            }
            was = now;
        }
        // A step is a pack, a label, a ship or a new order; labelling keeps a
        // card in Fulfilling, and shipping one line of a two-line order keeps
        // it there as partially fulfilled, so not every step changes a column.
        final int total = moved;
        final int steps = acted;
        assertTrue(steps >= 5, () -> "steps that did something: " + steps);
        assertTrue(total >= 2, () -> "cards arrived or moved: " + total);
    }

    private List<PipelineColumnDto> board(Store store) throws Exception {
        return pipeline.board(DemoAccounts.MERCHANT, store.getId(), null, 0);
    }

    private static Map<String, Integer> counts(List<PipelineColumnDto> board) {
        Map<String, Integer> out = new HashMap<String, Integer>();
        for (PipelineColumnDto c : board) {
            out.put(c.id(), c.total());
        }
        return out;
    }

    private static Map<String, String> columns(List<PipelineColumnDto> board) {
        Map<String, String> out = new HashMap<String, String>();
        for (PipelineColumnDto c : board) {
            for (PipelineCardDto card : c.cards()) {
                out.put(card.id(), c.id());
            }
        }
        return out;
    }
}
