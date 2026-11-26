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
import com.codenameone.examples.bazaar.api.AutomationDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.ValidationDto;
import com.codenameone.examples.bazaar.automation.Automations;
import com.codenameone.examples.bazaar.automation.DemoGraphs;
import com.codenameone.examples.bazaar.cart.AbandonedCarts;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Supplier;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.SupplierRepository;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Automations: the demo graphs run on real orders, the checks a graph must
/// pass, and the abandoned-cart sweep.
@BackendTest
class AutomationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private DataSource db;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private Automations automations;
    @Autowired
    private AbandonedCarts abandoned;
    @Autowired
    private StoreOrderRepository orders;
    @Autowired
    private CustomerRepository customers;
    @Autowired
    private SupplierRepository suppliers;

    private Shop shop(String name, int stock) throws Exception {
        return new Shop(name, stock, accounts, stores, pricing, db, inventory, carts, checkouts,
                mvc);
    }

    private long count(String sql, Object... args) throws Exception {
        Map row = db.queryOne(sql, args);
        return Rows.number(row.get("n"));
    }

    private static boolean tagged(String tags, String tag) {
        return ("," + tags + ",").indexOf("," + tag + ",") >= 0;
    }

    @Test
    void ordersOverTheThresholdAreTaggedVipAndSmallerOnesAreNot() throws Exception {
        Shop s = shop("auto-vip", 100);
        automations.seedDemo(s.storeId);
        assertEquals(2, automations.list(s.owner, s.storeId).size());

        String big = People.customer(accounts, "auto-vip-big");
        OrderDto large = s.buy(big, 25);
        StoreOrder lo = orders.findById(large.id());
        assertTrue(lo.getTotalCents() > 20000, "total " + lo.getTotalCents());
        assertTrue(tagged(lo.getTags(), "VIP"), "order tags: " + lo.getTags());
        Customer bc = customers.findById(lo.getCustomerId());
        assertTrue(tagged(bc.getTags(), "VIP"), "customer tags: " + bc.getTags());

        String small = People.customer(accounts, "auto-vip-small");
        StoreOrder so = orders.findById(s.buy(small, 1).id());
        assertTrue(so.getTotalCents() < 20000);
        assertFalse(tagged(so.getTags(), "VIP"), "order tags: " + so.getTags());
        assertFalse(tagged(customers.findById(so.getCustomerId()).getTags(), "VIP"));

        List<AutomationDto> after = automations.list(s.owner, s.storeId);
        int vipRuns = 0;
        for (AutomationDto a : after) {
            if ("order.created".equals(a.triggerKind())) {
                vipRuns = a.runs();
                assertEquals(2, automations.activity(s.owner, s.storeId, a.id()).size());
            }
        }
        assertEquals(2, vipRuns);
    }

    @Test
    void lowStockDraftsOnePurchaseOrder() throws Exception {
        Shop s = shop("auto-low", 10);
        Supplier sup = new Supplier();
        sup.setId(Ids.next());
        sup.setStoreId(s.storeId);
        sup.setName("Clay Co");
        suppliers.insert(sup);
        db.execute("UPDATE inventory_level SET reorder_point = 5 WHERE variant_id = ?",
                new Object[] {s.variantId});
        automations.seedDemo(s.storeId);

        s.buy(People.customer(accounts, "auto-low-a"), 6);
        assertEquals(1, count("SELECT COUNT(*) AS n FROM purchase_order WHERE store_id = ? "
                + "AND status = 'DRAFT'", s.storeId));
        // Available went 10 -> 4 with a reorder point of 5: order up to twice it.
        assertEquals(6, count("SELECT SUM(l.quantity) AS n FROM purchase_line l JOIN "
                + "purchase_order p ON p.id = l.purchase_order_id WHERE p.store_id = ? "
                + "AND l.variant_id = ?", s.storeId, s.variantId));

        // Already low: no second crossing, no second draft.
        s.buy(People.customer(accounts, "auto-low-b"), 1);
        assertEquals(1, count("SELECT COUNT(*) AS n FROM purchase_order WHERE store_id = ?",
                s.storeId));
    }

    @Test
    void aCycleIsRejected() throws Exception {
        String json = "{\"nodes\":["
                + "{\"id\":\"t\",\"style\":\"order.created\",\"outputs\":[{\"id\":\"out\"}]},"
                + "{\"id\":\"a\",\"style\":\"tag.order\",\"data\":{\"tag\":\"x\"},"
                + "\"inputs\":[{\"id\":\"in\"}],\"outputs\":[{\"id\":\"out\"}]},"
                + "{\"id\":\"b\",\"style\":\"tag.customer\",\"data\":{\"tag\":\"y\"},"
                + "\"inputs\":[{\"id\":\"in\"}],\"outputs\":[{\"id\":\"out\"}]}],"
                + "\"edges\":["
                + "{\"id\":\"e1\",\"from\":\"t\",\"fromPort\":\"out\",\"to\":\"a\",\"toPort\":\"in\"},"
                + "{\"id\":\"e2\",\"from\":\"a\",\"fromPort\":\"out\",\"to\":\"b\",\"toPort\":\"in\"},"
                + "{\"id\":\"e3\",\"from\":\"b\",\"fromPort\":\"out\",\"to\":\"a\",\"toPort\":\"in\"}]}";
        ValidationDto v = Automations.check(json);
        assertFalse(v.valid());
        boolean loop = false;
        for (String e : v.errors()) {
            loop |= e.indexOf("loop") >= 0;
        }
        assertTrue(loop, String.valueOf(v.errors()));

        assertFalse(Automations.check("{\"nodes\":[{\"id\":\"a\",\"style\":\"tag.order\","
                + "\"data\":{\"tag\":\"x\"}}],\"edges\":[]}").valid(), "no trigger");
        assertTrue(Automations.check(DemoGraphs.vip(20000)).valid(),
                String.valueOf(Automations.check(DemoGraphs.vip(20000)).errors()));
        assertTrue(Automations.check(DemoGraphs.lowStock()).valid());

        Shop s = shop("auto-cycle", 0);
        try {
            automations.create(s.owner, s.storeId, new AutomationDto(null, "Loop", "", json, true,
                    0, 0, 0));
            fail("an active automation that loops was saved");
        } catch (ResponseStatusException expected) {
            assertEquals(400, expected.getStatus());
        }
        // A draft may be unfinished.
        AutomationDto draft = automations.create(s.owner, s.storeId, new AutomationDto(null,
                "Loop draft", "", json, false, 0, 0, 0));
        assertFalse(draft.active());
    }

    @Test
    void anAbandonedCartFiresOnce() throws Exception {
        Shop s = shop("auto-cart", 10);
        String graph = "{\"nodes\":["
                + "{\"id\":\"t\",\"style\":\"cart.abandoned\",\"outputs\":[{\"id\":\"out\"}]},"
                + "{\"id\":\"m\",\"style\":\"email.customer\",\"data\":{\"subject\":\"Come back\","
                + "\"message\":\"Your cart misses you\"},\"inputs\":[{\"id\":\"in\"}],"
                + "\"outputs\":[{\"id\":\"out\"}]}],\"edges\":[{\"id\":\"e\",\"from\":\"t\","
                + "\"fromPort\":\"out\",\"to\":\"m\",\"toPort\":\"in\"}]}";
        AutomationDto a = automations.create(s.owner, s.storeId, new AutomationDto(null,
                "Win back", "", graph, true, 0, 0, 0));
        assertEquals("cart.abandoned", a.triggerKind());
        String shopper = People.customer(accounts, "auto-cart-shopper");
        carts.add(s.storeId, shopper, null, new CartLineChangeDto(s.variantId, 1));

        // Creating it is the first line of its activity.
        int base = automations.activity(s.owner, s.storeId, a.id()).size();
        long now = System.currentTimeMillis();
        abandoned.sweepAbandoned(now);
        assertEquals(base, automations.activity(s.owner, s.storeId, a.id()).size(),
                "a fresh cart is not abandoned");

        abandoned.sweepAbandoned(now + AbandonedCarts.IDLE_MILLIS + 60000);
        assertEquals(base + 1, automations.activity(s.owner, s.storeId, a.id()).size());
        abandoned.sweepAbandoned(now + 3 * AbandonedCarts.IDLE_MILLIS);
        assertEquals(base + 1, automations.activity(s.owner, s.storeId, a.id()).size(),
                "the reminder fires once");
    }
}
