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

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codenameone.examples.slate.Caller;
import com.codenameone.examples.slate.Roles;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.BusyUploadDto;
import com.codenameone.examples.slate.api.CalendarConfigDto;
import com.codenameone.examples.slate.api.CalendarLinkDto;
import com.codenameone.examples.slate.api.CancelDto;
import com.codenameone.examples.slate.api.ConnectCalendarDto;
import com.codenameone.examples.slate.api.ProviderApiServer;
import com.codenameone.examples.slate.api.ProviderDto;
import com.codenameone.examples.slate.api.ScheduleDto;
import com.codenameone.examples.slate.api.TimeOffDto;
import com.codenameone.examples.slate.api.WeekDto;
import com.codenameone.examples.slate.booking.Bookings;
import com.codenameone.examples.slate.catalog.Catalog;
import com.codenameone.examples.slate.sync.CalendarLinks;

import java.util.List;

/// The server's half of `ProviderApi`: a provider's own diary. Guarded by
/// path in the filter chain and by role here; every call acts on the
/// caller's own rows, and an appointment that is not theirs is answered as
/// one that does not exist.
@Component
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderEndpoint implements ProviderApiServer {
    private final Catalog catalog;
    private final Schedules schedules;
    private final CalendarLinks calendars;
    private final Bookings bookings;

    public ProviderEndpoint(Catalog catalog, Schedules schedules, CalendarLinks calendars,
            Bookings bookings) {
        this.catalog = catalog;
        this.schedules = schedules;
        this.calendars = calendars;
        this.bookings = bookings;
    }

    @Override
    public ProviderDto me() throws Exception {
        return catalog.provider(Caller.name());
    }

    @Override
    public ProviderDto saveMe(ProviderDto body) throws Exception {
        return catalog.saveProvider(Catalog.asSelf(Caller.name(), body), false);
    }

    @Override
    public ScheduleDto schedule(long from, int days) throws Exception {
        return schedules.schedule(Caller.name(), from, days);
    }

    @Override
    public WeekDto hours() throws Exception {
        return schedules.week(Caller.name());
    }

    @Override
    public WeekDto saveHours(WeekDto body) throws Exception {
        return schedules.saveWeek(Caller.name(), body);
    }

    @Override
    public List<TimeOffDto> timeOff() throws Exception {
        return schedules.timeOff(Caller.name());
    }

    @Override
    public TimeOffDto addTimeOff(TimeOffDto body) throws Exception {
        return schedules.addTimeOff(Caller.name(), body);
    }

    @Override
    public TimeOffDto removeTimeOff(String id) throws Exception {
        return schedules.removeTimeOff(Caller.name(), id);
    }

    @Override
    public List<CalendarLinkDto> calendars() throws Exception {
        return calendars.list(Caller.name());
    }

    @Override
    public CalendarConfigDto calendarConfig() throws Exception {
        return calendars.config(Caller.name());
    }

    @Override
    public CalendarLinkDto connect(ConnectCalendarDto body) throws Exception {
        return calendars.connect(Caller.name(), body);
    }

    @Override
    public CalendarLinkDto disconnect(String id) throws Exception {
        return calendars.disconnect(Caller.name(), id);
    }

    @Override
    public CalendarLinkDto sync(String id) throws Exception {
        return calendars.sync(Caller.name(), id);
    }

    @Override
    public CalendarLinkDto uploadBusy(BusyUploadDto body) throws Exception {
        return calendars.upload(Caller.name(), body);
    }

    @Override
    public AppointmentDto complete(String id) throws Exception {
        return bookings.complete(Caller.name(), Caller.hasRole(Roles.OWNER), id);
    }

    @Override
    public AppointmentDto noShow(String id) throws Exception {
        return bookings.noShow(Caller.name(), Caller.hasRole(Roles.OWNER), id);
    }

    @Override
    public AppointmentDto cancel(String id, CancelDto body) throws Exception {
        return bookings.cancelByBusiness(Caller.name(), Caller.hasRole(Roles.OWNER), id, body);
    }
}
