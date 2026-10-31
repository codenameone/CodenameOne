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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.api.CoachStatDto;
import com.codenameone.examples.pulse.api.InsightsDto;
import com.codenameone.examples.pulse.api.PlanMixDto;
import com.codenameone.examples.pulse.api.TypeFillDto;
import com.codenameone.examples.pulse.api.WeekPointDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Skeleton;
import com.codenameone.examples.pulse.ui.StatRow;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// Twelve weeks of the studio, for the owner: when people come, whether the
/// membership grows, how each kind of class fills, what the plans bring in
/// and how each coach's month went. Everything is the server's arithmetic;
/// this screen only draws it.
public final class InsightsForm {
    private InsightsForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Insights", "OwnerInsights");
        Ui.back(form, previous);
        final Container page = Ui.page();
        page.setName("insights");
        page.add(new Skeleton(6));
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.owner().insights(Net.to(found -> fill(form, page, found),
                Ui.failed(page, load[0])));
        Ui.pull(page, load[0]);
        form.add(BorderLayout.CENTER, page);
        form.show();
        load[0].run();
    }

    private static void fill(Form form, Container page, InsightsDto insights) {
        page.removeAll();
        StatRow tiles = Ui.stats(2);
        tiles.stat(insights.retention90() + "%", "Still with us after 90 days", "retention")
                .stat(Ui.tenths(insights.visitsPerMemberTenths() / 10.0), "Visits a member a week",
                        "visitsPerMember")
                .stat(insights.noShowPercent() + "%", "No-shows", "noShows")
                .stat(Ui.moneyShort(insights.revenuePerMemberCents(), null), "Revenue a member",
                        "revenuePerMember");
        page.add(tiles);

        Ui.section(page, "Busy hours");
        page.add(Ui.text("Check-ins over the last eight weeks.", "SvMuted"));
        page.add(new Graphs.Heatmap(insights.firstHour(), insights.lastHour(),
                insights.busyHours()));

        Ui.section(page, "Membership");
        membership(page, insights.weeks());

        Ui.section(page, "Classes");
        classes(page, insights.classes());

        Ui.section(page, "Plan mix");
        plans(page, insights.plans());

        Ui.section(page, "Coaches");
        coaches(page, insights.coaches());
        Ui.refresh(form);
    }

    private static void membership(Container page, List<WeekPointDto> weeks) {
        if (weeks == null || weeks.isEmpty()) {
            page.add(Ui.text("No members yet.", "SvMuted"));
            return;
        }
        WeekPointDto now = weeks.get(weeks.size() - 1);
        page.add(Ui.plain(String.valueOf(now.active()), "SvChartValue"));
        int change = now.active() - weeks.get(0).active();
        page.add(Ui.text(change >= 0
                ? Lang.tr("{0} more than twelve weeks ago", Integer.valueOf(change))
                : Lang.tr("{0} fewer than twelve weeks ago", Integer.valueOf(-change)), "SvMuted"));
        page.add(new Graphs.Membership(weeks));
        Container axis = new Container(new BorderLayout());
        axis.add(BorderLayout.WEST, Ui.plain(OwnerForm.shortDate(weeks.get(0).weekOf()),
                "SvChartAxis"));
        axis.add(BorderLayout.EAST, Ui.label("This week", "SvChartAxis"));
        page.add(axis);
        page.add(FlowLayout.encloseIn(Ui.label("Active", "SvLegendActive"),
                Ui.label("Joined", "SvLegendJoined"), Ui.label("Left", "SvLegendLeft")));
    }

    private static void classes(Container page, List<TypeFillDto> types) {
        if (types == null || types.isEmpty()) {
            page.add(Ui.text("No classes in the last four weeks.", "SvMuted"));
            return;
        }
        Container list = new Container(BoxLayout.y());
        list.setName("classFill");
        for (int iter = 0; iter < types.size(); iter++) {
            TypeFillDto type = types.get(iter);
            Container line = new Container(new BorderLayout(), "SvRankRow");
            line.add(BorderLayout.CENTER, Ui.plain(type.name(), "SvRowTitle"));
            line.add(BorderLayout.EAST, Ui.plain(Lang.tr("{0}% full, {1}% no-show",
                    Integer.valueOf(type.fillPercent()), Integer.valueOf(type.noShowPercent())),
                    "SvRowDetail"));
            list.add(line);
            Graphs.Bar bar = new Graphs.Bar(type.fillPercent() / 100.0,
                    Graphs.color(type.color(), Graphs.PALETTE[iter % Graphs.PALETTE.length]));
            bar.setAccessibilityText(Lang.tr("{0}: {1}% full", type.name(),
                    Integer.valueOf(type.fillPercent())));
            list.add(bar);
        }
        page.add(list);
    }

    private static void plans(Container page, List<PlanMixDto> plans) {
        if (plans == null || plans.isEmpty()) {
            page.add(Ui.text("No plans sold yet.", "SvMuted"));
            return;
        }
        int[] members = new int[plans.size()];
        StringBuilder spoken = new StringBuilder();
        for (int iter = 0; iter < members.length; iter++) {
            members[iter] = plans.get(iter).members();
            if (iter > 0) {
                spoken.append(", ");
            }
            spoken.append(Lang.tr("{0}: {1} members", plans.get(iter).name(),
                    Integer.valueOf(members[iter])));
        }
        page.add(FlowLayout.encloseCenter(new Graphs.Donut(members, spoken.toString())));
        for (int iter = 0; iter < members.length; iter++) {
            PlanMixDto plan = plans.get(iter);
            Container row = new Container(new BorderLayout(), "SvLegendRow");
            row.setName("plan-" + iter);
            row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(
                    new Graphs.Swatch(Graphs.PALETTE[iter % Graphs.PALETTE.length])));
            Container lines = new Container(BoxLayout.y());
            lines.add(Ui.plain(plan.name(), "SvRowTitle"));
            lines.add(Ui.plain(Lang.tr("{0} members", Integer.valueOf(plan.members())),
                    "SvRowDetail"));
            row.add(BorderLayout.CENTER, lines);
            row.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(Ui.plain(
                    // A pack is paid once, so it brings in nothing a month;
                    // the rest round to whole units, as a legend reads.
                    plan.monthlyCents() == 0 ? Lang.tr("Paid per pack")
                    : Lang.tr("{0} a month", Ui.moneyShort(
                            (plan.monthlyCents() + 50) / 100 * 100, null)),
                    "SvRowAmount")));
            page.add(row);
        }
    }

    private static void coaches(Container page, List<CoachStatDto> coaches) {
        if (coaches == null || coaches.isEmpty()) {
            page.add(Ui.text("No classes taught in the last four weeks.", "SvMuted"));
            return;
        }
        for (int iter = 0; iter < coaches.size(); iter++) {
            CoachStatDto coach = coaches.get(iter);
            Container row = MembersForm.person(coach.name(),
                    Lang.tr("{0} classes, {1}% full, {2}% show up",
                    Integer.valueOf(coach.classes()), Integer.valueOf(coach.fillPercent()),
                    Integer.valueOf(coach.showPercent())),
                    Ui.badge(Lang.tr("{0} kudos", Integer.valueOf(coach.kudos())), "SvBadgeGood"));
            row.setName("coach-" + iter);
            page.add(row);
        }
    }
}
