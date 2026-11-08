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
package com.codenameone.examples.slate.sync;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.slate.booking.States;
import com.codenameone.examples.slate.domain.Appointment;
import com.codenameone.examples.slate.domain.Location;
import com.codenameone.examples.slate.domain.Provider;
import com.codenameone.examples.slate.domain.Service;
import com.codenameone.examples.slate.repository.AppointmentRepository;
import com.codenameone.examples.slate.repository.LocationRepository;
import com.codenameone.examples.slate.repository.ProviderRepository;
import com.codenameone.examples.slate.repository.ServiceRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Calendar files, for calendar apps rather than for Slate's own.
///
/// - `/ics/p/{feedToken}.ics` is a provider's bookings as a feed their
///   calendar subscribes to, for a provider who would rather see Slate in the
///   calendar they already use than connect it the other way round.
/// - `/ics/a/{shareToken}.ics` is one booking, which the app offers a client
///   who would rather add it with a calendar app than let Slate write to the
///   phone's calendar itself.
///
/// Neither asks for a sign-in: a calendar app subscribing to a feed has no way
/// to give one. The token in the address is the whole of the access, random
/// and as long as an id, and it names one feed or one booking and nothing
/// else. A client's file shows no name but the service's.
@RestController
public class IcsPage {
    private static final long DAY = 86400000L;

    private final AppointmentRepository appointments;
    private final ProviderRepository providers;
    private final ServiceRepository services;
    private final LocationRepository locations;

    public IcsPage(AppointmentRepository appointments, ProviderRepository providers,
            ServiceRepository services, LocationRepository locations) {
        this.appointments = appointments;
        this.providers = providers;
        this.services = services;
        this.locations = locations;
    }

    @GetMapping("/ics/p/{file}")
    @Transactional(readOnly = true)
    public HttpServer.Response feed(@PathVariable("file") String file) throws Exception {
        Provider p = providers.findFirstByFeedToken(token(file));
        if (p == null || !p.isActive()) {
            return missing();
        }
        long now = System.currentTimeMillis();
        List<Appointment> theirs = appointments.forProvider(p.getUsername(),
                Instant.ofEpochMilli(now - 14 * DAY), Instant.ofEpochMilli(now + 120 * DAY));
        List<Ics.Event> events = new ArrayList<Ics.Event>();
        for (int iter = 0; iter < theirs.size(); iter++) {
            Appointment a = theirs.get(iter);
            if (States.HELD.equals(a.getState())) {
                continue;
            }
            Ics.Event e = event(a);
            e.summary = e.summary + " - " + a.getClientName();
            events.add(e);
        }
        return calendar(Ics.write("Slate - " + p.getDisplayName(), events));
    }

    @GetMapping("/ics/a/{file}")
    @Transactional(readOnly = true)
    public HttpServer.Response appointment(@PathVariable("file") String file) throws Exception {
        Appointment a = appointments.findFirstByShareToken(token(file));
        if (a == null || States.HELD.equals(a.getState())) {
            return missing();
        }
        List<Ics.Event> events = new ArrayList<Ics.Event>();
        Ics.Event e = event(a);
        e.alarmMinutes = 60;
        events.add(e);
        return calendar(Ics.write("Slate", events));
    }

    private Ics.Event event(Appointment a) {
        Service s = services.findById(a.getServiceId());
        Location l = locations.findById(a.getLocationId());
        Ics.Event e = new Ics.Event();
        e.uid = a.getId();
        e.start = a.getStartsAt().toEpochMilli();
        e.end = a.getEndsAt().toEpochMilli();
        e.summary = s == null ? "Appointment" : s.getName();
        e.location = a.isOnline() ? a.getMeetingUrl() : l == null ? "" : l.getName() + ", "
                + l.getAddress();
        e.url = a.getMeetingUrl();
        e.cancelled = States.CANCELLED.equals(a.getState());
        return e;
    }

    private static String token(String file) {
        String t = file == null ? "" : file;
        return t.endsWith(".ics") ? t.substring(0, t.length() - 4) : t;
    }

    private static HttpServer.Response calendar(String body) throws Exception {
        return new HttpServer.Response(200, "text/calendar; charset=utf-8",
                body.getBytes("UTF-8"));
    }

    private static HttpServer.Response missing() throws Exception {
        return new HttpServer.Response(404, "text/plain; charset=utf-8",
                "No such calendar".getBytes("UTF-8"));
    }
}
