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
package com.codenameone.examples.brickyard.ui;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.views.BarChart;
import com.codename1.charts.views.LineChart;
import com.codename1.charts.views.PointStyle;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.table.TableLayout;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.api.AuditDto;
import com.codenameone.examples.brickyard.api.ExperienceDto;
import com.codenameone.examples.brickyard.api.FraudFlagDto;
import com.codenameone.examples.brickyard.api.KpiDto;
import com.codenameone.examples.brickyard.api.TractionDayDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.net.Session;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The staff console as a desktop application: a sidebar of sections, a
/// header, KPI tiles and traction charts on the overview, and the work
/// queues (reports, appeals, payouts, fraud) as panes beside each other.
/// On a phone [AdminForm] is the same console as one scrolling list; both
/// call the same server, which checks every call against the caller's role.
public final class AdminConsole {
    static final String[][] SECTIONS = {
        {"Overview", "dashboard"},
        {"Moderation", "gavel"},
        {"Players", "people"},
        {"Economy", "savings"},
        {"Content", "star"},
        {"Operations", "build"},
        {"Audit log", "history"},
    };
    private static final char[] ICONS = {FontImage.MATERIAL_DASHBOARD, FontImage.MATERIAL_GAVEL,
        FontImage.MATERIAL_PEOPLE, FontImage.MATERIAL_SAVINGS, FontImage.MATERIAL_STAR,
        FontImage.MATERIAL_BUILD, FontImage.MATERIAL_HISTORY};
    static final int TRACTION_DAYS = 14;
    private static final int CHART_BG = 0xff161b2e;

    private final Form form;
    private final Container center = new Container(new BorderLayout());
    private final Label heading = new Label("", "ConsoleHeading");
    private final List<Button> nav = new ArrayList<Button>();

    private AdminConsole(Form form) {
        this.form = form;
    }

    /// True when the screen is wide enough for panes side by side.
    public static boolean fits() {
        return CN.getDisplayWidth() >= CN.convertToPixels(170);
    }

    /// Opens the console: the desktop layout when it fits, else the phone's.
    public static void show() {
        if (!fits()) {
            AdminForm.show();
            return;
        }
        final Form previous = CN.getCurrentForm();
        Form f = new Form("Brickyard Console", new BorderLayout());
        f.setName("adminConsole");
        f.setUIID("ConsoleForm");
        f.getToolbar().setUIID("ConsoleToolbar");
        f.getToolbar().getTitleComponent().setUIID("ConsoleTitle");
        f.setScrollable(false);
        Ui.back(f, () -> {
            if (previous != null) {
                previous.showBack();
            } else {
                Nav.home();
            }
        });
        AdminConsole c = new AdminConsole(f);
        f.add(BorderLayout.WEST, c.sidebar());
        Container main = new Container(new BorderLayout());
        main.setUIID("ConsoleMain");
        main.add(BorderLayout.NORTH, c.header());
        main.add(BorderLayout.CENTER, c.center);
        f.add(BorderLayout.CENTER, main);
        c.open(0);
        f.show();
    }

    private Container sidebar() {
        Container side = new Container(BoxLayout.y());
        side.setUIID("ConsoleSidebar");
        side.setName("consoleSidebar");
        Label brand = new Label("Brickyard", "ConsoleBrand");
        FontImage.setMaterialIcon(brand, FontImage.MATERIAL_VIEW_IN_AR, 4);
        side.add(brand);
        side.add(new Label("Operations console", "ConsoleBrandSub"));
        for (int i = 0; i < SECTIONS.length; i++) {
            final int k = i;
            Button b = new Button(SECTIONS[i][0], "ConsoleNav");
            b.setName("console-" + SECTIONS[i][1]);
            FontImage.setMaterialIcon(b, ICONS[i], 3.2f);
            b.setGap(CN.convertToPixels(1.5f));
            b.addActionListener(e -> open(k));
            nav.add(b);
            side.add(b);
        }
        return side;
    }

