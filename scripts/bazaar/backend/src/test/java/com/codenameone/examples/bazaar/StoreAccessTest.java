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

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Who may do what at a store: the staff list decides, by role, and someone
/// from another store is told the store does not exist.
@BackendTest
class StoreAccessTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;

    private Map addStaff(Shops shop, String who, String role, String pin) throws Exception {
        return Rest.json(mvc, People.asMerchant(Rest.body(post(shop.base() + "/staff"),
                "{\"username\":\"" + who + "\",\"role\":\"" + role + "\",\"pin\":\"" + pin
                + "\"}"), shop.owner));
    }

    private int as(String username, MockRequestBuilder request) throws Exception {
        return Rest.statusOf(mvc, People.asMerchant(request, username));
    }

    @Test
    void eachRoleReachesWhatItShould() throws Exception {
        Shops shop = Shops.open(accounts, stores, "matrix-shop");
        Map product = shop.product(mvc, "Matrix Mug", "ACTIVE", "Mugs", "M", "", 1000);
        String variantId = Shops.variantId(product, 0);
        String manager = People.customer(accounts, "matrix-manager");
        String packer = People.customer(accounts, "matrix-packer");
        String cashier = People.customer(accounts, "matrix-cashier");
        addStaff(shop, manager, "MANAGER", "");
        addStaff(shop, packer, "FULFILMENT", "");
        addStaff(shop, cashier, "CASHIER", "4321");
        String outsider = Shops.open(accounts, stores, "matrix-other").owner;

        String settings = "{\"name\":\"Matrix\",\"currency\":\"USD\",\"accentColor\":255}";
        String newProduct = "{\"title\":\"Another\",\"variants\":[{\"priceCents\":100}]}";
        String adjust = "{\"variantId\":\"" + variantId + "\",\"locationId\":\""
                + shop.locationId + "\",\"delta\":2,\"reason\":\"RESTOCK\"}";
        String[] people = {shop.owner, manager, packer, cashier, outsider};
        // owner, manager, fulfilment, cashier, someone else's owner
        Object[][] matrix = {
            {"PUT", "", settings, new int[] {200, 200, 403, 403, 404}},
            {"POST", "/products", newProduct, new int[] {200, 200, 403, 403, 404}},
            {"GET", "/products", null, new int[] {200, 200, 200, 200, 404}},
            {"GET", "/staff", null, new int[] {200, 200, 403, 403, 404}},
            {"POST", "/inventory/adjust", adjust, new int[] {200, 200, 200, 403, 404}},
            {"GET", "/inventory", null, new int[] {200, 200, 200, 403, 404}},
            {"GET", "/lookup?code=MatrixMug-0", null, new int[] {200, 200, 200, 200, 404}},
            {"GET", "/suppliers", null, new int[] {200, 200, 200, 403, 404}},
        };
        for (int row = 0; row < matrix.length; row++) {
            int[] expected = (int[]) matrix[row][3];
            for (int who = 0; who < people.length; who++) {
                String path = shop.base() + matrix[row][1];
                MockRequestBuilder request = "PUT".equals(matrix[row][0]) ? put(path)
                        : "POST".equals(matrix[row][0]) ? post(path) : get(path);
                if (matrix[row][2] != null) {
                    request = Rest.body(request, (String) matrix[row][2]);
                }
                assertEquals(expected[who], as(people[who], request), matrix[row][0] + " "
                        + matrix[row][1] + " as " + people[who]);
            }
        }

        // Without the merchant role the whole of /api/merchant is closed, and
        // without a token it asks for one; the storefront needs neither.
        String customer = People.customer(accounts, "matrix-customer");
        assertEquals(403, Rest.statusOf(mvc, People.asCustomer(get(shop.base() + "/products"),
                customer)));
        assertEquals(401, Rest.statusOf(mvc, get(shop.base() + "/products")));
        assertEquals(200, Rest.statusOf(mvc, get("/api/shop/stores")));
        assertEquals(200, Rest.statusOf(mvc, get("/api/shop/stores/" + shop.handle)));

        // An id from another store is no id at all.
        Shops other = Shops.open(accounts, stores, "matrix-third");
        Map foreign = other.product(mvc, "Foreign Fork", "ACTIVE", "Forks", "F", "", 500);
        assertEquals(404, as(shop.owner, put(shop.base() + "/products/" + foreign.get("id"))
                .contentType("application/json").content(newProduct)));
        assertEquals(404, as(shop.owner, Rest.body(post(shop.base() + "/inventory/adjust"),
                "{\"variantId\":\"" + Shops.variantId(foreign, 0) + "\",\"locationId\":\""
                + shop.locationId + "\",\"delta\":1}")));
    }

    @Test
    void staffAreAddedWithHashedPinsAndOnlyTheOwnerAppointsManagers() throws Exception {
        Shops shop = Shops.open(accounts, stores, "staff-shop");
        String manager = People.customer(accounts, "staff-manager");
        String cashier = People.customer(accounts, "staff-cashier");
        String hopeful = People.customer(accounts, "staff-hopeful");
        Map added = addStaff(shop, cashier, "CASHIER", "123456");
        assertEquals("", added.get("pin"));
        assertEquals(Boolean.TRUE, added.get("hasPin"));
        assertTrue(stores.checkPin(shop.storeId, cashier, "123456"));
        assertFalse(stores.checkPin(shop.storeId, cashier, "654321"));
        assertTrue(accounts.describe(cashier).merchant());
        assertEquals(400, Rest.statusOf(mvc, People.asMerchant(Rest.body(post(shop.base()
                + "/staff"), "{\"username\":\"" + cashier + "\",\"role\":\"CASHIER\","
                + "\"pin\":\"12a4\"}"), shop.owner)));

        addStaff(shop, manager, "MANAGER", "");
        // A manager adds a cashier, but not another manager.
        assertEquals(200, as(manager, Rest.body(post(shop.base() + "/staff"),
                "{\"username\":\"" + hopeful + "\",\"role\":\"FULFILMENT\"}")));
        assertEquals(403, as(manager, Rest.body(post(shop.base() + "/staff"),
                "{\"username\":\"" + hopeful + "\",\"role\":\"MANAGER\"}")));

        List staff = Rest.list(mvc, People.asMerchant(get(shop.base() + "/staff"), shop.owner));
        assertEquals(4, staff.size());
        String ownerRow = null;
        String cashierRow = null;
        for (int iter = 0; iter < staff.size(); iter++) {
            Map row = (Map) staff.get(iter);
            if ("OWNER".equals(row.get("role"))) {
                ownerRow = (String) row.get("id");
            }
            if (cashier.equals(row.get("username"))) {
                cashierRow = (String) row.get("id");
            }
        }
        assertEquals(400, as(manager, delete(shop.base() + "/staff/" + ownerRow)));
        assertEquals(200, as(manager, delete(shop.base() + "/staff/" + cashierRow)));
        // Working for no store any more, the cashier is a customer again.
        assertFalse(accounts.describe(cashier).merchant());

        List mine = Rest.list(mvc, People.asMerchant(get("/api/merchant/stores"), manager));
        assertEquals(1, mine.size());
        assertEquals("MANAGER", ((Map) mine.get(0)).get("role"));
    }
}
