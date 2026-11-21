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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.FulfillmentDto;
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.StaffDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The merchant back office under `/admin`: each page's main action, a store
/// the caller does not work at (and its rows reached through their own store's
/// address) answering 404, a post without its CSRF token refused, a shopper
/// refused, and a fulfilment member kept to what that role may do.
@BackendTest
class WebAdminTest {
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
    private Orders orders;
    @Autowired
    private Fulfillments fulfillments;
    @Autowired
    private CustomerRepository customers;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 20, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private Browser signedIn(String user) throws Exception {
        Browser b = new Browser(mvc);
        b.signIn(user, People.PASSWORD);
        return b;
    }

    private static void redirected(MockResponse r) throws Exception {
        assertTrue(r.getStatus() == 302 || r.getStatus() == 303,
                "answered " + r.getStatus() + " " + r.getContentAsString());
    }

    private int available(String variantId, String locationId) throws Exception {
        Map row = db.queryOne("SELECT on_hand FROM inventory_level WHERE variant_id = ? AND "
                + "location_id = ?", new Object[] {variantId, locationId});
        return ((Number) row.get("on_hand")).intValue();
    }

    @Test
    void dashboardShowsKpisAndServerDrawnCharts() throws Exception {
        Shop s = shop("wad-dash");
        String buyer = People.customer(accounts, "wad-dash-buyer");
        s.buy(buyer, 2);
        Browser b = signedIn(s.owner);

        MockResponse home = b.get("/admin");
        redirected(home);
        assertEquals("/admin/" + s.storeId, home.getRedirectedUrl());

        MockResponse dash = b.get("/admin/" + s.storeId);
        b.assertPage(dash, "Net sales");
        b.assertPage(dash, "Average order");
        b.assertPage(dash, "<polyline");
        b.assertPage(dash, "Top products");
        b.assertPage(dash, "Mug");
        assertFalse(dash.getContentAsString().contains("<script src=\"http"));
        assertEquals("private, no-store", dash.getHeader("Cache-Control"));
        assertTrue(dash.getHeader("Content-Security-Policy").contains("nonce-"));
    }

    @Test
    void ordersTableFiltersSortsExportsAndTheDetailFulfilsRefundsAndCancels() throws Exception {
        Shop s = shop("wad-orders");
        String buyer = People.customer(accounts, "wad-orders-buyer");
        OrderDto first = s.buy(buyer, 2);
        OrderDto second = s.buy(buyer, 1);
        Browser b = signedIn(s.owner);
        String base = "/admin/" + s.storeId + "/orders";

        MockResponse list = b.get(base);
        b.assertPage(list, "#" + first.number());
        b.assertPage(list, "aria-sort");
        assertEquals("private, no-store", list.getHeader("Cache-Control"));
        MockResponse cancelled = b.get(base + "?status=CANCELLED");
        b.assertPage(cancelled, "No orders match.");
        MockResponse sorted = b.get(base + "?sort=total&dir=desc&since=30");
        b.assertPage(sorted, "#" + second.number());
        MockResponse csv = b.get("/admin/" + s.storeId + "/export/orders?sort=number");
        assertEquals(200, csv.getStatus());
        assertTrue(csv.getContentType().startsWith("text/csv"), csv.getContentType());
        assertTrue(csv.getContentAsString().contains(Integer.toString(first.number())));

        String detail = base + "/" + first.id();
        b.assertPage(b.get(detail), "Create fulfilment");
        String line = orders.detailOf(first.id()).lines().get(0).id();
        redirected(b.post(detail + "/fulfil", "location", s.locationId, "qty_" + line, "2"));
        List<FulfillmentDto> made = fulfillments.ofOrder(s.owner, s.storeId, first.id());
        assertEquals(1, made.size());

        FulfillmentDto f = fulfillments.buyLabel(s.owner, s.storeId, made.get(0).id(),
                new LabelRequestDto("GROUND"));
        fulfillments.ship(s.owner, s.storeId, f.id());
        b.assertPage(b.get(detail), "Put the items back in stock");
        redirected(b.post(detail + "/refund", "refund_" + line, "1", "shipping", "",
                "restock", "on", "reason", "Chipped"));
        OrderDto refunded = orders.detailOf(first.id());
        assertEquals(1, refunded.lines().get(0).refundedQty());
        assertEquals(Orders.PARTIALLY_REFUNDED, refunded.financial());

        // A refund the service refuses is a message on the page, not an error.
        redirected(b.post(detail + "/refund", "refund_" + line, "9"));
        assertEquals(1, orders.detailOf(first.id()).lines().get(0).refundedQty());
        redirected(b.post(detail + "/refund", "shipping", "1.234"));

        String other = base + "/" + second.id();
        b.assertPage(b.get(other), "Cancel order");
        redirected(b.post(other + "/cancel"));
        assertEquals(Orders.CANCELLED, orders.detailOf(second.id()).status());
    }

