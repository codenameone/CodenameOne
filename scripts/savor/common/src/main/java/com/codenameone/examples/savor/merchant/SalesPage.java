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
package com.codenameone.examples.savor.merchant;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.savor.api.OrderDto;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.StatRow;
import com.codenameone.examples.savor.ui.Ui;

/// How the restaurant is doing: today and this week in numbers, and the
/// orders it has finished.
final class SalesPage {
    private SalesPage() {
    }

    static Container page() {
        final Container page = Ui.page();
        page.setName("sales");
        final Container numbers = new Container(BoxLayout.y());
        page.add(numbers);
        final Container past = new Container(BoxLayout.y());
        page.add(past);
        Api.merchant().summary(Net.to(summary -> {
            StatRow stats = Ui.stats(2);
            stats.stat(String.valueOf(summary.ordersToday()), Lang.tr("Orders today"), "orders-today");
            stats.stat(Ui.money(summary.salesTodayCents(), summary.currency()), Lang.tr("Sales today"),
                    "sales-today");
            stats.stat(Ui.money(summary.salesWeekCents(), summary.currency()),
                    Lang.tr("Last 7 days"), "sales-week");
            stats.stat(String.valueOf(summary.waiting()), Lang.tr("Waiting now"), "waiting");
            numbers.add(stats);
            Ui.relayout(page);
        }, Ui::fail));
        Api.merchant().history(Net.to(found -> {
            Ui.section(past, Lang.tr("Finished orders"));
            if (found.isEmpty()) {
                past.add(Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, Lang.tr("Nothing finished yet"),
                        null));
            }
            for (OrderDto order : found) {
                past.add(Ui.row((char) 0, order.customerName() + Ui.DOT
                        + Ui.money(order.subtotalCents(), order.currency()),
                        Ui.when(order.placedAt()) + Ui.DOT + Orders.summary(order),
                        Ui.badge(Orders.state(order.state()), Orders.badge(order.state()))));
            }
            Ui.relayout(page);
        }, Ui::fail));
        return page;
    }
}
