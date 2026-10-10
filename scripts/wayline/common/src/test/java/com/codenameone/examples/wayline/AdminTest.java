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

import com.codename1.components.Switch;
import com.codename1.ui.Component;

import java.util.Map;

/// The admin's screens, and a role granted from them taking effect on a token
/// that was issued before the grant.
public class AdminTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        // Someone to promote, and a car for the map.
        Actor rider = Actor.signIn(RIDER);
        rider.get("/api/admin/stats");
        assertEqual(403, rider.status);
        Actor driver = Actor.signIn(DRIVER);
        driver.online(true, HERE.getLatitude() + 0.003, HERE.getLongitude() - 0.004);

        onEdt(() -> AppConfig.setMode("admin"));
        signIn(ADMIN, "Admin");
        until(() -> text("driversOnline").startsWith("1 /"), 20000, "one driver online");
        shot("admin-dashboard");
        // The charts are under the fold.
        reveal("chartRevenue");
        shot("admin-trends");

        click("fleet");
        waitForForm("Fleet");
        untilText("fleetCount", "1 car online");
        driver.online(false, 0, 0);
        untilText("fleetCount", "No cars online");
        back();
        waitForForm("Admin");

        // The one application that is waiting, with its documents.
        until(() -> has("alert-applications"), 20000, "the dashboard to say one is waiting");
        click("applications");
        waitForForm("Applications");
        click("application-applicant@wayline.example");
        waitForForm("ApplicationReview");
        untilText("applicationStatus", "Waiting");
        click("document-licence");
        waitForForm("Document");
        until(() -> findByName("picture") != null, 20000, "the licence to be shown");
        back();
        waitForForm("ApplicationReview");
        back();
        waitForForm("Applications");
        back();
        waitForForm("Admin");

        // Make the rider an admin.
        click("users");
        waitForForm("Users");
        click("user-" + RIDER);
        waitForForm("User");
        onEdt(() -> {
            Component toggle = findByName("admin");
            ((Switch) toggle).setValue(true);
        });
        // The token the rider was holding all along now opens the admin API:
        // the server reads the account's roles on every request.
        until(() -> {
            rider.get("/api/admin/stats");
            return rider.status == 200;
        }, 20000, "the grant to reach the rider's existing token");
        Map<String, Object> me = rider.get("/api/me");
        assertEqual(Boolean.TRUE, bool(me.get("admin")), "/api/me reports the new role");

        // And taking it away closes it again.
        onEdt(() -> ((Switch) findByName("admin")).setValue(false));
        until(() -> {
            rider.get("/api/admin/stats");
            return rider.status == 403;
        }, 20000, "the role to be taken away again");

        // Blocking signs the account out wherever it is, and says why.
        click("block");
        waitForForm("Reason");
        click("confirm");
        until(() -> text("error").length() > 0, 20000, "a block with no reason to be refused");
        type("reason", "Asked to be locked while travelling");
        click("confirm");
        untilText("standing", "Blocked");
        rider.get("/api/me");
        assertEqual(401, rider.status, "a blocked account's token");
        click("unblock");
        untilText("standing", "In good standing");
        until(() -> {
            rider.get("/api/me");
            return rider.status == 200;
        }, 20000, "the block to be lifted");
        assertTrue(findByName("event-1") != null, "both are in the account's history");
        back();
        waitForForm("Users");
        back();
        waitForForm("Admin");

        // The rest of the console opens, and the prices can be saved.
        click("rides");
        waitForForm("Rides");
        until(() -> has("ride-0"), 20000, "the rides");
        click("ride-0");
        waitForForm("RideDetail");
        back();
        waitForForm("Rides");
        back();
        waitForForm("Admin");
        click("payments");
        waitForForm("Payments");
        back();
        waitForForm("Admin");
        click("pricing");
        waitForForm("Pricing");
        until(() -> text("baseFare").length() > 0, 20000, "the prices");
        final String base = text("baseFare");
        type("commissionPercent", "95");
        click("submit");
        until(() -> text("error").length() > 0, 20000, "a commission of 95% to be refused");
        back();
        waitForForm("Admin");
        click("pricing");
        waitForForm("Pricing");
        untilText("baseFare", base);
        return true;
    }

    private static Boolean bool(Object value) {
        return Boolean.valueOf(Boolean.TRUE.equals(value) || "true".equals(value));
    }
}
