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
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.bastion.Prefs;
import com.codenameone.examples.bastion.api.PasskeyDto;
import com.codenameone.examples.bastion.api.UserDto;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.net.Session;
import com.codenameone.examples.bastion.security.Lock;

import java.util.List;

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
        final Container passkeys = new Container(BoxLayout.y());
        form.add(passkeys);
        loadPasskeys(passkeys);
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

    // ------------------------------------------------------------- passkeys

    private static void loadPasskeys(final Container box) {
        Api.account().passkeys(Net.to((List<PasskeyDto> list) -> fillPasskeys(box, list),
                (status, message) -> {
                    box.removeAll();
                    box.add(Ui.muted(Lang.tr(message)));
                    box.revalidate();
                }));
    }

    private static void fillPasskeys(final Container box, List<PasskeyDto> list) {
        box.removeAll();
        for (final PasskeyDto passkey : list) {
            String label = passkey.label() == null || passkey.label().length() == 0
                    ? Lang.tr("Passkey") : passkey.label();
            box.add(Ui.row(label, Lang.tr("Last used {0}", Ui.when(passkey.lastUsed())),
                    Lang.tr("Remove"), "BnBadgeWarn", () -> removePasskey(box, passkey, label)));
        }
        if (Session.passkeysSupported()) {
            Button add = Ui.secondary("Add a passkey");
            add.addActionListener(evt -> addPasskey(box));
            box.add(add);
        } else if (list.isEmpty()) {
            box.add(Ui.muted(Lang.tr("This device cannot create passkeys.")));
        }
        box.revalidate();
    }

    private static void removePasskey(final Container box, PasskeyDto passkey, String label) {
        if (!Dialog.show(Lang.tr("Remove passkey"),
                Lang.tr("Remove {0}? It will no longer sign in to this account.", label),
                Lang.tr("Remove"), Lang.tr("Cancel"))) {
            return;
        }
        Api.account().removePasskey(passkey.id(), Net.to(
                (List<PasskeyDto> left) -> fillPasskeys(box, left),
                (status, message) -> Ui.toast(Lang.tr(message))));
    }

    /// A passkey is registered against a fresh signed-in server session, not
    /// the API token (see `Session.addPasskey`), so this signs in once more --
    /// password, and the app's code when the account has one -- keeping that
    /// session for the one ceremony.
    private static void addPasskey(final Container box) {
        final UserDto user = Session.user();
        if (user == null) {
            return;
        }
        TextField password = new TextField("", Lang.tr("Password"), 20, TextArea.PASSWORD);
        password.setUIID("BnField");
        Command ok = new Command(Lang.tr("Continue"));
        if (Dialog.show(Lang.tr("Confirm it is you"), password, ok,
                new Command(Lang.tr("Cancel"))) != ok) {
            return;
        }
        final Net.Failed failed = (status, message) -> {
            Session.endPasskeySession();
            Ui.toast(Lang.tr(message));
        };
        final Runnable ceremony = () -> Session.addPasskey(
                Lang.tr("Bastion on {0}", CN.getPlatformName()), () -> loadPasskeys(box), failed);
        Session.keepSessionForPasskey();
        Session.signIn(user.username(), password.getText(), pending -> {
            TextField code = new TextField("", Lang.tr("Code from your app"), 10, TextArea.NUMERIC);
            code.setUIID("BnField");
            Command go = new Command(Lang.tr("Continue"));
            if (Dialog.show(Lang.tr("Authenticator code"), code, go,
                    new Command(Lang.tr("Cancel"))) != go) {
                Session.endPasskeySession();
                return;
            }
            Session.submitCode(pending, code.getText(), ceremony, failed);
        }, ceremony, failed);
    }
}