    private Container header() {
        Container h = new Container(new BorderLayout());
        h.setUIID("ConsoleHeader");
        AccountDto me = Session.user();
        StringBuilder roles = new StringBuilder();
        if (me != null && me.roles() != null) {
            for (String r : me.roles()) {
                roles.append(roles.length() == 0 ? "" : ", ").append(r);
            }
        }
        Label who = new Label(me == null ? "" : me.displayName() + "  " + roles, "ConsoleWho");
        FontImage.setMaterialIcon(who, FontImage.MATERIAL_ACCOUNT_CIRCLE, 3.2f);
        Button refresh = new Button("Refresh", "ConsoleAction");
        refresh.setName("consoleRefresh");
        FontImage.setMaterialIcon(refresh, FontImage.MATERIAL_REFRESH, 3);
        refresh.addActionListener(e -> open(selected));
        h.add(BorderLayout.WEST, heading);
        h.add(BorderLayout.EAST, BoxLayout.encloseX(who, refresh));
        return h;
    }

    private int selected;

    void open(int section) {
        selected = section;
        for (int i = 0; i < nav.size(); i++) {
            nav.get(i).setUIID(i == section ? "ConsoleNavSelected" : "ConsoleNav");
        }
        heading.setText(SECTIONS[section][0]);
        center.removeAll();
        Container body;
        switch (section) {
            case 0:
                body = overview();
                break;
            case 1:
                body = columns(pane("Report queue", "reportQueue", AdminForm::loadReports),
                        pane("Appeals", "appeals", AdminForm::loadAppeals));
                break;
            case 2:
                body = players();
                break;
            case 3:
                body = economy();
                break;
            case 4:
                body = content();
                break;
            case 5:
                body = operations();
                break;
            default:
                body = columns(auditPane());
                break;
        }
        center.add(BorderLayout.CENTER, body);
        form.revalidate();
    }

    /// A loader in [AdminForm]'s shape: fill `into`, revalidate `f`.
    interface Loader {
        void load(Container into, Form f);
    }

    /// A titled card whose body a loader fills.
    private Container pane(String title, String name, Loader loader) {
        Container rows = new Container(BoxLayout.y());
        rows.setName(name);
        rows.setScrollableY(true);
        Container card = card(title, rows);
        loader.load(rows, form);
        return card;
    }

    private static Container card(String title, Component body) {
        Container card = new Container(new BorderLayout());
        card.setUIID("ConsoleCard");
        card.add(BorderLayout.NORTH, new Label(title, "ConsoleCardTitle"));
        card.add(BorderLayout.CENTER, body);
        return card;
    }

    private static Container columns(Component... panes) {
        Container c = new Container(new GridLayout(1, panes.length));
        c.setUIID("ConsoleBody");
        for (Component p : panes) {
            c.add(p);
        }
        return c;
    }

    // ---- overview ----

