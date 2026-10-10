/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.ui;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.wayline.net.Session;

/// Where the server is. A released app has one answer and no use for this
/// screen; while you are developing, it is how a phone is pointed at the server
/// running on your computer -- `http://` and the computer's address on the
/// network, since `localhost` on a phone is the phone.
public final class ServerForm {
    private ServerForm() {
    }

    public static void show(Form previous) {
        Form form = Ui.form("Server address", "Server");
        Ui.back(form, previous);
        Container page = Ui.page();
        page.add(Ui.text("The address of the Wayline server this app talks to. "
                + "Changing it signs you out.", "WlText"));
        final TextField url = Ui.field(page, "Address", "url", "http://192.168.1.20:8080",
                TextArea.URL);
        url.setText(Session.serverUrl());
        page.add(Ui.primary("Save", "submit", e -> Nav.changeServer(url.getText())));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
