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
package com.codenameone.examples.parley.ui;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.parley.api.DeviceDto;
import com.codenameone.examples.parley.crypto.Identity;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;

import java.util.List;

/// The devices signed in as this account. Each is its own end of every
/// conversation -- with its own keys, that never leave it -- so unlinking one
/// is final: the server stops addressing copies to it, and the keys it had
/// open nothing that is sent from then on.
public final class DevicesForm {
    private DevicesForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Linked devices", "Devices");
        Ui.back(form, previous);
        final Container page = Ui.page();
        final Container list = Ui.list("devices");
        page.add(Ui.text("Messages are encrypted for each of these separately. Sign in on a "
                + "new phone or in a browser and it appears here; it sees what is sent from "
                + "then on, not what came before.", "PlMuted"));
        page.add(list);
        form.add(BorderLayout.CENTER, page);
        form.show();
        load(form, list);
    }

    private static void load(final Form form, final Container list) {
        Api.devices().list(Net.to((List<DeviceDto> devices) -> {
            list.removeAll();
            for (final DeviceDto device : devices) {
                boolean mine = device.id().equals(Identity.deviceId());
                String detail = (mine ? "This device" + Ui.DOT : "")
                        + "Last active " + Ui.when(device.lastSeenAt());
                char icon = "HTML5".equals(device.platform()) ? FontImage.MATERIAL_LANGUAGE
                        : "ios".equals(device.platform()) || "and".equals(device.platform())
                        ? FontImage.MATERIAL_SMARTPHONE : FontImage.MATERIAL_COMPUTER;
                list.add(Ui.row(icon, device.name(), detail, mine ? null
                        : Ui.link("Unlink", "unlink-" + device.id(), e ->
                                Api.devices().unlink(device.id(),
                                        Net.to(gone -> load(form, list), Ui::fail)))));
            }
            Ui.refresh(form);
        }, Ui.failed(list, () -> load(form, list))));
    }

}
