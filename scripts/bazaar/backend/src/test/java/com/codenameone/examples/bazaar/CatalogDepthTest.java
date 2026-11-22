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
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The catalog's depth: cost and margin, digital goods, bundles, video and
/// 3D media, scheduled publishing per channel, the standard taxonomy, customs
/// data, unit prices and product translations.
@BackendTest
class CatalogDepthTest {
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

    private long cost(String variantId) throws Exception {
        Map v = db.queryOne("SELECT cost_cents FROM variant WHERE id = ?",
                new Object[] {variantId});
        return ((Number) v.get("cost_cents")).longValue();
    }

    @Test
    void costPerItemShowsMarginInTheEditorAndTheListAndDefaultsFromPurchases()
            throws Exception {
        Shop s = shop("cd-margin");
        Browser b = signedIn(s.owner);
        String page = "/admin/" + s.storeId + "/products/" + s.productId;
        // The seeded mug costs 4.00 and sells for 10.00.
        b.assertPage(b.get(page), "60.0% margin, 150.0% markup");
        redirected(b.post(page, "title", "Mug", "status", "ACTIVE", "v0_id", s.variantId,
                "v0_title", "Blue", "v0_sku", s.sku, "v0_price", "12.50", "v0_cost", "5.00"));
        assertEquals(500L, cost(s.variantId));
        b.assertPage(b.get(page), "60.0% margin");
        b.assertPage(b.get("/admin/" + s.storeId + "/products"), "60.0%");

        // No cost yet: the first purchase order received sets it.
        db.execute("UPDATE variant SET cost_cents = 0 WHERE id = ?", new Object[] {s.variantId});
        b.assertPage(b.get(page), "<td>-</td>");
        Map supplier = Rest.json(mvc, People.asMerchant(Rest.body(post("/api/merchant/stores/"
                + s.storeId + "/suppliers"), "{\"name\":\"Clayworks\",\"email\":\"\"}"), s.owner));
        String sid = (String) supplier.get("id");
        String base = "/api/merchant/stores/" + s.storeId + "/purchase-orders";
        Map po = Rest.json(mvc, People.asMerchant(Rest.body(post(base), "{\"supplierId\":\"" + sid + "\","
                + "\"locationId\":\"" + s.locationId + "\",\"lines\":[{\"variantId\":\""
                + s.variantId + "\",\"quantity\":4,\"costCents\":325}]}"), s.owner));
        String id = (String) po.get("id");
        Rest.json(mvc, People.asMerchant(post(base + "/" + id + "/order"), s.owner));
        String line = (String) ((Map) ((List) po.get("lines")).get(0)).get("id");
        Rest.json(mvc, People.asMerchant(Rest.body(post(base + "/" + id + "/receive"),
                "{\"lines\":[{\"lineId\":\"" + line + "\",\"quantity\":4}]}"), s.owner));
        assertEquals(325L, cost(s.variantId));
        // A cost the merchant set is not replaced by a later purchase.
        Map again = Rest.json(mvc, People.asMerchant(Rest.body(post(base), "{\"supplierId\":\"" + sid + "\","
                + "\"locationId\":\"" + s.locationId + "\",\"lines\":[{\"variantId\":\""
                + s.variantId + "\",\"quantity\":1,\"costCents\":999}]}"), s.owner));
        String id2 = (String) again.get("id");
        Rest.json(mvc, People.asMerchant(post(base + "/" + id2 + "/order"), s.owner));
        String line2 = (String) ((Map) ((List) again.get("lines")).get(0)).get("id");
        Rest.json(mvc, People.asMerchant(Rest.body(post(base + "/" + id2 + "/receive"),
                "{\"lines\":[{\"lineId\":\"" + line2 + "\",\"quantity\":1}]}"), s.owner));
        assertEquals(325L, cost(s.variantId));
    }
}
