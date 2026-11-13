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
package com.codenameone.examples.slate;

import com.codename1.ui.Button;
import com.codenameone.examples.slate.net.Session;
import com.codenameone.examples.slate.ui.Nav;

/// Signing in, and what a role does and does not open.
public class SignInTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        shot("welcome");

        // A wrong password says so and stays put.
        click("signIn");
        waitForForm("SignIn");
        type("email", CLIENT);
        type("password", "not-the-password");
        click("submit");
        until(() -> text("error").length() > 0, 20000, "the sign-in error");
        assertEqual("SignIn", com.codename1.ui.Display.getInstance().getCurrent().getName());
        assertNull(Session.user(), "nobody is signed in after a wrong password");

        // The right one lands a client on the client's home.
        type("password", PASSWORD);
        click("submit");
        waitForForm("Client");
        assertEqual(CLIENT, Session.user().username());
        assertTrue(Session.user().client() && !Session.user().provider() && !Session.user().owner(),
                "the demo client has the client role and no other");
        shot("client-home");

        // Asking for the provider's or the owner's screens without the role
        // gets the client's.
        onEdt(() -> Nav.switchTo(Nav.PROVIDER));
        waitForForm("Client");
        onEdt(() -> Nav.switchTo(Nav.OWNER));
        waitForForm("Client");
        // And the server agrees, which is the part that matters.
        Actor client = Actor.signIn(CLIENT);
        client.get("/api/provider/hours");
        assertEqual(403, client.status, "a client asking for a provider's hours");
        client.get("/api/owner/dashboard");
        assertEqual(403, client.status, "a client asking for the owner's dashboard");
        client.get("/api/me");
        assertEqual(200, client.status);

        assertFalse(findByName("switch-provider") instanceof Button, "a client is offered provider mode");
        assertFalse(findByName("switch-owner") instanceof Button, "a client is offered owner mode");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        click("signOut");
        waitForForm("Welcome");
        assertNull(Session.user());
        return true;
    }
}
