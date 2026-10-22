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

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.parley.api.AdminStatsDto;
import com.codenameone.examples.parley.api.DayStatsDto;
import com.codenameone.examples.parley.api.NameCountDto;
import com.codenameone.examples.parley.api.StatsSeriesDto;
import com.codenameone.examples.parley.live.LiveChannel;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;
import com.codenameone.examples.parley.ui.Chart;
import com.codenameone.examples.parley.ui.Lang;
import com.codenameone.examples.parley.ui.StatRow;
import com.codenameone.examples.parley.ui.Ui;
import java.util.List;

/// The numbers of the admin screen: what needs an answer, how busy the service
/// is right now, and how it has done over the last weeks.
///
/// Every number is a count. The server relays messages it cannot read, so
/// there is nothing else to show: no content, and no one's contacts.
final class Dashboard implements LiveChannel.AdminListener {
    /// Where the things that want attention lead.
    interface Places {
        void reports();

        void flagged();
    }

    private final Places places;
    private final Container page;
    private final Container alerts = new Container(BoxLayout.y());
    private final StatRow numbers = Ui.stats(2);
    private final Container series = new Container(BoxLayout.y());
    private int days = 14;

    Dashboard(Places places) {
        this.places = places;
        page = Ui.page();
        page.setSafeArea(false);
        page.setName("dashboard");
        alerts.setName("alerts");
        page.add(alerts);
        numbers.setName("stats");
        Ui.section(page, "Right now");
        page.add(numbers);
        Ui.section(page, "Trends");
        page.add(Ui.choice("range", new String[] {"7 days", "14 days", "30 days"},
                new String[] {"7", "14", "30"}, String.valueOf(days), value -> {
                    days = Integer.parseInt(value);
                    loadSeries();
                }));
        series.setName("series");
        page.add(series);
    }

    Container page() {
        return page;
    }

    /// Shows the numbers as nothing, before the first answer.
    void start() {
        fill(new AdminStatsDto(0, 0, 0, 0, 0, 0, 0, 0));
    }

    /// The page came into view: everything is fetched again.
    void shown() {
        LiveChannel.listenAdmin(this);
        load();
        loadSeries();
    }

    @Override
    public void changed() {
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

    private void refresh() {
        Form form = page.getComponentForm();
        if (form != null) {
            Ui.refresh(form);
        }
    }

    private void fill(AdminStatsDto stats) {
        numbers.clear();
        numbers.stat(String.valueOf(stats.messagesToday()), "Messages today", "statMessages");
        numbers.stat(String.valueOf(stats.callsToday()), "Calls today", "statCalls");
        numbers.stat(String.valueOf(stats.users()), "Accounts", "statUsers");
        numbers.stat(String.valueOf(stats.devices()), "Devices", "statDevices");
        numbers.stat(String.valueOf(stats.groups()), "Groups", "statGroups");
        numbers.stat(String.valueOf(stats.bridges()), "Bridges", "statBridges");
        refresh();
    }

    private void fill(StatsSeriesDto found) {
        alerts.removeAll();
        if (found.openReports() > 0) {
            alerts.add(alert(FontImage.MATERIAL_REPORT, found.openReports() == 1
                    ? Lang.tr("1 report to review")
                    : Lang.tr("{0} reports to review", String.valueOf(found.openReports())),
                    "alert-reports", places::reports));
        }
        if (found.flaggedUsers() > 0) {
            alerts.add(alert(FontImage.MATERIAL_FLAG, found.flaggedUsers() == 1
                    ? Lang.tr("1 flagged account") : Lang.tr("{0} flagged accounts",
                            String.valueOf(found.flaggedUsers())), "alert-flagged",
                    places::flagged));
        }
        series.removeAll();
        int count = found.days() == null ? 0 : found.days().size();
        double[] messages = new double[count];
        double[] active = new double[count];
        int sent = 0;
        int calls = 0;
        int joined = 0;
        int peak = 0;
        for (int iter = 0; iter < count; iter++) {
            DayStatsDto day = found.days().get(iter);
            messages[iter] = day.messages();
            active[iter] = day.activeUsers();
            sent += day.messages();
            calls += day.calls();
            joined += day.newUsers();
            peak = Math.max(peak, day.activeUsers());
        }
        String from = Lang.tr("{0} days ago", String.valueOf(Math.max(1, count - 1)));
        String to = Lang.tr("Today");
        series.add(tile("Messages", String.valueOf(sent), "seriesMessages",
                Lang.tr("{0} calls", String.valueOf(calls)),
                Chart.of("chartMessages", messages, from, to)));
        series.add(tile("Active people", String.valueOf(peak), "seriesActive",
                Lang.tr("{0} new accounts", String.valueOf(joined)),
                Chart.of("chartActive", active, from, to)));
        int hours = found.byHour() == null ? 0 : found.byHour().size();
        double[] hourly = new double[hours];
        for (int iter = 0; iter < hours; iter++) {
            Integer value = found.byHour().get(iter);
            hourly[iter] = value == null ? 0 : value.intValue();
        }
        series.add(tile("Messages by hour", null, null, "The busy hours, in UTC",
                Chart.of("chartHours", hourly, "00:00", "23:00")));
        series.add(shares("By kind of chat", found.byKind()));
        series.add(shares("By platform", found.byPlatform()));
        refresh();
    }

    private static Button alert(char icon, String text, String name, final Runnable go) {
        Button alert = new Button("", "PlAlert");
        alert.setShouldLocalize(false);
        alert.setText(text);
        alert.setName(name);
        Ui.icon(alert, icon, 3.4f);
        alert.addActionListener(e -> go.run());
        return alert;
    }

    private static Container box(String title) {
        Container box = new Container(BoxLayout.y());
        box.setUIID("PlTile");
        box.add(Ui.label(title, "PlStatLabel"));
        return box;
    }

    private static Container tile(String title, String value, String name, String detail,
            Container chart) {
        Container tile = box(title);
        if (value != null) {
            Label total = Ui.plain(value, "PlChartValue");
            total.setName(name);
            tile.add(total);
        }
        tile.add(Ui.plain(Lang.tr(detail), "PlMuted"));
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
            tile.add(Ui.fact(part.name(), String.valueOf(part.count())));
            tile.add(new Chart.Meter(total == 0 ? 0 : part.count() / (double) total));
        }
        return tile;
    }
}
