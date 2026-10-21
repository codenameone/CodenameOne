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

import com.codename1.components.Switch;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.bastion.Prefs;
import com.codenameone.examples.bastion.api.UserDto;
import com.codenameone.examples.bastion.net.Session;
import com.codenameone.examples.bastion.security.Lock;

/// The account's protections the user can change from the app: unlocking
/// with a biometric, the authenticator app, passkeys and this device's key.
public final class SettingsForm {
    private SettingsForm() {
    }

    public static void show(final Runnable back) {
        Form form = Nav.form("Settings", back);
        UserDto user = Session.user();
        form.add(Ui.section("Unlock"));
        if (Lock.biometricsAvailable()) {
            final Switch bio = new Switch();
            bio.setValue(Prefs.biometricUnlock());
            bio.addActionListener(evt -> Prefs.setBiometricUnlock(bio.isValue()));
            Container row = new Container(new BorderLayout(), "BnRow");
            row.add(BorderLayout.CENTER, new Label(Lang.tr("Unlock with fingerprint or face"),
                    "BnRowTitle"));
            row.add(BorderLayout.EAST, bio);
            form.add(row);
        } else {
            form.add(Ui.row(Lang.tr("Unlock with fingerprint or face"),
                    Lang.tr("This device has no biometric enrolled. The lock asks for your "
                            + "password."), Lang.tr("Off"), "BnBadgeWarn", null));
        }
        form.add(Ui.section("Sign-in"));
        boolean totp = user != null && user.totpEnabled();
        form.add(Ui.row(Lang.tr("Authenticator app"), totp
                ? Lang.tr("{0} recovery codes left", String.valueOf(user.recoveryCodesLeft()))
                : Lang.tr("Adds a six-digit code to every password sign-in."),
                Lang.tr(totp ? "On" : "Off"), totp ? "BnBadgeGood" : "BnBadgeWarn",
                totp ? () -> OnboardingForm.newRecoveryCodes(() -> show(back))
                        : () -> OnboardingForm.totp(() -> show(back))));
        form.add(Ui.section("This device"));
        boolean device = DeviceSetup.registered();
        form.add(Ui.row(Lang.tr("Payment signing key"), Lang.tr(device
                ? "Payments from this phone are signed by a key that never leaves it."
                : "Register this device to sign payments."),
                Lang.tr(device ? "On" : "Off"), device ? "BnBadgeGood" : "BnBadgeWarn",
                device ? null : () -> DeviceSetup.register(() -> show(back),
                        (status, message) -> Ui.toast(message))));
        Button center = Ui.secondary("Security Center");
        center.addActionListener(evt -> SecurityCenterForm.show(() -> show(back)));
        form.add(center);
        Ui.show(form, false);
    }
}
