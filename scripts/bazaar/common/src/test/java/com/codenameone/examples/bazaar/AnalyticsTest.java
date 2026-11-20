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

/// Analytics: once a sale has gone through (the shop and register tests run
/// first and each makes one), the owner's dashboard has non-zero KPIs.
public class AnalyticsTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        String sid = storeId();
        Actor owner = Actor.signIn(MERCHANT);
        Map<String, Object> dashboard = owner.get("/api/merchant/stores/" + sid + "/analytics");
        assertEqual(200, owner.status, "the owner reads the dashboard");
        Object current = dashboard.get("current");
        assertTrue(current instanceof Map, "the dashboard has the current period");
        Map<String, Object> kpis = (Map<String, Object>) current;
        assertTrue(Actor.num(kpis, "orders") > 0, "orders are counted");
        assertTrue(Actor.num(kpis, "grossSalesCents") > 0, "gross sales are counted");
        assertTrue(Actor.num(kpis, "netSalesCents") > 0, "net sales are counted");
        return true;
    }
}
