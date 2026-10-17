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
package com.codenameone.examples.savor.admin;

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
import com.codenameone.examples.savor.api.AdminStatsDto;
import com.codenameone.examples.savor.api.DayStatsDto;
import com.codenameone.examples.savor.api.CourierDto;
import com.codenameone.examples.savor.api.NameCountDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.api.StatsSeriesDto;
import com.codenameone.examples.savor.live.LiveChannel;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Chart;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Layouts;
import com.codenameone.examples.savor.ui.StatRow;
import com.codenameone.examples.savor.ui.Ui;
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
        void applications();

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
            page.setUIID("WlPage");
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
        fill(new AdminStatsDto(0, 0, 0, 0, 0, 0, 0L, null));
    }

    /// The page came into view: everything is fetched again.
    void shown() {
        LiveChannel.listen(this);
        load();
        loadSeries();
    }

    @Override
    public void deliveryChanged(String id, String state) {
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
        numbers.stat(String.valueOf(stats.deliveriesToday()), "Deliveries today", "deliveriesToday");
        numbers.stat(String.valueOf(stats.deliveriesActive()), "Under way", "deliveriesActive");
        numbers.stat(stats.couriersOnline() + " / " + stats.couriers(), "Couriers online",
                "couriersOnline");
        numbers.stat(Ui.money(stats.revenueCents(), stats.currency()), "Fares collected",
                "revenue");
        refresh();
    }

    private void fill(StatsSeriesDto found) {
        last = found;
        alerts.removeAll();
        if (found.pendingApplications() > 0) {
            alerts.add(alert(FontImage.MATERIAL_ASSIGNMENT_IND, found.pendingApplications() == 1
                    ? Lang.tr("1 application to review")
                    : Lang.tr("{0} applications to review",
                            String.valueOf(found.pendingApplications())), "alert-applications",
                    places::applications));
        }
        if (found.flaggedUsers() > 0) {
            alerts.add(alert(FontImage.MATERIAL_FLAG, found.flaggedUsers() == 1
                    ? Lang.tr("1 flagged account") : Lang.tr("{0} flagged accounts",
                            String.valueOf(found.flaggedUsers())), "alert-flagged",
                    places::flagged));
        }
        series.removeAll();
        int count = found.days() == null ? 0 : found.days().size();
        double[] deliveries = new double[count];
        double[] revenue = new double[count];
        int asked = 0;
        int completed = 0;
        int cancelled = 0;
        long taken = 0;
        int joined = 0;
        for (int iter = 0; iter < count; iter++) {
            DayStatsDto day = found.days().get(iter);
            deliveries[iter] = day.deliveries();
            revenue[iter] = day.revenue();
            asked += day.deliveries();
            completed += day.completed();
            cancelled += day.cancelled();
            taken += day.revenue();
            joined += day.newUsers();
        }

        StatRow rates = Ui.stats(3);
        rates.stat(Math.round(found.completionRate() * 100d) + "%", "Completed",
                "completionRate");
        rates.stat(Ui.money(found.averageFare(), found.currency()), "Average fare",
                "averageFare");
        rates.stat(wait(found.averageWaitSeconds()), "Average wait", "averageWait");

        // One column on a phone, in the order they are added. On a wide screen
        // as many columns as fit, dealt out in turn: each tile keeps its own
        // height, which a grid of tiles would not let it.
        columns = columns();
        Container[] column = new Container[columns];
        if (columns == 1) {
            column[0] = series;
        } else {
            series.add(rates);
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
        column[next++ % columns].add(tile("Deliveries", String.valueOf(asked), "seriesDeliveries",
                Lang.tr("{0} completed, {1} cancelled", String.valueOf(completed),
                        String.valueOf(cancelled)),
                Chart.of("chartDeliveries", deliveries, from, to)));
        column[next++ % columns].add(tile("Fares", Ui.money(taken, found.currency()),
                "seriesRevenue", Lang.tr("{0} new accounts", String.valueOf(joined)),
                Chart.of("chartRevenue", revenue, from, to)));
        if (columns == 1) {
            series.add(rates);
        }

        int hours = found.byHour() == null ? 0 : found.byHour().size();
        double[] hourly = new double[hours];
        for (int iter = 0; iter < hours; iter++) {
            Integer value = found.byHour().get(iter);
            hourly[iter] = value == null ? 0 : value.intValue();
        }
        column[next++ % columns].add(tile("Requests by hour", null, null,
                "The busy hours, in UTC", Chart.of("chartHours", hourly, "00:00", "23:00")));
        column[next++ % columns].add(shares("By outcome", found.byState(), true));
        column[next++ % columns].add(shares("By kind of delivery", found.byProduct(), false));

        if (found.topCouriers() != null && !found.topCouriers().isEmpty()) {
            Container top = box("Top couriers");
            for (int iter = 0; iter < found.topCouriers().size(); iter++) {
                CourierDto courier = found.topCouriers().get(iter);
                Container row = Ui.row((char) 0, courier.displayName(), courier.vehicle(),
                        Ui.plain(Lang.tr("{0} deliveries", String.valueOf(courier.deliveries())),
                                "WlRowValue"));
                row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(
                        Ui.avatar(courier.displayName())));
                top.add(row);
            }
            column[next++ % columns].add(top);
        }
        refresh();
    }

    /// Something that wants the admin's attention, as a button that goes to it.
    private static Button alert(char icon, String text, String name, final Runnable go) {
        Button alert = new Button("", "WlAlert");
        alert.setShouldLocalize(false);
        alert.setText(text);
        alert.setName(name);
        Ui.icon(alert, icon, 3.4f);
        alert.addActionListener(e -> go.run());
        return alert;
    }

    private static Container box(String title) {
        Container box = new Container(BoxLayout.y());
        box.setUIID("WlTile");
        box.add(Ui.label(title, "WlStatLabel"));
        return box;
    }

    /// A chart with what it adds up to written over it.
    private static Container tile(String title, String value, String name, String detail,
            Container chart) {
        Container tile = box(title);
        if (value != null) {
            Label total = Ui.plain(value, "WlChartValue");
            total.setName(name);
            tile.add(total);
        }
        tile.add(Ui.plain(Lang.tr(detail), "WlMuted"));
        tile.add(chart);
        return tile;
    }

    /// How a total divides: each part with its count and its share as a bar.
    private static Container shares(String title, List<NameCountDto> parts, boolean states) {
        Container tile = box(title);
        long total = 0;
        for (int iter = 0; parts != null && iter < parts.size(); iter++) {
            total += parts.get(iter).count();
        }
        for (int iter = 0; parts != null && iter < parts.size(); iter++) {
            NameCountDto part = parts.get(iter);
            String name = states ? Ui.state(DeliveryState.named(part.name())) : Ui.product(part.name());
            tile.add(Ui.fact(name.length() == 0 ? part.name() : name,
                    String.valueOf(part.count())));
            // A total of nothing has no shares; the bars are then empty.
            tile.add(new Chart.Meter(total == 0 ? 0 : part.count() / (double) total));
        }
        return tile;
    }

    private static String wait(double seconds) {
        if (seconds < 90) {
            return Lang.tr("{0} s", String.valueOf(Math.round(seconds)));
        }
        return Ui.minutes(seconds);
    }
}
