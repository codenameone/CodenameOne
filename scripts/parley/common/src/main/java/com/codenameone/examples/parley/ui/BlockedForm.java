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
import com.codenameone.examples.parley.Telemetry;
import com.codenameone.examples.parley.api.ContactDto;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;

import java.util.List;

/// The people this account blocked. A blocked person's messages are dropped
/// by the server without telling them, their calls do not ring, and they no
/// longer see last seen or the profile photo.
public final class BlockedForm {
    private BlockedForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Blocked", "Blocked");
        Ui.back(form, previous);
        Container page = Ui.page();
        final Container list = Ui.list("blocked");
        page.add(list);
        form.add(BorderLayout.CENTER, page);
        form.show();
        load(form, list);
    }

    private static void load(final Form form, final Container list) {
        Api.contacts().blocked(Net.to((List<ContactDto> blocked) -> {
            list.removeAll();
            if (blocked.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_BLOCK, "Nobody is blocked",
                        "Block someone from their chat's info screen."));
            }
            for (final ContactDto each : blocked) {
                list.add(Ui.person(each.displayName(), each.username(),
                        Ui.link("Unblock", "unblock-" + each.username(), e ->
                                Api.contacts().unblock(each.username(), Net.to(gone -> {
                                    Telemetry.blocked(false);
                                    load(form, list);
                                }, Ui::fail)))));
            }
            Ui.refresh(form);
        }, Ui.failed(list, () -> load(form, list))));
    }
}
