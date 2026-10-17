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
package com.codenameone.examples.morsel;

import java.util.Map;

/// A whole delivery, from the customer's side of the app: the courier is the other
/// actor, and everything the customer sees change is the server telling the app.
public class DeliveryFlowTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        // A car near the customer, online.
        Actor courier = Actor.signIn(COURIER);
        courier.online(true, HERE.getLatitude() + 0.004, HERE.getLongitude() + 0.003);
        assertEqual(200, courier.status, "the courier going online");

        signIn(CUSTOMER, "Customer");
        quote();
        assertTrue(text("price-standard").startsWith("$"), "each kind of delivery has its price");
        shot("delivery-quote");
        click("request");
        untilText("deliveryState", "Finding a courier");

        // The offer reaches the courier, who takes it.
        String id = offered(courier);
        courier.post("/api/courier/offers/" + id + "/accept", null);
        assertEqual(200, courier.status, "accepting the offer");
        untilText("deliveryState", "Courier on the way");
        courier.online(true, HERE.getLatitude() + 0.002, HERE.getLongitude() + 0.001);
        shot("delivery-courier-on-the-way");

        courier.post("/api/courier/deliveries/" + id + "/arrived", null);
        untilText("deliveryState", "Courier at pickup");
        courier.post("/api/courier/deliveries/" + id + "/start", null);
        untilText("deliveryState", "On the trip");
        courier.post("/api/courier/deliveries/" + id + "/complete", null);
        assertEqual(200, courier.status, "completing the delivery");
        untilText("deliveryState", "You have arrived");
        assertTrue(text("fare").startsWith("$"), "the receipt shows what was charged");
        shot("delivery-receipt");

        // The stars are sent as they are tapped, and the sheet stays for the
        // rest of what a customer may want to say about the delivery.
        click("star-5");
        click("done");
        until(() -> findByName("whereTo") != null, 20000, "the customer's screen to reset");

        // Rated once: the server holds the delivery as the customer left it.
        Actor customer = Actor.signIn(CUSTOMER);
        Map<String, Object> delivery = customer.get("/api/deliveries/" + id);
        assertEqual("COMPLETED", Actor.text(delivery, "state"));
        assertEqual(5, ((Number) delivery.get("rating")).intValue());
        courier.online(false, 0, 0);
        return true;
    }
}