    private Container overview() {
        final Container tiles = new Container(new GridLayout(1, 6));
        tiles.setName("consoleKpis");
        String[] names = {"Daily players", "Online now", "Sessions today", "Revenue today", "Purchases",
            "Errors / 1k sessions"};
        final Label[] values = new Label[names.length];
        for (int i = 0; i < names.length; i++) {
            values[i] = new Label("-", "ConsoleKpiValue");
            Container t = BoxLayout.encloseY(new Label(names[i], "ConsoleKpiName"), values[i]);
            t.setUIID("ConsoleKpi");
            tiles.add(t);
        }
        final Container trend = new Container(new BorderLayout());
        trend.setName("tractionChart");
        final Container bars = new Container(new BorderLayout());
        bars.setName("signupChart");
        final Container top = new Container(new TableLayout(1, 4));
        top.setName("topExperiences");
        final Container attention = new Container(BoxLayout.y());
        attention.setName("needsAttention");
        Container charts = new Container(new GridLayout(1, 2));
        charts.add(card("Players and visits, last " + TRACTION_DAYS + " days", trend));
        charts.add(card("New accounts and Bricks spent", bars));
        Container lists = new Container(new GridLayout(1, 2));
        lists.add(card("Top experiences", top));
        lists.add(card("Needs attention", attention));
        Container body = BoxLayout.encloseY(tiles, charts, lists);
        body.setUIID("ConsoleBody");
        body.setScrollableY(true);

        Api.moderation().kpis(Net.to((KpiDto k) -> {
            values[0].setText(count(k.dau()));
            values[1].setText(count(k.ccu()));
            values[2].setText(count(k.sessions()));
            values[3].setText(count(k.revenueBricks()) + " B");
            values[4].setText(count(k.purchases()));
            values[5].setText(String.valueOf(k.errorsPerThousandSessions()));
            form.revalidate();
        }, Ui::failed));
        Api.moderation().traction(TRACTION_DAYS, Net.to((List<TractionDayDto> days) -> {
            trend.add(BorderLayout.CENTER, lines(days));
            bars.add(BorderLayout.CENTER, bars(days));
            form.revalidate();
        }, Ui::failed));
        Api.experiences().browse("", Net.to((List<ExperienceDto> rows) -> {
            List<ExperienceDto> sorted = new ArrayList<ExperienceDto>(rows == null
                    ? Collections.<ExperienceDto>emptyList() : rows);
            Collections.sort(sorted, (a, b) -> Long.compare(b.visits(), a.visits()));
            top.setLayout(new TableLayout(Math.min(6, sorted.size()) + 1, 4));
            for (String h : new String[] {"Experience", "Creator", "Visits", "Rating"}) {
                top.add(new Label(h, "ConsoleTableHead"));
            }
            for (int i = 0; i < Math.min(6, sorted.size()); i++) {
                ExperienceDto x = sorted.get(i);
                long votes = x.upVotes() + x.downVotes();
                top.add(new Label(x.title(), "ConsoleCell"));
                top.add(new Label(x.ownerName(), "ConsoleCellMuted"));
                top.add(new Label(count(x.visits()), "ConsoleCell"));
                top.add(new Label(votes == 0 ? "-" : x.upVotes() * 100 / votes + "%", "ConsoleCell"));
            }
            form.revalidate();
        }, Ui::failed));
        Api.moderation().queue("OPEN", Net.to(r -> attention(attention, FontImage.MATERIAL_FLAG,
                r == null ? 0 : r.size(), "open report", "open reports", 1), Ui::failed));
        Api.moderation().appeals(Net.to(r -> attention(attention, FontImage.MATERIAL_HOW_TO_VOTE,
                r == null ? 0 : r.size(), "appeal to decide", "appeals to decide", 1), Ui::failed));
        Api.moderation().payouts(Net.to(r -> attention(attention, FontImage.MATERIAL_PAYMENTS,
                r == null ? 0 : r.size(), "payout request", "payout requests", 3), Ui::failed));
        Api.moderation().fraudFlags(Net.to(r -> attention(attention, FontImage.MATERIAL_REPORT,
                r == null ? 0 : r.size(), "fraud flag this month", "fraud flags this month", 3), Ui::failed));
        return body;
    }

    /// One "Needs attention" row that opens the section it is about.
    private void attention(Container into, char icon, int n, String one, String many, final int section) {
        String label = n == 1 ? one : many;
        Button b = new Button(n + " " + label, n == 0 ? "ConsoleAttentionClear" : "ConsoleAttention");
        FontImage.setMaterialIcon(b, icon, 3);
        b.setGap(CN.convertToPixels(1.5f));
        b.addActionListener(e -> open(section));
        into.add(b);
        form.revalidate();
    }

    static String count(long n) {
        if (n >= 10000) {
            return n / 1000 + "k";
        }
        if (n >= 1000) {
            return n / 1000 + "," + String.valueOf(1000 + n % 1000).substring(1);
        }
        return String.valueOf(n);
    }

