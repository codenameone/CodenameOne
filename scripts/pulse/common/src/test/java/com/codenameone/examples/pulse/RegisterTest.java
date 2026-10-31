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

import com.codenameone.examples.pulse.net.Session;

/// A new account: register, be signed in, verify the phone, arrive.
public class RegisterTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        // One address per pass, not per run: the server is new for every run, both
        // passes share it, and a timestamp in the address wrapped differently from
        // run to run in the screens that are compared.
        String email = "new." + (desktopRun() ? "desk" : "phone") + "@pulse.example";
        click("register");
        waitForForm("Register");
        type("name", "Nico Newcomer");
        type("email", email);
        type("phone", "+1 555 010 " + (System.currentTimeMillis() % 9000 + 1000));
        type("birthDate", "1990-04-12");
        type("password", PASSWORD);
        shot("register");
        click("submit");

        // Registered and signed in, and asked for the code. The test server
        // sends no text; it says what the code is.
        waitForForm("VerifyPhone");
        assertEqual(email, Session.user().username());
        assertFalse(Session.user().phoneVerified(), "a new account starts unverified");
        until(() -> text("demoCode").indexOf("code is ") > 0, 20000, "the demo code");
        shot("verify-phone");
        String notice = text("demoCode");
        int at = notice.indexOf("code is ") + 8;
        String code = notice.substring(at, at + 6);

        // A wrong code is refused by the server, and says so.
        type("code", "000000".equals(code) ? "111111" : "000000");
        click("submit");
        until(() -> text("error").length() > 0, 20000, "the wrong-code error");
        assertFalse(Session.user().phoneVerified());

        type("code", code);
        click("submit");
        waitForForm("Member");
        assertTrue(Session.user().phoneVerified(), "the right code verifies the number");
        return true;
    }
}
