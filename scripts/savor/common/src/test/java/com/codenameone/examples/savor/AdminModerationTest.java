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

/// Flagging an account and blocking one, from the admin's screens, and what
/// each does to the person it is done to.
public class AdminModerationTest extends E2e {
    private static final String OTHER_CUSTOMER = "customer2@savor.example";

    @Override
    protected boolean run() throws Exception {
        final Actor other = Actor.signIn(OTHER_CUSTOMER);
        other.get("/api/me");
        assertEqual(200, other.status);

        onEdt(() -> AppConfig.setMode("admin"));
        signIn(ADMIN, "Admin");
        // One account arrives already flagged, and the dashboard says so.
        until(() -> has("alert-flagged"), 20000, "the flagged account on the dashboard");
        click("users");
        waitForForm("Users");
        type("search", "CUSTOMER2");
        until(() -> has("user-" + OTHER_CUSTOMER) && !has("user-" + CUSTOMER), 20000,
                "the search to narrow the list, whatever the case");
        click("user-" + OTHER_CUSTOMER);
        waitForForm("User");
        untilText("standing", "In good standing");

        // A flag is for the admins. The account works as before.
        click("flag");
        waitForForm("Reason");
        type("reason", "Three couriers reported the same thing");
        click("confirm");
        untilText("standing", "Flagged");
        untilText("flagReason", "Three couriers reported the same thing");
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
        other.post("/api/deliveries", "{\"pickupLat\":37.8085,\"pickupLng\":-122.4125,"
                + "\"pickupAddress\":\"Pier 39\",\"dropoffLat\":37.7955,\"dropoffLng\":-122.3937,"
                + "\"dropoffAddress\":\"Ferry Building\",\"distanceMeters\":2400,"
                + "\"durationSeconds\":540}");
        assertEqual(401, other.status, "a blocked account asking for a delivery");
        assertFalse(Actor.canSignIn(OTHER_CUSTOMER), "a blocked account can sign in");
        assertTrue(findByName("event-1") != null, "the flag and the block are in the history");
        shot("admin-user");

        // Through the app, too: the sign-in screen says no.
        back();
        waitForForm("Users");
        click("filter-blocked");
        until(() -> has("user-" + OTHER_CUSTOMER), 20000, "the account among the blocked");
        back();
        waitForForm("Admin");
        signOut();
        click("signIn");
        waitForForm("SignIn");
        type("email", OTHER_CUSTOMER);
        type("password", PASSWORD);
        click("submit");
        until(() -> text("error").length() > 0, 20000, "the blocked sign-in to be refused");
        back();
        waitForForm("Welcome");

        // Both are lifted again, from the same screen.
        signIn(ADMIN, "Admin");
        click("alert-flagged");
        waitForForm("Users");
        click("user-" + OTHER_CUSTOMER);
        waitForForm("User");
        click("unblock");
        untilText("standing", "Flagged");
        click("unflag");
        untilText("standing", "In good standing");
        until(() -> {
            other.get("/api/me");
            return other.status == 200;
        }, 20000, "the account to work again");
        assertTrue(Actor.canSignIn(OTHER_CUSTOMER), "an unblocked account cannot sign in");
        return true;
    }
}
