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

import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.calendar.DeviceCalendar;
import com.codenameone.examples.slate.net.Api;
import com.codenameone.examples.slate.net.Net;

import java.util.List;

/// Keeps the events this app put in the phone's calendar true to the
/// bookings behind them.
///
/// An appointment can move or be cancelled from somewhere else -- the
/// business, the web, another phone -- so whenever the app comes forward it
/// reads the client's upcoming appointments and rewrites each event it owns:
/// moved ones are saved again in place, gone ones are deleted. Only events
/// this app created are touched; the mapping lives in `DeviceCalendar`.
public final class Reminders {
    private static final int ALARM_MINUTES = 60;

    private Reminders() {
    }

    /// Brings every event this app owns into line with the server.
    public static void resumed() {
        if (!DeviceCalendar.canWrite()) {
            return;
        }
        Api.appointments().upcoming(Net.to(Reminders::reconcile, (status, message) -> {
        }));
    }

    /// An appointment moved here: its event moves with it.
    public static void changed(AppointmentDto moved) {
        if (DeviceCalendar.added(moved.id())) {
            DeviceCalendar.save(moved, moved.serviceName() + " " + Appointments.with(moved),
                    ALARM_MINUTES, ok -> {
                    });
        }
    }

    /// Signing out takes this app's events off the phone: they belong to the
    /// account, not the device.
    public static void clear() {
        DeviceCalendar.removeAll();
    }

    private static void reconcile(List<AppointmentDto> upcoming) {
        List<String> owned = DeviceCalendar.owned();
        for (int iter = 0; iter < owned.size(); iter++) {
            String id = owned.get(iter);
            AppointmentDto a = find(upcoming, id);
            if (a == null || !Appointments.upcoming(a)) {
                DeviceCalendar.remove(id);
            } else {
                changed(a);
            }
        }
    }

    private static AppointmentDto find(List<AppointmentDto> upcoming, String id) {
        for (int iter = 0; iter < upcoming.size(); iter++) {
            if (upcoming.get(iter).id().equals(id)) {
                return upcoming.get(iter);
            }
        }
        return null;
    }
}
