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

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.AdminStatsDto;
import com.codenameone.examples.bazaar.api.DayStatsDto;
import com.codenameone.examples.bazaar.api.NameCountDto;
import com.codenameone.examples.bazaar.api.StatsSeriesDto;
import com.codenameone.examples.bazaar.live.LiveChannel;
import com.codenameone.examples.bazaar.net.Api;
import com.codename1.appkit.Net;
import com.codename1.appkit.Chart;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.StatRow;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The numbers of the admin console: what needs an answer, how the service is
/// doing right now, and how it has done over the last weeks.
///
/// It is the same on a phone and on a wide screen, laid out for the room there
/// is. On a phone everything is one column, with the four numbers two by two.
/// On a wide screen the numbers are one row and the charts are side by side,
/// in as many columns as fit -- see [#fit()], which is what a resized window
/// calls.
final class Dashboard extends LiveChannel.Adapter {
    /// A chart is not drawn narrower than this, in millimetres.
    private static final float TILE_MM = 88f;

    /// Where the things that want attention lead.
    interface Places {
        void flagged();
    }

    private final boolean wide;
    private final Places places;
    private final Container page;
    private final Container alerts = new Container(BoxLayout.y());
    private final StatRow numbers;
    private final Container series = new Container(BoxLayout.y());
    private int days = 14;
    private int columns;
    private StatsSeriesDto last;

    /// @param wide whether to lay out for a wide screen
    Dashboard(boolean wide, Places places) {
        this.wide = wide;
        this.places = places;
        numbers = Ui.stats(wide ? 4 : 2);
        if (wide) {
            // Not Ui.page(): that keeps to a column, and this is the one page
            // that is meant to use the width.
            page = new Container(BoxLayout.y());
            page.setUIID("SvPage");
            page.setScrollableY(true);
            page.setScrollVisible(false);
        } else {
            page = Ui.page();
            page.setSafeArea(false);
        }
        page.setName("dashboard");
        alerts.setName("alerts");
        page.add(alerts);
        numbers.setName("stats");
        Ui.section(page, "Right now");
        page.add(numbers);
        Ui.section(page, "Trends");
        Container range = Ui.choice("range", new String[] {"7 days", "14 days", "30 days"},
                new String[] {"7", "14", "30"}, String.valueOf(days), value -> {
                    days = Integer.parseInt(value);
                    loadSeries();
                });
        // On a wide screen the choice is as wide as it needs to be, not the
        // whole width of the window.
        page.add(wide ? FlowLayout.encloseIn(range) : range);
        series.setName("series");
        page.add(series);
    }

    /// The page, for the screen to place.
    Container page() {
        return page;
    }

    /// Shows the numbers as nothing, before the first answer.
    void start() {
        fill(new AdminStatsDto(0, 0, 0, 0, 0, 0L, null));
    }

    /// The page came into view: everything is fetched again.
    void shown() {
        LiveChannel.listen(this);
        load();
        loadSeries();
    }

    @Override
    public void orderChanged(String id, String status) {
        load();
    }

    void load() {
        Api.admin().stats(Net.to(this::fill, (status, message) -> { }));
    }

    private void loadSeries() {
        final int asked = days;
        Api.admin().statsSeries(asked, Net.to(found -> {
            if (asked == days) {
                fill(found);
            }
        }, (status, message) -> { }));
    }

    /// The room changed: the charts are laid out again if a different number
    /// of them now fits side by side.
    void fit() {
        if (wide && last != null && columns != columns()) {
            fill(last);
        }
    }

    private int columns() {
        if (!wide) {
            return 1;
        }
        int width = page.getWidth() > 0 ? page.getWidth() : CN.getDisplayWidth() * 3 / 4;
        return Layouts.columns(width, TILE_MM, 3);
    }

    private void refresh() {
        Layouts.dress(page);
        Form form = page.getComponentForm();
        if (form != null) {
            Ui.refresh(form);
        }
    }

    private void fill(AdminStatsDto stats) {
        numbers.clear();
        numbers.stat(String.valueOf(stats.users()), "Accounts", "users");
        numbers.stat(String.valueOf(stats.usersToday()), "New today", "usersToday");
        numbers.stat(String.valueOf(stats.paymentsToday()), "Payments today", "paymentsToday");
        numbers.stat(Ui.money(stats.revenueCents(), stats.currency()), "Revenue", "revenue");
        refresh();
    }

