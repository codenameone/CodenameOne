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

import java.util.Map;

/// A whole ride, from the rider's side of the app: the driver is the other
/// actor, and everything the rider sees change is the server telling the app.
public class RideFlowTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        // A car near the rider, online.
        Actor driver = Actor.signIn(DRIVER);
        driver.online(true, HERE.getLatitude() + 0.004, HERE.getLongitude() + 0.003);
        assertEqual(200, driver.status, "the driver going online");

        signIn(RIDER, "Rider");
        quote();
        assertTrue(text("price-standard").startsWith("$"), "each kind of ride has its price");
        shot("ride-quote");
        click("request");
        untilText("rideState", "Finding a driver");

        // The offer reaches the driver, who takes it.
        String id = offered(driver);
        driver.post("/api/driver/offers/" + id + "/accept", null);
        assertEqual(200, driver.status, "accepting the offer");
        untilText("rideState", "Driver on the way");
        driver.online(true, HERE.getLatitude() + 0.002, HERE.getLongitude() + 0.001);
        shot("ride-driver-on-the-way");

        driver.post("/api/driver/rides/" + id + "/arrived", null);
        untilText("rideState", "Driver at pickup");
        driver.post("/api/driver/rides/" + id + "/start", null);
        untilText("rideState", "On the trip");
        driver.post("/api/driver/rides/" + id + "/complete", null);
        assertEqual(200, driver.status, "completing the ride");
        untilText("rideState", "You have arrived");
        assertTrue(text("fare").startsWith("$"), "the receipt shows what was charged");
        shot("ride-receipt");

        // The stars are sent as they are tapped, and the sheet stays for the
        // rest of what a rider may want to say about the ride.
        click("star-5");
        click("done");
        until(() -> findByName("whereTo") != null, 20000, "the rider's screen to reset");

        // Rated once: the server holds the ride as the rider left it.
        Actor rider = Actor.signIn(RIDER);
        Map<String, Object> ride = rider.get("/api/rides/" + id);
        assertEqual("COMPLETED", Actor.text(ride, "state"));
        assertEqual(5, ((Number) ride.get("rating")).intValue());
        driver.online(false, 0, 0);
        return true;
    }
}
