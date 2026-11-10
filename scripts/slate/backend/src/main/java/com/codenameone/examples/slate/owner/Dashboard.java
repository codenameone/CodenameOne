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
package com.codenameone.examples.slate.owner;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.slate.api.DashboardDto;
import com.codenameone.examples.slate.api.NameCountDto;
import com.codenameone.examples.slate.booking.Availability;
import com.codenameone.examples.slate.booking.States;
import com.codenameone.examples.slate.catalog.Catalog;
import com.codenameone.examples.slate.domain.Appointment;
import com.codenameone.examples.slate.domain.AppointmentParty;
import com.codenameone.examples.slate.domain.Business;
import com.codenameone.examples.slate.domain.Provider;
import com.codenameone.examples.slate.domain.Service;
import com.codenameone.examples.slate.domain.WorkingHours;
import com.codenameone.examples.slate.repository.AppointmentPartyRepository;
import com.codenameone.examples.slate.repository.AppointmentRepository;
import com.codenameone.examples.slate.repository.ProviderRepository;
import com.codenameone.examples.slate.repository.ServiceRepository;
import com.codenameone.examples.slate.repository.WorkingHoursRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The owner's numbers for the last 28 days: how many bookings, what came of
/// them, the deposits taken, and how much of the hours worked was booked.
///
/// Utilisation is booked minutes over the minutes of the weekly hours, both
/// over the same 28 days. Buffers count as booked: they are time a provider
/// cannot sell.
@Component
public class Dashboard {
    static final int DAYS = 28;

    private final Catalog catalog;
    private final AppointmentRepository appointments;
    private final AppointmentPartyRepository parties;
    private final ServiceRepository services;
    private final ProviderRepository providers;
    private final WorkingHoursRepository hours;

    public Dashboard(Catalog catalog, AppointmentRepository appointments,
            AppointmentPartyRepository parties, ServiceRepository services,
            ProviderRepository providers, WorkingHoursRepository hours) {
        this.catalog = catalog;
        this.appointments = appointments;
        this.parties = parties;
        this.services = services;
        this.providers = providers;
        this.hours = hours;
    }

    // tag::slate-dashboard[]
    @Transactional(readOnly = true)
    public DashboardDto read() {
        Business business = catalog.main();
        ZoneId zone = catalog.zone();
        LocalDate today = LocalDate.now(zone);
        LocalDate first = today.minusDays(DAYS - 1);
        long from = Availability.at(first, 0, zone);
        long to = Availability.at(today.plusDays(1), 0, zone);
        List<Appointment> all = appointments.started(Instant.ofEpochMilli(from),
                Instant.ofEpochMilli(to));
        int booked = 0;
        int completed = 0;
        int noShows = 0;
        int cancelled = 0;
        long revenue = 0;
        long bookedMinutes = 0;
        int[] byDay = new int[DAYS];
        Map<String, Integer> byService = new HashMap<String, Integer>();
        Map<String, Integer> byProvider = new HashMap<String, Integer>();
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < all.size(); iter++) {
            Appointment a = all.get(iter);
            String state = a.getState();
            if (States.HELD.equals(state)) {
                continue;
            }
            if (States.CANCELLED.equals(state)) {
                cancelled++;
            } else {
                booked++;
                ids.add(a.getId());
                bump(byService, a.getServiceId());
                int day = (int) (Instant.ofEpochMilli(a.getStartsAt().toEpochMilli()).atZone(zone)
                        .toLocalDate().toEpochDay() - first.toEpochDay());
                if (day >= 0 && day < DAYS) {
                    byDay[day]++;
                }
                bookedMinutes += (a.getBlockEnd().toEpochMilli()
                        - a.getBlockStart().toEpochMilli()) / 60000L;
            }
            if (States.COMPLETED.equals(state)) {
                completed++;
            } else if (States.NO_SHOW.equals(state)) {
                noShows++;
            }
            if (States.PAY_CAPTURED.equals(a.getPaymentState())) {
                revenue += a.getDepositCents();
            }
            if (States.COMPLETED.equals(state)) {
                // The rest of the price is paid on the day.
                revenue += a.getPriceCents() - (States.PAY_CAPTURED.equals(a.getPaymentState())
                        ? a.getDepositCents() : 0);
            }
        }
        if (!ids.isEmpty()) {
            List<AppointmentParty> who = parties.findByAppointmentIdIn(ids);
            for (int iter = 0; iter < who.size(); iter++) {
                if (States.PROVIDER.equals(who.get(iter).getKind())) {
                    bump(byProvider, who.get(iter).getRef());
                }
            }
        }
        List<Integer> days = new ArrayList<Integer>();
        for (int iter = 0; iter < DAYS; iter++) {
            days.add(Integer.valueOf(byDay[iter]));
        }
        return new DashboardDto(booked, completed, noShows, cancelled, revenue,
                business.getCurrency(), utilisation(bookedMinutes), days, top(byService, true),
                top(byProvider, false));
    }
    // end::slate-dashboard[]

    private int utilisation(long bookedMinutes) {
        List<Provider> active = providers.findByActiveOrderByDisplayNameAsc(true);
        List<String> names = new ArrayList<String>();
        for (int iter = 0; iter < active.size(); iter++) {
            names.add(active.get(iter).getUsername());
        }
        if (names.isEmpty()) {
            return 0;
        }
        long weekly = 0;
        List<WorkingHours> week = hours.findByUsernameIn(names);
        for (int iter = 0; iter < week.size(); iter++) {
            weekly += week.get(iter).getEndMinute() - week.get(iter).getStartMinute();
        }
        long worked = weekly * DAYS / 7;
        if (worked <= 0) {
            return 0;
        }
        return (int) Math.min(100, bookedMinutes * 100 / worked);
    }

    private List<NameCountDto> top(Map<String, Integer> counts, boolean isService) {
        List<NameCountDto> out = new ArrayList<NameCountDto>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            String name = e.getKey();
            if (isService) {
                Service s = services.findById(name);
                name = s == null ? name : s.getName();
            } else {
                Provider p = providers.findById(name);
                name = p == null ? name : p.getDisplayName();
            }
            out.add(new NameCountDto(name, e.getValue().intValue()));
        }
        // Most first, then by name, so the order is the same on every read.
        for (int i = 1; i < out.size(); i++) {
            NameCountDto key = out.get(i);
            int j = i - 1;
            while (j >= 0 && (out.get(j).count() < key.count() || out.get(j).count() == key.count()
                    && out.get(j).name().compareTo(key.name()) > 0)) {
                out.set(j + 1, out.get(j));
                j--;
            }
            out.set(j + 1, key);
        }
        return out.size() > 5 ? new ArrayList<NameCountDto>(out.subList(0, 5)) : out;
    }

    private static void bump(Map<String, Integer> counts, String key) {
        Integer was = counts.get(key);
        counts.put(key, Integer.valueOf(was == null ? 1 : was.intValue() + 1));
    }
}
