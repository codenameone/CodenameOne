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
