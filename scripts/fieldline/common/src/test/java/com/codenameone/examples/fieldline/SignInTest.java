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

import com.codename1.ui.Button;
import com.codename1.ui.Display;
import com.codenameone.examples.fieldline.net.Session;
import com.codenameone.examples.fieldline.ui.Nav;

/// Signing in, and what a role does and does not open.
public class SignInTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        shot("welcome");

        // A wrong password says so and stays put.
        click("signIn");
        waitForForm("SignIn");
        type("email", TECH);
        type("password", "not-the-password");
        click("submit");
        until(() -> text("error").length() > 0, 20000, "the sign-in error");
        assertEqual("SignIn", Display.getInstance().getCurrent().getName());
        assertNull(Session.user(), "nobody is signed in after a wrong password");

        // The right one lands a technician on their day.
        type("password", PASSWORD);
        click("submit");
        waitForForm("Today");
        assertEqual(TECH, Session.user().username());
        assertTrue(Session.user().technician() && !Session.user().dispatcher()
                && !Session.user().admin(), "the demo technician has that role and no other");
        shot("today");

        // Asking for the board without the role keeps the technician's day.
        onEdt(() -> Nav.switchTo(Nav.DISPATCH));
        waitForForm("Today");
        // And the server agrees, which is the part that matters.
        Actor tech = Actor.signIn(TECH);
        tech.get("/api/dispatch/board");
        assertEqual(403, tech.status, "a technician asking for the dispatch board");
        tech.get("/api/me");
        assertEqual(200, tech.status);

        assertFalse(findByName("switch-dispatch") instanceof Button,
                "a technician is offered the dispatch board");
        click("menu-open");
        click("menu-settings");
        waitForForm("Settings");
        click("signOut");
        waitForForm("Welcome");
        assertNull(Session.user());

        // A dispatcher lands on the board.
        signIn(DISPATCH, "Board");
        shot("board");

        // The intake board: a status view beside the plan, on the phone.
        click("menu-open");
        click("menu-intake");
        waitForForm("Intake");
        until(() -> findByName("intakeBoard") != null, 20000, "the intake board");
        shot("intake-board");
        click("scheduleOnBoard");
        waitForForm("Board");
        // Intake never schedules: the server refuses, whatever the client.
        Actor dispatcher = Actor.signIn(DISPATCH);
        dispatcher.post("/api/dispatch/jobs/no-such-job/intake", "{\"status\":\"SCHEDULED\"}");
        assertEqual(409, dispatcher.status, "asking intake to schedule a job");
        signOut();
        return true;
    }
}
