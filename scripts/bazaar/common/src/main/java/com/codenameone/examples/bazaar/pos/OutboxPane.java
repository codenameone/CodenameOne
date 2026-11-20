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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Every sale this register took and where it is: waiting, uploaded (OK),
/// already on the server (DUPLICATE), or refused (ERROR, with the reason and
/// a retry). Sales the server sold past the stock it had are flagged.
final class OutboxPane {
    private OutboxPane() {
    }

    static Component create() {
        final Container root = new Container(new BorderLayout());
        root.setName("pos-outbox");
        root.setUIID("PosRoot");
        root.add(BorderLayout.NORTH, PosUi.header("Outbox", Ui.link("Sync now", "pos-sync-now",
                e -> {
                    if (PosState.hasRegister()) {
                        PosState.outbox().wake();
                    }
                    PosSync.kick();
                })));
        final Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        list.setUIID("PosPage");
        root.add(BorderLayout.CENTER, list);
        Runnable fill = () -> {
            list.removeAll();
            if (!PosState.hasRegister()) {
                list.add(Ui.empty(FontImage.MATERIAL_POINT_OF_SALE, "No register yet",
                        "Set up the register first."));
                list.revalidateLater();
                return;
            }
            List<Outbox.Entry> entries = PosState.outbox().entries();
            for (int iter = entries.size() - 1; iter >= 0; iter--) {
                list.add(row(entries.get(iter)));
            }
            if (entries.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_CLOUD_DONE, "Nothing yet",
                        "Sales show here until the server has them."));
            }
            list.revalidateLater();
        };
        fill.run();
        PosUi.live(list, fill, false);
        return root;
    }

    private static Component row(final Outbox.Entry e) {
        Container row = new Container(new BorderLayout());
        row.setUIID("PosOutboxRow");
        row.setName("pos-outbox-" + e.clientId());
        Container text = new Container(BoxLayout.y());
        long total = e.totalCents;
        String title = e.number > 0 ? Lang.tr("Order #{0}", e.number)
                : Lang.tr("Sale {0}", e.clientId().substring(0, Math.min(8, e.clientId().length())));
        text.add(PosUi.data(title, "PosCartTitle"));
        String detail = Ui.when(e.sale.createdAt());
        if (total > 0) {
            detail += Ui.DOT + PosState.money(total);
        }
        if (e.message != null && e.message.length() > 0) {
            detail += Ui.DOT + e.message;
        }
        text.add(PosUi.data(detail, "PosCartDetail"));
        if (e.oversold) {
            text.add(PosUi.label("Sold past the stock on hand", "PosError"));
        }
        row.add(BorderLayout.CENTER, text);
        Container east = new Container(BoxLayout.x());
        east.add(Ui.badge(e.state, "PosBadge" + e.state));
        if (Outbox.ERROR.equals(e.state)) {
            east.add(Ui.flat(FontImage.MATERIAL_REFRESH, "pos-outbox-retry-" + e.clientId(), ev -> {
                PosState.outbox().retry(e.clientId());
                PosState.fireStatus();
                PosSync.kick();
            }));
        }
        row.add(BorderLayout.EAST, east);
        return row;
    }
}
