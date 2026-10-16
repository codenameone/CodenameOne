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
package com.codenameone.examples.wayline;

import com.codename1.ui.Button;

import java.util.Map;

/// A whole ride from the driver's side of the app, with the rider as the
/// other actor.
public class DriverModeTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        onEdt(() -> AppConfig.setMode("driver"));
        signIn(DRIVER, "Driver");
        untilText("driverState", "You are offline");
        shot("driver-offline");
        click("goOnline");
        untilText("driverState", "Looking for rides");

        Actor rider = Actor.signIn(RIDER);
        Map<String, Object> ride = rider.post("/api/rides", "{\"pickupLat\":37.8085,"
                + "\"pickupLng\":-122.4125,\"pickupAddress\":\"Pier 39\","
                + "\"dropoffLat\":37.7955,\"dropoffLng\":-122.3937,"
                + "\"dropoffAddress\":\"Ferry Building\",\"distanceMeters\":2400,"
                + "\"durationSeconds\":540}");
        assertEqual(200, rider.status, "the rider's request");
        final String id = Actor.text(ride, "id");

        untilText("driverState", "Ride request");
        assertTrue(text("fare").startsWith("$"), "the offer shows the fare");
        shot("driver-offer");
        click("accept");
        untilText("driverState", "Pick up Riley Rider");
        assertEqual("ACCEPTED", Actor.text(rider.get("/api/rides/" + id), "state"));
        click("arrived");
        untilText("driverState", "Waiting for Riley Rider");
        click("start");
        untilText("driverState", "On the trip");
        shot("driver-on-trip");
        assertFalse(findByName("cancel") instanceof Button,
                "a ride under way cannot be cancelled by the driver");
        click("complete");
        untilText("driverState", "Looking for rides");
        assertEqual("COMPLETED", Actor.text(rider.get("/api/rides/" + id), "state"));

        // What the driver earned shows up on their own screen.
        until(() -> !"0".equals(text("trips")) && text("trips").length() > 0, 20000,
                "the trip to be counted");
        click("goOffline");
        untilText("driverState", "You are offline");

        // And on the screen that says what driving has paid.
        click("menu-open");
        click("menu-earnings");
        waitForForm("Earnings");
        until(() -> text("balance").startsWith("$"), 20000, "the balance");
        shot("driver-earnings");
        back();
        waitForForm("Driver");
        return true;
    }
}
