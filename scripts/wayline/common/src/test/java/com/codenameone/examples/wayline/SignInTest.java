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
import com.codenameone.examples.wayline.net.Session;
import com.codenameone.examples.wayline.ui.Nav;

/// Signing in, and what a role does and does not open.
public class SignInTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        shot("welcome");

        // A wrong password says so and stays put.
        click("signIn");
        waitForForm("SignIn");
        type("email", RIDER);
        type("password", "not-the-password");
        click("submit");
        until(() -> text("error").length() > 0, 20000, "the sign-in error");
        assertEqual("SignIn", com.codename1.ui.Display.getInstance().getCurrent().getName());
        assertNull(Session.user(), "nobody is signed in after a wrong password");

        // The right one lands a rider on the rider's screen.
        type("password", PASSWORD);
        click("submit");
        waitForForm("Rider");
        assertEqual(RIDER, Session.user().username);
        assertTrue(Session.user().rider && !Session.user().driver && !Session.user().admin,
                "the demo rider has the rider role and no other");
        shot("rider-home");

        // Asking for the admin's screens without the role gets the rider's.
        onEdt(() -> Nav.switchTo(Nav.ADMIN));
        waitForForm("Rider");
        // Asking to drive gets the way there, the application, and nothing a
        // driver has: there is no going online from it.
        onEdt(() -> Nav.switchTo(Nav.DRIVER));
        waitForForm("Apply");
        untilText("applicationStatus", "Not started");
        assertFalse(has("goOnline"), "someone who is not a driver can go online");
        onEdt(() -> Nav.switchTo(Nav.RIDER));
        waitForForm("Rider");
        // And the server agrees, which is the part that matters: the same
        // person's token is refused on the driver's and the admin's API.
        Actor rider = Actor.signIn(RIDER);
        rider.get("/api/admin/stats");
        assertEqual(403, rider.status, "a rider asking for the admin's numbers");
        rider.online(true, HERE.getLatitude(), HERE.getLongitude());
        assertEqual(403, rider.status, "a rider going online as a driver");
        rider.get("/api/me");
        assertEqual(200, rider.status);

        // The menu offers no mode to switch to -- only the way to apply for
        // one -- and leads to the settings.
        assertTrue(has("menu-drive"), "a rider is shown how to become a driver");
        assertFalse(findByName("switch-driver") instanceof Button, "a rider is offered driving");
        assertFalse(findByName("switch-admin") instanceof Button, "a rider is offered admin");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        click("signOut");
        waitForForm("Welcome");
        assertNull(Session.user());
        return true;
    }
}
