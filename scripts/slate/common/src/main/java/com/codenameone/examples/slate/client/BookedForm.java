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

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.slate.Telemetry;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.PolicyDto;
import com.codenameone.examples.slate.calendar.DeviceCalendar;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Nav;
import com.codenameone.examples.slate.ui.Ui;

/// Booked: what happens next, and the offer to put it in the phone's calendar.
public final class BookedForm {
    private BookedForm() {
    }

    public static void show(final AppointmentDto a, PolicyDto policy) {
        final Form form = Ui.form("Booked", "Booked");
        Ui.back(form, Nav::home);
        final Container page = Ui.page();
        page.add(Ui.banner(FontImage.MATERIAL_EVENT_AVAILABLE, a.serviceName(),
                Ui.weekday(a.start()) + Ui.DOT + Times.clock(a.start()) + " " + Appointments.with(a),
                "SvBannerIconGood"));
        page.add(Ui.text(Appointments.payment(a), "SvText"));
        final int alarm = policy == null ? 60 : Math.max(60, policy.reminderHours() * 60 / 24);
        if (DeviceCalendar.canWrite()) {
            page.add(Ui.primary("Add to my calendar", "add-calendar", e -> add(form, page, a, alarm)));
        }
        page.add(Ui.secondary("See the appointment", "details",
                e -> AppointmentForm.show(Nav.build(), a)));
        page.add(Ui.link("Done", "done", e -> Nav.home()));
        Layouts.broaden(page);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    static void add(final Form form, final Container page, final AppointmentDto a, int alarm) {
        DeviceCalendar.save(a, a.serviceName() + " " + Appointments.with(a), alarm, ok -> {
            if (ok.booleanValue()) {
                Telemetry.addedToCalendar();
                Ui.say("Added to your calendar, with a reminder");
            } else {
                Ui.say("The calendar could not be reached. Calendar access may be off in Settings.");
            }
        });
    }
}
