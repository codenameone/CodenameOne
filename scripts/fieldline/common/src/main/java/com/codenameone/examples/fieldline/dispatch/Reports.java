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
package com.codenameone.examples.fieldline.dispatch;

import com.codename1.appkit.Net;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.fieldline.api.DayLoadDto;
import com.codenameone.examples.fieldline.api.ReasonCountDto;
import com.codenameone.examples.fieldline.api.SkillLoadDto;
import com.codenameone.examples.fieldline.api.StaffingDto;
import com.codenameone.examples.fieldline.api.TechLoadDto;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Layouts;
import com.codenameone.examples.fieldline.ui.StatRow;
import com.codenameone.examples.fieldline.ui.Ui;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/// Is the crew the right size for the work? The server re-plans the next one
/// or two weeks with the same optimizer as the board and reports, per day,
/// what the crew can give against what is booked; per technician, how full
/// their days are; per skill, whether enough people hold it; and why the work
/// that found nobody was left.
///
/// A day is *understaffed* when the booked work and the driving to it take
/// more than the policy's high mark of the hours on hand, and *overstaffed*
/// when they take less than its low mark: people paid to wait.
final class Reports {
    private Reports() {
    }

    static Component page() {
        final Container page = Ui.page();
        page.setName("reports");
        final Container body = new Container(BoxLayout.y());
        page.add(Ui.choice("span", new String[] {Lang.tr("This week"), Lang.tr("Two weeks")},
                new String[] {"7", "14"}, "14", value -> load(body, "7".equals(value) ? 7 : 14)));
        page.add(body);
        load(body, 14);
        return page;
    }

    private static void load(final Container body, int days) {
        body.removeAll();
        body.add(Ui.text("Planning the coming days...", "WlMuted"));
        Ui.relayout(body);
        Api.dispatch().staffing(days, Net.to(report -> {
            body.removeAll();
            fill(body, report);
            Layouts.dress(body);
            Component top = body.getComponentForm();
            if (top != null) {
                Ui.relayout(body);
            }
        }, Ui::fail));
    }

    static void fill(Container body, StaffingDto r) {
        List<DayLoadDto> days = new ArrayList<DayLoadDto>();
        int short1 = 0;
        int idle = 0;
        int fullest = 0;
        for (DayLoadDto d : r.days()) {
            fullest = Math.max(fullest, d.capacityMinutes());
        }
        for (DayLoadDto d : r.days()) {
            if (d.capacityMinutes() * 5 < fullest) {
                // A weekend, or the one Saturday morning someone works: not a
                // day the crew is sized for.
                continue;
            }
            days.add(d);
            int pct = load(d);
            if (pct > r.highPct()) {
                short1++;
            } else if (pct < r.lowPct()) {
                idle++;
            }
        }
        StatRow stats = Ui.stats(4);
        stats.stat(String.valueOf(short1), Lang.tr("Understaffed days"), "stat-short");
        stats.stat(String.valueOf(idle), Lang.tr("Overstaffed days"), "stat-idle");
        stats.stat(r.jobsDue() == 0 ? "-" : (r.onTime() * 100 / r.jobsDue()) + "%",
                Lang.tr("On time"), "stat-ontime");
        stats.stat(String.valueOf(r.unassigned()), Lang.tr("Unassigned"), "stat-open");
        body.add(stats);
        if (r.plannedCostCents() > 0) {
            body.add(Ui.fact("Planned labour cost", Labour.money(r.plannedCostCents())));
        }

        Ui.section(body, "Crew load per day");
        body.add(Ui.text(Lang.tr("Booked work and driving against the hours on hand. Above "
                + "{0}% the day is understaffed, below {1}% people wait for work.",
                String.valueOf(r.highPct()), String.valueOf(r.lowPct())), "WlMuted"));
        body.add(new DayChart(days, r.lowPct(), r.highPct()));
        body.add(legend());
        if (r.days().size() > 7) {
            body.add(Ui.text("Next week is still being booked: its low days fill as calls come "
                    + "in, so read them as room to take work, not people to send home.", "WlMuted"));
        }

        Ui.section(body, "Utilization per technician");
        List<TechLoadDto> techs = new ArrayList<TechLoadDto>(r.technicians());
        sortTechs(techs);
        for (TechLoadDto t : techs) {
            int used = t.workMinutes() + t.driveMinutes();
            int pct = t.availableMinutes() <= 0 ? 0 : used * 100 / t.availableMinutes();
            body.add(new Bar(t.name(), pct, r.lowPct(), r.highPct(),
                    Lang.count(t.jobs(), "1 job", "{0} jobs") + Ui.DOT
                    + Lang.tr("{0} driving", Ui.duration(t.driveMinutes()))
                    + (t.overtimeMinutes() > 0 ? Ui.DOT + Lang.tr("{0} overtime",
                            Ui.duration(t.overtimeMinutes())) : ""), "tech-" + t.id()));
        }

        Ui.section(body, "Skill coverage");
        body.add(Ui.text("The work each trade has booked against the hours of everyone who "
                + "holds it. A trade near the top needs another person or overtime.", "WlMuted"));
        for (SkillLoadDto s : r.skills()) {
            int pct = s.capacityMinutes() <= 0 ? 100 : s.demandMinutes() * 100 / s.capacityMinutes();
            body.add(new Bar(s.skill(), pct, -1, r.highPct(),
                    Lang.count(s.technicians(), "1 person", "{0} people") + Ui.DOT
                    + Lang.tr("{0} booked", Ui.duration(s.demandMinutes())), "skill-" + s.skill()));
        }

        Ui.section(body, "Why work is unassigned");
        if (r.reasons().isEmpty()) {
            body.add(Ui.row(FontImage.MATERIAL_TASK_ALT, Lang.tr("Everything has a technician"),
                    Lang.tr("{0} of {1} jobs on time", String.valueOf(r.onTime()),
                            String.valueOf(r.jobsDue())), null));
        }
        for (ReasonCountDto reason : r.reasons()) {
            body.add(Ui.row(icon(reason.code()), Lang.tr(reason.text()), reason.code(),
                    Ui.badge(String.valueOf(reason.jobs()), "FlBadgeWarn")));
        }
        if (r.late() > 0) {
            body.add(Ui.row(FontImage.MATERIAL_SCHEDULE, Lang.tr("Planned past their SLA"),
                    Lang.tr("Placed, but later than the policy promises"),
                    Ui.badge(String.valueOf(r.late()), "FlBadgeUrgent")));
        }
    }

