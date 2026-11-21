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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.scheduling.DefaultScheduleModel;
import com.codename1.scheduling.ScheduleItem;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.scheduler.Scheduler;
import com.codename1.ui.table.DataGrid;
import com.codenameone.examples.bazaar.api.RenewalDto;
import com.codenameone.examples.bazaar.api.SubscriptionDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;
import java.util.TimeZone;

/// Subscriptions: every customer's subscription in a grid, and the coming
/// renewals on a month calendar, each an order the store will charge.
final class SubscriptionsPane extends MerchantPane {
    private boolean calendar;

    SubscriptionsPane() {
        super("Subscriptions", "merchant-subscriptions");
    }

    @Override
    void load() {
        clearTools();
        tool(calendar ? FontImage.MATERIAL_LIST : FontImage.MATERIAL_CALENDAR_MONTH,
                calendar ? "List" : "Renewals", "subscriptions-view", () -> {
                    calendar = !calendar;
                    load();
                });
        if (calendar) {
            long[] w = PromoDates.monthWindow(System.currentTimeMillis(), TimeZone.getDefault());
            Api.subscriptions().calendar(MerchantState.storeId(), String.valueOf(w[0]),
                    String.valueOf(w[1]), Net.to(this::renewals, Ui::fail));
        } else {
            Api.subscriptions().list(MerchantState.storeId(), Net.to(this::list, Ui::fail));
        }
    }

    private void list(List<SubscriptionDto> subs) {
        Container page = new Container(new BorderLayout());
        final Rows<SubscriptionDto> rows = new Rows<SubscriptionDto>()
                .col(Lang.tr("Customer"), s -> s.username())
                .col(Lang.tr("Quantity"), s -> String.valueOf(s.quantity()))
                .col(Lang.tr("Every"), s -> s.cadence() + " " + Lang.tr(Words.of(s.unit())))
                .col(Lang.tr("Status"), s -> Lang.tr(Words.of(s.status())))
                .col(Lang.tr("Next renewal"), s -> s.dueAt() > 0 ? MUi.when(s.dueAt()) : "")
                .col(Lang.tr("Failures"), s -> String.valueOf(s.failures()));
        rows.set(subs);
        final DataGrid grid = MUi.grid(rows, "subscriptions-grid");
        grid.setSelectionMode(DataGrid.SelectionMode.SINGLE_ROW);
        RowMenus.attach(grid, () -> {
            final SubscriptionDto s = rows.at(grid.getModelRow(grid.getSelectedRow()));
            if (s == null) {
                return null;
            }
            return new Command[] {
                Command.create(Lang.tr("Pause"), null, e ->
                        Api.subscriptions().pause(MerchantState.storeId(), s.id(), done())),
                Command.create(Lang.tr("Resume"), null, e ->
                        Api.subscriptions().resume(MerchantState.storeId(), s.id(), done())),
                Command.create(Lang.tr("Skip next"), null, e ->
                        Api.subscriptions().skip(MerchantState.storeId(), s.id(), done())),
                Command.create(Lang.tr("Cancel"), null, e ->
                        Api.subscriptions().cancel(MerchantState.storeId(), s.id(), done()))
            };
        });
        page.add(BorderLayout.CENTER, grid);
        root(page);
    }

    private com.codename1.util.OnComplete<com.codename1.io.rest.Response<SubscriptionDto>>
            done() {
        return Net.to(s -> {
            Ui.say("Updated");
            load();
        }, Ui::fail);
    }

    private void renewals(List<RenewalDto> due) {
        DefaultScheduleModel model = new DefaultScheduleModel();
        for (int iter = 0; iter < due.size(); iter++) {
            RenewalDto r = due.get(iter);
            model.addItem(new ScheduleItem(r.subscriptionId() + "@" + r.dueAt(),
                    r.username() + " " + MerchantState.money(r.amountCents()), r.dueAt(),
                    r.dueAt() + PromoDates.CAMPAIGN_LENGTH).setMovable(false)
                    .setResizable(false).setUserObject(r));
        }
        Scheduler s = new Scheduler(model);
        s.setName("subscriptions-calendar");
        s.setView(Scheduler.View.MONTH);
        s.setEditable(false);
        root(s);
    }
}
