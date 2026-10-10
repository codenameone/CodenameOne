/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
