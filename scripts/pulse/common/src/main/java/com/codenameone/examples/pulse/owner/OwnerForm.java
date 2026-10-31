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
package com.codenameone.examples.pulse.owner;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.views.BarChart;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.pulse.api.DayAmountDto;
import com.codenameone.examples.pulse.api.NameCountDto;
import com.codenameone.examples.pulse.api.OwnerDashboardDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Menu;
import com.codenameone.examples.pulse.ui.Nav;
import com.codenameone.examples.pulse.ui.StatRow;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The studio owner's home, which is the dashboard: what the studio earns,
/// how full it is, who is drifting away and which classes carry it. The menu
/// leads to plans, class templates, the timetable, staff, shifts and campaigns.
public class OwnerForm implements Nav.Home {
    /// The colours of the charts' series. The charts draw themselves and
    /// read no theme, so they are given the brand's colours here.
    private static final int[] PALETTE = {0xe4572e, 0x17bebb, 0xffc914, 0x2e282a, 0x76b041,
        0x7e6b8f};

    private final Form form = Ui.home("Owner");
    private final Menu menu = new Menu(form, Nav.OWNER);
    private final Container page = Ui.page();
    private final StatRow stats = Ui.stats(2);

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        menu.item(FontImage.MATERIAL_INSIGHTS, "Dashboard", "dashboard", () -> form.showBack());
        menu.item(FontImage.MATERIAL_QUERY_STATS, "Insights", "insights",
                () -> InsightsForm.show(form));
        menu.item(FontImage.MATERIAL_GROUP, "Members", "members", () -> MembersForm.show(form));
        menu.item(FontImage.MATERIAL_RECEIPT_LONG, "Billing", "billing",
                () -> BillingForm.show(form));
        menu.item(FontImage.MATERIAL_CARD_MEMBERSHIP, "Plans and pricing", "ownerPlans",
                () -> PlansForm.show(form));
        menu.item(FontImage.MATERIAL_CATEGORY, "Class templates", "templates",
                () -> TemplatesForm.show(form));
        menu.item(FontImage.MATERIAL_EVENT, "Timetable", "timetable",
                () -> TimetableForm.show(form));
        menu.item(FontImage.MATERIAL_PEOPLE, "Staff", "staff", () -> StaffForm.show(form));
        menu.item(FontImage.MATERIAL_SCHEDULE, "Shifts", "ownerShifts",
                () -> StaffForm.shifts(form));
        menu.item(FontImage.MATERIAL_CAMPAIGN, "Campaigns", "campaigns",
                () -> CampaignsForm.show(form));
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, menu.flatButton());
        bar.add(BorderLayout.CENTER, Ui.label("Studio", "SvTitle"));
        page.setName("owner");
        page.setScrollableY(true);
        Ui.pull(page, this::load);
        Container shell = new Container(new BorderLayout());
        shell.add(BorderLayout.NORTH, bar);
        shell.add(BorderLayout.CENTER, page);
        form.add(shell);
        menu.install();
        form.addShowListener(e -> load());
    }

    private void load() {
        Api.owner().dashboard(Net.to(this::fill, Ui::fail));
    }

    private void fill(OwnerDashboardDto board) {
        page.removeAll();
        stats.clear();
        stats.stat(Ui.moneyShort(board.mrrCents(), null), "Monthly recurring", "mrr")
                .stat(String.valueOf(board.activeMembers()), "Members", "members")
                .stat(board.utilisation() + "%", "Places taken", "utilisation")
                .stat(String.valueOf(board.atRisk()), "At risk", "atRisk");
        page.add(stats);
        // The rest of the month at a glance, as tiles rather than a sentence.
        StatRow month = Ui.stats(2);
        month.stat(String.valueOf(board.newMembers30()), "Joined in 30 days", "joined")
                .stat(String.valueOf(board.churned30()), "Left in 30 days", "left")
                .stat(String.valueOf(board.checkIns7()), "Visits this week", "checkIns7")
                .stat(String.valueOf(board.pastDue()), "Invoices past due", "pastDue");
        page.add(month);
        // The deeper screens, each from the figure that sends an owner there.
        page.add(Ui.tap(Ui.row(FontImage.MATERIAL_QUERY_STATS, Lang.tr("Insights"),
                Lang.tr("Busy hours, growth, classes and coaches"), null), "insightsButton",
                e -> InsightsForm.show(form)));
        page.add(Ui.tap(Ui.row(FontImage.MATERIAL_GROUP, Lang.tr("Members"),
                Lang.tr("{0} at risk", Integer.valueOf(board.atRisk())), null), "membersButton",
                e -> MembersForm.show(form)));
        page.add(Ui.tap(Ui.row(FontImage.MATERIAL_RECEIPT_LONG, Lang.tr("Billing"),
                Lang.tr("{0} invoices past due", Integer.valueOf(board.pastDue())),
                board.pastDue() > 0 ? Ui.badge(String.valueOf(board.pastDue()), "SvBadgeBad")
                : null), "billingButton", e -> BillingForm.show(form)));

        // Takings: everything paid, packs and drop-ins too, which is why it
        // is not the recurring figure above.
        Ui.section(page, "Takings");
        page.add(Ui.text(Lang.tr("{0} in the last 30 days", Ui.moneyShort(board.revenue30Cents(), null)),
                "SvMuted"));
        page.add(sized(revenue(board.revenueByDay()), "revenueChart"));

        Ui.section(page, "Popular classes");
        if (board.popularClasses() == null || board.popularClasses().isEmpty()) {
            page.add(Ui.text("No bookings in the last month.", "SvMuted"));
        } else {
            page.add(popular(board.popularClasses()));
        }

        Ui.section(page, "Under-filled this week");
        List<SessionDto> under = board.underfilled();
        for (int iter = 0; under != null && iter < under.size(); iter++) {
            final SessionDto session = under.get(iter);
            page.add(Ui.tap(Ui.row(FontImage.MATERIAL_TRENDING_DOWN, session.name(),
                    Ui.when(session.startsAt()), Ui.badge(session.booked() + "/"
                    + session.capacity(), "SvBadgeWarn")), "under-" + iter,
                    e -> CampaignsForm.compose(form, session)));
        }
        if (under == null || under.isEmpty()) {
            page.add(Ui.text("Every class is filling up.", "SvMuted"));
        }
        if (board.atRisk() > 0) {
            page.add(Ui.secondary(board.atRisk() == 1 ? Lang.tr("Win back a lapsing member")
                    : Lang.tr("Win back {0} lapsing members",
                    Integer.valueOf(board.atRisk())), "winBack",
                    e -> CampaignsForm.compose(form, null)));
        }
        Ui.refresh(form);
    }

    private static Component sized(Component chart, String name) {
        chart.setName(name);
        chart.setPreferredH(CN.convertToPixels(45));
        return chart;
    }

    /// The last 30 days of takings, a bar a day, in whole currency units.
    static Component revenue(List<DayAmountDto> days) {
        XYSeries series = new XYSeries(Lang.tr("Revenue"));
        XYMultipleSeriesRenderer renderer = renderer();
        double most = 0;
        for (int iter = 0; days != null && iter < days.size(); iter++) {
            double amount = days.get(iter).amount() / 100.0;
            series.add(iter + 1, amount);
            most = Math.max(most, amount);
            if (iter % 7 == 0) {
                String date = days.get(iter).date();
                renderer.addXTextLabel(iter + 1, shortDate(date));
            }
        }
        XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
        data.addSeries(series);
        XYSeriesRenderer bars = new XYSeriesRenderer();
        bars.setColor(PALETTE[0] | 0xff000000);
        renderer.addSeriesRenderer(bars);
        renderer.setXAxisMin(0);
        renderer.setXAxisMax(days == null ? 1 : days.size() + 1);
        renderer.setYAxisMin(0);
        renderer.setYAxisMax(most <= 0 ? 1 : most * 1.1);
        renderer.setXLabels(0);
        renderer.setBarSpacing(0.3);
        return new ChartComponent(new BarChart(data, renderer, BarChart.Type.DEFAULT));
    }

    private static XYMultipleSeriesRenderer renderer() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setShowLegend(false);
        renderer.setLabelsTextSize(CN.convertToPixels(2.2f));
        renderer.setMargins(new int[] {CN.convertToPixels(2), CN.convertToPixels(8),
            CN.convertToPixels(2), CN.convertToPixels(2)});
        // The margins are painted, and 0 paints them black: give them the
        // page's own colour, which follows dark mode.
        renderer.setMarginsColor(0xff000000 | com.codename1.ui.plaf.UIManager.getInstance()
                .getComponentStyle("SvPage").getBgColor());
        renderer.setApplyBackgroundColor(false);
        renderer.setLabelsColor(0xff7a7a7a);
        renderer.setXLabelsColor(0xff7a7a7a);
        renderer.setYLabelsColor(0, 0xff7a7a7a);
        renderer.setAxesColor(0xffbdbdbd);
        renderer.setPanEnabled(false, false);
        renderer.setZoomEnabled(false, false);
        return renderer;
    }

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
        "Aug", "Sep", "Oct", "Nov", "Dec"};

    /// `2026-10-01` as "Oct 1".
    static String shortDate(String date) {
        if (date == null || date.length() < 10) {
            return date == null ? "" : date;
        }
        int month = (date.charAt(5) - '0') * 10 + date.charAt(6) - '0';
        int day = (date.charAt(8) - '0') * 10 + date.charAt(9) - '0';
        return month < 1 || month > 12 ? date : Lang.tr(MONTHS[month - 1]) + " " + day;
    }

    /// The most booked kinds of class, ranked, each with a bar for its share
    /// of the most booked one: easier to read on a phone than a pie, and no
    /// label falls off the edge.
    public static Component popular(List<NameCountDto> classes) {
        Container list = new Container(BoxLayout.y());
        list.setName("popularChart");
        int most = 1;
        for (int iter = 0; iter < classes.size(); iter++) {
            most = Math.max(most, classes.get(iter).count());
        }
        for (int iter = 0; iter < classes.size(); iter++) {
            NameCountDto row = classes.get(iter);
            Container line = new Container(new BorderLayout(), "SvRankRow");
            line.add(BorderLayout.CENTER, Ui.plain(row.name(), "SvRowTitle"));
            line.add(BorderLayout.EAST, Ui.plain(Lang.tr("{0} booked",
                    Integer.valueOf(row.count())), "SvRowDetail"));
            list.add(line);
            list.add(new Share(row.count() / (double) most,
                    PALETTE[iter % PALETTE.length]));
        }
        return list;
    }

    /// A rounded bar filled to `share` of the width.
    private static final class Share extends Component {
        private final double share;
        private final int color;

        Share(double share, int color) {
            this.share = share;
            this.color = color;
            setUIID("SvRankBar");
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(30),
                    CN.convertToPixels(1.6f) + getStyle().getVerticalPadding());
        }

        @Override
        public void paint(Graphics g) {
            int x = getX() + getStyle().getPaddingLeftNoRTL();
            int y = getY() + getStyle().getPaddingTop();
            int w = getWidth() - getStyle().getHorizontalPadding();
            int h = getHeight() - getStyle().getVerticalPadding();
            g.setAntiAliased(true);
            g.setColor(getStyle().getFgColor());
            g.fillRoundRect(x, y, w, h, h, h);
            g.setColor(color);
            g.fillRoundRect(x, y, Math.max(h, (int) (w * share)), h, h, h);
        }
    }
}
