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

import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codenameone.examples.bastion.api.DeviceDto;
import com.codenameone.examples.bastion.api.UserDto;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.net.Session;
import com.codenameone.examples.bastion.security.DeviceKey;

/// Registering this phone's signing key, from wherever it is asked for:
/// onboarding, the dashboard's banner, and a payment the server refused for
/// want of a signature.
///
/// The server asks for two things before it trusts a new key: a sign-in a
/// few minutes old, and -- for an account without an authenticator app --
/// the password again, since a stolen session alone must not be able to add
/// the device that then signs payments. A sign-in made with a passkey is
/// asked for neither: its token says so (`amr`), and the server accepts it.
public final class DeviceSetup {
    private DeviceSetup() {
    }

    /// Whether this phone can sign payments for the signed-in account (a
    /// key registered for another account does not count).
    public static boolean registered() {
        return DeviceKey.deviceId() != null && DeviceKey.ownedBy(Session.username());
    }

    /// Registers the device, asking for the password first when the account
    /// needs it. `failed` is not called when the user cancels.
    public static void register(final Runnable done, final Net.Failed failed) {
        UserDto user = Session.user();
        if (user == null) {
            failed.failed(401, "Sign in first");
            return;
        }
        String password = null;
        // A passkey sign-in already proved a key bound to this site; the
        // server does not ask it for a password (see Devices on the server).
        if (!user.totpEnabled() && !Session.signedInWith("hwk")) {
            TextField field = new TextField("", Lang.tr("Password"), 20, TextArea.PASSWORD);
            field.setUIID("BnField");
            Command ok = new Command(Lang.tr("Continue"));
            if (Dialog.show(Lang.tr("Confirm it is you"), field, ok,
                    new Command(Lang.tr("Cancel"))) != ok) {
                return;
            }
            password = field.getText();
        }
        DeviceKey.register(user.username(), password, (DeviceDto device) -> done.run(), failed);
    }

    /// A refusal that a registered device would have avoided, as the server
    /// words it: no signature, or one from a device it does not know.
    public static boolean needsDevice(int status, String message) {
        String code = Net.refusalCode();
        return status == 403 && ("needs-device-signature".equals(code)
                || "device-not-attested".equals(code));
    }

    /// After such a refusal: offers to register this device, and runs
    /// `retry` once it is.
    public static void offer(final Runnable retry, final Net.Failed failed) {
        if (registered()) {
            // A registered device that is still refused is untrusted (rooted,
            // or not attested); registering again would not change that.
            return;
        }
        if (Dialog.show(Lang.tr("Sign with this device"),
                Lang.tr("Payments leaving your accounts are signed by a key that never leaves "
                        + "this phone. Register this device to continue."),
                Lang.tr("Register"), Lang.tr("Not now"))) {
            register(retry, failed);
        }
    }

    /// One try of a signed action. It signs afresh each time it runs -- a
    /// signature's nonce is good once -- and reports a refusal to `failed`.
    public interface Attempt {
        void send(Net.Failed failed);
    }

    /// Runs `attempt`; when the server refuses it for want of this device's
    /// signature, offers to register the device and tries once more. Any
    /// other refusal, or the second one, goes to `shown`.
    public static void withDevice(final Attempt attempt, final Net.Failed shown) {
        attempt.send((status, message) -> {
            if (needsDevice(status, message) && !registered()) {
                offer(() -> attempt.send(shown), shown);
            } else {
                shown.failed(status, message);
            }
        });
    }
}
