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
import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codenameone.examples.bastion.api.UserDto;
import com.codenameone.examples.bastion.net.Session;
import com.codenameone.examples.bastion.security.DeviceKey;

/// The signed-in home screen. The accounts, transfers, cards and exchange
/// screens arrive in the next phase; until then this is the way to the
/// Security Center, to registering this device's signing key, and out.
public final class HomeForm {
    private HomeForm() {
    }

    public static void show(final Runnable signedOut) {
        UserDto user = Session.user();
        Form form = Ui.form("Bastion");
        form.add(Ui.title(user == null || user.displayName() == null ? "Welcome"
                : user.displayName()));
        form.add(Ui.muted(Lang.tr("Accounts, transfers, cards and the exchange are on their way.")));
        final Label status = new Label("", "BnMuted");
        refresh(status);
        form.add(status);
        Button register = Ui.secondary("Register this device");
        register.addActionListener(evt -> register(status, user));
        Button center = Ui.primary("Security Center");
        center.addActionListener(evt -> SecurityCenterForm.show(() -> show(signedOut)));
        Button out = Ui.secondary("Sign out");
        out.addActionListener(evt -> Session.signOut(signedOut));
        form.addAll(center, register, out);
        Ui.show(form, true);
    }

    private static void refresh(Label status) {
        status.setText(DeviceKey.deviceId() == null
                ? Lang.tr("This device cannot sign payments yet.")
                : Lang.tr("This device signs your payments."));
    }

    private static void register(final Label status, UserDto user) {
        final String owner = user == null ? null : user.username();
        if (owner == null) {
            return;
        }
        String password = null;
        if (!user.totpEnabled()) {
            // Without an authenticator app the server wants the password as
            // the second proof that the owner is adding this device.
            TextField field = new TextField("", Lang.tr("Password"), 20, TextArea.PASSWORD);
            Command ok = new Command(Lang.tr("Continue"));
            if (Dialog.show(Lang.tr("Confirm it is you"), field, ok,
                    new Command(Lang.tr("Cancel"))) != ok) {
                return;
            }
            password = field.getText();
        }
        DeviceKey.register(owner, password, device -> refresh(status),
                (code, message) -> {
                    status.setText(Lang.tr(message));
                    status.getComponentForm().revalidate();
                });
    }
}
