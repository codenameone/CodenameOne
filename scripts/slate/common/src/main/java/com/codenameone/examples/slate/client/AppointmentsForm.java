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
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.calendar.DeviceCalendar;
import com.codenameone.examples.slate.live.LiveChannel;
import com.codenameone.examples.slate.net.Api;
import com.codenameone.examples.slate.net.Net;
import com.codenameone.examples.slate.ui.Ui;

import java.util.List;

/// The client's agenda: what is coming, a day at a time, and what is past.
public final class AppointmentsForm extends LiveChannel.Adapter {
    private static final String COMING = "coming";
    private static final String PAST = "past";

    private final Form form = Ui.form("My appointments", "Appointments");
    private final Container list = Ui.list("appointments");
    private String which = COMING;

    private AppointmentsForm(Form previous) {
        Ui.back(form, previous);
        Container top = Ui.choice("which", new String[] {"Coming up", "Past"},
                new String[] {COMING, PAST}, which, chosen -> {
                    which = chosen;
                    load();
                });
        form.add(BorderLayout.NORTH, top);
        form.add(BorderLayout.CENTER, list);
        Ui.pull(list, this::load);
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
    }

    public static void show(Form previous) {
        new AppointmentsForm(previous).form.show();
    }

    @Override
    public void appointmentChanged(String id, String state) {
        load();
    }

    private void load() {
        if (COMING.equals(which)) {
            Api.appointments().upcoming(Net.to(this::show, Ui.failed(list, this::load)));
        } else {
            Api.appointments().history(Net.to(this::show, Ui.failed(list, this::load)));
        }
    }

    private void show(List<AppointmentDto> appointments) {
        list.removeAll();
        if (appointments.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_EVENT, COMING.equals(which) ? "Nothing coming up"
                    : "Nothing yet", COMING.equals(which)
                    ? "What you book shows here, a day at a time." : "Past appointments show here."));
        }
        String day = "";
        for (int iter = 0; iter < appointments.size(); iter++) {
            final AppointmentDto a = appointments.get(iter);
            String thisDay = Times.iso(a.start());
            if (!thisDay.equals(day)) {
                day = thisDay;
                list.add(Ui.label(Ui.weekday(a.start()), "SvSection"));
            }
            String detail = Times.clock(a.start()) + " - " + Times.clock(a.end()) + Ui.DOT
                    + Appointments.state(a);
            String with = Appointments.with(a);
            if (with.length() > 0) {
                detail = detail + Ui.DOT + with;
            }
            list.add(Ui.tap(Ui.row(DeviceCalendar.added(a.id()) ? FontImage.MATERIAL_EVENT_AVAILABLE
                    : FontImage.MATERIAL_EVENT, a.serviceName(), detail, null),
                    "appointment-" + a.id(), e -> AppointmentForm.show(form, a)));
        }
        Ui.relayout(list);
    }
}
