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
package com.codenameone.examples.dash.admin;

import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.dash.AppConfig;
import com.codenameone.examples.dash.api.AdminStatsDto;
import com.codenameone.examples.dash.api.InsightsDto;
import com.codenameone.examples.dash.api.NameCountDto;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.ui.Ui;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/// The console's first page: what is happening now, and how the game has
/// done over the last days -- players, new accounts, races, sales, the
/// funnel from joining to paying, retention and the trophy ladder.
///
/// Every number is read from the game's own records by the server's
/// `Insights`. Behaviour inside the app (screens, sessions, shares, push
/// opens) is in Codename One analytics; the last button opens it.
// tag::dash-reports-tab[]
final class ReportsTab {
    private static final String[] RANGES = {"Last 7 days", "Last 14 days", "Last 30 days", "Last 90 days"};
    private static final int[] RANGE_DAYS = {7, 14, 30, 90};
    private static final long DAY = 24L * 60 * 60 * 1000;

    private final Container page = Desk.page();
    private final Container now = new Container(BoxLayout.y());
    private final Container trends = new Container(BoxLayout.y());
    private int range = 2;

    ReportsTab() {
        page.setName("reports");
        page.add(Desk.section("Right now"));
        page.add(now);
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.CENTER, Desk.section("Trends"));
        head.add(BorderLayout.EAST, Desk.choice("reportRange", RANGES, range, i -> {
            range = i;
            loadTrends();
        }));
        page.add(head);
        page.add(trends);
    }

    Container page() {
        return page;
    }

    void refresh() {
        Api.admin().stats(Net.to(s -> Ui.later(() -> showNow(s)), Ui::failed));
        loadTrends();
    }

    private void loadTrends() {
        final int asked = range;
        Api.admin().reports(RANGE_DAYS[asked], Net.to(r -> Ui.later(() -> {
            if (asked == range) {
                showTrends(r);
            }
        }), Ui::failed));
    }

    private void showNow(AdminStatsDto s) {
        now.removeAll();
        int columns = AdminConsole.wide() ? 4 : 2;
        now.add(Desk.cards(columns,
                Desk.card("Players", String.valueOf(s.players()), s.online() + " online"),
                Desk.card("Live races", String.valueOf(s.liveRaces()), s.racesToday() + " today"),
                Desk.card("Open flags", String.valueOf(s.openFlags()), s.suspended() + " suspended"),
                Desk.card("Sales today", String.valueOf(s.purchasesToday()), s.gemsSoldToday() + " gems")));
        page.revalidate();
    }

    @SuppressWarnings("unchecked")
    private void showTrends(InsightsDto r) {
        trends.removeAll();
        int days = r.active().size();
        String[] labels = new String[days];
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d");
        for (int i = 0; i < days; i++) {
            labels[i] = fmt.format(new Date(r.firstDay() + i * DAY));
        }
        Container charts = new Container(new GridLayout(AdminConsole.wide() ? 2 : 1));
        charts.add(titled("Daily players", "Ran a race or a level that day, and accounts created",
                Desk.line("chartPlayers", new String[] {"Played", "Joined"},
                        new List[] {r.active(), r.joined()}, labels)));
        charts.add(titled("Races", "Finished races a day",
                Desk.line("chartRaces", new String[] {"Races"}, new List[] {r.races()}, labels)));
        charts.add(titled("Funnel", "Every player so far, by how far they got",
                bars("chartFunnel", r.funnel())));
        charts.add(titled("Gems sold", "Through the store, a day",
                Desk.line("chartGems", new String[] {"Gems"}, new List[] {r.gems()}, labels)));
        charts.add(titled("Trophy ladder", "Players in each trophy band", bars("chartTrophies", r.trophies())));
        Container back = Desk.panel();
        back.add(new Label("Retention", "DeskPanelTitle"));
        back.add(new Label("Seen again at least this long after joining", "DeskHelp"));
        List<Component> cards = new ArrayList<>();
        for (NameCountDto d : r.retention()) {
            cards.add(Desk.card(d.name(), d.count() + "%", null));
        }
        if (cards.isEmpty()) {
            back.add(Desk.empty("No players yet."));
        } else {
            int columns = AdminConsole.wide() ? cards.size() : Math.min(2, cards.size());
            back.add(Desk.cards(columns, cards.toArray(new Component[cards.size()])));
        }
        charts.add(back);
        trends.add(charts);
        trends.add(Desk.help("Read from the game's own records: accounts, battles and purchases. What players "
                + "do inside the app (screens, sessions, shared invites, opened pushes) is reported to Codename "
                + "One analytics."));
        trends.add(Desk.actions(Desk.button("Open Codename One analytics", "openAnalytics",
                e -> CN.execute(AppConfig.ANALYTICS_CONSOLE))));
        page.revalidate();
    }

    private static Component bars(String name, List<NameCountDto> counts) {
        List<String> names = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        for (NameCountDto c : counts) {
            names.add(c.name());
            values.add(c.count());
        }
        return Desk.bars(name, names, values);
    }

    private static Container titled(String title, String detail, Component chart) {
        Container c = Desk.panel();
        c.add(new Label(title, "DeskPanelTitle"));
        c.add(new Label(detail, "DeskHelp"));
        c.add(chart);
        return c;
    }
}
// end::dash-reports-tab[]
