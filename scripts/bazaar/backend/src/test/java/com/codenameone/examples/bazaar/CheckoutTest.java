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
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Cart to order: merging, placing, every way placing can fail and leave
/// nothing behind, and what happens to an order afterwards.
@BackendTest
class CheckoutTest {
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
    private Orders orders;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;

    private static final AddressDto NY = new AddressDto("Ada Lovelace", "1 Main St", "", "Albany",
            "NY", "12207", "US", "", 0, 0);

    /// `{storeId, locationId, variantId, owner}`: a priced store selling one
    /// tracked $10.00 variant with `stock` on hand.
    private String[] shop(String name, int stock) throws Exception {
        String owner = People.customer(accounts, name + "-owner");
        Store store = stores.create(owner, name, name, "USD");
        LocationDto location = stores.addLocation(owner, store.getId(),
                new LocationDto(null, "Back room", "WAREHOUSE", "", true, true, 0));
        pricing.seed(store.getId());
        String productId = Ids.next();
        db.execute("INSERT INTO product (id, store_id, handle, title, description, vendor, "
                + "product_type, tags, status, seo_title, seo_description, photo_id, rating_total, "
                + "rating_count, created_at, updated_at) VALUES (?, ?, ?, 'Mug', '', '', '', '', "
                + "'ACTIVE', '', '', '', 0, 0, 0, 0)",
                new Object[] {productId, store.getId(), name + "-mug"});
        String variantId = Ids.next();
        db.execute("INSERT INTO variant (id, product_id, store_id, title, option1, option2, "
                + "option3, sku, barcode, price_cents, compare_at_cents, cost_cents, weight_grams, "
                + "taxable, track_inventory, allow_backorder, position) VALUES "
                + "(?, ?, ?, 'Blue', '', '', '', ?, '', 1000, 0, 400, 100, 1, 1, 0, 0)",
                new Object[] {variantId, productId, store.getId(), name + "-SKU"});
        if (stock > 0) {
            inventory.restock(store.getId(), variantId, location.id(), stock, "opening", "test",
                    Inventory.RESTOCK);
        }
        return new String[] {store.getId(), location.id(), variantId, owner};
    }

    private String express(String storeId, String user) throws Exception {
        CartDto c = carts.estimate(storeId, user, null, new EstimateDto(NY, null));
        for (ShippingOptionDto o : c.shippingOptions()) {
            if ("Express".equals(o.name())) {
                return o.rateId();
            }
        }
        throw new AssertionError("no Express rate in " + c.shippingOptions());
    }

    private String card(String user) throws Exception {
        return (String) Rest.addCard(mvc, user, Rest.VISA).get("id");
    }

    private OrderDto buy(String[] s, String user, int qty, String method, String key)
            throws Exception {
        carts.add(s[0], user, null, new CartLineChangeDto(s[2], qty));
        return checkouts.place(user, s[0], key, new PlaceOrderDto(NY, express(s[0], user), method,
                user + "@example.com", "", null));
    }

    private static int status(Throwable t) {
        return t instanceof ResponseStatusException ? ((ResponseStatusException) t).getStatus() : -1;
    }

    private long count(String sql, Object... args) throws Exception {
        Map row = db.queryOne(sql, args);
        return Rows.number(row.get("n"));
    }

    @Test
    void guestCartMergesOnSignIn() throws Exception {
        String[] s = shop("merge-shop", 10);
        CartDto guest = carts.add(s[0], null, null, new CartLineChangeDto(s[2], 2));
        assertNotNull(guest.token());
        assertEquals(2000, guest.subtotalCents());
        String user = People.customer(accounts, "merge-buyer");
        carts.add(s[0], user, null, new CartLineChangeDto(s[2], 1));
        CartDto merged = carts.merge(s[0], user, guest.token());
        assertEquals(1, merged.lines().size());
        assertEquals(3, merged.lines().get(0).quantity());
        // The guest cart is gone: its token finds an empty cart.
        assertTrue(carts.get(s[0], null, guest.token()).lines().isEmpty());
    }

    @Test
    void happyPathHoldsStockAndPayment() throws Exception {
        String[] s = shop("happy-shop", 5);
        String user = People.customer(accounts, "happy-buyer");
        OrderDto o = buy(s, user, 2, card(user), "k1");
        assertEquals(1001, o.number());
        assertEquals(Orders.AUTHORIZED, o.financial());
        // 20.00 + 19.99 express + 4% NY tax on both = 41.59
        assertEquals(2000, o.subtotalCents());
        assertEquals(1999, o.shippingCents());
        assertEquals(160, o.taxCents());
        assertEquals(4159, o.totalCents());
        assertEquals(3, inventory.available(s[2], s[1]));
        assertTrue(carts.get(s[0], user, null).lines().isEmpty(), "the cart is emptied");
        assertEquals(1, count("SELECT orders_count AS n FROM customer WHERE store_id = ? "
                + "AND username = ?", s[0], user));
        assertEquals(1002, buy(s, user, 1, card(user), "k2").number());
    }

