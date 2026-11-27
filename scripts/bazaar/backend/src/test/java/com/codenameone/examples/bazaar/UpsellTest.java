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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.BuyNowDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.UpsellDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.checkout.Upsells;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

/// Post-purchase upsell and one-tap "Buy now": each is a separate order,
/// authorized on the saved card like any checkout.
@BackendTest
class UpsellTest {
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
    private Upsells upsells;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 20, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private OrderDto place(Shop s, String user) throws Exception {
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        String card = (String) Rest.addCard(mvc, user, Rest.VISA).get("id");
        return checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(Shop.NY, null, card,
                user + "@example.com", "", null, 0, null), null, null, null, null);
    }

    @Test
    void theUpsellIsASeparateOrderOnTheSameCardLinkedByNotes() throws Exception {
        Shop s = shop("up-a");
        String buyer = People.customer(accounts, "up-a-buyer");
        OrderDto first = place(s, buyer);
        assertEquals("", upsells.get(s.owner, s.storeId).productId());
        assertNull(upsells.offer(buyer, s.storeId, first.id()));
        upsells.set(s.owner, s.storeId, new UpsellDto(s.productId, "", 0, 0));
        UpsellDto offer = upsells.offer(buyer, s.storeId, first.id());
        assertEquals(s.productId, offer.productId());
        assertEquals(0, offer.orderNumber());
        try {
            upsells.offer(People.customer(accounts, "up-a-other"), s.storeId, first.id());
            throw new AssertionError("another shopper saw the offer");
        } catch (ResponseStatusException e) {
            assertEquals(404, e.getStatus());
        }

        OrderDto addOn = upsells.accept(buyer, s.storeId, first.id());
        assertNotEquals(first.id(), addOn.id());
        assertEquals(1, addOn.lines().size());
        assertEquals(Orders.AUTHORIZED, addOn.financial());
        assertTrue(addOn.note().contains("#" + first.number()));
        OrderDto original = orders.detail(s.owner, s.storeId, first.id());
        assertTrue(original.note().contains("#" + addOn.number()));
        // The paid order is left as it was.
        assertEquals(first.totalCents(), original.totalCents());
        assertEquals(first.lines().size(), original.lines().size());
        // Taken once: a second tap answers the same order.
        assertEquals(addOn.id(), upsells.accept(buyer, s.storeId, first.id()).id());
        assertEquals(addOn.number(), upsells.offer(buyer, s.storeId, first.id()).orderNumber());
    }

    @Test
    void buyNowPlacesOneLineAndLeavesTheCartAlone() throws Exception {
        Shop s = shop("up-b");
        String buyer = People.customer(accounts, "up-b-buyer");
        Rest.addCard(mvc, buyer, Rest.VISA);
        carts.add(s.storeId, buyer, null, new CartLineChangeDto(s.variantId, 3));
        String key = Ids.next();
        OrderDto o = upsells.buyNow(buyer, s.storeId, new BuyNowDto(s.variantId, 1, Shop.NY,
                key));
        assertEquals(1, o.lines().size());
        assertEquals(1, o.lines().get(0).quantity());
        assertEquals(Orders.AUTHORIZED, o.financial());
        assertEquals(o.id(), upsells.buyNow(buyer, s.storeId, new BuyNowDto(s.variantId, 1,
                Shop.NY, key)).id());
        CartDto cart = carts.get(s.storeId, buyer, null);
        assertEquals(3, cart.lines().get(0).quantity());
    }
}
