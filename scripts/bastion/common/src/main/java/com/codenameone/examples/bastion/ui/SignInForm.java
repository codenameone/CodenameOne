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
import com.codenameone.examples.bastion.security.Keys;
import com.codenameone.examples.bastion.security.Lock;

/// Signing in, with the strongest way this device has offered first:
///
/// 1. **A passkey** whose authenticator verifies the user -- a fingerprint,
///    a face, the device PIN -- in one step ([Session#signInWithPasskey]).
///    The server asks for that verification on every passkey sign-in.
/// 2. **This device's key, opened by a biometric**: a session stored on this
///    device is sealed under the app key, which the OS releases only after a
///    biometric check (the lock screen); the device key that signs payments
///    is sealed under the same key.
/// 3. **E-mail and password**, then -- for an account with an authenticator
///    app -- the six-digit code.
///
/// The server holds an account to the strongest factor it has enrolled: once
/// it has a passkey, the password is refused, and the code step after it
/// takes a recovery code and nothing else. That is the documented way back
/// for a customer whose passkey is gone, and the messages below say so.
public final class SignInForm {
    /// The ways in, strongest first ([#strongest]).
    static final int PASSKEY = 0;
    static final int DEVICE = 1;
    static final int PASSWORD = 2;

    private SignInForm() {
    }

    /// The strongest way in this device offers: a passkey where the platform
    /// has a passkey UI, else a stored session a biometric opens, else the
    /// password.
    static int strongest(boolean passkeys, boolean biometricSession) {
        if (passkeys) {
            return PASSKEY;
        }
        return biometricSession ? DEVICE : PASSWORD;
    }

    /// Whether a session is stored on this device behind a biometric.
    static boolean biometricSession() {
        return Session.hasStoredSession() && Keys.exists() && Keys.biometricEnabled();
    }

    public static void show(final Runnable signedIn) {
        Container form = Ui.landing("Sign in", "Bastion", "Your bank, locked down.");
        form.add(new Label(Lang.tr("Sign in"), "BnLandingHeading"));
        final Label error = Ui.error();
        boolean passkeys = Session.passkeysSupported();
        boolean device = biometricSession();
        int best = strongest(passkeys, device);
        if (passkeys) {
            Button passkey = best == PASSKEY ? Ui.primary("Sign in with a passkey")
                    : Ui.secondary("Sign in with a passkey");
            passkey.addActionListener(evt -> {
                Ui.showError(error, null);
                Session.signInWithPasskey(signedIn,
                        (status, message) -> Ui.showError(error, Lang.tr(message)));
            });
            form.add(passkey);
        }
        if (device) {
            Button unlock = best == DEVICE ? Ui.primary("Unlock with fingerprint or face")
                    : Ui.secondary("Unlock with fingerprint or face");
            // The lock screen asks for the biometric, opens the app key and
            // with it the stored session and this device's signing key.
            unlock.addActionListener(evt -> Lock.lockAtStart());
            form.add(unlock);
        }
        if (best != PASSWORD) {
            form.add(Ui.muted(Lang.tr("Or sign in with your e-mail and password. An account "
                    + "with a passkey takes its password only with a recovery code.")));
        }
        final TextField email = Ui.field("E-mail", TextArea.EMAILADDR);
        final TextField password = Ui.field("Password", TextArea.PASSWORD);
        final Button go = best == PASSWORD ? Ui.primary("Sign in") : Ui.secondary("Sign in");
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
        Button join = Ui.secondary("Open an account");
        join.addActionListener(evt -> OnboardingForm.show(() -> show(signedIn), signedIn));
        form.add(join);
        Ui.show(form.getComponentForm(), false);
    }

    private static void codeStep(final Session.Pending pending, final Runnable signedIn) {
        Container form = Ui.landing("Verification", "Enter your code", null);
        form.add(Ui.muted(Lang.tr("Open your authenticator app and enter the six-digit code, "
                + "or one of your recovery codes.")));
        final TextField code = Ui.field("Code", TextArea.ANY);
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
        if (Session.passkeysSupported()) {
            Button passkey = Ui.secondary("Sign in with a passkey");
            passkey.addActionListener(evt -> Session.signInWithPasskey(signedIn,
                    (status, message) -> Ui.showError(error, Lang.tr(message))));
            form.add(passkey);
        }
        Ui.show(form.getComponentForm(), true);
    }
}
