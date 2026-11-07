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
package com.codenameone.examples.slate.client;

import com.codename1.io.Preferences;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.slate.Telemetry;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.DaySlotsDto;
import com.codenameone.examples.slate.api.FindTimeDto;
import com.codenameone.examples.slate.api.FindTimeRequestDto;
import com.codenameone.examples.slate.api.HoldRequestDto;
import com.codenameone.examples.slate.api.LocationDto;
import com.codenameone.examples.slate.api.ProviderDto;
import com.codenameone.examples.slate.api.RescheduleDto;
import com.codenameone.examples.slate.api.ServiceDto;
import com.codenameone.examples.slate.api.SlotDto;
import com.codenameone.examples.slate.calendar.DeviceCalendar;
import com.codenameone.examples.slate.live.LiveChannel;
import com.codenameone.examples.slate.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// tag::slate-slot-picker[]
/// Choosing when: a service, who with, and a free start, by the day or the
/// week.
///
/// The free starts are the server's answer and nobody else's: the slot
/// engine has every provider's hours, bookings, buffers and connected
/// calendars, and the rules the business set. The phone adds one thing the
/// server cannot know -- the client's own calendar on this phone. With
/// "Check my calendar" on, a start that clashes with something there is
/// marked, though it can still be booked: the client knows whether that
/// dentist appointment can move.
///
/// The same screen moves an existing appointment: given one, a start is a
/// reschedule rather than a new hold.
public final class BookForm extends LiveChannel.Adapter {
    private static final String CHECK_CALENDAR = "slate.checkCalendar";
    private static final int DAYS_SHOWN = 21;
    private static final String DAY = "day";
    private static final String WEEK = "week";

    private final Form form;
    private final AppointmentDto moving;
    private final Container choices = new Container(BoxLayout.y());
    private final Container slots = new Container(BoxLayout.y());
    // Below the times, not above them: the times are what the screen is for,
    // and the switch is set once and left alone.
    private final Container myCalendar = new Container(BoxLayout.y());
    private final List<ServiceDto> services = new ArrayList<ServiceDto>();
    private final List<ProviderDto> providers = new ArrayList<ProviderDto>();
    private ServiceDto service;
    private String location = "";
    private String provider = "";
    private String view = DAY;
    private long day = Times.dayStart(System.currentTimeMillis());
    /// The client's own busy time on this phone over the days shown, once read.
    private List<long[]> mine = new ArrayList<long[]>();

    private BookForm(Form previous, ServiceDto service, AppointmentDto moving) {
        this.service = service;
        this.moving = moving;
        form = Ui.form(moving == null ? "Book" : "Move appointment", "Book");
        Ui.back(form, previous);
        Container page = Ui.page();
        choices.setName("choices");
        slots.setName("slots");
        page.add(choices).add(slots).add(myCalendar);
        Layouts.broaden(page);
        form.add(BorderLayout.CENTER, page);
        form.addShowListener(e -> LiveChannel.listen(this));
    }

    /// Books `service`, or asks which first when it is null.
    public static void show(Form previous, ServiceDto service) {
        new BookForm(previous, service, null).load();
    }

    /// Picks a new time for `a`, with the same service and provider.
    public static void reschedule(Form previous, AppointmentDto a) {
        BookForm book = new BookForm(previous, null, a);
        book.provider = Appointments.providerRef(a);
        book.day = Times.dayStart(a.start());
        book.load();
    }

    private void load() {
        form.show();
        Api.business().services(Net.to(list -> {
            services.clear();
            services.addAll(list);
            String wanted = moving != null ? moving.serviceId() : service == null ? "" : service.id();
            service = null;
            for (int iter = 0; iter < list.size(); iter++) {
                if (list.get(iter).id().equals(wanted)) {
                    service = list.get(iter);
                }
            }
            if (service == null && !list.isEmpty()) {
                service = list.get(0);
            }
            Api.business().providers(Net.to(people -> {
                providers.clear();
                providers.addAll(people);
                Api.business().locations(Net.to(this::located, Ui::fail));
            }, Ui::fail));
        }, Ui::fail));
    }

    private void located(List<LocationDto> places) {
        location = moving != null ? moving.locationId() : places.isEmpty() ? "" : places.get(0).id();
        buildChoices(places);
        readMine();
    }

