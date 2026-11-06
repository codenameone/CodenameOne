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
package com.codenameone.examples.slate.booking;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.appkit.server.Caller;
import com.codenameone.examples.slate.api.AppointmentApiServer;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.BusinessApiServer;
import com.codenameone.examples.slate.api.BusinessDto;
import com.codenameone.examples.slate.api.CancelDto;
import com.codenameone.examples.slate.api.ConfirmDto;
import com.codenameone.examples.slate.api.DaySlotsDto;
import com.codenameone.examples.slate.api.FindTimeDto;
import com.codenameone.examples.slate.api.FindTimeRequestDto;
import com.codenameone.examples.slate.api.HoldRequestDto;
import com.codenameone.examples.slate.api.LocationDto;
import com.codenameone.examples.slate.api.PolicyDto;
import com.codenameone.examples.slate.api.ProviderDto;
import com.codenameone.examples.slate.api.RescheduleDto;
import com.codenameone.examples.slate.api.ServiceDto;
import com.codenameone.examples.slate.api.WaitlistDto;
import com.codenameone.examples.slate.api.WaitlistRequestDto;
import com.codenameone.examples.slate.catalog.Catalog;
import com.codenameone.examples.slate.domain.Profile;
import com.codenameone.examples.slate.repository.ProfileRepository;

import java.util.List;

/// The server's half of `BusinessApi` and `AppointmentApi`: what any signed-in
/// account sees of the business, and its own bookings.
///
/// The caller is always the subject. No id here reaches another client's
/// appointment: each is looked up together with the caller's name, and one
/// that is someone else's is answered as one that does not exist.
@Component
public class BookingEndpoint implements BusinessApiServer, AppointmentApiServer {
    private final Catalog catalog;
    private final Availability availability;
    private final Bookings bookings;
    private final Waitlists waitlists;
    private final ProfileRepository profiles;

    public BookingEndpoint(Catalog catalog, Availability availability, Bookings bookings,
            Waitlists waitlists, ProfileRepository profiles) {
        this.catalog = catalog;
        this.availability = availability;
        this.bookings = bookings;
        this.waitlists = waitlists;
        this.profiles = profiles;
    }

    // ------------------------------------------------------------ the business

    @Override
    public BusinessDto business() throws Exception {
        return catalog.business();
    }

    @Override
    public PolicyDto policy() throws Exception {
        return catalog.policy();
    }

    @Override
    public List<LocationDto> locations() throws Exception {
        return catalog.locations(true);
    }

    @Override
    public List<ServiceDto> services() throws Exception {
        return catalog.services(true);
    }

    @Override
    public List<ProviderDto> providers() throws Exception {
        return catalog.providers(true);
    }

    @Override
    public DaySlotsDto slots(String service, String location, String day, String providers)
            throws Exception {
        return availability.day(Caller.name(), service, location, day, providers);
    }

    @Override
    public FindTimeDto findTime(FindTimeRequestDto body) throws Exception {
        return availability.find(Caller.name(), body);
    }

    // ------------------------------------------------------------ appointments

    @Override
    public List<AppointmentDto> upcoming() throws Exception {
        return bookings.upcoming(Caller.name());
    }

    @Override
    public List<AppointmentDto> history() throws Exception {
        return bookings.history(Caller.name());
    }

    @Override
    public AppointmentDto appointment(String id) throws Exception {
        return bookings.mine(Caller.name(), id);
    }

    @Override
    public AppointmentDto hold(HoldRequestDto body) throws Exception {
        String me = Caller.name();
        return bookings.hold(me, displayName(me), body);
    }

    /// Read in a transaction of its own and not inside the hold's, which must
    /// commit or fail by itself: the row claim is what decides a race.
    @Transactional(readOnly = true)
    private String displayName(String me) {
        Profile profile = profiles.findById(me);
        return profile == null ? me : profile.getDisplayName();
    }

    @Override
    public AppointmentDto confirm(String id, ConfirmDto body) throws Exception {
        return bookings.confirm(Caller.name(), id, body);
    }

    @Override
    public AppointmentDto cancel(String id, CancelDto body) throws Exception {
        return bookings.cancelByClient(Caller.name(), id, body);
    }

    @Override
    public AppointmentDto reschedule(String id, RescheduleDto body) throws Exception {
        return bookings.reschedule(Caller.name(), id, body);
    }

    @Override
    public List<WaitlistDto> waitlist() throws Exception {
        return waitlists.mine(Caller.name());
    }

    @Override
    public WaitlistDto join(WaitlistRequestDto body) throws Exception {
        return waitlists.join(Caller.name(), body);
    }

    @Override
    public WaitlistDto leave(String id) throws Exception {
        return waitlists.leave(Caller.name(), id);
    }
}
