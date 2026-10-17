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

import com.codename1.ui.Button;

import java.util.Map;

/// A whole delivery from the courier's side of the app, with the customer and
/// the kitchen as the other actors.
public class CourierModeTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        onEdt(() -> AppConfig.setMode("courier"));
        signIn(COURIER, "Courier");
        untilText("courierState", "You are offline");
        shot("courier-offline");
        click("goOnline");
        untilText("courierState", "Looking for deliveries");

        // An order from the demo kitchen to the customer, which the kitchen
        // takes: a courier is looked for once it is cooking.
        Actor customer = Actor.signIn(CUSTOMER);
        Actor kitchen = Actor.signIn(MERCHANT);
        Map<String, Object> order = customer.post("/api/orders", "{\"restaurantId\":\"" + KITCHEN
                + "\",\"lines\":[{\"dishId\":\"" + dish(customer) + "\",\"quantity\":1,"
                + "\"optionIds\":[]}],\"dropoffLat\":" + HERE.getLatitude() + ",\"dropoffLng\":"
                + HERE.getLongitude() + ",\"dropoffAddress\":\"Pier 39\",\"note\":\"\","
                + "\"tipCents\":0,\"paymentMethodId\":\"cash\"}");
        assertEqual(200, customer.status, "the customer's order");
        final String id = Actor.text(order, "id");
        kitchen.post("/api/merchant/orders/" + id + "/accept", "{\"prepMinutes\":10}");
        assertEqual(200, kitchen.status, "the kitchen accepting the order");

        untilText("courierState", "Delivery request");
        assertTrue(text("fare").startsWith("$"), "the offer shows the fare");
        shot("courier-offer");
        click("accept");
        untilText("courierState", "Head to the restaurant");
        click("arrived");
        untilText("courierState", "Collect the order for Riley Customer");
        kitchen.post("/api/merchant/orders/" + id + "/ready", null);
        assertEqual(200, kitchen.status, "the kitchen marking it ready");
        click("start");
        untilText("courierState", "Deliver to Riley Customer");
        shot("courier-on-the-way");
        assertFalse(findByName("cancel") instanceof Button,
                "an order on its way cannot be dropped by the courier");
        click("complete");
        untilText("courierState", "Looking for deliveries");
        assertEqual("DELIVERED", Actor.text(customer.get("/api/orders/" + id), "state"));

        // What the courier earned shows up on their own screen.
        until(() -> !"0".equals(text("deliveries")) && text("deliveries").length() > 0, 20000,
                "the delivery to be counted");
        click("goOffline");
        untilText("courierState", "You are offline");

        // And on the screen that says what delivering has paid.
        click("menu-open");
        click("menu-earnings");
        waitForForm("Earnings");
        until(() -> text("balance").startsWith("$"), 20000, "the balance");
        shot("courier-earnings");
        back();
        waitForForm("Courier");
        return true;
    }
}
