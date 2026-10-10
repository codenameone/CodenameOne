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

/// Flagging an account and blocking one, from the admin's screens, and what
/// each does to the person it is done to.
public class AdminModerationTest extends E2e {
    private static final String OTHER_RIDER = "rider2@wayline.example";

    @Override
    protected boolean run() throws Exception {
        final Actor other = Actor.signIn(OTHER_RIDER);
        other.get("/api/me");
        assertEqual(200, other.status);

        onEdt(() -> AppConfig.setMode("admin"));
        signIn(ADMIN, "Admin");
        // One account arrives already flagged, and the dashboard says so.
        until(() -> has("alert-flagged"), 20000, "the flagged account on the dashboard");
        click("users");
        waitForForm("Users");
        type("search", "RIDER2");
        until(() -> has("user-" + OTHER_RIDER) && !has("user-" + RIDER), 20000,
                "the search to narrow the list, whatever the case");
        click("user-" + OTHER_RIDER);
        waitForForm("User");
        untilText("standing", "In good standing");

        // A flag is for the admins. The account works as before.
        click("flag");
        waitForForm("Reason");
        type("reason", "Three drivers reported the same thing");
        click("confirm");
        untilText("standing", "Flagged");
        untilText("flagReason", "Three drivers reported the same thing");
        other.get("/api/me");
        assertEqual(200, other.status, "a flagged account still works");

        // A block is for the account: signed out, and kept out.
        click("block");
        waitForForm("Reason");
        type("reason", "Blocked while the reports are looked into");
        click("confirm");
        untilText("standing", "Blocked");
        other.get("/api/me");
        assertEqual(401, other.status, "a blocked account's token");
        other.post("/api/rides", "{\"pickupLat\":37.8085,\"pickupLng\":-122.4125,"
                + "\"pickupAddress\":\"Pier 39\",\"dropoffLat\":37.7955,\"dropoffLng\":-122.3937,"
                + "\"dropoffAddress\":\"Ferry Building\",\"distanceMeters\":2400,"
                + "\"durationSeconds\":540}");
        assertEqual(401, other.status, "a blocked account asking for a ride");
        assertFalse(Actor.canSignIn(OTHER_RIDER), "a blocked account can sign in");
        assertTrue(findByName("event-1") != null, "the flag and the block are in the history");
        shot("admin-user");

        // Through the app, too: the sign-in screen says no.
        back();
        waitForForm("Users");
        click("filter-blocked");
        until(() -> has("user-" + OTHER_RIDER), 20000, "the account among the blocked");
        back();
        waitForForm("Admin");
        signOut();
        click("signIn");
        waitForForm("SignIn");
        type("email", OTHER_RIDER);
        type("password", PASSWORD);
        click("submit");
        until(() -> text("error").length() > 0, 20000, "the blocked sign-in to be refused");
        back();
        waitForForm("Welcome");

        // Both are lifted again, from the same screen.
        signIn(ADMIN, "Admin");
        click("alert-flagged");
        waitForForm("Users");
        click("user-" + OTHER_RIDER);
        waitForForm("User");
        click("unblock");
        untilText("standing", "Flagged");
        click("unflag");
        untilText("standing", "In good standing");
        until(() -> {
            other.get("/api/me");
            return other.status == 200;
        }, 20000, "the account to work again");
        assertTrue(Actor.canSignIn(OTHER_RIDER), "an unblocked account cannot sign in");
        return true;
    }
}
