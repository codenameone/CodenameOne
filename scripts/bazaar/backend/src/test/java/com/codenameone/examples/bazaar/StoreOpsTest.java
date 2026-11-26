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
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.admin.Platform;
import com.codenameone.examples.bazaar.api.AnalyticsDto;
import com.codenameone.examples.bazaar.api.CustomerDetailDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.StoreStatusDto;
import com.codenameone.examples.bazaar.analytics.Analytics;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.customer.Customers;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Customers' counters across channels, the customer screens, the
/// dashboard on real orders, and the platform admin suspending a store.
@BackendTest
class StoreOpsTest {
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
    private Customers customers;
    @Autowired
    private Analytics analytics;
    @Autowired
    private Platform platform;
    @Autowired
    private CustomerRepository customerRows;
    @Autowired
    private StoreOrderRepository orderRows;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 50, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private Map owner(Shop s, com.codename1.backend.test.MockRequestBuilder r, String json)
            throws Exception {
        return Rest.json(mvc, People.asMerchant(json == null ? r : Rest.body(r, json), s.owner));
    }

    @Test
    void customerCountersFollowOnlineAndPosOrders() throws Exception {
        Shop s = shop("ops-count");
        String buyer = People.customer(accounts, "ops-count-buyer");
        OrderDto first = s.buy(buyer, 2);
        OrderDto second = s.buy(buyer, 1);
        StoreOrder a = orderRows.findById(first.id());
        StoreOrder b = orderRows.findById(second.id());
        Customer c = customerRows.findById(a.getCustomerId());
        assertEquals(2, c.getOrdersCount());
        assertEquals(a.getTotalCents() + b.getTotalCents(), c.getSpentCents());

        // The same customer at the register.
        String base = "/api/merchant/stores/" + s.storeId + "/pos";
        String registerId = (String) owner(s, post(base + "/registers"), "{\"locationId\":\""
                + s.locationId + "\",\"name\":\"Front\",\"active\":true,\"taxCountry\":\"US\","
                + "\"taxRegion\":\"NY\"}").get("id");
        String shiftId = (String) owner(s, post(base + "/registers/" + registerId + "/shifts"),
                "{\"openingCents\":10000,\"note\":\"\"}").get("id");
        Map out = owner(s, post(base + "/registers/" + registerId + "/sales"), "{\"sales\":[{"
                + "\"clientId\":\"ops-pos\",\"shiftId\":\"" + shiftId + "\",\"customerId\":\""
                + c.getId() + "\",\"lines\":[{\"variantId\":\"" + s.variantId
                + "\",\"quantity\":1,\"unitCents\":0}],\"tenders\":[{\"kind\":\"CASH\","
                + "\"amountCents\":5000,\"reference\":\"\"}]}]}");
        Map r = (Map) ((List) out.get("results")).get(0);
        assertEquals("OK", r.get("status"), String.valueOf(r.get("message")));
        StoreOrder pos = orderRows.findById((String) r.get("orderId"));
        Customer after = customerRows.findById(c.getId());
        assertEquals(3, after.getOrdersCount());
        assertEquals(c.getSpentCents() + pos.getTotalCents(), after.getSpentCents());

        CustomerDetailDto detail = customers.detail(s.owner, s.storeId, c.getId());
        assertEquals(3, detail.orders().size());
        assertEquals(after.getSpentCents(), detail.lifetimeValueCents());
        assertEquals(after.getSpentCents() / 3, detail.averageOrderCents());
        assertTrue(detail.customer().lastOrderAt() > 0);

        List found = Rest.list(mvc, People.asMerchant(get("/api/merchant/stores/" + s.storeId
                + "/customers?minOrders=3"), s.owner));
        assertEquals(1, found.size());
        Map segs = (Map) Rest.list(mvc, People.asMerchant(get("/api/merchant/stores/"
                + s.storeId + "/customer-segments"), s.owner)).get(0);
        assertEquals(Customers.VIP, segs.get("id"));

        AnalyticsDto dash = analytics.dashboard(s.owner, s.storeId, 0, 0);
        assertEquals(3, dash.current().orders());
        assertEquals(2, dash.byChannel().size());
        assertEquals(4, dash.topProducts().get(0).quantity());
        // On hand drops when stock leaves: the register's sale did, the two
        // online orders are committed until they ship.
        assertEquals(49, dash.inventory().units());
    }

    @Test
    void aSuspendedStoreIsHiddenFromShoppers() throws Exception {
        Shop s = shop("ops-suspend");
        String handle = "ops-suspend";
        assertEquals(200, Rest.statusOf(mvc, get("/api/shop/stores/" + handle)));
        platform.setStatus("admin", s.storeId, new StoreStatusDto("SUSPENDED", "Unpaid fees"));
        assertEquals(404, Rest.statusOf(mvc, get("/api/shop/stores/" + handle)));
        for (Object o : Rest.list(mvc, get("/api/shop/stores"))) {
            assertFalse(handle.equals(((Map) o).get("handle")));
        }
        boolean listed = false;
        for (com.codenameone.examples.bazaar.api.StoreStatsDto st : platform.stats().perStore()) {
            if (st.storeId().equals(s.storeId)) {
                listed = true;
                assertEquals("SUSPENDED", st.status());
            }
        }
        assertTrue(listed);
        assertEquals(403, Rest.statusOf(mvc, People.asMerchant(get("/api/admin/platform"),
                s.owner)));
        assertEquals(200, Rest.statusOf(mvc, People.asAdmin(get("/api/admin/platform"),
                "ops-admin")));
        platform.setStatus("admin", s.storeId, new StoreStatusDto("active", ""));
        assertEquals(200, Rest.statusOf(mvc, get("/api/shop/stores/" + handle)));
    }
}
