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

import com.codenameone.examples.wayline.net.Session;
import com.codenameone.examples.wayline.ui.Photos;

/// Becoming a driver, start to finish: an account that asks to drive, the
/// application with its photographs, an admin's approval, and the driver's
/// screen that only then opens.
public class DriverOnboardingTest extends E2e {
    private static final String[] KINDS = {"photo_id", "licence", "selfie",
        "vehicle_registration", "insurance"};

    @Override
    protected boolean run() throws Exception {
        // One address per pass, not per run: the server is new for every run, both
        // passes share it, and a timestamp in the address wrapped differently from
        // run to run in the screens that are compared.
        final String email = "dana." + (desktopRun() ? "desk" : "phone") + "@wayline.example";
        Photos.useSource(camera -> photograph());

        click("register");
        waitForForm("Register");
        type("name", "Dana Driver");
        type("email", email);
        type("phone", "+1 555 011 " + (System.currentTimeMillis() % 9000 + 1000));
        type("password", PASSWORD);
        click("want-driver");
        click("submit");
        waitForForm("VerifyPhone");
        until(() -> text("demoCode").indexOf("code is ") > 0, 20000, "the demo code");
        String notice = text("demoCode");
        int at = notice.indexOf("code is ") + 8;
        type("code", notice.substring(at, at + 6));
        click("submit");

        // Asking to drive does not make a driver. It leads to the application.
        waitForForm("Apply");
        assertFalse(Session.user().driver, "a new account is not a driver");
        untilText("applicationStatus", "Not started");
        assertFalse(has("goOnline"), "an applicant can go online");
        click("apply");
        waitForForm("Application");

        untilText("step", "Step 1 of 5");
        type("legalName", "Dana Morgan Driver");
        date("dateOfBirth", 1990, 5, 17);
        click("gender-female");
        click("next");
        untilText("step", "Step 2 of 5");
        type("licenceNumber", "D7781204");
        date("licenceExpiry", 2031, 8, 31);
        click("next");
        untilText("step", "Step 3 of 5");
        // Moving on with the car left out is refused.
        click("next");
        until(() -> text("error").length() > 0, 20000, "an empty step to be refused");
        type("vehicleMake", "Toyota");
        type("vehicleModel", "Corolla");
        type("vehicleYear", "2022");
        type("vehicleColor", "Silver");
        type("vehiclePlate", "8DNA204");
        click("next");
        untilText("step", "Step 4 of 5");

        // One by the camera, one from the gallery; three of the five so far.
        untilText("status-photo_id", "Needed");
        click("camera-photo_id");
        untilText("status-photo_id", "Added");
        click("gallery-licence");
        untilText("status-licence", "Added");
        click("camera-selfie");
        untilText("status-selfie", "Added");
        shot("onboarding-documents");
        // The server will not take an application with papers missing.
        click("next");
        untilText("step", "Step 5 of 5");
        untilText("documentsAdded", "3 / 5");
        click("submitApplication");
        until(() -> text("error").length() > 0, 20000, "an incomplete application to be refused");
        click("backStep");
        untilText("step", "Step 4 of 5");
        click("camera-vehicle_registration");
        untilText("status-vehicle_registration", "Added");
        click("gallery-insurance");
        untilText("status-insurance", "Added");
        click("next");
        untilText("documentsAdded", "5 / 5");
        click("submitApplication");

        waitForForm("Apply");
        untilText("applicationStatus", "Under review");
        assertFalse(has("goOnline"), "an applicant under review can go online");
        Actor applicant = Actor.signIn(email);
        applicant.online(true, HERE.getLatitude(), HERE.getLongitude());
        assertEqual(403, applicant.status, "an applicant going online through the API");
        shot("onboarding-under-review");
        signOut();

        // The admin reads it, opens every paper, and approves.
        signIn(ADMIN, "Admin");
        click("applications");
        waitForForm("Applications");
        click("application-" + email);
        waitForForm("ApplicationReview");
        untilText("applicationStatus", "Waiting");
        shot("admin-application");
        for (int iter = 0; iter < KINDS.length; iter++) {
            click("document-" + KINDS[iter]);
            waitForForm("Document");
            until(() -> findByName("picture") != null, 20000, KINDS[iter] + " to be shown");
            back();
            waitForForm("ApplicationReview");
        }
        click("approve");
        waitForForm("Applications");
        back();
        waitForForm("Admin");
        signOut();

        // Approved: the same account now opens on the driver's screen, and
        // going online works.
        signIn(email, "Driver");
        assertTrue(Session.user().driver, "approval makes the account a driver");
        untilText("driverState", "You are offline");
        click("goOnline");
        untilText("driverState", "Looking for rides");
        click("goOffline");
        untilText("driverState", "You are offline");
        return true;
    }
}
