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
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.alerts.StockAlerts;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.PreorderDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Pre-orders at checkout: a variant out of stock and open for pre-order
/// sells ahead of stock, through the API and the HTML storefront, reserves
/// nothing, and stops at the pre-order allowance.
@BackendTest
class PreorderTest {
    private static final long DAY = 86400000L;

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
    private StockAlerts alerts;

    private Shop soldOut(String name, int limit, int incoming) throws Exception {
        Shop s = new Shop(name, 0, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
        alerts.savePreorder(s.owner, s.storeId, s.variantId, new PreorderDto(s.variantId, true,
                System.currentTimeMillis() + 30 * DAY, StockAlerts.AUTHORIZE, limit, incoming, ""));
        return s;
    }

    private long preorderUnits(Shop s) throws Exception {
        return Rows.number(db.queryOne("SELECT COALESCE(SUM(l.quantity), 0) AS n FROM order_line l "
                + "JOIN store_order o ON o.id = l.order_id WHERE o.store_id = ? AND l.preorder = 1 "
                + "AND l.location_id = ''", new Object[] {s.storeId}).get("n"));
    }

    private long committed(Shop s) throws Exception {
        return Rows.number(db.queryOne("SELECT COALESCE(SUM(committed), 0) AS n FROM inventory_level "
                + "WHERE variant_id = ?", new Object[] {s.variantId}).get("n"));
    }

    @Test
    void theApiSellsAheadOfStockUpToTheAllowance() throws Exception {
        Shop s = soldOut("pre-api", 3, 0);
        String user = People.customer(accounts, "pre-api-buyer");
        CartDto c = carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 2));
        assertTrue(c.lines().get(0).preorder(), "the line is a pre-order");
        assertTrue(c.lines().get(0).available(), "and can be bought");
        carts.update(s.storeId, user, null, c.lines().get(0).id(), 0);
        s.buy(user, 2);
        assertEquals(2, preorderUnits(s), "placed as a pre-order line with no location");
        assertEquals(0, committed(s), "nothing is reserved");
        // One unit of the allowance is left: two more are refused.
        String late = People.customer(accounts, "pre-api-late");
        try {
            s.buy(late, 2);
            fail("the allowance is spent");
        } catch (ResponseStatusException expected) {
            assertTrue(expected.getStatus() >= 400 && expected.getStatus() < 500,
                    "answered " + expected.getStatus());
        }
        CartDto left = carts.get(s.storeId, late, null);
        assertFalse(left.lines().get(0).available(), "the cart says so");
        assertEquals(2, preorderUnits(s));
    }

    @Test
    void theStorefrontShowsAndPlacesAPreorder() throws Exception {
        Shop s = soldOut("pre-web", 0, 5);
        Browser b = new Browser(mvc);
        b.assertPage(b.get("/s/pre-web/products/pre-web-mug"), "Pre-order");
        MockResponse added = b.send(Browser.form("/s/pre-web/cart/add", "variantId",
                s.variantId, "quantity", "1"));
        assertTrue(added.getStatus() == 302 || added.getStatus() == 303, "add " + added.getStatus());
        b.assertPage(b.get("/s/pre-web/cart"), "Pre-order");
        MockResponse page = b.get("/s/pre-web/checkout");
        b.assertPage(page, "Place order");
        String html = page.getContentAsString();
        String marker = "name=\"idempotencyKey\" value=\"";
        int at = html.indexOf(marker);
        String key = html.substring(at + marker.length(), html.indexOf('"', at + marker.length()));
        MockResponse placed = b.post("/s/pre-web/checkout/place", "email", "pre@example.com",
                "name", "Ada Lovelace", "line1", "1 Main St", "city", "Albany", "region", "NY",
                "postal", "12207", "country", "US", "cardNumber", "4242 4242 4242 4242",
                "cardExpiry", "12/39", "cardCvc", "123", "idempotencyKey", key);
        assertEquals(303, placed.getStatus(), placed.getContentAsString());
        assertEquals(1, preorderUnits(s));
        assertEquals(0, committed(s));
    }
}
