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
package com.codenameone.examples.bastion.ui;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codenameone.examples.bastion.net.Session;

/// E-mail and password, then -- for an account with an authenticator app --
/// the six-digit code; or a passkey instead of both.
public final class SignInForm {
    private SignInForm() {
    }

    public static void show(final Runnable signedIn) {
        Container form = Ui.landing("Sign in", "Bastion", "Your bank, locked down.");
        form.add(new Label(Lang.tr("Sign in"), "BnLandingHeading"));
        final TextField email = Ui.field("E-mail", TextArea.EMAILADDR);
        final TextField password = Ui.field("Password", TextArea.PASSWORD);
        final Label error = Ui.error();
        final Button go = Ui.primary("Sign in");
        form.addAll(email, password, error, go);
        go.addActionListener(evt -> {
            go.setEnabled(false);
            Ui.showError(error, null);
            Session.signIn(email.getText(), password.getText(),
                    pending -> codeStep(pending, signedIn),
                    signedIn,
                    (status, message) -> {
                        go.setEnabled(true);
                        Ui.showError(error, Lang.tr(message));
                    });
            // The password is not kept in the field once it has been sent.
            password.setText("");
        });
        if (Session.passkeysSupported()) {
            Button passkey = Ui.secondary("Sign in with a passkey");
            passkey.addActionListener(evt -> Session.signInWithPasskey(signedIn,
                    (status, message) -> Ui.showError(error, Lang.tr(message))));
            form.add(passkey);
        }
        Button join = Ui.secondary("Open an account");
        join.addActionListener(evt -> OnboardingForm.show(() -> show(signedIn), signedIn));
        form.add(join);
        Ui.show(form.getComponentForm(), false);
    }

    private static void codeStep(final Session.Pending pending, final Runnable signedIn) {
        Container form = Ui.landing("Verification", "Enter your code", null);
        form.add(Ui.muted(Lang.tr("Open your authenticator app and enter the six-digit code, "
                + "or one of your recovery codes.")));
        final TextField code = Ui.field("Code", TextArea.NUMERIC);
        final Label error = Ui.error();
        final Button go = Ui.primary("Continue");
        form.addAll(code, error, go);
        go.addActionListener(evt -> {
            go.setEnabled(false);
            Session.submitCode(pending, code.getText(), signedIn, (status, message) -> {
                go.setEnabled(true);
                code.setText("");
                Ui.showError(error, Lang.tr(message));
            });
        });
        Ui.show(form.getComponentForm(), true);
    }
}
