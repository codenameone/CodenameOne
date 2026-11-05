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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.slate.Ids;
import com.codenameone.examples.slate.Text;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.BusyDto;
import com.codenameone.examples.slate.api.ScheduleDto;
import com.codenameone.examples.slate.api.ShiftDto;
import com.codenameone.examples.slate.api.TimeOffDto;
import com.codenameone.examples.slate.api.WeekDto;
import com.codenameone.examples.slate.api.WorkingHoursDto;
import com.codenameone.examples.slate.booking.Availability;
import com.codenameone.examples.slate.booking.Bookings;
import com.codenameone.examples.slate.booking.States;
import com.codenameone.examples.slate.catalog.Catalog;
import com.codenameone.examples.slate.domain.Appointment;
import com.codenameone.examples.slate.domain.BusyBlock;
import com.codenameone.examples.slate.domain.Location;
import com.codenameone.examples.slate.domain.Provider;
import com.codenameone.examples.slate.domain.TimeOff;
import com.codenameone.examples.slate.domain.WorkingHours;
import com.codenameone.examples.slate.live.LiveHub;
import com.codenameone.examples.slate.repository.AppointmentRepository;
import com.codenameone.examples.slate.repository.BusyBlockRepository;
import com.codenameone.examples.slate.repository.LocationRepository;
import com.codenameone.examples.slate.repository.ProviderRepository;
import com.codenameone.examples.slate.repository.TimeOffRepository;
import com.codenameone.examples.slate.repository.WorkingHoursRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/// A provider's working week, their time off, and the schedule the day,
/// week and agenda views draw.
///
/// A schedule is three layers over a range of time: the shifts the weekly
/// hours make, the appointments, and the time that is not free for other
/// reasons -- time off and what a connected calendar says is busy. The views
/// draw the same three layers whatever the zoom; nothing here knows whether it
/// is drawn as a day or a week.
@Component
public class Schedules {
    /// The longest schedule read at once, so a week view of a month is refused
    /// rather than read.
    private static final int MAX_DAYS = 31;
    private static final int MAX_SHIFTS = 28;

    private final Catalog catalog;
    private final Bookings bookings;
    private final WorkingHoursRepository hours;
    private final TimeOffRepository timeOff;
    private final BusyBlockRepository busy;
    private final AppointmentRepository appointments;
    private final ProviderRepository providers;
    private final LocationRepository locations;
    private final LiveHub live;

    public Schedules(Catalog catalog, Bookings bookings, WorkingHoursRepository hours,
            TimeOffRepository timeOff, BusyBlockRepository busy,
            AppointmentRepository appointments, ProviderRepository providers,
            LocationRepository locations, LiveHub live) {
        this.catalog = catalog;
        this.bookings = bookings;
        this.hours = hours;
        this.timeOff = timeOff;
        this.busy = busy;
        this.appointments = appointments;
        this.providers = providers;
        this.locations = locations;
        this.live = live;
    }

    // ------------------------------------------------------------------ hours

