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
package com.codenameone.examples.dash.ui;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codenameone.examples.dash.api.RegisterDto;
import com.codenameone.examples.dash.engage.Telemetry;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.net.Session;

/// Creating an account. The player name is the one piece of text a player
/// types that everyone else sees, so its field is styled as a banner -- the
/// `NameField` UIID in `theme.css` -- and previews the name as it will appear
/// over the arena.
// tag::dash-name-field[]
public final class RegisterForm {
    private RegisterForm() {
    }

    public static void show() {
        Form f = Ui.form("New player", "register");
        Ui.back(f, SignInForm::show);
        final TextField name = new TextField("", "Your runner name", 16, TextField.ANY);
        name.setUIID("NameField");
        name.setName("displayName");
        name.setMaxSize(16);
        final Label preview = new Label("???", "NamePreview");
        name.addDataChangedListener((type, index) -> {
            String n = name.getText().trim();
            preview.setText(n.length() == 0 ? "???" : n);
            preview.getParent().revalidate();
        });
        final TextField email = SignInForm.field("Email", TextField.EMAILADDR, "email");
        final TextField password = SignInForm.field("Password (8+ characters)", TextField.PASSWORD,
                "password");
        Container col = Ui.column();
        col.add(SignInForm.heroes());
        col.add(Ui.label("Choose your runner name", "SectionTitle"));
        col.add(Ui.panel(name, Ui.centered(preview)));
        col.add(Ui.panel(email, password, Ui.wood("Enter the arena", "createButton", e -> {
            final String mail = email.getText().trim();
            final String pass = password.getText();
            Api.account().register(new RegisterDto(mail, pass, name.getText().trim()),
                    Net.to(player -> {
                        Telemetry.registered();
                        Session.signIn(mail, pass, () -> Ui.later(Nav::signedIn), Ui::failed);
                    }, Ui::failed));
        })));
        f.add(Ui.centeredColumn(col));
        f.show();
    }
}
// end::dash-name-field[]
