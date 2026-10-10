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

/// A new account: register, be signed in, verify the phone, arrive.
public class RegisterTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        // One address per pass, not per run: the server is new for every run, both
        // passes share it, and a timestamp in the address wrapped differently from
        // run to run in the screens that are compared.
        String email = "new." + (desktopRun() ? "desk" : "phone") + "@wayline.example";
        click("register");
        waitForForm("Register");
        type("name", "Nico Newcomer");
        type("email", email);
        type("phone", "+1 555 010 " + (System.currentTimeMillis() % 9000 + 1000));
        type("password", PASSWORD);
        shot("register");
        click("submit");

        // Registered and signed in, and asked for the code. The test server
        // sends no text; it says what the code is.
        waitForForm("VerifyPhone");
        assertEqual(email, Session.user().username);
        assertFalse(Session.user().phoneVerified, "a new account starts unverified");
        until(() -> text("demoCode").indexOf("code is ") > 0, 20000, "the demo code");
        shot("verify-phone");
        String notice = text("demoCode");
        int at = notice.indexOf("code is ") + 8;
        String code = notice.substring(at, at + 6);

        // A wrong code is refused by the server, and says so.
        type("code", "000000".equals(code) ? "111111" : "000000");
        click("submit");
        until(() -> text("error").length() > 0, 20000, "the wrong-code error");
        assertFalse(Session.user().phoneVerified);

        type("code", code);
        click("submit");
        waitForForm("Rider");
        assertTrue(Session.user().phoneVerified, "the right code verifies the number");
        return true;
    }
}
