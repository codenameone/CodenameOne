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

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.slate.People;
import com.codenameone.examples.slate.account.Accounts;
import com.codenameone.examples.slate.api.ScheduleDto;
import com.codenameone.examples.slate.api.ShiftDto;
import com.codenameone.examples.slate.domain.Service;
import com.codenameone.examples.slate.repository.LocationRepository;
import com.codenameone.examples.slate.repository.ProviderServiceRepository;
import com.codenameone.examples.slate.repository.ServiceRepository;
import com.codenameone.examples.slate.repository.WorkingHoursRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A provider's schedule asked for by a phone in another time zone than the
/// location: the phone's day must come back, not the location's day before.
@BackendTest
class ScheduleZoneTest {
    @Autowired
    private Schedules schedules;
    @Autowired
    private Accounts accounts;
    @Autowired
    private ServiceRepository services;
    @Autowired
    private ProviderServiceRepository offers;
    @Autowired
    private WorkingHoursRepository hours;
    @Autowired
    private LocationRepository locations;

    @Test
    void aPhoneAheadOfTheLocationGetsItsOwnDay() throws Exception {
        Service service = People.service(services, 60);
        String provider = People.provider(accounts, offers, hours, service);
        ZoneId zone = ZoneId.of(People.tx(() -> locations.findById(People.LOCATION).getTimeZone()));
        LocalDate day = LocalDate.now(zone).plusDays(3);
        long nineThere = day.atTime(9, 0).atZone(zone).toInstant().toEpochMilli();
        // Midnight on a phone one hour ahead of the location: 23:00 the
        // evening before, there.
        long phoneMidnight = day.atStartOfDay(zone).toInstant().toEpochMilli() - 3600000L;

        ScheduleDto s = schedules.schedule(provider, phoneMidnight, 1);

        boolean found = false;
        for (ShiftDto shift : s.shifts()) {
            found |= shift.start() == nineThere;
        }
        assertTrue(found, "the shift at 09:00 on " + day + " is missing from " + s.shifts());
    }

    @Test
    void theStaffingPlanCoversTheWholeWeekAndNoMore() throws Exception {
        Service service = People.service(services, 60);
        String provider = People.provider(accounts, offers, hours, service);
        ZoneId zone = ZoneId.of(People.tx(() -> locations.findById(People.LOCATION).getTimeZone()));
        LocalDate monday = LocalDate.now(zone).plusDays(7).with(java.time.DayOfWeek.MONDAY);

        List<ScheduleDto> week = schedules.days(monday.toString(), 7);
        List<ScheduleDto> capped = schedules.days(monday.toString(), 400);

        ScheduleDto mine = null;
        for (ScheduleDto s : week) {
            if (s.username().equals(provider)) {
                mine = s;
            }
        }
        assertTrue(mine != null, "the provider is missing from the plan");
        assertEquals(monday.atStartOfDay(zone).toInstant().toEpochMilli(), mine.from());
        assertEquals(monday.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli(), mine.to());
        for (ShiftDto shift : mine.shifts()) {
            assertTrue(shift.start() >= mine.from() && shift.end() <= mine.to(),
                    "a shift outside the week: " + shift);
        }
        for (ScheduleDto s : capped) {
            assertTrue(s.to() - s.from() <= 32 * 86400000L, "the plan was not capped: " + s);
        }
    }
}
