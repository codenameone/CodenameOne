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
package com.codenameone.examples.savor;

import java.util.Map;

/// An order from the restaurant's side of the app: it arrives in the queue, the
/// kitchen accepts it with a preparation time and marks it ready.
public class MerchantModeTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        onEdt(() -> AppConfig.setMode("merchant"));
        signIn(MERCHANT, "Restaurant");

        Actor customer = Actor.signIn(CUSTOMER);
        Map<String, Object> order = customer.post("/api/orders", "{\"restaurantId\":\"" + KITCHEN
                + "\",\"lines\":[{\"dishId\":\"" + dish(customer) + "\",\"quantity\":2,"
                + "\"optionIds\":[]}],\"dropoffLat\":" + HERE.getLatitude() + ",\"dropoffLng\":"
                + HERE.getLongitude() + ",\"dropoffAddress\":\"Pier 39\",\"note\":\"No onions\","
                + "\"tipCents\":0,\"paymentMethodId\":\"cash\"}");
        assertEqual(200, customer.status, "the customer's order");
        final String id = Actor.text(order, "id");

        // It arrives without anyone reloading the queue: the live channel says so.
        until(() -> has("queue-" + id), 20000, "the order to arrive in the queue");
        shot("merchant-queue");
        click("queue-" + id);
        waitForForm("merchant-order");
        until(() -> has("accept"), 20000, "the order's page");
        shot("merchant-order");
        click("prep-20");
        click("accept");
        waitForForm("Restaurant");
        until(() -> "ACCEPTED".equals(Actor.text(customer.get("/api/orders/" + id), "state")),
                20000, "the order to be accepted");
        assertEqual(20, ((Number) customer.get("/api/orders/" + id).get("prepMinutes")).intValue(),
                "the preparation time chosen");

        click("queue-" + id);
        waitForForm("merchant-order");
        until(() -> has("ready"), 20000, "the button that marks it ready");
        click("ready");
        waitForForm("Restaurant");
        until(() -> "READY".equals(Actor.text(customer.get("/api/orders/" + id), "state")),
                20000, "the order to be ready");

        // No courier is online in this test, so an admin cancels it rather
        // than leave it searching for the next test to find.
        Actor.signIn(ADMIN).post("/api/admin/orders/" + id + "/cancel", "{\"reason\":\"Test over\"}");
        return true;
    }
}
