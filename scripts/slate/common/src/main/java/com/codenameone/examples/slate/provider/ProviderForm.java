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
package com.codenameone.examples.slate.provider;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.BusyDto;
import com.codenameone.examples.slate.api.CancelDto;
import com.codenameone.examples.slate.api.ScheduleDto;
import com.codenameone.examples.slate.api.ShiftDto;
import com.codenameone.examples.slate.api.UserDto;
import com.codenameone.examples.slate.client.Appointments;
import com.codenameone.examples.slate.client.Times;
import com.codenameone.examples.slate.live.LiveChannel;
import com.codenameone.examples.slate.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.slate.net.Session;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Timeline;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Menu;
import com.codenameone.examples.slate.ui.Nav;
import com.codenameone.examples.slate.ui.SettingsForm;
import com.codenameone.examples.slate.ui.Ui;

import java.util.ArrayList;
import java.util.List;

// tag::slate-provider-timeline[]
/// The provider's home: their own time, by the day, the week or as an
/// agenda, with every appointment, every block a connected calendar says is
/// busy and the hours they work, merged into one timeline.
///
/// One request answers all of it (`/api/provider/schedule`), and the live
/// channel's `schedule` frame reloads it when a client books, moves or
/// cancels, so nothing here polls.
public class ProviderForm extends LiveChannel.Adapter implements Nav.Home {
    private static final String DAY = "day";
    private static final String WEEK = "week";
    private static final String AGENDA = "agenda";