    private void buildChoices(final List<LocationDto> places) {
        choices.removeAll();
        if (service == null) {
            choices.add(Ui.empty(FontImage.MATERIAL_EVENT_BUSY, "Nothing to book",
                    "This business has no services on offer yet."));
            Ui.relayout(choices);
            return;
        }
        if (moving == null && services.size() > 1) {
            String[] labels = new String[services.size()];
            String[] values = new String[services.size()];
            for (int iter = 0; iter < services.size(); iter++) {
                labels[iter] = services.get(iter).name();
                values[iter] = services.get(iter).id();
            }
            choices.add(Ui.label(ClientForm.business().serviceNoun(), "SvLabel"));
            choices.add(Ui.pills("service", labels, values, service.id(), id -> {
                for (int iter = 0; iter < services.size(); iter++) {
                    if (services.get(iter).id().equals(id)) {
                        service = services.get(iter);
                    }
                }
                provider = "";
                buildChoices(places);
                loadSlots();
            }));
        }
        choices.add(Ui.text(service.description() == null ? "" : service.description(), "SvText"));
        // What it takes and costs, before a time is picked: every booking app
        // answers this first.
        String terms = Times.length(service.minutes()) + Ui.DOT
                + Ui.money(service.priceCents(), service.currency());
        if (service.depositPercent() > 0) {
            terms += Ui.DOT + Lang.tr("Deposit {0}", Ui.money(service.priceCents()
                    * service.depositPercent() / 100, service.currency()));
        }
        choices.add(Ui.text(terms, "SvCaption"));
        if (places.size() > 1 && moving == null && !service.online()) {
            String[] labels = new String[places.size()];
            String[] values = new String[places.size()];
            for (int iter = 0; iter < places.size(); iter++) {
                labels[iter] = places.get(iter).name();
                values[iter] = places.get(iter).id();
            }
            choices.add(Ui.label("Where", "SvLabel"));
            choices.add(Ui.pills("location", labels, values, location, id -> {
                location = id;
                loadSlots();
            }));
        }
        List<ProviderDto> offering = offering();
        String[] labels = new String[offering.size() + 1];
        String[] values = new String[offering.size() + 1];
        labels[0] = Lang.tr("Anyone");
        values[0] = "";
        for (int iter = 0; iter < offering.size(); iter++) {
            labels[iter + 1] = offering.get(iter).displayName();
            values[iter + 1] = offering.get(iter).username();
        }
        choices.add(Ui.label(ClientForm.business().providerNoun(), "SvLabel"));
        choices.add(Ui.pills("provider", labels, values, provider, who -> {
            provider = who;
            loadSlots();
        }));

        Container when = new Container(new BorderLayout());
        when.add(BorderLayout.CENTER, Ui.choice("view", new String[] {Lang.tr("Day"), Lang.tr("Week")},
                new String[] {DAY, WEEK}, view, chosen -> {
                    view = chosen;
                    loadSlots();
                }));
        choices.add(when);
        myCalendar.removeAll();
        if (DeviceCalendar.canReadBusy()) {
            myCalendar.add(Ui.row(FontImage.MATERIAL_EVENT_NOTE, "Check my calendar",
                    "Marks times that clash with this phone's calendar",
                    Ui.toggle("checkCalendar", Preferences.get(CHECK_CALENDAR, false), on -> {
                        Preferences.set(CHECK_CALENDAR, on);
                        readMine();
                    })));
        }
        choices.add(dayStrip());
        Ui.relayout(choices);
    }

    private Container dayStrip() {
        long today = Times.dayStart(System.currentTimeMillis());
        String[] labels = new String[DAYS_SHOWN];
        String[] values = new String[DAYS_SHOWN];
        for (int iter = 0; iter < DAYS_SHOWN; iter++) {
            long d = Times.addDays(today, iter);
            labels[iter] = iter == 0 ? Lang.tr("Today") : iter == 1 ? Lang.tr("Tomorrow") : Times.shortDay(d);
            values[iter] = Times.iso(d);
        }
        return Ui.pills("day", labels, values, Times.iso(day), iso -> {
            for (int iter = 0; iter < DAYS_SHOWN; iter++) {
                long d = Times.addDays(today, iter);
                if (Times.iso(d).equals(iso)) {
                    day = d;
                }
            }
            loadSlots();
        });
    }

    private List<ProviderDto> offering() {
        List<ProviderDto> out = new ArrayList<ProviderDto>();
        for (int iter = 0; iter < providers.size(); iter++) {
            ProviderDto p = providers.get(iter);
            if (p.services() != null && p.services().contains(service.id())) {
                out.add(p);
            }
        }
        return out;
    }

    /// The client's own busy time, when they asked for it to be checked.
    private void readMine() {
        if (!DeviceCalendar.canReadBusy() || !Preferences.get(CHECK_CALENDAR, false)) {
            mine = new ArrayList<long[]>();
            loadSlots();
            return;
        }
        long from = Times.dayStart(System.currentTimeMillis());
        DeviceCalendar.busy(null, from, Times.addDays(from, DAYS_SHOWN + 7), busy -> {
            mine = busy;
            loadSlots();
        });
    }

    @Override
    public void slotsChanged(String changedService, String changedDay) {
        if (service == null || changedService.length() > 0 && !changedService.equals(service.id())) {
            return;
        }
        if (changedDay.length() > 0 && DAY.equals(view) && !changedDay.equals(Times.iso(day))) {
            return;
        }
        loadSlots();
    }