    @Test
    void placingTwiceWithOneKeyPlacesOnce() throws Exception {
        String[] s = shop("idem-shop", 5);
        String user = People.customer(accounts, "idem-buyer");
        String method = card(user);
        OrderDto first = buy(s, user, 1, method, "same-key");
        OrderDto again = buy(s, user, 1, method, "same-key");
        assertEquals(first.id(), again.id());
        assertEquals(4, inventory.available(s[2], s[1]));
        assertEquals(1, count("SELECT COUNT(*) AS n FROM store_order WHERE store_id = ?", s[0]));
    }

    /// The simulated processor never refuses a hold, so the refusal comes
    /// from a payment method the shopper does not have; [Checkouts] undoes
    /// the same way for any failure of [com.codenameone.examples.bazaar.pay.Payments#authorize].
    @Test
    void refusedPaymentGivesEverythingBack() throws Exception {
        String[] s = shop("decline-shop", 3);
        String user = People.customer(accounts, "decline-buyer");
        carts.add(s[0], user, null, new CartLineChangeDto(s[2], 2));
        carts.applyDiscount(s[0], user, null, "WELCOME10");
        try {
            checkouts.place(user, s[0], "d1", new PlaceOrderDto(NY, express(s[0], user),
                    "no-such-card", "", "", null));
            fail("refused");
        } catch (ResponseStatusException e) {
            assertTrue(e.getStatus() >= 400 && e.getStatus() < 500, "status " + e.getStatus());
        }
        assertEquals(3, inventory.available(s[2], s[1]));
        assertEquals(0, count("SELECT COUNT(*) AS n FROM store_order WHERE store_id = ?", s[0]));
        assertEquals(0, count("SELECT used_count AS n FROM discount WHERE store_id = ? "
                + "AND code = 'WELCOME10'", s[0]));
        assertEquals(0, count("SELECT COUNT(*) AS n FROM discount_use"
                + " WHERE username = ?", user));
        assertEquals(1, carts.get(s[0], user, null).lines().size(), "the cart is kept");
        // And the same cart then goes through.
        OrderDto o = checkouts.place(user, s[0], "d2", new PlaceOrderDto(NY, express(s[0], user),
                card(user), "", "", null));
        assertEquals(200, o.discountCents());
    }

    @Test
    void outOfStockLineRollsBack() throws Exception {
        String[] s = shop("oos-shop", 5);
        String[] other = shop("oos-shop2", 0);
        // A second variant in the same store with no stock.
        String empty = Ids.next();
        db.execute("INSERT INTO variant (id, product_id, store_id, title, option1, option2, "
                + "option3, sku, barcode, price_cents, compare_at_cents, cost_cents, weight_grams, "
                + "taxable, track_inventory, allow_backorder, position) SELECT ?, product_id, "
                + "store_id, 'Red', '', '', '', 'RED', '', 1000, 0, 0, 100, 1, 1, 0, 1 FROM variant "
                + "WHERE id = ?", new Object[] {empty, s[2]});
        assertNotNull(other);
        String user = People.customer(accounts, "oos-buyer");
        carts.add(s[0], user, null, new CartLineChangeDto(s[2], 2));
        carts.add(s[0], user, null, new CartLineChangeDto(empty, 1));
        try {
            checkouts.place(user, s[0], "o1", new PlaceOrderDto(NY, express(s[0], user), card(user),
                    "", "", null));
            fail("out of stock");
        } catch (ResponseStatusException e) {
            assertEquals(409, e.getStatus());
        }
        assertEquals(5, inventory.available(s[2], s[1]));
        assertEquals(0, count("SELECT COUNT(*) AS n FROM store_order WHERE store_id = ?", s[0]));
    }

    @Test
    void lastUnitGoesToExactlyOneBuyer() throws Exception {
        final String[] s = shop("last-shop", 1);
        final int buyers = 6;
        final String[] users = new String[buyers];
        final String[] methods = new String[buyers];
        final String[] rates = new String[buyers];
        for (int iter = 0; iter < buyers; iter++) {
            users[iter] = People.customer(accounts, "last-buyer-" + iter);
            methods[iter] = card(users[iter]);
            carts.add(s[0], users[iter], null, new CartLineChangeDto(s[2], 1));
            rates[iter] = express(s[0], users[iter]);
        }
        List<Integer> outcomes = race(buyers, new Attempt() {
            @Override
            public void run(int i) throws Exception {
                checkouts.place(users[i], s[0], "last", new PlaceOrderDto(NY, rates[i],
                        methods[i], "", "", null));
            }
        });
        assertEquals(1, frequency(outcomes, 200), "outcomes " + outcomes);
        assertEquals(buyers - 1, frequency(outcomes, 409), "outcomes " + outcomes);
        assertEquals(0, inventory.available(s[2], s[1]));
        assertEquals(1, count("SELECT COUNT(*) AS n FROM store_order WHERE store_id = ?", s[0]));
    }

