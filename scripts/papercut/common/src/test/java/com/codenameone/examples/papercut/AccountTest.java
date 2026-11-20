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
package com.codenameone.examples.papercut;

import com.codenameone.examples.papercut.net.Session;
import com.codenameone.examples.papercut.play.ArenaView;
import com.codenameone.examples.papercut.ui.SettingsForm;

/// A new player opens an account, plays a run the server accepts, and finds
/// it on the boards; then signs out again.
public class AccountTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        click("signIn");
        waitForForm("signin");
        shot("signin");
        click("registerButton");
        waitForForm("register");
        String stamp = String.valueOf(System.currentTimeMillis() % 100000);
        type("displayName", "Origami" + stamp);
        type("email", "new" + stamp + "@papercut.example");
        type("password", "a-long-password");
        click("createButton");
        until(() -> Session.user() != null, 30000, "the new account");
        waitForForm("home");
        assertEqual("Origami" + stamp, Session.user().displayName());

        click("playClassic");
        waitForForm("play");
        ArenaView view = arena();
        driveFor(view, 6000);
        finish(view);
        awaitResult("the submitted run");
        shot("result-submitted");
        click("menuFromResult");
        waitForForm("home");

        click("boards");
        waitForForm("boards");
        until(() -> has("boardRows"), 10000, "the board");
        shot("boards");
        onEdt(SettingsForm::show);
        waitForForm("settings");
        shot("settings");
        click("signOut");
        until(() -> Session.user() == null, 20000, "signing out");
        waitForForm("home");
        return true;
    }
}
