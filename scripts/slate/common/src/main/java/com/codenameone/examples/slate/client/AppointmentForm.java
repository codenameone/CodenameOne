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

import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.slate.Telemetry;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.CancelDto;
import com.codenameone.examples.slate.calendar.DeviceCalendar;
import com.codenameone.examples.slate.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.slate.net.Session;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Ui;

/// One appointment: when, where, who with, what was paid, and what can still
/// be done about it -- moved, cancelled, put in the phone's calendar.
///
/// The deadlines come from the server with the appointment
/// (`freeCancelUntil`, `rescheduleUntil`), so the screen says exactly what
/// cancelling costs right now rather than repeating the policy.
public final class AppointmentForm {
    private final Form form;
    private final Container page = Ui.page();

    private AppointmentForm(Form previous, AppointmentDto a) {
        form = Ui.form(a.serviceName(), "Appointment");
        Ui.back(form, previous);
        fill(a);
        Layouts.broaden(page);
        form.add(BorderLayout.CENTER, page);
    }

    public static void show(Form previous, AppointmentDto a) {
        new AppointmentForm(previous, a).form.show();
    }

    private void fill(final AppointmentDto a) {
        page.removeAll();
        page.add(Ui.label(Appointments.state(a), "SvModeBadge"));
        page.add(Ui.label(Ui.weekday(a.start()), "SvTitle"));
        page.add(Ui.text(Times.clock(a.start()) + " - " + Times.clock(a.end()), "SvHeading"));
        String with = Appointments.with(a);
        if (with.length() > 0) {
            page.add(Ui.text(with, "SvText"));
        }
        page.add(Ui.fact("Where", a.online() ? Lang.tr("Online") : a.locationName()));
        if (a.online() && a.meetingUrl() != null && a.meetingUrl().length() > 0) {
            page.add(Ui.link("Join the meeting", "join", e -> CN.execute(a.meetingUrl())));
        }
        page.add(Ui.fact("Price", Ui.money(a.priceCents(), a.currency())));
        page.add(Ui.fact("Payment", Appointments.payment(a)));
        if (a.note() != null && a.note().length() > 0) {
            page.add(Ui.fact("Your note", a.note()));
        }
        if (!Appointments.upcoming(a)) {
            Ui.relayout(page);
            return;
        }
        long now = System.currentTimeMillis();
        Ui.section(page, "Calendar");
        if (DeviceCalendar.canWrite()) {
            page.add(Ui.secondary(DeviceCalendar.added(a.id()) ? "Update in my calendar"
                    : "Add to my calendar", "add-calendar",
                    e -> BookedForm.add(form, page, a, 60)));
        }
        if (a.icsUrl() != null && a.icsUrl().length() > 0) {
            // An .ics file opens in whatever calendar the phone or computer
            // uses, including one this app cannot write to.
            page.add(Ui.link("Open as a calendar file", "ics",
                    e -> CN.execute(Session.serverUrl() + a.icsUrl())));
        }
        Ui.section(page, "Change");
        if (now < a.rescheduleUntil()) {
            page.add(Ui.secondary("Move to another time", "reschedule",
                    e -> BookForm.reschedule(form, a)));
        } else {
            page.add(Ui.text(Lang.tr("It is too close to the appointment to move it here. "
                    + "Call the business instead."), "SvSmall"));
        }
        String cost = now < a.freeCancelUntil()
                ? Lang.tr("Free to cancel until {0}.", Ui.when(a.freeCancelUntil()))
                : a.depositCents() > 0 ? Lang.tr("Cancelling now keeps the deposit of {0}.",
                        Ui.money(a.depositCents(), a.currency()))
                : Lang.tr("The free cancellation time has passed.");
        page.add(Ui.text(cost, "SvSmall"));
        page.add(Ui.danger("Cancel appointment", "cancel", e -> Ui.reason(form, "Cancel appointment",
                cost, "Cancel appointment", why -> Api.appointments().cancel(a.id(), new CancelDto(why),
                        Net.to(gone -> {
                            Telemetry.bookingCancelled();
                            DeviceCalendar.remove(a.id());
                            fill(gone);
                            form.showBack();
                        }, Ui::fail)))));
        Ui.relayout(page);
    }

}
