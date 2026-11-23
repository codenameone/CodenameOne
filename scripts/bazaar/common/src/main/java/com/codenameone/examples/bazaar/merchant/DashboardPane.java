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
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.AnalyticsDto;
import com.codenameone.examples.bazaar.api.LevelDto;
import com.codenameone.examples.bazaar.api.SeriesPointDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Sections;
import com.codenameone.examples.bazaar.ui.Shell;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Today at a glance: sales, orders, average order value and low stock, each
/// with a fortnight's sparkline, and the latest orders.
///
/// The numbers and sparklines come from the server's analytics
/// (`AnalyticsApi`) for the last fortnight; staff who may not see analytics
/// get the recent orders only.
final class DashboardPane extends MerchantPane {
    static final int DAYS = 14;
    private static final long DAY = 24L * 60 * 60 * 1000;

    private List<OrderDto> orders;
    private AnalyticsDto stats;
    private int lowStock = -1;

    DashboardPane() {
        super("Dashboard", "merchant-dashboard");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "dashboard-refresh", this::load);
        final String store = MerchantState.storeId();
        Api.orders().list(store, null, null, null, null, null, null, null, Net.to(list -> {
            if (store.equals(MerchantState.storeId())) {
                orders = list;
                paint();
            }
        }, Ui::fail));
        Api.inventory().lowStock(store, Net.to(list -> {
            if (store.equals(MerchantState.storeId())) {
                lowStock = list.size();
                paint();
            }
        }, (status, message) -> { }));
        long now = System.currentTimeMillis();
        Period fortnight = Period.custom(now - (DAYS - 1) * DAY, now);
        // Managers only: anyone else simply sees no figures.
        Api.analytics().dashboard(store, fortnight.fromParam(), fortnight.toParam(),
                Net.to(found -> {
                    if (store.equals(MerchantState.storeId())) {
                        stats = found;
                        paint();
                    }
                }, (status, message) -> { }));
    }

    @Override
    void orderChanged(String id, String status) {
        load();
    }

    @Override
    void stockChanged(String variantId, String locationId) {
        Api.inventory().lowStock(MerchantState.storeId(), Net.to((List<LevelDto> list) -> {
            lowStock = list.size();
            paint();
        }, (status, message) -> { }));
    }

    private void paint() {
        if (orders == null) {
            return;
        }
        double[] sales = new double[DAYS];
        double[] counts = new double[DAYS];
        double[] aov = new double[DAYS];
        List<SeriesPointDto> daily = stats == null || stats.daily() == null
                ? new java.util.ArrayList<SeriesPointDto>() : stats.daily();
        int offset = DAYS - daily.size();
        for (int iter = 0; iter < daily.size(); iter++) {
            int at = iter + offset;
            if (at >= 0 && at < DAYS) {
                sales[at] = daily.get(iter).salesCents() / 100.0;
                counts[at] = daily.get(iter).orders();
                aov[at] = counts[at] == 0 ? 0 : sales[at] / counts[at];
            }
        }
        long todaySales = Math.round(sales[DAYS - 1] * 100);
        int todayOrders = (int) counts[DAYS - 1];
        long todayAov = todayOrders == 0 ? 0 : todaySales / todayOrders;

        Container page = MUi.page("dashboard");
        Container tiles = new Container(new GridLayout(Layouts.wide() ? 1 : 2,
                Layouts.wide() ? 4 : 2));
        tiles.setUIID("MeTiles");
        if (stats != null) {
            tiles.add(MUi.tile(MerchantState.money(todaySales), Lang.tr("Sales today"),
                    MCharts.sparkline(sales, "spark-sales"), "tile-sales"));
            tiles.add(MUi.tile(String.valueOf(todayOrders), Lang.tr("Orders today"),
                    MCharts.sparkline(counts, "spark-orders"), "tile-orders"));
            tiles.add(MUi.tile(MerchantState.money(todayAov), Lang.tr("Average order"),
                    MCharts.sparkline(aov, "spark-aov"), "tile-aov"));
        }
        Container low = MUi.tile(lowStock < 0 ? "-" : String.valueOf(lowStock),
                Lang.tr("Low stock"), null, "tile-low");
        tiles.add(Ui.tap(low, "dashboard-low", e -> go(Sections.INVENTORY)));
        page.add(tiles);

        Ui.section(page, "Recent orders");
        Container recent = new Container(BoxLayout.y());
        recent.setUIID("MeCard");
        int shown = 0;
        for (int iter = 0; iter < orders.size() && shown < 8; iter++) {
            final OrderDto o = orders.get(iter);
            shown++;
            Container row = new Container(new BorderLayout());
            row.setUIID("MeListRow");
            Container words = new Container(BoxLayout.y());
            words.add(Ui.plain(Lang.tr("Order #{0}", String.valueOf(o.number())), "SvRowTitle"));
            words.add(Ui.plain(nonEmpty(o.email(), o.username()) + Ui.DOT + MUi.when(o.createdAt()),
                    "SvRowDetail"));
            row.add(BorderLayout.CENTER, words);
            Container end = new Container(BoxLayout.y());
            end.add(Ui.plain(MerchantState.money(o.totalCents()), "SvRowValue"));
            end.add(MUi.badge(o.fulfilment()));
            row.add(BorderLayout.EAST, end);
            recent.add(Ui.tap(row, "recent-" + o.number(), e -> {
                OrdersPane.open = o.id();
                go(Sections.ORDERS);
            }));
        }
        if (shown == 0) {
            recent.add(Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, "No orders yet",
                    "Orders show up here the moment they are placed."));
        }
        page.add(recent);
        root(page);
    }

    private static String nonEmpty(String a, String b) {
        return a != null && a.length() > 0 ? a : b == null ? "" : b;
    }

    static void go(String id) {
        Shell shell = Shell.current();
        if (shell != null) {
            shell.show(id);
        }
    }
}