    @Test
    void productEditorSavesWithoutLosingWhatItDoesNotShowAndAdjustsStock() throws Exception {
        Shop s = shop("wad-prod");
        Browser b = signedIn(s.owner);
        String list = "/admin/" + s.storeId + "/products";
        b.assertPage(b.get(list), "Mug");
        b.assertPage(b.get(list + "?status=ARCHIVED"), "No products match.");
        String page = list + "/" + s.productId;
        MockResponse editor = b.get(page);
        b.assertPage(editor, "Inventory by location");
        b.assertPage(editor, "10.00");

        redirected(b.post(page, "title", "Big mug", "description", "Holds a lot", "status",
                "DRAFT", "v0_id", s.variantId, "v0_title", "Blue", "v0_sku", s.sku,
                "v0_price", "12.50"));
        Map p = db.queryOne("SELECT title, status FROM product WHERE id = ?",
                new Object[] {s.productId});
        assertEquals("Big mug", p.get("title"));
        assertEquals("DRAFT", p.get("status"));
        Map v = db.queryOne("SELECT price_cents, barcode, cost_cents FROM variant WHERE id = ?",
                new Object[] {s.variantId});
        assertEquals(1250L, ((Number) v.get("price_cents")).longValue());
        assertEquals("0123456789012", v.get("barcode"));
        assertEquals(400L, ((Number) v.get("cost_cents")).longValue());
        assertEquals(20, available(s.variantId, s.locationId));

        // A bad price changes nothing.
        redirected(b.post(page, "title", "Big mug", "status", "DRAFT", "v0_price", "abc"));
        v = db.queryOne("SELECT price_cents FROM variant WHERE id = ?", new Object[] {s.variantId});
        assertEquals(1250L, ((Number) v.get("price_cents")).longValue());

        redirected(b.post(page + "/adjust", "variant", s.variantId, "location", s.locationId,
                "delta", "3", "reason", "RESTOCK"));
        assertEquals(23, available(s.variantId, s.locationId));
        b.assertPage(b.get("/admin/" + s.storeId + "/inventory"), s.sku);
        assertEquals(200, b.get("/admin/" + s.storeId + "/export/products").getStatus());
    }

    @Test
    void customersListAndDetail() throws Exception {
        Shop s = shop("wad-cust");
        String buyer = People.customer(accounts, "wad-cust-buyer");
        OrderDto o = s.buy(buyer, 1, "wad-cust@example.com");
        Customer c = customers.findByStoreIdAndUsername(s.storeId, buyer);
        Browser b = signedIn(s.owner);
        b.assertPage(b.get("/admin/" + s.storeId + "/customers"), "wad-cust@example.com");
        MockResponse page = b.get("/admin/" + s.storeId + "/customers/" + c.getId());
        b.assertPage(page, "Lifetime value");
        b.assertPage(page, "#" + o.number());
    }

