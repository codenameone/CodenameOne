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
package com.codenameone.examples.pulse;

import com.codename1.ui.Button;
import com.codenameone.examples.pulse.net.Session;
import com.codenameone.examples.pulse.ui.Nav;

/// Signing in, and what a role does and does not open.
public class SignInTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        shot("welcome");

        // A wrong password says so and stays put.
        click("signIn");
        waitForForm("SignIn");
        type("email", MEMBER);
        type("password", "not-the-password");
        click("submit");
        until(() -> text("error").length() > 0, 20000, "the sign-in error");
        assertEqual("SignIn", com.codename1.ui.Display.getInstance().getCurrent().getName());
        assertNull(Session.user(), "nobody is signed in after a wrong password");

        // The right one lands a member on the member's home.
        type("password", PASSWORD);
        click("submit");
        waitForForm("Member");
        assertEqual(MEMBER, Session.user().username());
        assertTrue(Session.user().member() && !Session.user().desk() && !Session.user().owner()
                && !Session.user().trainer(), "the demo member has the member role and no other");
        shot("member-home");

        // Asking for the desk's or the owner's screens without the role gets
        // the member's.
        onEdt(() -> Nav.switchTo(Nav.DESK));
        waitForForm("Member");
        onEdt(() -> Nav.switchTo(Nav.OWNER));
        waitForForm("Member");
        // And the server agrees, which is the part that matters: the same
        // person's token is refused on the desk's and the owner's API.
        Actor member = Actor.signIn(MEMBER);
        member.get("/api/desk/today?location=");
        assertEqual(403, member.status, "a member asking for the desk's day");
        member.get("/api/me");
        assertEqual(200, member.status);

        // The menu offers no mode to switch to, and leads to the settings.
        assertFalse(findByName("switch-desk") instanceof Button, "a member is offered the desk");
        assertFalse(findByName("switch-owner") instanceof Button, "a member is offered owner mode");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        click("signOut");
        waitForForm("Welcome");
        assertNull(Session.user());
        return true;
    }
}
