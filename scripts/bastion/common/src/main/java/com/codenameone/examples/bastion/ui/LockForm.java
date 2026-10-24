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
import com.codenameone.examples.bastion.security.Keys;
import com.codenameone.examples.bastion.security.Lock;

/// What is on screen while the app is locked: nothing of the account.
///
/// The ways back in, strongest first: a biometric (when the user turned it
/// on and the OS still holds that copy of the key), then the app PIN, then
/// signing in again -- which gives up the keys on this device.
public final class LockForm {
    private LockForm() {
    }

    /// `signInAgain` is the way out when there is no biometric or PIN to ask
    /// for, or the user would rather.
    public static void show(final Runnable signInAgain) {
        Container form = Ui.landing("Locked", "Bastion is locked", null);
        final Label error = Ui.error();
        form.add(error);
        if (biometric()) {
            Button unlock = Ui.primary("Unlock");
            unlock.addActionListener(evt -> ask(signInAgain, error));
            form.add(unlock);
        }
        if (Keys.pinEnabled()) {
            final TextField pin = Ui.field("PIN", TextArea.NUMERIC | TextArea.PASSWORD);
            pin.setMaxSize(Keys.MAX_PIN);
            Button withPin = biometric() ? Ui.secondary("Unlock with PIN")
                    : Ui.primary("Unlock with PIN");
            withPin.addActionListener(evt -> tryPin(pin, withPin, error, signInAgain));
            pin.setDoneListener(evt -> tryPin(pin, withPin, error, signInAgain));
            form.addAll(pin, withPin);
        }
        if (!Keys.protectedByUser()) {
            form.add(Ui.muted(Lang.tr("No biometric or PIN is set up on this device, so the "
                    + "way back in is your password.")));
        }
        Button again = Keys.protectedByUser() ? Ui.secondary("Sign in again")
                : Ui.primary("Sign in again");
        again.addActionListener(evt -> {
            if (!Keys.protectedByUser() || Ui.confirm("Sign in again",
                    Lang.tr("Signing in again sets this device up from the start: its "
                            + "payment key, biometric and PIN are removed."),
                    "Sign in again")) {
                signInAgain.run();
            }
        });
        form.add(again);
        // Locked screens are sensitive too: the last frame before the lock
        // must not survive in the recent-apps list.
        Ui.showLockScreen(form.getComponentForm());
        if (biometric()) {
            ask(signInAgain, error);
        }
    }

    /// Whether to offer the biometric: the device has one and the user
    /// stored the biometric copy of the key.
    private static boolean biometric() {
        return Lock.biometricsAvailable() && Keys.biometricEnabled();
    }

    private static void ask(final Runnable signInAgain, final Label error) {
        Lock.unlockWithBiometric(gone -> {
            if (gone != null && gone.booleanValue()) {
                // The OS revoked that copy (a biometric was enrolled since):
                // show the screen again without the biometric.
                show(signInAgain);
                return;
            }
            Ui.showError(error, Keys.pinEnabled()
                    ? Lang.tr("Not recognised. Try again, or use your PIN.")
                    : Lang.tr("Not recognised. Try again."));
        });
    }

    /// One PIN attempt at a time: the field and the button stay disabled
    /// until the vault answers. Two attempts in flight would read the same
    /// failure count, and each record one -- so a guesser typing fast got
    /// more than [Keys#PIN_ATTEMPTS] tries before the wipe.
    static void tryPin(final TextField pin, final Button withPin, final Label error,
            final Runnable signInAgain) {
        if (!pin.isEnabled()) {
            return;
        }
        String typed = pin.getText();
        pin.setText("");
        if (typed.length() < Keys.MIN_PIN) {
            Ui.showError(error, Lang.tr("Enter your PIN of at least {0} digits.",
                    String.valueOf(Keys.MIN_PIN)));
            return;
        }
        pin.setEnabled(false);
        withPin.setEnabled(false);
        final Runnable again = () -> {
            pin.setEnabled(true);
            withPin.setEnabled(true);
        };
        Lock.unlockWithPin(typed, new Keys.PinResult() {
            @Override
            public void unlocked() {
            }

            @Override
            public void wrong(int left) {
                again.run();
                Ui.showError(error, left == 1
                        ? Lang.tr("Wrong PIN. One try left before the keys on this device are erased.")
                        : Lang.tr("Wrong PIN. {0} tries left.", String.valueOf(left)));
            }

            @Override
            public void wiped() {
                Ui.toast("Too many wrong PINs. The keys on this device were erased.");
                signInAgain.run();
            }

            @Override
            public void failed(String why) {
                again.run();
                Ui.showError(error, Lang.tr(why));
            }
        });
    }
}
