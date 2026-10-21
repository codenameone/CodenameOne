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
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codenameone.examples.bastion.Prefs;
import com.codenameone.examples.bastion.security.Lock;

/// What is on screen while the app is locked: nothing of the account.
public final class LockForm {
    private LockForm() {
    }

    /// `signInAgain` is the way out when there is no biometric to ask for.
    public static void show(final Runnable signInAgain) {
        Form form = Ui.form("Locked");
        form.add(Ui.title("Bastion is locked"));
        final Label error = Ui.error();
        Button unlock = Ui.primary(biometric() ? "Unlock" : "Sign in again");
        unlock.addActionListener(evt -> {
            if (biometric()) {
                ask(signInAgain, error);
            } else {
                signInAgain.run();
            }
        });
        form.addAll(error, unlock);
        // Locked screens are sensitive too: the last frame before the lock
        // must not survive in the recent-apps list.
        Ui.show(form, true);
        if (biometric()) {
            ask(signInAgain, error);
        }
    }

    /// Whether to ask for a biometric: the device has one and the user chose
    /// it during onboarding or in Settings. Otherwise the way back in is the
    /// password, by signing in again.
    private static boolean biometric() {
        return Lock.biometricsAvailable() && Prefs.biometricUnlock();
    }

    private static void ask(Runnable signInAgain, final Label error) {
        if (!biometric()) {
            return;
        }
        Lock.unlock(signInAgain, () -> Ui.showError(error, Lang.tr("Not recognised. Try again.")));
    }
}
