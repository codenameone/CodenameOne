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
package com.codenameone.examples.dash;

import com.codenameone.examples.dash.net.Session;
import com.codenameone.examples.dash.run.RunView;
import com.codenameone.examples.dash.ui.Nav;

/// Registering with a runner name, the first level, choosing a runner, the
/// settings and signing out.
public class AccountTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        shot("signin");
        click("registerButton");
        waitForForm("register");
        String stamp = String.valueOf(System.currentTimeMillis() % 100000);
        type("displayName", "Ironclad");
        type("email", "new" + stamp + "@dash.example");
        type("password", "a-long-password");
        shot("register-name");
        click("createButton");
        waitForForm("home");
        // A new player is taken straight into the first level.
        waitForForm("run");
        final RunView view = track();
        shot("tutorial-first-visit");
        onEdt(() -> {
            view.stop();
            view.driver().dispose();
            Nav.home();
        });
        waitForForm("home");
        assertEqual("Ironclad", Session.user().displayName());
        assertEqual("runner-dash", Session.user().runner() == null ? "runner-dash" : Session.user().runner(), "a new player runs as Dash");
        shot("home");

        // Picking another runner saves it on the account.
        click("runnerButton");
        waitForForm("runner");
        shot("runner");
        if (has("choose-runner-kite")) {
            click("choose-runner-kite");
            until(() -> "runner-kite".equals(Session.user().runner()), 10000, "the runner to be saved");
        }

        onEdt(() -> com.codenameone.examples.dash.ui.SettingsForm.show());
        waitForForm("settings");
        shot("settings");
        click("signOut");
        waitForForm("signin");
        assertNull(Session.user());
        return true;
    }
}
