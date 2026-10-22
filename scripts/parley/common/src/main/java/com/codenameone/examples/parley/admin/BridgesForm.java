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
package com.codenameone.examples.parley.admin;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.parley.api.BridgeLinkDto;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;
import com.codenameone.examples.parley.ui.Ui;
import java.util.List;

/// The conversations linked to another network, every one of them relayed by
/// the server in the clear. Read-only: a link is made and removed by the
/// conversation's own admins, from its info screen.
final class BridgesForm {
    private final Panes panes;
    private final Container list = Ui.list("bridges");

    private BridgesForm(Panes panes) {
        this.panes = panes;
        Container top = new Container(BoxLayout.y());
        top.setUIID("PlPage");
        top.add(Ui.text("Messages in these chats are not end-to-end encrypted. Bridges are "
                + "off until the server is given a Telegram bot token or WhatsApp "
                + "Cloud API credentials.", "PlMuted"));
        panes.list("Bridges", "Bridges", Panes.listed(top, list), this::load);
    }

    static void show(Form previous) {
        new BridgesForm(Panes.phone(previous));
    }

    private void load() {
        Api.admin().bridges(Net.to(this::fill, Ui.failed(list, this::load)));
    }

    private void fill(List<BridgeLinkDto> links) {
        list.removeAll();
        for (int iter = 0; links != null && iter < links.size(); iter++) {
            BridgeLinkDto link = links.get(iter);
            Container row = Ui.row(FontImage.MATERIAL_LINK, link.label(),
                    ("WHATSAPP".equals(link.network()) ? "WhatsApp" : "Telegram") + Ui.DOT
                    + link.externalChat() + Ui.DOT + Ui.day(link.createdAt()), null);
            row.setName("bridge-" + link.id());
            list.add(row);
        }
        if (list.getComponentCount() == 0) {
            list.add(Ui.empty(FontImage.MATERIAL_LINK_OFF, "No linked chats", null));
        }
        panes.refresh();
    }
}
