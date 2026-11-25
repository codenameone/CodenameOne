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
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;

import java.util.Map;

/// A store selling one tracked $10.00 mug and an order from it, shared by
/// the fulfilment, returns and documents tests.
final class Shop {
    static final AddressDto NY = new AddressDto("Ada Lovelace", "1 Main St", "", "Albany",
            "NY", "12207", "US", "", 42.65, -73.75);

    final String storeId;
    final String locationId;
    final String variantId;
    final String productId;
    final String owner;
    final String sku;
    private final Carts carts;
    private final Checkouts checkouts;
    private final MockMvc mvc;

    Shop(String name, int stock, Accounts accounts, Stores stores, DemoPricing pricing,
            DataSource db, Inventory inventory, Carts carts, Checkouts checkouts, MockMvc mvc)
            throws Exception {
        this(name, stock, "USD", 1000, accounts, stores, pricing, db, inventory, carts, checkouts,
                mvc);
    }

    /// A store selling in `currency`, its mug at `price` minor units of it.
    Shop(String name, int stock, String currency, long price, Accounts accounts, Stores stores,
            DemoPricing pricing, DataSource db, Inventory inventory, Carts carts,
            Checkouts checkouts, MockMvc mvc) throws Exception {
        this.carts = carts;
        this.checkouts = checkouts;
        this.mvc = mvc;
        owner = People.customer(accounts, name + "-owner");
        Store store = stores.create(owner, name, name, currency);
        storeId = store.getId();
        locationId = stores.addLocation(owner, storeId,
                new LocationDto(null, "Back room", "WAREHOUSE", "9 Dock Rd", true, true, 0)).id();
        pricing.seed(storeId);
        productId = Ids.next();
        db.execute("INSERT INTO product (id, store_id, handle, title, description, vendor, "
                + "product_type, tags, status, seo_title, seo_description, photo_id, rating_total, "
                + "rating_count, created_at, updated_at) VALUES (?, ?, ?, 'Mug', '', '', '', '', "
                + "'ACTIVE', '', '', '', 0, 0, 0, 0)",
                new Object[] {productId, storeId, name + "-mug"});
        variantId = Ids.next();
        sku = name + "-SKU";
        db.execute("INSERT INTO variant (id, product_id, store_id, title, option1, option2, "
                + "option3, sku, barcode, price_cents, compare_at_cents, cost_cents, weight_grams, "
                + "taxable, track_inventory, allow_backorder, position) VALUES "
                + "(?, ?, ?, 'Blue', '', '', '', ?, '0123456789012', ?, 0, 400, 350, 1, 1, 0, 0)",
                new Object[] {variantId, productId, storeId, sku, Long.valueOf(price)});
        if (stock > 0) {
            inventory.restock(storeId, variantId, locationId, stock, "opening", "test",
                    Inventory.RESTOCK);
        }
    }

    /// `qty` mugs bought by `user` with a card, shipped Express to Albany.
    OrderDto buy(String user, int qty) throws Exception {
        return buy(user, qty, user + "@example.com");
    }

    /// [#buy] with the order's e-mail address given.
    OrderDto buy(String user, int qty, String email) throws Exception {
        carts.add(storeId, user, null, new CartLineChangeDto(variantId, qty));
        CartDto c = carts.estimate(storeId, user, null, new EstimateDto(NY, null));
        String rate = null;
        for (ShippingOptionDto o : c.shippingOptions()) {
            if ("Express".equals(o.name())) {
                rate = o.rateId();
            }
        }
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        return checkouts.place(user, storeId, Ids.next(), new PlaceOrderDto(NY, rate,
                (String) card.get("id"), email, "", null, 0, null));
    }
}
