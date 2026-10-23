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
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.api.AccountDto;
import com.codenameone.examples.reel.api.DeviceDto;
import com.codenameone.examples.reel.api.ProfileDto;
import com.codenameone.examples.reel.api.SessionDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.net.Session;
import java.util.List;

/// The account: its name, the devices signed in to it and what is playing on
/// them right now. Signing a device out here ends its sign-in on the server,
/// so a lost phone stops working within minutes.
public final class ProfileForm {
    private ProfileForm() {
    }

    /// @param saved where to go once the name is saved: a screen that shows
    ///     the name is built again there
    public static void show(final Form previous, final Runnable saved) {
        final Form form = Ui.form("Account", "Profile");
        Ui.back(form, previous);
        AccountDto user = Session.user();
        Container page = Ui.page();

        Ui.section(page, "About you");
        final TextField name = Ui.field(page, "Name", "name", "The name on the account");
        name.setText(user.name());
        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        final Button save = Ui.primary("Save", "submit", null);
        save.addActionListener(e -> {
            save.setEnabled(false);
            error.setText("");
            Api.account().updateProfile(new ProfileDto(name.getText().trim()), Net.to(updated -> {
                Session.setUser(updated);
                saved.run();
            }, (status, message) -> {
                save.setEnabled(true);
                error.setText(message);
                Ui.refresh(form);
            }));
        });
        page.add(error).add(save);

        Ui.section(page, "Watching now");
        final Container streams = SettingsForm.group();
        streams.setName("streams");
        page.add(streams);

        Ui.section(page, "Devices");
        final Container devices = SettingsForm.group();
        devices.setName("devices");
        page.add(devices);
        form.add(BorderLayout.CENTER, page);
        form.show();
        load(form, devices, streams);
    }

    private static void load(final Form form, final Container devices, final Container streams) {
        Api.account().sessions(Net.to(found -> {
            streams.removeAll();
            List<SessionDto> all = found;
            if (all == null || all.isEmpty()) {
                streams.add(Ui.text("Nothing is playing on this account.", "SvMuted"));
            }
            for (int iter = 0; all != null && iter < all.size(); iter++) {
                SessionDto s = all.get(iter);
                streams.add(Ui.row(FontImage.MATERIAL_PLAY_CIRCLE_OUTLINE, s.titleName(),
                        s.profileName() + Ui.DOT + s.deviceName(), null));
            }
            Ui.refresh(form);
        }, Ui::fail));
        Api.account().devices(Net.to(found -> {
            devices.removeAll();
            List<DeviceDto> all = found;
            for (int iter = 0; all != null && iter < all.size(); iter++) {
                final DeviceDto d = all.get(iter);
                String detail = (d.current() ? "This device" : Ui.when(d.lastSeenAt()))
                        + (d.lastCountry().length() > 0 ? Ui.DOT + d.lastCountry() : "");
                devices.add(Ui.row(FontImage.MATERIAL_DEVICES, d.name(), detail, d.current() ? null
                        : Ui.flat(FontImage.MATERIAL_LOGOUT, "signOutDevice-" + iter,
                                e -> Api.account().signOutDevice(d.id(),
                                        Net.to(gone -> load(form, devices, streams), Ui::fail)))));
            }
            Ui.refresh(form);
        }, Ui::fail));
    }
}
