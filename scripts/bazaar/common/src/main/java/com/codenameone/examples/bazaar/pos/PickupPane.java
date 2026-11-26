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
package com.codenameone.examples.bazaar.pos;

import com.codename1.appkit.Net;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.PosPickupDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// Online orders to collect at this register's location: make one ready
/// (the shopper is mailed) and hand it over when they come.
final class PickupPane {
    private PickupPane() {
    }

    static com.codename1.ui.Component create() {
        final Container root = new Container(new BorderLayout());
        root.setName("pos-pickups");
        root.setUIID("PosRoot");
        final Container page = new Container(BoxLayout.y());
        page.setScrollableY(true);
        page.setUIID("PosPage");
        root.add(BorderLayout.NORTH, PosUi.header("Pickups", PosUi.icon(FontImage.MATERIAL_REFRESH,
                "Refresh", "pos-pickups-refresh", "PosChoice", e -> list(page))));
        root.add(BorderLayout.CENTER, page);
        list(page);
        return root;
    }

    private static void list(final Container page) {
        page.removeAll();
        page.add(PosUi.label("Looking...", "PosMuted"));
        page.revalidateLater();
        Api.posCounter().pickups(PosState.storeId(), PosState.registerId(), Net.to(list -> {
            page.removeAll();
            if (list == null || list.isEmpty()) {
                page.add(PosUi.label("Nothing waiting to be collected here", "PosMuted"));
            }
            for (int iter = 0; list != null && iter < list.size(); iter++) {
                page.add(row(page, list.get(iter), iter));
            }
            page.revalidateLater();
        }, (status, message) -> {
            page.removeAll();
            page.add(PosUi.label("The pickup queue needs the network", "PosMuted"));
            page.revalidateLater();
        }));
    }

    private static Container row(final Container page, final PosPickupDto p, int index) {
        Container row = new Container(new BorderLayout());
        row.setUIID("PosCartRow");
        row.add(BorderLayout.CENTER, PosUi.data(Lang.tr("Order #{0}", p.orderNumber()) + Ui.DOT
                + p.customer(), "PosCartTitle"));
        row.add(BorderLayout.EAST, PosUi.data(PosState.money(p.totalCents()), "PosCartDetail"));
        Container south = new Container(BoxLayout.y());
        south.add(PosUi.data(p.items(), "PosMuted"));
        if ("PACKED".equals(p.status())) {
            south.add(PosUi.key(Lang.tr("Picked up"), "pos-pickup-done-" + index, "PosPay",
                    e -> Api.posCounter().pickedUp(PosState.storeId(), p.orderId(),
                        Net.to(done -> {
                            Ui.say("Handed over");
                            list(page);
                        }, Ui::fail))));
        } else {
            south.add(PosUi.key(Lang.tr("Ready for pickup"), "pos-pickup-ready-" + index,
                    "PosChoice", e -> Api.posCounter().pickupReady(PosState.storeId(),
                        p.orderId(), Net.to(done -> {
                            Ui.say("The customer was told the order is ready");
                            list(page);
                        }, Ui::fail))));
        }
        row.add(BorderLayout.SOUTH, south);
        return row;
    }
}
