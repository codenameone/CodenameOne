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
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.fieldline.api.RateDto;
import com.codenameone.examples.fieldline.api.TimeEntryDto;
import com.codenameone.examples.fieldline.api.TimesheetDto;
import com.codenameone.examples.fieldline.api.TimesheetRowDto;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Layouts;
import com.codenameone.examples.fieldline.ui.StatRow;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.List;

/// What the company pays and what the crew clocked: the labour rate table
/// and the weekly timesheets costed at it. Admin only, like the rules.
final class Labour {
    private static final long WEEK = 7L * 24 * 3600000L;

    private Labour() {
    }

    // ---- Rates --------------------------------------------------------------

    /// The labour rates, cheapest first, and a way to add one.
    static Component rates(final Host host) {
        final Container list = Ui.list("rates");
        Api.admin().rates(Net.to(rates -> fillRates(host, list, rates), Ui::fail));
        Container page = new Container(BoxLayout.y());
        page.add(Ui.secondary("Add a rate", "addRate", e -> host.detail(Lang.tr("New rate"), "Rate",
                rate(host, new RateDto("", "", 4000, 150, 200, 6000, 0)))));
        return BackOffice.listed(page, list);
    }

    private static void fillRates(final Host host, Container list, List<RateDto> rates) {
        list.removeAll();
        for (final RateDto r : rates) {
            final Container row = Ui.row(FontImage.MATERIAL_PAYMENTS, r.name(),
                    Lang.tr("{0} an hour", money(r.hourlyCents())) + Ui.DOT
                    + Lang.tr("overtime {0}%", String.valueOf(r.overtimePct())) + Ui.DOT
                    + Lang.tr("weekend {0}%", String.valueOf(r.weekendPct())),
                    Ui.badge(Lang.tr("{0} people", String.valueOf(r.members())), "FlBadge"));
            list.add(Ui.tap(row, "rate-" + r.id(), e -> {
                host.picked(row);
                host.detail(r.name(), "Rate", rate(host, r));
            }));
        }
        Layouts.dress(list);
        list.revalidate();
    }

    private static Component rate(final Host host, final RateDto r) {
        Container page = Ui.page();
        page.add(Ui.text("What an hour costs, and the uplift on it. Overtime is any hour past a "
                + "technician's regular week; weekend hours are paid at the weekend rate instead. "
                + "The call-out fee is added for every urgent job finished.", "WlMuted"));
        final TextField name = Ui.field(page, "Name", "rateName", "");
        name.setText(r.name());
        final TextField hourly = Ui.field(page, "An hour, 42.00", "rateHourly", "", TextArea.ANY);
        hourly.setText(plain(r.hourlyCents()));
        final TextField overtime = Ui.field(page, "Overtime, percent of the hourly rate", "rateOvertime",
                "", TextArea.NUMERIC);
        overtime.setText(String.valueOf(r.overtimePct()));
        final TextField weekend = Ui.field(page, "Weekend, percent of the hourly rate", "rateWeekend",
                "", TextArea.NUMERIC);
        weekend.setText(String.valueOf(r.weekendPct()));
        final TextField callout = Ui.field(page, "Urgent call-out fee, 65.00", "rateCallout", "",
                TextArea.ANY);
        callout.setText(plain(r.calloutCents()));
        page.add(Ui.primary("Save", "saveRate", e -> save(host, new RateDto(r.id(),
                name.getText(), cents(hourly), number(overtime), number(weekend), cents(callout),
                r.members()))));
        if (r.id().length() > 0) {
            page.add(Ui.danger("Remove", "removeRate", e -> save(host,
                    new RateDto(r.id(), "", 0, 0, 0, 0, 0))));
        }
        return page;
    }

    private static void save(final Host host, RateDto rate) {
        Api.admin().saveRate(rate, Net.to(all -> {
            Ui.say(Lang.tr("Rates saved"));
            host.reload();
        }, Ui::fail));
    }

    // ---- Timesheets ---------------------------------------------------------

    /// Everyone's clocked week against their rostered one, costed.
    static Component timesheets(final Host host) {
        final Container list = Ui.list("timesheets");
        final Container top = new Container(BoxLayout.y());
        final long[] week = {0};
        Container weeks = Ui.choice("week", new String[] {Lang.tr("Last week"), Lang.tr("This week")},
                new String[] {"last", "this"}, "last", v -> {
                    week[0] = "last".equals(v) ? System.currentTimeMillis() - WEEK : 0;
                    load(host, top, list, week[0]);
                });
        BoardForm.segments(weeks);
        Container head = new Container(BoxLayout.y());
        head.add(weeks);
        head.add(top);
        week[0] = System.currentTimeMillis() - WEEK;
        load(host, top, list, week[0]);
        return BackOffice.listed(head, list);
    }

    private static void load(final Host host, final Container top, final Container list, long week) {
        Api.admin().timesheets(week, Net.to(sheet -> {
            fill(host, top, list, sheet);
            if (list.getComponentForm() != null) {
                list.getComponentForm().revalidate();
            }
        }, Ui::fail));
    }

