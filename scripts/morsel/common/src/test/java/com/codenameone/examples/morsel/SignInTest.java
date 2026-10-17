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
import com.codenameone.examples.morsel.net.Session;
import com.codenameone.examples.morsel.ui.Nav;

/// Signing in, and what a role does and does not open.
public class SignInTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        shot("welcome");

        // A wrong password says so and stays put.
        click("signIn");
        waitForForm("SignIn");
        type("email", CUSTOMER);
        type("password", "not-the-password");
        click("submit");
        until(() -> text("error").length() > 0, 20000, "the sign-in error");
        assertEqual("SignIn", com.codename1.ui.Display.getInstance().getCurrent().getName());
        assertNull(Session.user(), "nobody is signed in after a wrong password");

        // The right one lands a customer on the customer's screen.
        type("password", PASSWORD);
        click("submit");
        waitForForm("Customer");
        assertEqual(CUSTOMER, Session.user().username());
        assertTrue(Session.user().customer() && !Session.user().courier() && !Session.user().admin(),
                "the demo customer has the customer role and no other");
        shot("customer-home");

        // Asking for the admin's screens without the role gets the customer's.
        onEdt(() -> Nav.switchTo(Nav.ADMIN));
        waitForForm("Customer");
        // Asking to drive gets the way there, the application, and nothing a
        // courier has: there is no going online from it.
        onEdt(() -> Nav.switchTo(Nav.COURIER));
        waitForForm("Apply");
        untilText("applicationStatus", "Not started");
        assertFalse(has("goOnline"), "someone who is not a courier can go online");
        onEdt(() -> Nav.switchTo(Nav.CUSTOMER));
        waitForForm("Customer");
        // And the server agrees, which is the part that matters: the same
        // person's token is refused on the courier's and the admin's API.
        Actor customer = Actor.signIn(CUSTOMER);
        customer.get("/api/admin/stats");
        assertEqual(403, customer.status, "a customer asking for the admin's numbers");
        customer.online(true, HERE.getLatitude(), HERE.getLongitude());
        assertEqual(403, customer.status, "a customer going online as a courier");
        customer.get("/api/me");
        assertEqual(200, customer.status);

        // The menu offers no mode to switch to -- only the way to apply for
        // one -- and leads to the settings.
        assertTrue(has("menu-drive"), "a customer is shown how to become a courier");
        assertFalse(findByName("switch-courier") instanceof Button, "a customer is offered onboarding");
        assertFalse(findByName("switch-admin") instanceof Button, "a customer is offered admin");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        click("signOut");
        waitForForm("Welcome");
        assertNull(Session.user());
        return true;
    }
}
