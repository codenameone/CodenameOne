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
package com.codenameone.examples.fieldline;

import com.codenameone.examples.fieldline.net.Session;

/// A new account: register, be signed in, and arrive on a technician's day.
public class RegisterTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        // One address per pass: both passes share the run's server.
        String email = "new." + (desktopRun() ? "desk" : "phone") + "@fieldline.example";
        click("register");
        waitForForm("Register");
        type("name", "Nico Newcomer");
        type("email", email);
        type("phone", "+1 555 010 4242");
        type("password", PASSWORD);
        shot("register");
        click("submit");

        // A new account is a technician with the ordinary week, and nothing
        // booked yet.
        waitForForm("Today");
        assertEqual(email, Session.user().username());
        assertTrue(Session.user().technician(), "a new account is a technician");
        return true;
    }
}
