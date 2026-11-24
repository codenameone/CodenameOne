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
import com.codename1.ui.Label;
import com.codename1.ui.layouts.FlowLayout;
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
        announce(id, status);
        load();
    }

    /// The orders already announced or seen, so a later change to one is not
    /// a "new order" again.
    private static final java.util.Set<String> SEEN = new java.util.HashSet<String>();
    /// The milestones celebrated this session, by store, day and kind.
    private static final java.util.Set<String> CELEBRATED = new java.util.HashSet<String>();
    /// Today's order count at the last paint, by store; -1 before the first.
    private static final java.util.Map<String, Integer> LAST_TODAY =
            new java.util.HashMap<String, Integer>();
    private static final java.util.Map<String, Integer> LAST_COUNT =
            new java.util.HashMap<String, Integer>();

    /// A paid order the store has not heard of, placed in the last two
    /// minutes, slides in as a toast with a pulse; it makes no sound.
    private void announce(final String id, String status) {
        if (id == null || SEEN.contains(id) || status == null || !status.startsWith("OPEN/PAID")) {
            return;
        }
        SEEN.add(id);
        Api.orders().detail(MerchantState.storeId(), id, Net.to(o -> {
            if (System.currentTimeMillis() - o.createdAt() < 120000L) {
                Delight.toast(getComponentForm(), Lang.tr("New order #{0}",
                        String.valueOf(o.number())), MerchantState.money(o.totalCents()) + Ui.DOT
                        + nonEmpty(o.email(), o.username()), "toast-new-order");
            }
        }, (code, message) -> { }));
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

        Container page = MUi.widePage("dashboard");
        Container tiles = DashGrid.of(40f, 4);
        tiles.setUIID("MeTiles");
        Delight.Format cash = MerchantState::money;
        if (stats != null) {
            tiles.add(MUi.tile(Delight.counter(todaySales, cash, "MeTileValue"),
                    Lang.tr("Sales today"), MCharts.sparkline(sales, "spark-sales"), "tile-sales"));
            tiles.add(MUi.tile(Delight.counter(todayOrders, String::valueOf, "MeTileValue"),
                    Lang.tr("Orders today"), MCharts.sparkline(counts, "spark-orders"),
                    "tile-orders"));
            tiles.add(MUi.tile(Delight.counter(todayAov, cash, "MeTileValue"),
                    Lang.tr("Average order"), MCharts.sparkline(aov, "spark-aov"), "tile-aov"));
        }
        Container low = MUi.tile(lowStock < 0 ? "-" : String.valueOf(lowStock),
                Lang.tr("Low stock"), null, "tile-low");
        tiles.add(Ui.tap(low, "dashboard-low", e -> go(Sections.INVENTORY)));
        page.add(tiles);

        long[] cents = new long[DAYS];
        for (int iter = 0; iter < DAYS; iter++) {
            cents[iter] = Math.round(sales[iter] * 100);
        }
        if (stats != null) {
            Container row = DashGrid.of(105f, 3);
            row.add(goals(cents));
            Container trend = new Container(new BorderLayout(), "MeCard");
            trend.add(BorderLayout.NORTH, Ui.plain(Lang.tr("Sales, last 14 days"), "MeCardTitle"));
            String[] days = new String[daily.size()];
            double[] values = new double[daily.size()];
            for (int iter = 0; iter < daily.size(); iter++) {
                days[iter] = daily.get(iter).label();
                values[iter] = daily.get(iter).salesCents();
            }
            trend.add(BorderLayout.CENTER, MCharts.bars("chart-dash-sales", days, values,
                    MCharts.MONEY));
            row.add(trend);
            page.add(row);
            celebrate(cents, todayOrders);
        }

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

    /// Today's and this week's goal as rings that fill, and the streak.
    private Container goals(long[] cents) {
        Container card = new Container(new BorderLayout(), "MeCard");
        card.setName("dashboard-goals");
        card.add(BorderLayout.NORTH, Ui.plain(Lang.tr("Goals"), "MeCardTitle"));
        long day = SellerGoals.dailyGoal(cents);
        long week = SellerGoals.weeklyGoal(cents);
        long thisWeek = SellerGoals.week(cents);
        Container rings = new Container(new GridLayout(1, 2));
        rings.add(goal(Lang.tr("Daily goal"), cents[cents.length - 1], day, "goal-day"));
        rings.add(goal(Lang.tr("Weekly goal"), thisWeek, week, "goal-week"));
        card.add(BorderLayout.CENTER, rings);
        int streak = SellerGoals.streak(cents);
        Label s = new Label(streak == 1 ? Lang.tr("1-day selling streak")
                : Lang.tr("{0}-day selling streak", String.valueOf(streak)), "MeStreak");
        s.setName("goal-streak");
        FontImage.setMaterialIcon(s, FontImage.MATERIAL_LOCAL_FIRE_DEPARTMENT, 3.2f);
        card.add(BorderLayout.SOUTH, FlowLayout.encloseCenter(s));
        return card;
    }

    private static Container goal(String caption, long value, long goal, String name) {
        Container box = new Container(BoxLayout.y());
        box.add(FlowLayout.encloseCenter(Delight.ring(SellerGoals.progressBp(value, goal),
                name)));
        Label c = new Label(caption, "MeGoalCaption");
        box.add(FlowLayout.encloseCenter(c));
        Label v = new Label(MerchantState.money(value) + " / " + MerchantState.money(goal),
                "MeGoalValue");
        v.setShouldLocalize(false);
        box.add(FlowLayout.encloseCenter(v));
        return box;
    }

    /// A burst and a toast for a milestone, once a session for each. A first
    /// sale is only celebrated as it happens, never on opening the app.
    private void celebrate(long[] cents, int todayOrders) {
        String store = MerchantState.storeId();
        int count = orders.size();
        Integer lastToday = LAST_TODAY.get(store);
        Integer lastCount = LAST_COUNT.get(store);
        LAST_TODAY.put(store, Integer.valueOf(todayOrders));
        LAST_COUNT.put(store, Integer.valueOf(count));
        String kind = SellerGoals.milestone(cents, todayOrders,
                lastCount == null ? count : lastCount.intValue(), count);
        if (SellerGoals.FIRST_SALE.equals(kind) && (lastToday == null || lastToday.intValue() > 0)) {
            return;
        }
        if (SellerGoals.NONE.equals(kind)) {
            return;
        }
        String key = store + "/" + (System.currentTimeMillis() / DAY) + "/" + kind
                + (SellerGoals.HUNDREDS.equals(kind) ? String.valueOf(count / 100) : "");
        if (!CELEBRATED.add(key)) {
            return;
        }
        final String title = SellerGoals.HUNDREDS.equals(kind)
                ? Lang.tr("{0} orders!", String.valueOf(count / 100 * 100))
                : SellerGoals.RECORD_DAY.equals(kind) ? Lang.tr("Record day!")
                : Lang.tr("First sale today!");
        final String detail = Lang.tr("Keep it going");
        com.codename1.ui.CN.callSerially(() -> {
            if (getComponentForm() != null) {
                Delight.burst(getComponentForm());
                Delight.toast(getComponentForm(), title, detail, "toast-milestone");
            }
        });
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