    /// The share of a day's hours the booked work and its driving take.
    static int load(DayLoadDto d) {
        return d.capacityMinutes() <= 0 ? 0
                : (d.demandMinutes() + d.driveMinutes()) * 100 / d.capacityMinutes();
    }

    private static void sortTechs(List<TechLoadDto> techs) {
        // Busiest first; a short list, so a simple insertion sort.
        for (int i = 1; i < techs.size(); i++) {
            TechLoadDto t = techs.get(i);
            int j = i - 1;
            while (j >= 0 && share(techs.get(j)) < share(t)) {
                techs.set(j + 1, techs.get(j));
                j--;
            }
            techs.set(j + 1, t);
        }
    }

    private static int share(TechLoadDto t) {
        return t.availableMinutes() <= 0 ? 0
                : (t.workMinutes() + t.driveMinutes()) * 1000 / t.availableMinutes();
    }

    private static char icon(String code) {
        if ("CAPACITY".equals(code)) {
            return FontImage.MATERIAL_GROUP_ADD;
        }
        if ("SKILL".equals(code) || "CERTIFICATION".equals(code)) {
            return FontImage.MATERIAL_WORKSPACE_PREMIUM;
        }
        if ("WINDOW".equals(code)) {
            return FontImage.MATERIAL_SCHEDULE;
        }
        if ("TERRITORY".equals(code)) {
            return FontImage.MATERIAL_MAP;
        }
        return FontImage.MATERIAL_HELP_OUTLINE;
    }

    private static Container legend() {
        Container row = new Container(BoxLayout.x());
        row.setUIID("FlChartLegend");
        row.add(key("FlChartShort", "Understaffed"));
        row.add(key("FlChartFine", "Right size"));
        row.add(key("FlChartIdle", "Overstaffed"));
        return row;
    }

    private static Component key(String uiid, String text) {
        Container key = new Container(new BorderLayout());
        Component swatch = new Component() {
            @Override
            protected Dimension calcPreferredSize() {
                int side = CN.convertToPixels(2.4f);
                return new Dimension(side, side);
            }

            @Override
            public void paint(Graphics g) {
                g.setColor(color(uiid));
                int side = Math.min(getWidth(), getHeight());
                g.fillRoundRect(getX(), getY() + (getHeight() - side) / 2, side, side, side / 3,
                        side / 3);
            }
        };
        key.add(BorderLayout.WEST, swatch);
        key.add(BorderLayout.CENTER, Ui.label(text, "FlChartKey"));
        return key;
    }

