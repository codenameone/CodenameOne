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
package com.codenameone.examples.papercut.ui;

import com.codename1.appkit.Net;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codenameone.examples.papercut.api.RegisterDto;
import com.codenameone.examples.papercut.net.Api;
import com.codenameone.examples.papercut.net.Session;

/// Signing in, and opening an account. Neither is needed to play: a guest
/// plays every mode, and signs in only to put a run on the boards.
public final class SignInForm {
    private SignInForm() {
    }

    // tag::papercut-signin[]
    public static void show() {
        Form f = Ui.form("Sign in", "signin");
        Ui.desktopColumn(f);
        Ui.back(f, Nav::home);
        final TextField email = field("Email", TextField.EMAILADDR, "email");
        final TextField password = field("Password", TextField.PASSWORD, "password");
        Container card = Ui.panel(Ui.label("Papercut", "CardTitle"),
                Ui.label("Sign in to put your runs on the boards.", "CardText"),
                email, password,
                Ui.primary("Sign in", "signInButton", e -> Session.signIn(email.getText().trim(),
                        password.getText(), () -> Ui.later(Nav::signedIn), Ui::failed)),
                Ui.secondary("New here? Open an account", "registerButton", e -> register()));
        f.add(Ui.centeredColumn(Ui.column(card)));
        f.show();
    }
    // end::papercut-signin[]

    /// A new account: the name on the boards, then the sign-in details.
    public static void register() {
        Form f = Ui.form("New account", "register");
        Ui.desktopColumn(f);
        Ui.back(f, SignInForm::show);
        final TextField name = field("Name on the boards", TextField.ANY, "displayName");
        final TextField email = field("Email", TextField.EMAILADDR, "email");
        final TextField password = field("Password", TextField.PASSWORD, "password");
        Container card = Ui.panel(Ui.label("Pick a pen name", "CardTitle"), name, email, password,
                Ui.primary("Open account", "createButton", e -> {
                    final String mail = email.getText().trim();
                    final String pass = password.getText();
                    Api.account().register(new RegisterDto(mail, pass, name.getText().trim()),
                            Net.to(player -> Session.signIn(mail, pass, () -> Ui.later(Nav::signedIn),
                                    Ui::failed), Ui::failed));
                }));
        f.add(Ui.centeredColumn(Ui.column(card)));
        f.show();
    }

    static TextField field(String hint, int constraint, String name) {
        TextField t = new TextField("", hint, 20, constraint);
        t.setName(name);
        return t;
    }
}