    @Test
    void anotherStoresPagesAndRowsAnswer404AndChangeNothing() throws Exception {
        Shop mine = shop("wad-idor-a");
        Shop theirs = shop("wad-idor-b");
        String buyer = People.customer(accounts, "wad-idor-buyer");
        OrderDto o = theirs.buy(buyer, 1);
        Customer c = customers.findByStoreIdAndUsername(theirs.storeId, buyer);
        int stock = available(theirs.variantId, theirs.locationId);
        Browser b = signedIn(mine.owner);
        String a = "/admin/" + mine.storeId;
        String other = "/admin/" + theirs.storeId;

        String[] gets = {other, other + "/orders", other + "/products", other + "/inventory",
            other + "/customers", other + "/orders/" + o.id(), other + "/export/orders",
            a + "/orders/" + o.id(), a + "/products/" + theirs.productId,
            a + "/customers/" + c.getId()};
        for (int iter = 0; iter < gets.length; iter++) {
            assertEquals(404, b.get(gets[iter]).getStatus(), gets[iter]);
        }
        // A page first, so the posts carry a token and it is the 404 that answers.
        b.get(a);
        String[] posts = {a + "/orders/" + o.id() + "/cancel", other + "/orders/" + o.id()
            + "/cancel", a + "/orders/" + o.id() + "/fulfil", a + "/orders/" + o.id() + "/refund",
            a + "/products/" + theirs.productId, other + "/products/" + theirs.productId};
        for (int iter = 0; iter < posts.length; iter++) {
            assertEquals(404, b.post(posts[iter], "title", "Mine now", "status", "ACTIVE")
                    .getStatus(), posts[iter]);
        }
        assertEquals(404, b.post(a + "/products/" + theirs.productId + "/adjust", "variant",
                theirs.variantId, "location", theirs.locationId, "delta", "-5").getStatus());
        assertEquals(404, b.post(a + "/products/" + mine.productId + "/adjust", "variant",
                theirs.variantId, "location", theirs.locationId, "delta", "-5").getStatus());
        assertEquals(Orders.OPEN, orders.detailOf(o.id()).status());
        assertEquals("Mug", db.queryOne("SELECT title FROM product WHERE id = ?",
                new Object[] {theirs.productId}).get("title"));
        assertEquals(stock, available(theirs.variantId, theirs.locationId));
    }

    @Test
    void aPostWithoutItsTokenIsRefused() throws Exception {
        Shop s = shop("wad-csrf");
        String buyer = People.customer(accounts, "wad-csrf-buyer");
        OrderDto o = s.buy(buyer, 1);
        Browser b = signedIn(s.owner);
        b.get("/admin/" + s.storeId);
        assertEquals(403, b.postWithoutToken("/admin/" + s.storeId + "/orders/" + o.id()
                + "/cancel").getStatus());
        assertEquals(403, b.postWithoutToken("/admin/" + s.storeId + "/products/" + s.productId,
                "title", "Forged", "status", "ACTIVE").getStatus());
        assertEquals(Orders.OPEN, orders.detailOf(o.id()).status());
    }

    @Test
    void aShopperIsRefusedTheBackOffice() throws Exception {
        Shop s = shop("wad-shopper");
        String shopper = People.customer(accounts, "wad-shopper-buyer");
        OrderDto o = s.buy(shopper, 1);
        Browser b = signedIn(shopper);
        assertEquals(403, b.get("/admin").getStatus());
        assertEquals(403, b.get("/admin/" + s.storeId).getStatus());
        assertEquals(403, b.get("/admin/" + s.storeId + "/orders/" + o.id()).getStatus());
        b.get("/account");
        assertEquals(403, b.post("/admin/" + s.storeId + "/orders/" + o.id() + "/cancel")
                .getStatus());
        assertEquals(Orders.OPEN, orders.detailOf(o.id()).status());
    }

    @Test
    void fulfilmentStaffWorkOrdersAndStockButNotMoney() throws Exception {
        Shop s = shop("wad-staff");
        String buyer = People.customer(accounts, "wad-staff-buyer");
        OrderDto o = s.buy(buyer, 1);
        String packer = People.customer(accounts, "wad-staff-packer");
        stores.saveStaff(s.owner, s.storeId, new StaffDto(null, packer, null, "FULFILMENT", null,
                null, false));
        int stock = available(s.variantId, s.locationId);
        Browser b = signedIn(packer);
        String base = "/admin/" + s.storeId;
        b.assertPage(b.get(base), "does not include its sales figures");
        b.assertPage(b.get(base + "/orders"), "#" + o.number());
        assertEquals(403, b.post(base + "/orders/" + o.id() + "/cancel").getStatus());
        assertEquals(Orders.OPEN, orders.detailOf(o.id()).status());
        assertEquals(403, b.post(base + "/products/" + s.productId, "title", "Renamed",
                "status", "ACTIVE").getStatus());
        redirected(b.post(base + "/products/" + s.productId + "/adjust", "variant", s.variantId,
                "location", s.locationId, "delta", "-2", "reason", "DAMAGE"));
        assertEquals(stock - 2, available(s.variantId, s.locationId));
    }
}