    static int color(String uiid) {
        return UIManager.getInstance().getComponentStyle(uiid).getFgColor();
    }

    static String band(int pct, int low, int high) {
        return pct > high ? "FlChartShort" : pct < low ? "FlChartIdle" : "FlChartFine";
    }

    /// A column a working day: the load as a bar against the full day, with the
    /// low and high marks drawn across.
    static final class DayChart extends Component {
        private final List<DayLoadDto> days;
        private final int low;
        private final int high;

        DayChart(List<DayLoadDto> days, int low, int high) {
            this.days = days;
            this.low = low;
            this.high = high;
            setUIID("FlChart");
            setName("dayChart");
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(CN.convertToPixels(80), CN.convertToPixels(44));
        }

        @Override
        public void paint(Graphics g) {
            Style s = getStyle();
            Font small = UIManager.getInstance().getComponentStyle("FlChartAxis").getFont();
            int x0 = getX() + s.getPaddingLeftNoRTL();
            int w = getWidth() - s.getHorizontalPadding();
            int top = getY() + s.getPaddingTop() + small.getHeight() + 2;
            int bottom = getY() + getHeight() - s.getPaddingBottom() - small.getHeight() - 4;
            int n = Math.max(1, days.size());
            // The scale reaches 120%: a day past full still has room to show by how much.
            int span = bottom - top;
            int slot = w / n;
            int bar = Math.max(4, slot * 3 / 5);
            g.setFont(small);
            int axis = color("FlChartAxis");
            int track = color("FlChartTrack");
            SimpleDateFormat dayName = new SimpleDateFormat("EEE d");
            for (int i = 0; i < days.size(); i++) {
                DayLoadDto d = days.get(i);
                int pct = load(d);
                int bx = x0 + i * slot + (slot - bar) / 2;
                g.setColor(track);
                int fullY = bottom - span * 100 / 120;
                g.fillRoundRect(bx, fullY, bar, bottom - fullY, 6, 6);
                int h = span * Math.min(pct, 120) / 120;
                g.setColor(color(band(pct, low, high)));
                g.fillRoundRect(bx, bottom - h, bar, h, 6, 6);
                g.setColor(axis);
                String value = pct + "%";
                g.drawString(value, bx + (bar - small.stringWidth(value)) / 2,
                        bottom - h - small.getHeight() - 1);
                String label = dayName.format(new Date(d.day()));
                g.drawString(label, x0 + i * slot + (slot - small.stringWidth(label)) / 2,
                        bottom + 3);
            }
            mark(g, x0, w, bottom - span * high / 120, color("FlChartShort"));
            mark(g, x0, w, bottom - span * low / 120, color("FlChartIdle"));
        }

        private static void mark(Graphics g, int x, int w, int y, int rgb) {
            g.setColor(rgb);
            int dash = CN.convertToPixels(1.2f);
            for (int at = x; at < x + w; at += dash * 2) {
                g.drawLine(at, y, Math.min(x + w, at + dash), y);
            }
        }
    }

    /// One row of a horizontal bar chart: a name, the share as a bar and its
    /// figure, and what it is made of under it.
    static final class Bar extends Container {
        Bar(String name, final int pct, final int low, final int high, String detail, String id) {
            super(new BorderLayout());
            setUIID("FlChartRow");
            setName(id);
            Container head = new Container(new BorderLayout());
            head.add(BorderLayout.CENTER, Ui.label(name, "FlChartName"));
            head.add(BorderLayout.EAST, Ui.label(pct + "%", "FlChartFigure"));
            Component fill = new Component() {
                @Override
                protected Dimension calcPreferredSize() {
                    return new Dimension(CN.convertToPixels(40), CN.convertToPixels(2.2f));
                }

                @Override
                public void paint(Graphics g) {
                    int r = getHeight();
                    g.setColor(color("FlChartTrack"));
                    g.fillRoundRect(getX(), getY(), getWidth(), getHeight(), r, r);
                    int w = getWidth() * Math.min(100, Math.max(0, pct)) / 100;
                    g.setColor(color(band(pct, low < 0 ? -1 : low, high)));
                    if (w > 0) {
                        g.fillRoundRect(getX(), getY(), Math.max(r, w), getHeight(), r, r);
                    }
                }
            };
            fill.setUIID("FlChartBar");
            Container column = new Container(BoxLayout.y());
            column.add(head).add(fill).add(Ui.label(detail, "FlChartDetail"));
            add(BorderLayout.CENTER, column);
        }
    }
}
