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
import com.codenameone.examples.brickyard.api.RegisterDto;
import com.codenameone.examples.brickyard.engage.Telemetry;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.net.Session;

/// A new account. The birth year is the age gate: the server decides from it
/// what a young player may do, and Settings shows the result.
public final class RegisterForm {
    private RegisterForm() {
    }

    public static void show() {
        Form f = Ui.form("Create an account", "register");
        Ui.back(f, SignInForm::show);
        final TextField name = Ui.field("Display name", TextField.ANY, "displayName");
        final TextField email = Ui.field("Email", TextField.EMAILADDR, "email");
        final TextField password = Ui.field("Password", TextField.PASSWORD, "password");
        final TextField year = Ui.field("Birth year", TextField.NUMERIC, "birthYear");
        Container panel = Ui.panel(name, email, password, year,
                Ui.text("Players under 13 get chat off and a spending limit until a parent changes them.",
                        "Muted"),
                Ui.primary("Create account", "createButton", e -> {
                    int born = parseYear(year.getText());
                    if (born <= 0) {
                        Ui.toast("Enter the year you were born.");
                        return;
                    }
                    final String mail = email.getText().trim();
                    final String pass = password.getText();
                    Api.account().register(new RegisterDto(mail, pass, name.getText().trim(), born),
                            Net.to(account -> {
                                Telemetry.registered();
                                Session.signIn(mail, pass, () -> Ui.later(Nav::signedIn), Ui::failed);
                            }, Ui::failed));
                }));
        f.add(panel);
        f.show();
    }

    static int parseYear(String text) {
        String t = text == null ? "" : text.trim();
        if (t.length() != 4) {
            return -1;
        }
        int y = 0;
        for (int i = 0; i < 4; i++) {
            char c = t.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            y = y * 10 + (c - '0');
        }
        return y < 1900 ? -1 : y;
    }
}