    private static ChartComponent lines(List<TractionDayDto> days) {
        XYSeries players = new XYSeries("Players");
        XYSeries visits = new XYSeries("Visits");
        for (int i = 0; i < days.size(); i++) {
            players.add(i, days.get(i).activePlayers());
            visits.add(i, days.get(i).visits());
        }
        XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
        data.addSeries(visits);
        data.addSeries(players);
        XYMultipleSeriesRenderer r = renderer(days);
        r.addSeriesRenderer(line(0xff5c8dff));
        r.addSeriesRenderer(line(0xff5ee6a0));
        r.setShowLegend(true);
        r.setLegendTextSize(CN.convertToPixels(2.4f));
        ChartComponent c = new ChartComponent(new LineChart(data, r));
        c.setName("tractionLines");
        c.setPreferredH(CN.convertToPixels(52));
        return c;
    }

    private static XYSeriesRenderer line(int color) {
        XYSeriesRenderer s = new XYSeriesRenderer();
        s.setColor(color);
        s.setLineWidth(CN.convertToPixels(0.5f));
        s.setPointStyle(PointStyle.CIRCLE);
        s.setFillPoints(true);
        return s;
    }

    private static ChartComponent bars(List<TractionDayDto> days) {
        XYSeries signups = new XYSeries("New accounts");
        XYSeries bricks = new XYSeries("Bricks / 100");
        for (int i = 0; i < days.size(); i++) {
            signups.add(i, days.get(i).signups());
            bricks.add(i, days.get(i).bricks() / 100.0);
        }
        XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
        data.addSeries(signups);
        data.addSeries(bricks);
        XYMultipleSeriesRenderer r = renderer(days);
        XYSeriesRenderer a = new XYSeriesRenderer();
        a.setColor(0xffffc43c);
        XYSeriesRenderer b = new XYSeriesRenderer();
        b.setColor(0xffb57aff);
        r.addSeriesRenderer(a);
        r.addSeriesRenderer(b);
        r.setBarSpacing(0.25);
        r.setShowLegend(true);
        r.setLegendTextSize(CN.convertToPixels(2.4f));
        ChartComponent c = new ChartComponent(new BarChart(data, r, BarChart.Type.DEFAULT));
        c.setName("signupBars");
        c.setPreferredH(CN.convertToPixels(52));
        return c;
    }

    /// The theme's chart frame: the card's colour, quiet axes, day labels.
    private static XYMultipleSeriesRenderer renderer(List<TractionDayDto> days) {
        XYMultipleSeriesRenderer r = new XYMultipleSeriesRenderer();
        r.setApplyBackgroundColor(true);
        r.setBackgroundColor(CHART_BG);
        r.setMarginsColor(CHART_BG);
        r.setAxesColor(0xff3a4468);
        r.setGridColor(0xff2a3352);
        r.setShowGridY(true);
        r.setLabelsColor(0xffaab3cf);
        r.setXLabelsColor(0xffaab3cf);
        r.setYLabelsColor(0, 0xffaab3cf);
        r.setLabelsTextSize(CN.convertToPixels(2.2f));
        int m = CN.convertToPixels(3);
        r.setMargins(new int[] {m / 2, m * 2, m * 2, m / 2});
        r.setXAxisMin(-0.5);
        r.setXAxisMax(days.size() - 0.5);
        r.setYAxisMin(0);
        r.setXLabels(0);
        r.setPanEnabled(false, false);
        r.setZoomEnabled(false, false);
        for (int k = 0; k < days.size(); k += 2) {
            r.addXTextLabel(k, DashboardForm.dayLabel(days.get(k).dayStart()));
        }
        return r;
    }

    // ---- sections ----