    private final Form form = Ui.home("Provider");
    private final Menu menu = new Menu(form, Nav.PROVIDER);
    private final Container timeline = Ui.list("timeline");
    private final Container heading = new Container(new BorderLayout());
    private final Label title = Ui.label("", "SvHeading");
    private String view = DAY;
    private long from = Times.dayStart(System.currentTimeMillis());

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        menu.item(FontImage.MATERIAL_TODAY, "Schedule", "schedule", () -> form.showBack());
        menu.item(FontImage.MATERIAL_SCHEDULE, "Working hours", "hours", () -> HoursForm.show(form));
        menu.item(FontImage.MATERIAL_BEACH_ACCESS, "Time off", "time-off",
                () -> TimeOffForm.show(form));
        menu.item(FontImage.MATERIAL_SYNC, "Calendars", "calendars", () -> CalendarsForm.show(form));

        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, menu.flatButton());
        UserDto user = Session.user();
        bar.add(BorderLayout.CENTER, Ui.label(user == null ? "" : user.displayName(), "SvTitle"));
        bar.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_SETTINGS, "Settings", "home-settings",
                e -> SettingsForm.show(form)));

        Container top = new Container(BoxLayout.y());
        top.add(bar);
        top.add(Ui.choice("view", new String[] {"Day", "Week", "Agenda"},
                new String[] {DAY, WEEK, AGENDA}, view, chosen -> {
                    view = chosen;
                    from = WEEK.equals(view) ? Times.weekStart(from) : from;
                    load();
                }));
        heading.add(BorderLayout.WEST, Ui.flat(FontImage.MATERIAL_CHEVRON_LEFT, "Earlier",
                "earlier", e -> move(-1)));
        heading.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_CHEVRON_RIGHT, "Later",
                "later", e -> move(1)));
        title.setName("range");
        heading.add(BorderLayout.CENTER, title);
        top.add(heading);

        Container shell = new Container(new BorderLayout());
        shell.add(BorderLayout.NORTH, top);
        shell.add(BorderLayout.CENTER, timeline);
        Ui.pull(timeline, this::load);
        form.add(shell);
        menu.install();
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            DeviceSync.resume();
            load();
        });
    }

    @Override
    public void scheduleChanged(String username) {
        load();
    }

    @Override
    public void appointmentChanged(String id, String state) {
        load();
    }

    private int days() {
        return DAY.equals(view) ? 1 : WEEK.equals(view) ? 7 : 14;
    }

    private void move(int direction) {
        from = Times.addDays(from, direction * days());
        load();
    }

    private void load() {
        title.setText(days() == 1 ? Ui.weekday(from)
                : Times.monthDay(from) + " - " + Times.monthDay(Times.addDays(from, days() - 1)));
        Api.provider().schedule(from, days(), Net.to(this::show, Ui.failed(timeline, this::load)));
    }

    private void show(ScheduleDto s) {
        if (!Nav.isHome(form)) {
            return;
        }
        timeline.removeAll();
        boolean agenda = AGENDA.equals(view);
        if (!agenda) {
            timeline.add(grid(s));
            timeline.add(Timeline.legend());
            Layouts.broaden(timeline);
            Ui.relayout(timeline);
            return;
        }
        int shown = 0;
        for (int d = 0; d < days(); d++) {
            long dayFrom = Times.addDays(from, d);
            long dayTo = Times.addDays(from, d + 1);
            List<Object> items = items(s, dayFrom, dayTo, agenda);
            if (items.isEmpty() && agenda) {
                continue;
            }
            if (days() > 1) {
                timeline.add(Ui.label(Ui.weekday(dayFrom), "SvSection"));
            }
            if (items.isEmpty()) {
                timeline.add(Ui.text(Lang.tr("Not working"), "SvSmall"));
            }
            for (int iter = 0; iter < items.size(); iter++) {
                timeline.add(entry(items.get(iter)));
                shown++;
            }
        }
        if (agenda && shown == 0) {
            timeline.add(Ui.empty(FontImage.MATERIAL_EVENT_AVAILABLE, "Nothing booked",
                    "Appointments show here as clients book them."));
        }
        Layouts.broaden(timeline);
        Ui.relayout(timeline);
    }

    /// The day or the week as a timeline: a lane per day, the shifts as bands,
    /// bookings as blocks a tap opens, calendar busy time hatched.
    private Timeline grid(ScheduleDto s) {
        List<Timeline.Lane> lanes = new ArrayList<Timeline.Lane>();
        for (int d = 0; d < days(); d++) {
            long dayFrom = Times.addDays(from, d);
            Timeline.Lane lane = new Timeline.Lane(Times.shortDay(dayFrom), dayFrom);
            List<Object> items = items(s, dayFrom, Times.addDays(from, d + 1), false);
            for (int iter = 0; iter < items.size(); iter++) {
                lane.add(block(items.get(iter)));
            }
            lanes.add(lane);
        }
        Timeline t = new Timeline(lanes, days() > 1);
        t.setName("timeline-grid");
        return t;
    }

    private Timeline.Block block(Object o) {
        if (o instanceof ShiftDto) {
            ShiftDto shift = (ShiftDto) o;
            return new Timeline.Block(Timeline.SHIFT, shift.start(), shift.end(), "", "", null);
        }
        if (o instanceof BusyDto) {
            BusyDto b = (BusyDto) o;
            String what = "TIME_OFF".equals(b.kind()) ? Lang.tr("Time off")
                    : b.label() == null || b.label().length() == 0 ? Lang.tr("Busy in a calendar")
                    : b.label();
            return new Timeline.Block(Timeline.BUSY, b.start(), b.end(), what, "", null);
        }
        final AppointmentDto a = (AppointmentDto) o;
        return new Timeline.Block(Timeline.kindOf(a.state()), a.start(), a.end(),
                a.serviceName(), Times.clock(a.start()) + Ui.DOT
                        + (a.clientName() == null ? "" : a.clientName()), () -> booking(a));
    }

    /// Shifts, busy blocks and appointments that start in a day, in time order.
    private static List<Object> items(ScheduleDto s, long dayFrom, long dayTo, boolean agenda) {
        List<Object> out = new ArrayList<Object>();
        if (!agenda) {
            for (int iter = 0; iter < s.shifts().size(); iter++) {
                ShiftDto shift = s.shifts().get(iter);
                if (shift.start() >= dayFrom && shift.start() < dayTo) {
                    out.add(shift);
                }
            }
            for (int iter = 0; iter < s.busy().size(); iter++) {
                BusyDto b = s.busy().get(iter);
                if (b.start() < dayTo && b.end() > dayFrom) {
                    out.add(b);
                }
            }
        }
        for (int iter = 0; iter < s.appointments().size(); iter++) {
            AppointmentDto a = s.appointments().get(iter);
            if (a.start() >= dayFrom && a.start() < dayTo) {
                out.add(a);
            }
        }
        // Insertion sort: a day holds a handful of entries.
        for (int i = 1; i < out.size(); i++) {
            Object moving = out.get(i);
            int j = i - 1;
            while (j >= 0 && start(out.get(j)) > start(moving)) {
                out.set(j + 1, out.get(j));
                j--;
            }
            out.set(j + 1, moving);
        }
        return out;
    }

    private static long start(Object o) {
        if (o instanceof AppointmentDto) {
            return ((AppointmentDto) o).start();
        }
        if (o instanceof BusyDto) {
            return ((BusyDto) o).start();
        }
        return o instanceof ShiftDto ? ((ShiftDto) o).start() : 0;
    }

    private Container entry(Object o) {
        if (o instanceof ShiftDto) {
            ShiftDto shift = (ShiftDto) o;
            return Ui.row(FontImage.MATERIAL_SCHEDULE, Lang.tr("Working"),
                    Times.clock(shift.start()) + " - " + Times.clock(shift.end()), null);
        }
        if (o instanceof BusyDto) {
            BusyDto b = (BusyDto) o;
            String what = "TIME_OFF".equals(b.kind()) ? Lang.tr("Time off")
                    : b.label() == null || b.label().length() == 0 ? Lang.tr("Busy in a calendar")
                    : b.label();
            Container row = Ui.row(FontImage.MATERIAL_BLOCK, what,
                    Times.clock(b.start()) + " - " + Times.clock(b.end()), null);
            row.setUIID("SvBusyRow");
            return row;
        }
        final AppointmentDto a = (AppointmentDto) o;
        String who = a.clientName() == null ? "" : a.clientName();
        return Ui.tap(Ui.row(FontImage.MATERIAL_EVENT, a.serviceName(),
                Times.clock(a.start()) + " - " + Times.clock(a.end()) + Ui.DOT + who + Ui.DOT
                        + Appointments.state(a), null),
                "booking-" + a.id(), e -> booking(a));
    }

    /// One booking from the provider's side: who, and how it ended.
    private void booking(final AppointmentDto a) {
        final Form shown = Ui.form(a.serviceName(), "Booking");
        Ui.back(shown, form);
        Container page = Ui.page();
        page.add(Ui.label(Ui.weekday(a.start()), "SvTitle"));
        page.add(Ui.text(Times.clock(a.start()) + " - " + Times.clock(a.end()), "SvHeading"));
        page.add(Ui.fact("Client", a.clientName()));
        page.add(Ui.fact("Where", a.online() ? Lang.tr("Online") : a.locationName()));
        page.add(Ui.fact("State", Appointments.state(a)));
        page.add(Ui.fact("Payment", Appointments.payment(a)));
        if (a.note() != null && a.note().length() > 0) {
            page.add(Ui.fact("Note", a.note()));
        }
        if (Appointments.CONFIRMED.equals(a.state())) {
            long now = System.currentTimeMillis();
            if (now >= a.start()) {
                page.add(Ui.primary("Mark as done", "complete", e -> Api.provider().complete(a.id(),
                        Net.to(done -> form.showBack(), Ui::fail))));
                page.add(Ui.secondary("Did not come", "no-show", e -> Api.provider().noShow(a.id(),
                        Net.to(done -> form.showBack(), Ui::fail))));
            }
            page.add(Ui.danger("Cancel", "cancel", e -> Ui.reason(shown, "Cancel booking",
                    Lang.tr("The client is told at once and refunded in full."), "Cancel booking",
                    why -> Api.provider().cancel(a.id(), new CancelDto(why),
                            Net.to(done -> form.showBack(), Ui::fail)))));
        }
        Layouts.broaden(page);
        shown.add(BorderLayout.CENTER, page);
        shown.show();
    }
}
// end::slate-provider-timeline[]
