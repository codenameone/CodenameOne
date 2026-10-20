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

/// A whole order, from the customer's side of the app: the kitchen and the
/// courier are the other actors, and everything the customer sees change is
/// the server telling the app.
public class DeliveryFlowTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        // A courier by the kitchen, online, and the kitchen's own account.
        Actor courier = Actor.signIn(COURIER);
        courier.online(true, KITCHEN_AT.getLatitude() + 0.002, KITCHEN_AT.getLongitude() + 0.001);
        assertEqual(200, courier.status, "the courier going online");
        Actor kitchen = Actor.signIn(MERCHANT);
        Actor customer = Actor.signIn(CUSTOMER);

        signIn(CUSTOMER, "Customer");
        shot("customer-home");
        cart();
        shot("checkout");
        click("place");
        waitForForm("track");
        untilText("orderState", "Waiting for the restaurant");
        final String id = placed(customer);
        assertTrue(id != null, "the order is on its way");
        shot("order-placed");

        // The kitchen takes it; the courier is asked once it is cooking.
        kitchen.post("/api/merchant/orders/" + id + "/accept", "{\"prepMinutes\":10}");
        assertEqual(200, kitchen.status, "the kitchen accepting the order");
        untilText("orderState", "Being prepared");
        assertEqual(id, offered(courier), "the courier is offered this order");
        courier.post("/api/courier/offers/" + id + "/accept", null);
        assertEqual(200, courier.status, "accepting the offer");
        courier.post("/api/courier/deliveries/" + id + "/arrived", null);
        assertEqual(200, courier.status, "reaching the restaurant");

        // Not before the kitchen says it is ready.
        courier.post("/api/courier/deliveries/" + id + "/start", null);
        assertEqual(409, courier.status, "collecting food the kitchen has not finished");
        kitchen.post("/api/merchant/orders/" + id + "/ready", null);
        assertEqual(200, kitchen.status, "the kitchen marking it ready");
        untilText("orderState", "Ready, waiting for the courier");
        courier.post("/api/courier/deliveries/" + id + "/start", null);
        assertEqual(200, courier.status, "collecting the order");
        untilText("orderState", "On its way");
        courier.online(true, HERE.getLatitude() + 0.001, HERE.getLongitude());
        shot("order-on-its-way");

        courier.post("/api/courier/deliveries/" + id + "/complete", null);
        assertEqual(200, courier.status, "delivering the order");
        untilText("orderState", "Delivered");
        shot("order-delivered");

        // Rated once: the server holds the order as the customer left it.
        click("star-5");
        until(() -> findByName("stars") == null, 20000, "the rating to be taken");
        Map<String, Object> order = customer.get("/api/orders/" + id);
        assertEqual("DELIVERED", Actor.text(order, "state"));
        assertEqual(5, ((Number) order.get("stars")).intValue());
        Map<String, Object> receipt = customer.get("/api/payments/receipts/" + id);
        // The demo customer pays in cash; PaymentsTest follows a card's hold.
        assertEqual("cash", Actor.text(receipt, "status"), "the cash paid on delivery");
        courier.online(false, 0, 0);
        return true;
    }
}