    private Container players() {
        final Container sanctions = new Container(BoxLayout.y());
        sanctions.setName("sanctions");
        final TextField who = Ui.field("Username", TextField.ANY, "sanctionUser");
        Container lookup = BoxLayout.encloseY(who,
                BoxLayout.encloseX(Ui.secondary("Look up", "sanctionsButton",
                        e -> AdminForm.loadSanctions(who.getText().trim(), sanctions, form)),
                        Ui.secondary("Issue a sanction", "sanctionButton", e -> AdminForm.sanction(sanctions, form)),
                        Ui.secondary("Grant or revoke Bricks", "adjustButton", e -> AdminForm.adjust())),
                sanctions);
        final Container active = new Container(BoxLayout.y());
        active.setName("activeSanctions");
        active.setScrollableY(true);
        AdminForm.loadActiveSanctions(new int[] {0}, active, form);
        return columns(card("Look up a player", lookup), card("Active sanctions", active));
    }

    private Container economy() {
        final Container fraud = new Container(BoxLayout.y());
        fraud.setName("fraudFlags");
        fraud.setScrollableY(true);
        Api.moderation().fraudFlags(Net.to((List<FraudFlagDto> rows) -> {
            AdminForm.empty(fraud, rows);
            if (rows != null) {
                for (FraudFlagDto x : rows) {
                    fraud.add(row(x.username(), x.kind() + " - " + x.detail()));
                }
            }
            form.revalidate();
        }, Ui::failed));
        return columns(pane("Payout requests", "payouts", AdminForm::loadPayouts), card("Fraud flags", fraud));
    }

    private Container content() {
        Container featured = new Container(BoxLayout.y());
        featured.setName("featured");
        featured.setScrollableY(true);
        AdminForm.loadFeatured(featured, form);
        Container c = new Container(new BorderLayout());
        c.add(BorderLayout.CENTER, featured);
        c.add(BorderLayout.SOUTH, Ui.secondary("Feature an experience", "featureButton",
                e -> AdminForm.feature(featured, form)));
        return columns(card("Home rows", c));
    }

    private Container operations() {
        Container jobs = BoxLayout.encloseY(
                Ui.text("Engagement payouts pay creators for the time Plus members spend in their experiences.",
                        "Body"),
                Ui.secondary("Run engagement payouts", "jobEngagementButton",
                        e -> AdminForm.job("engagement-payouts")),
                Ui.text("Plus stipends credit every Plus member's monthly Bricks.", "Body"),
                Ui.secondary("Run Plus stipends", "jobStipendsButton", e -> AdminForm.job("plus-stipends")));
        Container seasons = BoxLayout.encloseY(
                Ui.text("A new season resets the season pass for everyone and starts its tracks.", "Body"),
                Ui.secondary("Start a new season", "seasonButton", e -> AdminForm.startSeason()));
        return columns(card("Scheduled jobs", jobs), card("Seasons", seasons));
    }

    private Container auditPane() {
        final Container rows = new Container(new TableLayout(1, 4));
        rows.setName("auditLog");
        rows.setScrollableY(true);
        Api.moderation().audit(Net.to((List<AuditDto> list) -> {
            int n = list == null ? 0 : list.size();
            rows.setLayout(new TableLayout(n + 1, 4));
            for (String h : new String[] {"When", "Who", "Action", "Target"}) {
                rows.add(new Label(h, "ConsoleTableHead"));
            }
            for (int i = 0; i < n; i++) {
                AuditDto a = list.get(i);
                rows.add(new Label(DashboardForm.dayLabel(a.createdAt()), "ConsoleCellMuted"));
                rows.add(new Label(a.actor(), "ConsoleCell"));
                rows.add(new Label(a.action(), "ConsoleCell"));
                rows.add(new Label(a.target() + (a.detail() == null || a.detail().length() == 0 ? ""
                        : " - " + a.detail()), "ConsoleCellMuted"));
            }
            form.revalidate();
        }, Ui::failed));
        return card("Every staff action, newest first", rows);
    }

    private static Container row(String title, String detail) {
        return BoxLayout.encloseY(new Label(title, "ConsoleCell"), Ui.text(detail, "ConsoleCellMuted"));
    }
}
