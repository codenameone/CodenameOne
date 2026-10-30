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
import com.codename1.charts.models.CategorySeries;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.views.BarChart;
import com.codename1.charts.views.PieChart;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
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
        stats.stat(Ui.money(board.mrrCents(), null), "Monthly revenue", "mrr")
                .stat(String.valueOf(board.activeMembers()), "Members", "members")
                .stat(board.utilisation() + "%", "Places taken", "utilisation")
                .stat(String.valueOf(board.atRisk()), "At risk", "atRisk");
        page.add(stats);
        page.add(Ui.text(Lang.tr("{0} joined and {1} left in 30 days; {2} check-ins this week, "
                + "{3} invoices past due.", Integer.valueOf(board.newMembers30()),
                Integer.valueOf(board.churned30()), Integer.valueOf(board.checkIns7()),
                Integer.valueOf(board.pastDue())), "SvMuted"));

        Ui.section(page, "Revenue per day");
        page.add(Ui.text(Lang.tr("{0} in the last 30 days", Ui.money(board.revenue30Cents(), null)),
                "SvMuted"));
        page.add(sized(revenue(board.revenueByDay()), "revenueChart"));

        Ui.section(page, "Popular classes");
        if (board.popularClasses() == null || board.popularClasses().isEmpty()) {
            page.add(Ui.text("No bookings in the last month.", "SvMuted"));
        } else {
            page.add(sized(popular(board.popularClasses()), "popularChart"));
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
            page.add(Ui.secondary(Lang.tr("Win back {0} lapsing members",
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
                renderer.addXTextLabel(iter + 1, date != null && date.length() >= 10
                        ? date.substring(5) : date);
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
        renderer.setMarginsColor(0);
        renderer.setApplyBackgroundColor(false);
        renderer.setLabelsColor(0xff7a7a7a);
        renderer.setXLabelsColor(0xff7a7a7a);
        renderer.setYLabelsColor(0, 0xff7a7a7a);
        renderer.setAxesColor(0xffbdbdbd);
        renderer.setPanEnabled(false, false);
        renderer.setZoomEnabled(false, false);
        return renderer;
    }

    /// The share of bookings of the most booked kinds of class.
    static Component popular(List<NameCountDto> classes) {
        CategorySeries series = new CategorySeries(Lang.tr("Bookings"));
        DefaultRenderer renderer = new DefaultRenderer();
        for (int iter = 0; iter < classes.size(); iter++) {
            series.add(classes.get(iter).name(), classes.get(iter).count());
            SimpleSeriesRenderer slice = new SimpleSeriesRenderer();
            slice.setColor(PALETTE[iter % PALETTE.length] | 0xff000000);
            renderer.addSeriesRenderer(slice);
        }
        renderer.setShowLegend(true);
        renderer.setLabelsTextSize(CN.convertToPixels(2.2f));
        renderer.setLegendTextSize(CN.convertToPixels(2.2f));
        renderer.setLabelsColor(0xff7a7a7a);
        renderer.setPanEnabled(false);
        renderer.setZoomEnabled(false);
        return new ChartComponent(new PieChart(series, renderer));
    }
}
