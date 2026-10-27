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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.net.Session;

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
        page.add(Ui.text("The address of the Reel server this app talks to. "
                + "Changing it signs you out.", "SvText"));
        final TextField url = Ui.field(page, "Address", "url", "http://192.168.1.20:8080",
                TextArea.URL);
        url.setText(Session.serverUrl());
        page.add(Ui.primary("Save", "submit", e -> Nav.changeServer(url.getText())));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