    private void loadSlots() {
        if (service == null) {
            return;
        }
        slots.removeAll();
        slots.add(new com.codename1.appkit.Skeleton(3, "SvSkeleton"));
        Ui.relayout(slots);
        if (WEEK.equals(view)) {
            List<String> who = provider.length() == 0 ? new ArrayList<String>()
                    : Collections.singletonList(provider);
            Api.business().findTime(new FindTimeRequestDto(service.id(), location, Times.iso(day), 7,
                    who), Net.to(this::showWeek, Ui.failed(slots, this::loadSlots)));
        } else {
            Api.business().slots(service.id(), location, Times.iso(day), provider,
                    Net.to(this::showDay, Ui.failed(slots, this::loadSlots)));
        }
    }
    // end::slate-slot-picker[]

    private void showDay(DaySlotsDto answer) {
        slots.removeAll();
        if (answer.slots().isEmpty()) {
            slots.add(Ui.empty(FontImage.MATERIAL_EVENT_BUSY, "Nothing free that day",
                    Reasons.say(answer.emptyReason())));
            if (moving == null) {
                slots.add(Ui.secondary("Tell me if a time frees up", "waitlist-join",
                        e -> WaitlistForm.join(form, service, provider, day)));
            }
            slots.add(Ui.link("Look at the whole week", "to-week", e -> {
                view = WEEK;
                loadSlots();
            }));
        } else {
            slots.add(grid(answer.slots()));
        }
        Ui.relayout(slots);
    }

    private void showWeek(FindTimeDto answer) {
        slots.removeAll();
        if (answer.slots().isEmpty()) {
            slots.add(Ui.empty(FontImage.MATERIAL_EVENT_BUSY, "Nothing free that week",
                    "Try another week, or anyone instead of one person."));
        }
        List<SlotDto> sameDay = new ArrayList<SlotDto>();
        String current = "";
        for (int iter = 0; iter <= answer.slots().size(); iter++) {
            SlotDto s = iter < answer.slots().size() ? answer.slots().get(iter) : null;
            String d = s == null ? "" : Times.iso(s.start());
            if (s == null || !d.equals(current)) {
                if (!sameDay.isEmpty()) {
                    slots.add(Ui.label(Ui.weekday(sameDay.get(0).start()), "SvSection"));
                    slots.add(grid(sameDay));
                    sameDay = new ArrayList<SlotDto>();
                }
                current = d;
            }
            if (s != null) {
                sameDay.add(s);
            }
        }
        Ui.relayout(slots);
    }

    private Container grid(List<SlotDto> list) {
        int columns = Layouts.columns(Layouts.column(), 18f, 6);
        Container grid = new Container(new GridLayout(Math.max(1, (list.size() + columns - 1) / columns),
                columns));
        grid.setUIID("SvSlotGrid");
        for (int iter = 0; iter < list.size(); iter++) {
            grid.add(slot(list.get(iter)));
        }
        return grid;
    }

    private Button slot(final SlotDto s) {
        boolean clash = DeviceCalendar.clashes(mine, s.start(), s.end());
        Button b = new Button(Times.clock(s.start()), clash ? "SvSlotClash" : "SvSlot");
        b.setName("slot-" + Times.clock(s.start()).replace(':', '-'));
        StringBuilder spoken = new StringBuilder(Times.clock(s.start()));
        if (s.providerNames() != null && !s.providerNames().isEmpty() && provider.length() == 0) {
            spoken.append(", ").append(s.providerNames().get(0));
        }
        if (clash) {
            spoken.append(", ").append(Lang.tr("clashes with your calendar"));
            b.setMaterialIcon(FontImage.MATERIAL_WARNING, 2.6f);
        }
        if (s.seatsLeft() > 1) {
            spoken.append(", ").append(Lang.tr("{0} places left", String.valueOf(s.seatsLeft())));
        }
        b.setAccessibilityText(spoken.toString());
        b.addActionListener(e -> take(s));
        return b;
    }

    private void take(final SlotDto s) {
        List<String> who = provider.length() > 0 ? Collections.singletonList(provider)
                : s.providers();
        if (moving != null) {
            Api.appointments().reschedule(moving.id(), new RescheduleDto(s.start(), who),
                    Net.to(moved -> {
                        Telemetry.rescheduled();
                        Reminders.changed(moved);
                        AppointmentForm.show(form, moved);
                    }, this::refused));
            return;
        }
        Api.appointments().hold(new HoldRequestDto(service.id(), location, s.start(), who),
                Net.to(held -> ConfirmForm.show(form, held, service), this::refused));
    }

    /// The start went to someone else between the list and the tap, or a
    /// rule turned it down: said, and the list read again.
    private void refused(int status, String message) {
        Ui.fail(status, message);
        loadSlots();
    }
}
