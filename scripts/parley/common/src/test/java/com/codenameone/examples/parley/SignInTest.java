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
package com.codenameone.examples.parley;

import com.codename1.ui.Button;
import com.codenameone.examples.parley.net.Session;
import com.codenameone.examples.parley.ui.Nav;

import com.codename1.ui.Display;
import com.codenameone.examples.parley.net.Session;
import com.codenameone.examples.parley.ui.Nav;

/// Signing in, a wrong password, and the admin's API refused to a member.
public class SignInTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        shot("welcome");
        click("signIn");
        waitForForm("SignIn");
        type("email", ALICE);
        type("password", "not-the-password");
        click("submit");
        until(() -> text("error").length() > 0, 20000, "the sign-in error");
        assertEqual("SignIn", Display.getInstance().getCurrent().getName());
        assertNull(Session.user(), "nobody is signed in after a wrong password");

        type("password", PASSWORD);
        click("submit");
        waitForForm("Chats");
        assertEqual(ALICE, Session.user().username());
        assertFalse(Session.user().admin(), "alice is not an admin");
        until(() -> has("chat-Weekend hike"), 30000, "the seeded group in the list");
        shot("chats");

        // Asking for the admin's screens without the role gets the chats,
        // and the server refuses the admin's API to the same token.
        onEdt(() -> Nav.switchTo(Nav.ADMIN));
        waitForForm("Chats");
        Actor alice = Actor.signIn(ALICE);
        alice.get("/api/admin/stats");
        assertEqual(403, alice.status, "a member asking for the admin's numbers");
        alice.get("/api/me");
        assertEqual(200, alice.status);

        onEdt(() -> Nav.signOut());
        waitForForm("Welcome");
        assertNull(Session.user());
        return true;
    }
}
