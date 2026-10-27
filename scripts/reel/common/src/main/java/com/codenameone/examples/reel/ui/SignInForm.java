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
package com.codenameone.examples.reel.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.Telemetry;
import com.codenameone.examples.reel.net.Session;

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
        final SpanLabel error = Ui.text("", "SvError");
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
