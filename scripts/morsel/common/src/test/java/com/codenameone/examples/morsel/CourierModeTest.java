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

import com.codename1.ui.Button;

import java.util.Map;

/// A whole delivery from the courier's side of the app, with the customer as the
/// other actor.
public class CourierModeTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        onEdt(() -> AppConfig.setMode("courier"));
        signIn(COURIER, "Courier");
        untilText("courierState", "You are offline");
        shot("courier-offline");
        click("goOnline");
        untilText("courierState", "Looking for deliveries");

        Actor customer = Actor.signIn(CUSTOMER);
        Map<String, Object> delivery = customer.post("/api/deliveries", "{\"pickupLat\":37.8085,"
                + "\"pickupLng\":-122.4125,\"pickupAddress\":\"Pier 39\","
                + "\"dropoffLat\":37.7955,\"dropoffLng\":-122.3937,"
                + "\"dropoffAddress\":\"Ferry Building\",\"distanceMeters\":2400,"
                + "\"durationSeconds\":540}");
        assertEqual(200, customer.status, "the customer's request");
        final String id = Actor.text(delivery, "id");

        untilText("courierState", "Delivery request");
        assertTrue(text("fare").startsWith("$"), "the offer shows the fare");
        shot("courier-offer");
        click("accept");
        untilText("courierState", "Pick up Riley Customer");
        assertEqual("ACCEPTED", Actor.text(customer.get("/api/deliveries/" + id), "state"));
        click("arrived");
        untilText("courierState", "Waiting for Riley Customer");
        click("start");
        untilText("courierState", "On the trip");
        shot("courier-on-trip");
        assertFalse(findByName("cancel") instanceof Button,
                "a delivery under way cannot be cancelled by the courier");
        click("complete");
        untilText("courierState", "Looking for deliveries");
        assertEqual("COMPLETED", Actor.text(customer.get("/api/deliveries/" + id), "state"));

        // What the courier earned shows up on their own screen.
        until(() -> !"0".equals(text("trips")) && text("trips").length() > 0, 20000,
                "the trip to be counted");
        click("goOffline");
        untilText("courierState", "You are offline");

        // And on the screen that says what onboarding has paid.
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
