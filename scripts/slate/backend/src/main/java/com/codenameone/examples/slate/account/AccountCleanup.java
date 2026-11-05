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
package com.codenameone.examples.slate.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.slate.booking.States;
import com.codenameone.examples.slate.domain.Appointment;
import com.codenameone.examples.slate.domain.GroupSlot;
import com.codenameone.examples.slate.repository.AppointmentRepository;
import com.codenameone.examples.slate.repository.BusyBlockRepository;
import com.codenameone.examples.slate.repository.CalendarLinkRepository;
import com.codenameone.examples.slate.repository.GroupSlotRepository;
import com.codenameone.examples.slate.repository.LiveTicketRepository;
import com.codenameone.examples.slate.repository.PhoneCodeRepository;
import com.codenameone.examples.slate.repository.PreferenceRepository;
import com.codenameone.examples.slate.repository.ProviderServiceRepository;
import com.codenameone.examples.slate.repository.PushDeviceRepository;
import com.codenameone.examples.slate.repository.SlotClaimRepository;
import com.codenameone.examples.slate.repository.TimeOffRepository;
import com.codenameone.examples.slate.repository.WaitlistRepository;
import com.codenameone.examples.slate.repository.WorkingHoursRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// What closing an account does to the rows other parts of the server keep
/// under its name.
///
/// These are statements over whole sets of rows -- everything of one account
/// in a table -- and not the work of the services that own those tables,
/// which is why they are gathered here. Each deletion is a bulk statement, so
/// whatever the session had loaded before it is stale afterwards. It has no
/// transaction of its own: [Accounts] calls it inside one.
///
/// Past appointments stay: they are the business's record of work done and
/// money taken, kept under the closed account's name. A provider's row stays
/// too, switched off, because those appointments name it.
@Component
public class AccountCleanup {
    private final AppointmentRepository appointments;
    private final SlotClaimRepository claims;
    private final GroupSlotRepository groups;
    private final WaitlistRepository waitlist;
    private final WorkingHoursRepository hours;
    private final TimeOffRepository timeOff;
    private final CalendarLinkRepository links;
    private final BusyBlockRepository busy;
    private final ProviderServiceRepository offers;
    private final PreferenceRepository preferences;
    private final PhoneCodeRepository codes;
    private final LiveTicketRepository tickets;
    private final PushDeviceRepository pushDevices;

    public AccountCleanup(AppointmentRepository appointments, SlotClaimRepository claims,
            GroupSlotRepository groups, WaitlistRepository waitlist, WorkingHoursRepository hours,
            TimeOffRepository timeOff, CalendarLinkRepository links, BusyBlockRepository busy,
            ProviderServiceRepository offers, PreferenceRepository preferences,
            PhoneCodeRepository codes, LiveTicketRepository tickets,
            PushDeviceRepository pushDevices) {
        this.appointments = appointments;
        this.claims = claims;
        this.groups = groups;
        this.waitlist = waitlist;
        this.hours = hours;
        this.timeOff = timeOff;
        this.links = links;
        this.busy = busy;
        this.offers = offers;
        this.preferences = preferences;
        this.codes = codes;
        this.tickets = tickets;
        this.pushDevices = pushDevices;
    }

    /// Removes everything an account keeps and cancels its bookings still to
    /// come. Returns the services of the bookings cancelled, so the caller can
    /// tell whoever is looking at their slots. The caller has already refused a
    /// closure while a deposit is held, so nothing here owes the processor.
    public List<String> removeEverythingOf(String username) {
        Instant now = Instant.now();
        List<String> freed = new ArrayList<String>();
        List<Appointment> coming = appointments.upcoming(username, now);
        for (int iter = 0; iter < coming.size(); iter++) {
            Appointment a = coming.get(iter);
            if (appointments.move(a.getId(), a.getState(), States.CANCELLED, now) == 0) {
                continue;
            }
            if (a.getGroupKey().length() == 0) {
                claims.deleteByAppointmentId(a.getId());
            } else {
                groups.give(a.getGroupKey());
                GroupSlot group = groups.findById(a.getGroupKey());
                if (group == null || group.getSeats() == 0) {
                    claims.deleteByAppointmentId(a.getGroupKey());
                }
            }
            if (!freed.contains(a.getServiceId())) {
                freed.add(a.getServiceId());
            }
        }
        waitlist.deleteByClient(username);
        hours.deleteByUsername(username);
        timeOff.deleteByUsername(username);
        busy.deleteByUsername(username);
        links.deleteByUsername(username);
        offers.deleteByUsername(username);
        preferences.deleteByUsername(username);
        codes.deleteByUsername(username);
        tickets.deleteByUsername(username);
        pushDevices.deleteByUsername(username);
        return freed;
    }
}
