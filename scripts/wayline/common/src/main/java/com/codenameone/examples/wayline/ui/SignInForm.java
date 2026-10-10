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
package com.codenameone.examples.wayline.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.net.Session;

/// E-mail and password. The form is the app's own; what happens when it is
/// submitted is in [Session#signIn].
public final class SignInForm {
    private SignInForm() {
    }

    public static void show(Form previous) {
        final Form form = Ui.form("Sign in", "SignIn");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField email = Ui.field(page, "E-mail", "email", "you@example.com",
                TextArea.EMAILADDR);
        final TextField password = Ui.field(page, "Password", "password", "",
                TextArea.PASSWORD);
        final SpanLabel error = Ui.text("", "WlError");
        error.setName("error");
        final Button signIn = Ui.primary("Sign in", "submit", null);
        signIn.addActionListener(e -> {
            if (email.getText().trim().length() == 0 || password.getText().length() == 0) {
                error.setText("Enter your e-mail and password.");
                Ui.refresh(form);
                return;
            }
            signIn.setEnabled(false);
            error.setText("");
            Session.signIn(email.getText().trim(), password.getText(), () -> {
                Telemetry.signedIn();
                Nav.enter();
            }, (status, message) -> {
                        signIn.setEnabled(true);
                        error.setText(message);
                        Ui.refresh(form);
                    });
        });
        page.add(error).add(signIn);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