    private void fill(StatsSeriesDto found) {
        last = found;
        alerts.removeAll();
        if (found.flaggedUsers() > 0) {
            alerts.add(alert(FontImage.MATERIAL_FLAG, found.flaggedUsers() == 1
                    ? Lang.tr("1 flagged account") : Lang.tr("{0} flagged accounts",
                            String.valueOf(found.flaggedUsers())), "alert-flagged",
                    places::flagged));
        }
        series.removeAll();
        int count = found.days() == null ? 0 : found.days().size();
        double[] payments = new double[count];
        double[] revenue = new double[count];
        int paid = 0;
        int refunds = 0;
        long taken = 0;
        int joined = 0;
        for (int iter = 0; iter < count; iter++) {
            DayStatsDto day = found.days().get(iter);
            payments[iter] = day.payments();
            revenue[iter] = day.revenue();
            paid += day.payments();
            refunds += day.refunds();
            taken += day.revenue();
            joined += day.newUsers();
        }

        columns = columns();
        Container[] column = new Container[columns];
        if (columns == 1) {
            column[0] = series;
        } else {
            Container grid = new Container(new GridLayout(1, columns));
            for (int iter = 0; iter < columns; iter++) {
                column[iter] = new Container(BoxLayout.y());
                grid.add(column[iter]);
            }
            series.add(grid);
        }
        int next = 0;
        String from = Lang.tr("{0} days ago", String.valueOf(Math.max(1, count - 1)));
        String to = Lang.tr("Today");
        column[next++ % columns].add(tile("Payments", String.valueOf(paid), "seriesPayments",
                Lang.tr("{0} refunded, average {1}", String.valueOf(refunds),
                        Ui.money(found.averagePayment(), found.currency())),
                Chart.of("Sv", "chartPayments", payments, from, to)));
        column[next++ % columns].add(tile("Revenue", Ui.money(taken, found.currency()),
                "seriesRevenue", Lang.tr("{0} new accounts", String.valueOf(joined)),
                Chart.of("Sv", "chartRevenue", revenue, from, to)));

        int hours = found.byHour() == null ? 0 : found.byHour().size();
        double[] hourly = new double[hours];
        for (int iter = 0; iter < hours; iter++) {
            Integer value = found.byHour().get(iter);
            hourly[iter] = value == null ? 0 : value.intValue();
        }
        column[next++ % columns].add(tile("Payments by hour", null, null,
                "The busy hours, in UTC", Chart.of("Sv", "chartHours", hourly, "00:00", "23:00")));
        column[next++ % columns].add(shares("By status", found.byStatus()));
        refresh();
    }

    /// Something that wants the admin's attention, as a button that goes to it.
    private static Button alert(char icon, String text, String name, final Runnable go) {
        Button alert = new Button("", "SvAlert");
        alert.setShouldLocalize(false);
        alert.setText(text);
        alert.setName(name);
        Ui.icon(alert, icon, 3.4f);
        alert.addActionListener(e -> go.run());
        return alert;
    }

    private static Container box(String title) {
        Container box = new Container(BoxLayout.y());
        box.setUIID("SvTile");
        box.add(Ui.label(title, "SvStatLabel"));
        return box;
    }

    /// A chart with what it adds up to written over it.
    private static Container tile(String title, String value, String name, String detail,
            Container chart) {
        Container tile = box(title);
        if (value != null) {
            Label total = Ui.plain(value, "SvChartValue");
            total.setName(name);
            tile.add(total);
        }
        tile.add(Ui.plain(Lang.tr(detail), "SvMuted"));
        tile.add(chart);
        return tile;
    }

    /// How a total divides: each part with its count and its share as a bar.
    private static Container shares(String title, List<NameCountDto> parts) {
        Container tile = box(title);
        long total = 0;
        for (int iter = 0; parts != null && iter < parts.size(); iter++) {
            total += parts.get(iter).count();
        }
        for (int iter = 0; parts != null && iter < parts.size(); iter++) {
            NameCountDto part = parts.get(iter);
            tile.add(Ui.fact(com.codenameone.examples.bazaar.pay.ReceiptForm.status(part.name()), String.valueOf(part.count())));
            // A total of nothing has no shares; the bars are then empty.
            tile.add(new Chart.Meter("Sv", total == 0 ? 0 : part.count() / (double) total));
        }
        return tile;
    }
}