    @Transactional(readOnly = true)
    public WeekDto week(String username) {
        provider(username);
        List<WorkingHours> rows = hours.findByUsernameOrderByWeekdayAscStartMinuteAsc(username);
        List<WorkingHoursDto> out = new ArrayList<WorkingHoursDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            WorkingHours w = rows.get(iter);
            out.add(new WorkingHoursDto(w.getId(), w.getLocationId(), w.getWeekday(),
                    w.getStartMinute(), w.getEndMinute()));
        }
        return new WeekDto(out);
    }

    // tag::slate-save-hours[]
    /// Replaces a provider's week. Shifts are on the five-minute grid, at an
    /// open location, and do not overlap on one day. Bookings already made
    /// keep their time: hours decide what is offered, not what was sold.
    public WeekDto saveWeek(String username, WeekDto week) throws IOException {
        WeekDto saved = saveWeekTx(username, week);
        live.scheduleChanged(username);
        return saved;
    }
    // end::slate-save-hours[]

    @Transactional(rollbackFor = IOException.class)
    private WeekDto saveWeekTx(String username, WeekDto week) throws IOException {
        provider(username);
        if (week == null || week.hours() == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        List<WorkingHoursDto> list = week.hours();
        if (list.size() > MAX_SHIFTS) {
            throw new ResponseStatusException(400, "That is more shifts than a week has room for");
        }
        for (int iter = 0; iter < list.size(); iter++) {
            WorkingHoursDto a = list.get(iter);
            if (a == null) {
                throw new ResponseStatusException(400, "A shift is missing");
            }
            Catalog.range(a.weekday(), 1, 7, "The day");
            Catalog.grid(a.startMinute(), 0, 1435, "A shift's start");
            Catalog.grid(a.endMinute(), 5, 1440, "A shift's end");
            if (a.endMinute() <= a.startMinute()) {
                throw new ResponseStatusException(400, "A shift has to end after it starts");
            }
            catalog.openLocation(a.locationId());
            for (int other = 0; other < iter; other++) {
                WorkingHoursDto b = list.get(other);
                if (b.weekday() == a.weekday() && b.startMinute() < a.endMinute()
                        && a.startMinute() < b.endMinute()) {
                    throw new ResponseStatusException(400, "Two shifts on one day overlap");
                }
            }
        }
        hours.deleteByUsername(username);
        for (int iter = 0; iter < list.size(); iter++) {
            WorkingHoursDto a = list.get(iter);
            WorkingHours row = new WorkingHours();
            row.setId(Ids.next());
            row.setUsername(username);
            row.setLocationId(a.locationId());
            row.setWeekday(a.weekday());
            row.setStartMinute(a.startMinute());
            row.setEndMinute(a.endMinute());
            hours.insert(row);
        }
        return week(username);
    }

    // --------------------------------------------------------------- time off

    @Transactional(readOnly = true)
    public List<TimeOffDto> timeOff(String username) {
        List<TimeOff> rows = timeOff.findByUsernameOrderByStartsAtAsc(username);
        List<TimeOffDto> out = new ArrayList<TimeOffDto>();
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter).getEndsAt().toEpochMilli() > now) {
                out.add(dto(rows.get(iter)));
            }
        }
        return out;
    }

    /// Takes time off. Bookings inside it are not cancelled for the provider:
    /// the answer says how many there are, as a 409, and the provider moves or
    /// cancels them first. Time off over a booking would otherwise read as
    /// free on the schedule while a client is on their way.
    public TimeOffDto addTimeOff(String username, TimeOffDto ask) throws IOException {
        TimeOffDto added = addTimeOffTx(username, ask);
        live.scheduleChanged(username);
        return added;
    }

    @Transactional(rollbackFor = IOException.class)
    private TimeOffDto addTimeOffTx(String username, TimeOffDto ask) throws IOException {
        provider(username);
        if (ask == null || ask.end() <= ask.start()) {
            throw new ResponseStatusException(400, "Time off has to end after it starts");
        }
        if (ask.end() - ask.start() > 120L * 86400000L) {
            throw new ResponseStatusException(400, "Take up to four months off at a time");
        }
        int clashes = 0;
        List<Appointment> theirs = appointments.forProvider(username,
                Instant.ofEpochMilli(ask.start()), Instant.ofEpochMilli(ask.end()));
        for (int iter = 0; iter < theirs.size(); iter++) {
            if (open(theirs.get(iter))) {
                clashes++;
            }
        }
        if (clashes > 0) {
            throw new ResponseStatusException(409, clashes == 1
                    ? "One booking falls in that time. Move or cancel it first"
                    : clashes + " bookings fall in that time. Move or cancel them first");
        }
        TimeOff row = new TimeOff();
        row.setId(Ids.next());
        row.setUsername(username);
        row.setStartsAt(Instant.ofEpochMilli(ask.start()));
        row.setEndsAt(Instant.ofEpochMilli(ask.end()));
        row.setReason(Text.optional(ask.reason(), 120, "the reason"));
        row.setCreatedAt(Instant.now());
        timeOff.insert(row);
        return dto(row);
    }

    public TimeOffDto removeTimeOff(String username, String id) throws IOException {
        TimeOffDto removed = removeTimeOffTx(username, id);
        live.scheduleChanged(username);
        return removed;
    }

    @Transactional(rollbackFor = IOException.class)
    private TimeOffDto removeTimeOffTx(String username, String id) {
        TimeOff row = timeOff.findFirstByIdAndUsername(id, username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such time off");
        }
        TimeOffDto dto = dto(row);
        timeOff.deleteById(id);
        return dto;
    }

    // -------------------------------------------------------------- schedules

    // tag::slate-schedule[]
    /// One provider's schedule for `days` days from `from`, the start of a day
    /// at their default location.
    @Transactional(readOnly = true)
    public ScheduleDto schedule(String username, long from, int days) {
        Provider p = provider(username);
        int span = Math.max(1, Math.min(MAX_DAYS, days));
        ZoneId zone = zoneOf(p.getDefaultLocation());
        LocalDate first = Instant.ofEpochMilli(from).atZone(zone).toLocalDate();
        return schedule(p, first, span, zone);
    }
    // end::slate-schedule[]

    /// Every active provider's schedule for one day, for the owner's
    /// side-by-side view.
    @Transactional(readOnly = true)
    public List<ScheduleDto> day(String dayText) {
        LocalDate day = Availability.day(dayText);
        List<Provider> all = providers.findByActiveOrderByDisplayNameAsc(true);
        List<ScheduleDto> out = new ArrayList<ScheduleDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            Provider p = all.get(iter);
            out.add(schedule(p, day, 1, zoneOf(p.getDefaultLocation())));
        }
        return out;
    }

    private ScheduleDto schedule(Provider p, LocalDate first, int span, ZoneId zone) {
        long start = Availability.at(first, 0, zone);
        long end = Availability.at(first.plusDays(span), 0, zone);
        Instant from = Instant.ofEpochMilli(start);
        Instant to = Instant.ofEpochMilli(end);

        List<ShiftDto> shifts = new ArrayList<ShiftDto>();
        List<WorkingHours> week = hours.findByUsernameOrderByWeekdayAscStartMinuteAsc(
                p.getUsername());
        for (int d = 0; d < span; d++) {
            LocalDate day = first.plusDays(d);
            int weekday = day.getDayOfWeek().getValue();
            for (int iter = 0; iter < week.size(); iter++) {
                WorkingHours w = week.get(iter);
                if (w.getWeekday() == weekday) {
                    ZoneId there = zoneOf(w.getLocationId());
                    shifts.add(new ShiftDto(Availability.at(day, w.getStartMinute(), there),
                            Availability.at(day, w.getEndMinute(), there), w.getLocationId()));
                }
            }
        }

        List<Appointment> theirs = appointments.forProvider(p.getUsername(), from, to);
        List<Appointment> shown = new ArrayList<Appointment>();
        for (int iter = 0; iter < theirs.size(); iter++) {
            if (!States.CANCELLED.equals(theirs.get(iter).getState())) {
                shown.add(theirs.get(iter));
            }
        }
        List<AppointmentDto> booked = bookings.dtos(shown);

        List<BusyDto> blocked = new ArrayList<BusyDto>();
        List<String> me = new ArrayList<String>();
        me.add(p.getUsername());
        List<TimeOff> off = timeOff.overlapping(me, from, to);
        for (int iter = 0; iter < off.size(); iter++) {
            TimeOff t = off.get(iter);
            blocked.add(new BusyDto(t.getStartsAt().toEpochMilli(), t.getEndsAt().toEpochMilli(),
                    "TIME_OFF", t.getReason(), t.getId()));
        }
        List<BusyBlock> external = busy.overlapping(me, from, to);
        for (int iter = 0; iter < external.size(); iter++) {
            BusyBlock b = external.get(iter);
            // A calendar says busy and nothing more: Slate never reads what
            // the provider's other appointments are.
            blocked.add(new BusyDto(b.getStartsAt().toEpochMilli(), b.getEndsAt().toEpochMilli(),
                    "CALENDAR", "Busy", b.getLinkId()));
        }
        return new ScheduleDto(p.getUsername(), p.getDisplayName(), p.getColor(), start, end,
                shifts, booked, blocked);
    }

    private Provider provider(String username) {
        Provider p = providers.findById(username);
        if (p == null) {
            throw new ResponseStatusException(404, "No such provider");
        }
        return p;
    }

    private ZoneId zoneOf(String locationId) {
        Location l = locationId == null || locationId.length() == 0 ? null
                : locations.findById(locationId);
        return l == null ? catalog.zone() : ZoneId.of(l.getTimeZone());
    }

    private static boolean open(Appointment a) {
        return States.HELD.equals(a.getState()) || States.CONFIRMED.equals(a.getState());
    }

    private static TimeOffDto dto(TimeOff t) {
        return new TimeOffDto(t.getId(), t.getStartsAt().toEpochMilli(),
                t.getEndsAt().toEpochMilli(), t.getReason());
    }
}
