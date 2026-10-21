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
package com.codenameone.examples.skirmish.ui;

import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.skirmish.AppConfig;
import com.codenameone.examples.skirmish.net.Session;

/// Signing in, and the way to the practice arena without an account.
public final class SignInForm {
    private SignInForm() {
    }

    public static void show() {
        Form f = Ui.form("", "signin");
        f.setLayout(BoxLayout.yCenter());
        f.add(Ui.label("SKIRMISH", "Logo"));
        f.add(Ui.label("Deploy. Defend. Destroy.", "Tagline"));
        final TextField email = field("Email", TextField.EMAILADDR, "email");
        final TextField password = field("Password", TextField.PASSWORD, "password");
        f.add(Ui.panel(email, password,
                Ui.wood("Sign in", "signInButton", e -> {
                    Session.signIn(email.getText().trim(), password.getText(),
                            () -> Ui.later(Nav::signedIn), Ui::failed);
                }),
                Ui.stone("Create an account", "registerButton", e -> RegisterForm.show())));
        f.add(Ui.stone("Practice without an account", "practiceButton",
                e -> BattleForm.practice()));
        if (AppConfig.serverSwitchable()) {
            f.add(Ui.button("Server: " + AppConfig.serverUrl(), "LinkButton", "serverButton",
                    e -> SettingsForm.chooseServer()));
        }
        f.show();
    }

    static TextField field(String hint, int constraint, String name) {
        TextField t = new TextField("", hint, 20, constraint);
        t.setUIID("GameField");
        t.setName(name);
        return t;
    }
}
