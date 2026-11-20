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

import java.util.Map;

/// The platform console: the admin signs in through the app, sees every store,
/// suspends one -- whose storefront then stops answering -- and restores it.
public class AdminTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        signIn(ADMIN, "home-admin");
        Actor admin = Actor.signIn(ADMIN);
        Map<String, Object> stats = admin.get("/api/admin/platform");
        assertTrue(Actor.num(stats, "stores") >= 2, "the platform lists the demo stores");
        String target = null;
        for (Object o : Actor.maps(stats.get("perStore"))) {
            Map<String, Object> s = (Map<String, Object>) o;
            if ("littleclay".equals(Actor.text(s, "handle"))) {
                target = Actor.text(s, "storeId");
            }
        }
        assertNotNull(target, "the second demo store is listed");

        admin.post("/api/admin/stores/" + target + "/status",
                "{\"status\":\"SUSPENDED\",\"reason\":\"e2e\"}");
        assertEqual(200, admin.status, "the admin suspends the store");
        assertTrue(Actor.num(admin.get("/api/admin/platform"), "suspendedStores") >= 1,
                "the console counts it suspended");
        Actor guest = Actor.guest();
        guest.get("/api/shop/stores/littleclay");
        assertTrue(guest.status >= 400, "a suspended store's storefront is closed");

        admin.post("/api/admin/stores/" + target + "/status", "{\"status\":\"ACTIVE\",\"reason\":\"\"}");
        assertEqual(200, admin.status, "the store is restored");
        signOut();
        return true;
    }
}