    static void fill(final Host host, Container top, Container list, TimesheetDto sheet) {
        top.removeAll();
        list.removeAll();
        int worked = 0;
        int planned = 0;
        int overtime = 0;
        for (TimesheetRowDto r : sheet.rows()) {
            worked += r.shiftMinutes() - r.breakMinutes();
            planned += r.plannedMinutes();
            overtime += r.overtimeMinutes();
        }
        StatRow stats = Ui.stats(4);
        stats.stat(BackOffice.hours(worked), Lang.tr("Hours worked"), "stat-worked");
        stats.stat(BackOffice.hours(planned), Lang.tr("Hours rostered"), "stat-rostered");
        stats.stat(BackOffice.hours(overtime), Lang.tr("Overtime"), "stat-overtime");
        stats.stat(money(sheet.totalCostCents()), Lang.tr("Labour cost"), "stat-cost");
        top.add(stats);
        top.add(Ui.label(Lang.tr("Week of {0}", Ui.day(sheet.weekStart())), "WlMuted"));
        for (final TimesheetRowDto r : sheet.rows()) {
            int paid = r.shiftMinutes() - r.breakMinutes();
            String detail = Lang.tr("{0} worked", BackOffice.hours(paid)) + Ui.DOT
                    + Lang.tr("{0} rostered", BackOffice.hours(r.plannedMinutes()))
                    + (r.overtimeMinutes() > 0 ? Ui.DOT + Lang.tr("{0} overtime",
                            BackOffice.hours(r.overtimeMinutes())) : "");
            final Container row = Ui.row(FontImage.MATERIAL_PUNCH_CLOCK, r.name(), detail,
                    Ui.badge(money(r.costCents()), r.overtimeMinutes() > 0 ? "FlBadgeWarn" : "FlBadge"));
            list.add(Ui.tap(row, "sheet-" + r.techId(), e -> {
                host.picked(row);
                host.detail(r.name(), "Timesheet", sheet(r));
            }));
        }
        if (sheet.rows().isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_PUNCH_CLOCK, "Nothing clocked",
                    "Nobody has clocked in this week yet."));
        }
        Layouts.dress(list);
    }

    private static Component sheet(TimesheetRowDto r) {
        Container page = Ui.page();
        int paid = r.shiftMinutes() - r.breakMinutes();
        int pct = r.plannedMinutes() <= 0 ? 0 : paid * 100 / r.plannedMinutes();
        page.add(new Reports.Bar(Lang.tr("Worked against rostered"), Math.min(pct, 150), 80, 110,
                BackOffice.hours(paid) + " / " + BackOffice.hours(r.plannedMinutes()), "sheetLoad"));
        Ui.section(page, "Week");
        page.add(Ui.fact("Rate", r.rateName().length() == 0 ? Lang.tr("None set") : r.rateName()));
        page.add(Ui.fact("On shift", BackOffice.hours(r.shiftMinutes())));
        page.add(Ui.fact("Unpaid breaks", BackOffice.hours(r.breakMinutes())));
        page.add(Ui.fact("Driving", BackOffice.hours(r.travelMinutes())));
        page.add(Ui.fact("On site", BackOffice.hours(r.jobMinutes())));
        page.add(Ui.fact("Overtime", BackOffice.hours(r.overtimeMinutes()),
                r.overtimeMinutes() > 0 ? "FlFactBad" : null));
        page.add(Ui.fact("Weekend", BackOffice.hours(r.weekendMinutes())));
        page.add(Ui.fact("Urgent call-outs", String.valueOf(r.callouts())));
        page.add(Ui.fact("Cost", money(r.costCents())));
        Ui.section(page, "Shifts");
        for (TimeEntryDto e : r.entries()) {
            if (!"SHIFT".equals(e.kind())) {
                continue;
            }
            int minutes = e.end() <= 0 ? -1 : (int) ((e.end() - e.start()) / 60000L);
            page.add(Ui.row(FontImage.MATERIAL_SCHEDULE, Ui.weekday(e.start()),
                    Ui.clock(e.start()) + " - " + (e.end() <= 0 ? Lang.tr("on the clock") : Ui.clock(e.end())),
                    minutes < 0 ? Ui.badge(Lang.tr("Open"), "FlBadgeWarn")
                            : Ui.badge(BackOffice.hours(minutes), "FlBadge")));
        }
        return page;
    }

    // ---- Money --------------------------------------------------------------

    /// Cents as dollars, with thousands grouped: $1,234 or $42.50.
    static String money(long cents) {
        long whole = cents / 100;
        int part = (int) (Math.abs(cents) % 100);
        StringBuilder digits = new StringBuilder(String.valueOf(Math.abs(whole)));
        for (int at = digits.length() - 3; at > 0; at -= 3) {
            digits.insert(at, ',');
        }
        String sign = cents < 0 ? "-" : "";
        return sign + "$" + digits + (whole >= 1000 || part == 0 ? "" : "." + (part < 10 ? "0" : "") + part);
    }

    private static String plain(int cents) {
        int part = cents % 100;
        return (cents / 100) + "." + (part < 10 ? "0" : "") + part;
    }

    /// 42, 42.5 or 42.50 as cents; anything else is 0, which the server refuses.
    private static int cents(TextField field) {
        String text = field.getText() == null ? "" : field.getText().trim();
        int dot = text.indexOf('.');
        String whole = dot < 0 ? text : text.substring(0, dot);
        String frac = dot < 0 ? "" : text.substring(dot + 1);
        if (whole.length() == 0 || whole.length() > 6 || frac.length() > 2) {
            return 0;
        }
        int value = 0;
        String all = whole + (frac.length() == 1 ? frac + "0" : frac.length() == 0 ? "00" : frac);
        for (int iter = 0; iter < all.length(); iter++) {
            char ch = all.charAt(iter);
            if (ch < '0' || ch > '9') {
                return 0;
            }
            value = value * 10 + (ch - '0');
        }
        return value;
    }

    private static int number(TextField field) {
        String text = field.getText() == null ? "" : field.getText().trim();
        int value = 0;
        if (text.length() == 0 || text.length() > 4) {
            return 0;
        }
        for (int iter = 0; iter < text.length(); iter++) {
            char ch = text.charAt(iter);
            if (ch < '0' || ch > '9') {
                return 0;
            }
            value = value * 10 + (ch - '0');
        }
        return value;
    }
}