    @Test
    void limitedCodeIsNeverUsedPastItsLimit() throws Exception {
        final String[] s = shop("limit-shop", 50);
        db.execute("UPDATE discount SET usage_limit = 2 WHERE store_id = ? AND code = 'BUNDLE'",
                new Object[] {s[0]});
        final int buyers = 6;
        final String[] users = new String[buyers];
        final String[] methods = new String[buyers];
        final String[] rates = new String[buyers];
        for (int iter = 0; iter < buyers; iter++) {
            users[iter] = People.customer(accounts, "limit-buyer-" + iter);
            methods[iter] = card(users[iter]);
            carts.add(s[0], users[iter], null, new CartLineChangeDto(s[2], 3));
            carts.applyDiscount(s[0], users[iter], null, "bundle");
            rates[iter] = express(s[0], users[iter]);
        }
        List<Integer> outcomes = race(buyers, new Attempt() {
            @Override
            public void run(int i) throws Exception {
                checkouts.place(users[i], s[0], "limit", new PlaceOrderDto(NY, rates[i],
                        methods[i], "", "", null));
            }
        });
        assertEquals(2, frequency(outcomes, 200), "outcomes " + outcomes);
        assertEquals(2, count("SELECT used_count AS n FROM discount WHERE store_id = ? "
                + "AND code = 'BUNDLE'", s[0]));
        assertEquals(2, count("SELECT COUNT(*) AS n FROM store_order WHERE store_id = ?", s[0]));
        assertEquals(44, inventory.available(s[2], s[1]), "losers released their stock");
    }

    @Test
    void cancelReleasesStockAndVoidsTheHold() throws Exception {
        String[] s = shop("cancel-shop", 4);
        String user = People.customer(accounts, "cancel-buyer");
        OrderDto o = buy(s, user, 3, card(user), "c1");
        assertEquals(1, inventory.available(s[2], s[1]));
        OrderDto cancelled = orders.cancelMine(user, o.id());
        assertEquals(Orders.CANCELLED, cancelled.status());
        assertEquals(Orders.VOIDED, cancelled.financial());
        assertEquals(4, inventory.available(s[2], s[1]));
    }

    @Test
    void partialRefundRestocks() throws Exception {
        String[] s = shop("refund-shop", 5);
        String user = People.customer(accounts, "refund-buyer");
        OrderDto o = buy(s, user, 3, card(user), "r1");
        OrderLineDto line = o.lines().get(0);
        OrderDto after = orders.refund(s[3], s[0], o.id(), new RefundRequestDto(
                Arrays.asList(new RefundLineDto(line.id(), 1)), 0, true, "Chipped"));
        assertEquals(Orders.PARTIALLY_REFUNDED, after.financial());
        // One $10.00 mug plus its 4% tax.
        assertEquals(1040, after.refundedCents());
        assertEquals(1, after.lines().get(0).refundedQty());
        assertEquals(3, inventory.available(s[2], s[1]));
        assertEquals(1, orders.refunds(s[3], s[0], o.id()).size());
    }

    // ------------------------------------------------------------ racing

    interface Attempt {
        void run(int index) throws Exception;
    }

    /// Runs `n` attempts at once; answers 200 for each that succeeded and
    /// the status of each that failed.
    private static List<Integer> race(int n, final Attempt attempt) throws Exception {
        final List<Integer> outcomes = new ArrayList<Integer>();
        Thread[] threads = new Thread[n];
        for (int iter = 0; iter < n; iter++) {
            final int index = iter;
            threads[iter] = new Thread(new Runnable() {
                @Override
                public void run() {
                    int code;
                    try {
                        attempt.run(index);
                        code = 200;
                    } catch (Throwable failed) {
                        code = status(failed);
                        if (code < 0) {
                            failed.printStackTrace();
                        }
                    }
                    synchronized (outcomes) {
                        outcomes.add(Integer.valueOf(code));
                    }
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        return outcomes;
    }

    private static int frequency(List<Integer> list, int value) {
        int n = 0;
        for (Integer i : list) {
            if (i.intValue() == value) {
                n++;
            }
        }
        return n;
    }
}
