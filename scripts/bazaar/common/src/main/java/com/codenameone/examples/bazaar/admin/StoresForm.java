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
package com.codenameone.examples.bazaar.admin;

import com.codename1.appkit.Net;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.PlatformStatsDto;
import com.codenameone.examples.bazaar.api.StoreStatsDto;
import com.codenameone.examples.bazaar.api.StoreStatusDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Every store on the platform with its orders and gross sales, biggest
/// first, and the switch that suspends one or lets it trade again.
public final class StoresForm {
    static final String ACTIVE = "ACTIVE";
    static final String SUSPENDED = "SUSPENDED";

    private StoresForm() {
    }

    static void show(final Panes panes) {
        final Container list = Ui.list("stores");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.platform().stats(Net.to(found -> fill(panes, list, found, load[0]),
                Ui.failed(list, () -> load[0].run())));
        panes.list("Stores", "Stores", Panes.listed(null, list), load[0]);
    }

    private static void fill(final Panes panes, Container list, PlatformStatsDto stats,
            final Runnable reload) {
        list.removeAll();
        List<StoreStatsDto> stores = stats.perStore();
        if (stores == null || stores.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_STORE, "No stores yet",
                    "A store shows up here once a merchant opens one."));
        }
        for (int iter = 0; stores != null && iter < stores.size(); iter++) {
            final StoreStatsDto s = stores.get(iter);
            Container value = new Container(BoxLayout.y());
            value.add(Ui.plain(Ui.money(s.gmvCents(), s.currency()), "SvRowValue"));
            value.add(Ui.badge(Lang.tr(SUSPENDED.equals(s.status()) ? "Suspended" : "Active"),
                    SUSPENDED.equals(s.status()) ? "SvBadgeBad" : "SvBadgeGood"));
            final Container picked = Ui.row(FontImage.MATERIAL_STORE, s.name(),
                    "@" + s.handle() + Ui.DOT + Lang.tr("{0} orders", String.valueOf(s.orders())),
                    value);
            list.add(Ui.tap(picked, "store-" + iter, e -> {
                panes.picked(picked);
                open(panes, s, reload);
            }));
        }
        panes.refresh();
    }

    private static void open(final Panes panes, final StoreStatsDto s, final Runnable reload) {
        Container page = Ui.page();
        page.setName("store-detail");
        page.add(Ui.person(s.name(), "@" + s.handle(), null));
        page.add(Ui.stats(2)
                .stat(Ui.money(s.gmvCents(), s.currency()), "Gross sales", "storeGmv")
                .stat(String.valueOf(s.orders()), "Orders", "storeOrders"));
        final boolean suspended = SUSPENDED.equals(s.status());
        // Who runs it and whether it sells, as rows in a card of the same
        // inset as the stat cards above, not a loose line under them.
        Container facts = new Container(BoxLayout.y());
        facts.setUIID("SvStoreFacts");
        facts.add(Ui.row(FontImage.MATERIAL_PERSON, "Owner",
                s.ownerUsername() == null ? "-" : s.ownerUsername(), null));
        facts.add(Ui.row(suspended ? FontImage.MATERIAL_BLOCK : FontImage.MATERIAL_CHECK_CIRCLE,
                "Status", Lang.tr(suspended ? "Suspended" : "Active"), null));
        page.add(facts);
        page.add(suspended
                ? Ui.primary(Lang.tr("Activate store"), "store-activate",
                        e -> setStatus(s, ACTIVE, null, reload))
                : Ui.danger(Lang.tr("Suspend store"), "store-suspend", e -> suspend(s, reload)));
        panes.detail(s.name(), s.name(), page);
    }

    private static void suspend(StoreStatsDto s, Runnable reload) {
        Container body = new Container(BoxLayout.y());
        body.add(Ui.text(Lang.tr("Shoppers can no longer find or buy from this store."),
                "SvMuted"));
        TextField reason = Ui.field(body, "Reason", "suspend-reason", "");
        Command ok = new com.codename1.ui.Command(Lang.tr("Suspend"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Suspend store"), body, cancel, ok) == ok) {
            setStatus(s, SUSPENDED, reason.getText().trim(), reload);
        }
    }

    private static void setStatus(StoreStatsDto s, String status, String reason,
            final Runnable reload) {
        Api.platform().setStatus(s.storeId(), new StoreStatusDto(status, reason),
                Net.to(saved -> {
                    Ui.say(SUSPENDED.equals(status) ? "Store suspended" : "Store activated");
                    reload.run();
                }, Ui::fail));
    }
}
