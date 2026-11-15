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
package com.codenameone.examples.brickyard.ui;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.brickyard.AppConfig;
import com.codenameone.examples.brickyard.net.Session;

/// The first screen of a signed-out app.
public final class SignInForm {
    private SignInForm() {
    }

    public static void show() {
        Form f = Ui.form("", "signin");
        f.setLayout(BoxLayout.yCenter());
        f.getToolbar().hideToolbar();
        Container col = new Container(BoxLayout.y());
        col.add(new AvatarView(Avatar.load(), true));
        col.add(Ui.label("BRICKYARD", "Logo"));
        col.add(Ui.label("Build it. Share it. Play it.", "Tagline"));
        final TextField email = Ui.field("Email", TextField.EMAILADDR, "email");
        final TextField password = Ui.field("Password", TextField.PASSWORD, "password");
        col.add(Ui.panel(email, password,
                Ui.primary("Sign in", "signInButton", e -> Session.signIn(email.getText().trim(),
                        password.getText(), () -> Ui.later(Nav::signedIn), Ui::failed)),
                Ui.secondary("Create an account", "registerButton", e -> RegisterForm.show())));
        if (AppConfig.serverSwitchable()) {
            col.add(Ui.link("Server: " + AppConfig.serverUrl(), "serverButton",
                    e -> SettingsForm.chooseServer()));
        }
        Container safe = BorderLayout.centerCenter(col);
        safe.setSafeArea(true);
        f.add(safe);
        f.show();
    }
}
