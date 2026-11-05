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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.slate.api.DaySlotsDto;
import com.codenameone.examples.slate.api.FindTimeDto;
import com.codenameone.examples.slate.api.FindTimeRequestDto;
import com.codenameone.examples.slate.api.PreviewDto;
import com.codenameone.examples.slate.api.PreviewRequestDto;
import com.codenameone.examples.slate.api.PreviewRowDto;
import com.codenameone.examples.slate.api.SlotDto;
import com.codenameone.examples.slate.catalog.Catalog;
import com.codenameone.examples.slate.domain.Appointment;
import com.codenameone.examples.slate.domain.AppointmentParty;
import com.codenameone.examples.slate.domain.Business;
import com.codenameone.examples.slate.domain.BusyBlock;
import com.codenameone.examples.slate.domain.GroupSlot;
import com.codenameone.examples.slate.domain.Location;
import com.codenameone.examples.slate.domain.Provider;
import com.codenameone.examples.slate.domain.Resource;
import com.codenameone.examples.slate.domain.Service;
import com.codenameone.examples.slate.domain.TimeOff;
import com.codenameone.examples.slate.domain.TravelTime;
import com.codenameone.examples.slate.domain.WorkingHours;
import com.codenameone.examples.slate.repository.AppointmentPartyRepository;
import com.codenameone.examples.slate.repository.AppointmentRepository;
import com.codenameone.examples.slate.repository.BusyBlockRepository;
import com.codenameone.examples.slate.repository.GroupSlotRepository;
import com.codenameone.examples.slate.repository.LocationRepository;
import com.codenameone.examples.slate.repository.ResourceRepository;
import com.codenameone.examples.slate.repository.TimeOffRepository;
import com.codenameone.examples.slate.repository.TravelTimeRepository;
import com.codenameone.examples.slate.repository.WorkingHoursRepository;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Turns the database into what [SlotEngine] decides on, one day at a time.
///
/// A day is a calendar day at the location, in the location's zone. Its start
/// times are read off that clock -- 9:00, 9:15, 9:30 -- and only then made
/// instants, so a day that changes to or from daylight saving still starts at
/// 9:00. A local time the change skips is not offered; one it repeats is
/// offered once, at its first occurrence.
@Component
public class Availability {
    /// How far either side of the day the busy time is read: far enough for
    /// buffers, travel and a run of back-to-back work that began the day before.
    private static final long MARGIN = 86400000L;

    private final Catalog catalog;
    private final LocationRepository locations;
    private final WorkingHoursRepository hours;
    private final TimeOffRepository timeOff;
    private final BusyBlockRepository busyBlocks;
    private final AppointmentRepository appointments;
    private final AppointmentPartyRepository parties;
    private final ResourceRepository resources;
    private final GroupSlotRepository groups;
    private final TravelTimeRepository travel;

    public Availability(Catalog catalog, LocationRepository locations,
            WorkingHoursRepository hours, TimeOffRepository timeOff,
            BusyBlockRepository busyBlocks, AppointmentRepository appointments,
            AppointmentPartyRepository parties, ResourceRepository resources,
            GroupSlotRepository groups, TravelTimeRepository travel) {
        this.catalog = catalog;
        this.locations = locations;
        this.hours = hours;
        this.timeOff = timeOff;
        this.busyBlocks = busyBlocks;
        this.appointments = appointments;
        this.parties = parties;
        this.resources = resources;
        this.groups = groups;
        this.travel = travel;
    }

    /// The starts a day offers a client.
    @Transactional(readOnly = true)
    public DaySlotsDto day(String client, String serviceId, String locationId, String dayText,
            String providersCsv) {
        Business business = catalog.main();
        Service service = catalog.offered(serviceId);
        Location location = catalog.openLocation(locationId);
        LocalDate day = day(dayText);
        List<String> required = csv(providersCsv);
        List<Provider> candidates = candidates(service, required);
        SlotEngine.Request request = request(business, service, location, day, candidates,
                required, client, System.currentTimeMillis(), null);
        SlotEngine.Result result = SlotEngine.evaluate(request);
        String empty = "";
        if (result.slots.isEmpty()) {
            empty = candidates.isEmpty() ? SlotEngine.NOT_ENOUGH_PROVIDERS
                    : request.starts.length == 0 ? SlotEngine.OUTSIDE_HOURS
                    : mostCommon(result);
        }
        return new DaySlotsDto(day.toString(), location.getTimeZone(),
                slots(result.slots, candidates), empty);
    }

    /// The first times that suit every provider named, over up to 14 days.
    @Transactional(readOnly = true)
    public FindTimeDto find(String client, FindTimeRequestDto ask) {
        if (ask == null || ask.providers() == null || ask.providers().isEmpty()) {
            throw new ResponseStatusException(400, "Name who has to be there");
        }
        Business business = catalog.main();
        Service service = catalog.offered(ask.serviceId());
        Location location = catalog.openLocation(ask.locationId());
        LocalDate from = day(ask.fromDay());
        int days = Math.max(1, Math.min(14, ask.days()));
        List<Provider> candidates = candidates(service, ask.providers());
        for (int iter = 0; iter < ask.providers().size(); iter++) {
            if (!contains(candidates, ask.providers().get(iter))) {
                throw new ResponseStatusException(409, ask.providers().get(iter)
                        + " does not offer " + service.getName());
            }
        }
        List<SlotDto> out = new ArrayList<SlotDto>();
        long now = System.currentTimeMillis();
        for (int d = 0; d < days && out.size() < 20; d++) {
            SlotEngine.Request request = request(business, service, location, from.plusDays(d),
                    candidates, ask.providers(), client, now, null);
            request.need.providersNeeded = Math.max(request.need.providersNeeded,
                    ask.providers().size());
            List<SlotEngine.Slot> found = SlotEngine.evaluate(request).slots;
            for (int iter = 0; iter < found.size() && out.size() < 20; iter++) {
                out.add(slot(found.get(iter), candidates));
            }
        }
        return new FindTimeDto(out);
    }

    // tag::slate-preview[]
    /// What a day would offer under rules that are not saved: the business
    /// row is copied, the draft rules applied to the copy, and the engine asked
    /// to explain every start for every provider. Nothing is written.
    @Transactional(readOnly = true)
    public PreviewDto preview(PreviewRequestDto ask) {
        if (ask == null) {
            throw new ResponseStatusException(400, "Nothing to preview");
        }
        Business draft = copy(catalog.main());
        if (ask.policy() != null) {
            Catalog.apply(ask.policy(), draft);
        }
        Service service = catalog.offered(ask.serviceId());
        Location location = catalog.openLocation(ask.locationId());
        LocalDate day = day(ask.day());
        List<Provider> candidates = candidates(service, new ArrayList<String>());
        // The preview is of the rules, not of the clock: a day that is already
        // half over would show most of it as too soon.
        long now = ZonedDateTime.of(day, LocalTime.MIDNIGHT, ZoneId.of(location.getTimeZone()))
                .toInstant().toEpochMilli() - (long) draft.getMinNoticeMinutes() * 60000L;
        SlotEngine.Request request = request(draft, service, location, day, candidates,
                new ArrayList<String>(), "", now, null);
        request.explain = true;
        SlotEngine.Result result = SlotEngine.evaluate(request);
        // end::slate-preview[]
        ZoneId zone = ZoneId.of(location.getTimeZone());
        List<Integer> starts = new ArrayList<Integer>();
        Map<Long, Integer> index = new HashMap<Long, Integer>();
        for (int iter = 0; iter < request.starts.length; iter++) {
            ZonedDateTime local = Instant.ofEpochMilli(request.starts[iter]).atZone(zone);
            starts.add(Integer.valueOf(local.getHour() * 60 + local.getMinute()));
            index.put(Long.valueOf(request.starts[iter]), Integer.valueOf(iter));
        }
        List<PreviewRowDto> rows = new ArrayList<PreviewRowDto>();
        for (int p = 0; p < candidates.size(); p++) {
            String[] reasons = new String[starts.size()];
            for (int iter = 0; iter < result.verdicts.size(); iter++) {
                SlotEngine.Verdict v = result.verdicts.get(iter);
                Integer at = index.get(Long.valueOf(v.start));
                if (at == null) {
                    continue;
                }
                if (v.provider.length() == 0 || v.provider.equals(candidates.get(p).getUsername())) {
                    if (reasons[at.intValue()] == null || v.provider.length() == 0) {
                        reasons[at.intValue()] = v.reason;
                    }
                }
            }
            List<String> list = new ArrayList<String>();
            for (int iter = 0; iter < reasons.length; iter++) {
                list.add(reasons[iter] == null ? SlotEngine.OK : reasons[iter]);
            }
            rows.add(new PreviewRowDto(candidates.get(p).getUsername(),
                    candidates.get(p).getDisplayName(), list));
        }
        return new PreviewDto(day.toString(), service.getId(), starts, rows, result.slots.size());
    }

    /// Everything the engine needs for one day. `skipAppointment` leaves one
    /// appointment out of the busy time, which is how a booking being moved
    /// does not stand in its own way.
    public SlotEngine.Request request(Business business, Service service, Location location,
            LocalDate day, List<Provider> candidates, List<String> required, String client,
            long now, String skipAppointment) {
        ZoneId zone = ZoneId.of(location.getTimeZone());
        long dayStart = ZonedDateTime.of(day, LocalTime.MIDNIGHT, zone).toInstant().toEpochMilli();
        long dayEnd = ZonedDateTime.of(day.plusDays(1), LocalTime.MIDNIGHT, zone).toInstant()
                .toEpochMilli();
        Instant from = Instant.ofEpochMilli(dayStart - MARGIN);
        Instant to = Instant.ofEpochMilli(dayEnd + MARGIN);

        SlotEngine.Request r = new SlotEngine.Request();
        r.rules = rules(business, service, now);
        r.need = need(service, location.getId());
        r.starts = starts(day, zone, business.getStepMinutes());
        for (int iter = 0; iter < required.size(); iter++) {
            r.required.add(required.get(iter));
        }
        List<TravelTime> trips = travel.everyById();
        for (int iter = 0; iter < trips.size(); iter++) {
            r.travel(trips.get(iter).getFromLocation(), trips.get(iter).getToLocation(),
                    trips.get(iter).getMinutes());
        }

        // Every appointment that takes time near the day, once, with who and
        // what it takes.
        List<Appointment> near = appointments.between(from, to);
        Map<String, Appointment> byId = new HashMap<String, Appointment>();
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < near.size(); iter++) {
            Appointment a = near.get(iter);
            if (skipAppointment != null && skipAppointment.equals(a.getId())) {
                continue;
            }
            byId.put(a.getId(), a);
            ids.add(a.getId());
        }
        Map<String, List<SlotEngine.Busy>> busyByRef = new HashMap<String, List<SlotEngine.Busy>>();
        Map<String, Integer> todayByRef = new HashMap<String, Integer>();
        if (!ids.isEmpty()) {
            List<AppointmentParty> taken = parties.findByAppointmentIdIn(ids);
            List<String> countedGroups = new ArrayList<String>();
            for (int iter = 0; iter < taken.size(); iter++) {
                AppointmentParty party = taken.get(iter);
                if (States.GUEST.equals(party.getKind())) {
                    continue;
                }
                Appointment a = byId.get(party.getAppointmentId());
                add(busyByRef, party.getRef(), new SlotEngine.Busy(a.getBlockStart().toEpochMilli(),
                        a.getBlockEnd().toEpochMilli(), SlotEngine.KIND_APPOINTMENT,
                        a.getLocationId(), a.getGroupKey()));
                long start = a.getStartsAt().toEpochMilli();
                // The seats of one group slot are one appointment of the
                // provider's day, not one each.
                String counted = party.getRef() + "|" + (a.getGroupKey().length() > 0
                        ? a.getGroupKey() : a.getId());
                if (start >= dayStart && start < dayEnd && !countedGroups.contains(counted)) {
                    countedGroups.add(counted);
                    Integer was = todayByRef.get(party.getRef());
                    todayByRef.put(party.getRef(), Integer.valueOf(was == null ? 1
                            : was.intValue() + 1));
                }
            }
        }

        List<String> usernames = new ArrayList<String>();
        for (int iter = 0; iter < candidates.size(); iter++) {
            usernames.add(candidates.get(iter).getUsername());
        }
        Map<String, List<WorkingHours>> weekByUser = new HashMap<String, List<WorkingHours>>();
        Map<String, List<SlotEngine.Busy>> offByUser = new HashMap<String, List<SlotEngine.Busy>>();
        if (!usernames.isEmpty()) {
            List<WorkingHours> week = hours.findByUsernameIn(usernames);
            for (int iter = 0; iter < week.size(); iter++) {
                WorkingHours w = week.get(iter);
                List<WorkingHours> list = weekByUser.get(w.getUsername());
                if (list == null) {
                    list = new ArrayList<WorkingHours>();
                    weekByUser.put(w.getUsername(), list);
                }
                list.add(w);
            }
            List<TimeOff> off = timeOff.overlapping(usernames, from, to);
            for (int iter = 0; iter < off.size(); iter++) {
                TimeOff t = off.get(iter);
                add(offByUser, t.getUsername(), new SlotEngine.Busy(t.getStartsAt().toEpochMilli(),
                        t.getEndsAt().toEpochMilli(), SlotEngine.KIND_TIME_OFF, "", ""));
            }
            List<BusyBlock> external = busyBlocks.overlapping(usernames, from, to);
            for (int iter = 0; iter < external.size(); iter++) {
                BusyBlock b = external.get(iter);
                add(offByUser, b.getUsername(), new SlotEngine.Busy(b.getStartsAt().toEpochMilli(),
                        b.getEndsAt().toEpochMilli(), SlotEngine.KIND_CALENDAR, "", ""));
            }
        }

        Map<String, ZoneId> zones = new HashMap<String, ZoneId>();
        int weekday = day.getDayOfWeek().getValue();
        for (int iter = 0; iter < candidates.size(); iter++) {
            Provider p = candidates.get(iter);
            SlotEngine.Party party = new SlotEngine.Party();
            party.id = p.getUsername();
            party.maxPerDay = p.getMaxPerDay();
            party.maxContinuousMinutes = p.getMaxContinuousMinutes();
            party.breakMinutes = p.getBreakMinutes();
            Integer today = todayByRef.get(p.getUsername());
            party.appointmentsToday = today == null ? 0 : today.intValue();
            List<WorkingHours> week = weekByUser.get(p.getUsername());
            for (int w = 0; week != null && w < week.size(); w++) {
                WorkingHours shift = week.get(w);
                if (shift.getWeekday() != weekday) {
                    continue;
                }
                ZoneId there = zoneOf(zones, shift.getLocationId(), zone);
                party.shifts.add(new SlotEngine.Shift(at(day, shift.getStartMinute(), there),
                        at(day, shift.getEndMinute(), there), shift.getLocationId()));
            }
            addAll(party.busy, busyByRef.get(p.getUsername()));
            addAll(party.busy, offByUser.get(p.getUsername()));
            r.providers.add(party);
        }

        if (service.getResourceKind().length() > 0) {
            List<Resource> pool = resources.findByLocationIdAndKindAndActiveOrderByIdAsc(
                    location.getId(), service.getResourceKind(), true);
            for (int iter = 0; iter < pool.size(); iter++) {
                SlotEngine.Res res = new SlotEngine.Res();
                res.id = pool.get(iter).getId();
                addAll(res.busy, busyByRef.get(res.id));
                r.resources.add(res);
            }
        }

        if (client != null && client.length() > 0) {
            List<Appointment> mine = appointments.clientBetween(client, from, to);
            for (int iter = 0; iter < mine.size(); iter++) {
                Appointment a = mine.get(iter);
                if (skipAppointment != null && skipAppointment.equals(a.getId())) {
                    continue;
                }
                r.client.add(new SlotEngine.Busy(a.getStartsAt().toEpochMilli(),
                        a.getEndsAt().toEpochMilli(), SlotEngine.KIND_APPOINTMENT,
                        a.getLocationId(), a.getGroupKey()));
                long start = a.getStartsAt().toEpochMilli();
                if (start >= dayStart && start < dayEnd) {
                    r.clientAppointmentsToday++;
                }
            }
        }

        if (service.getCapacity() > 1) {
            for (int iter = 0; iter < near.size(); iter++) {
                Appointment a = near.get(iter);
                if (a.getServiceId().equals(service.getId()) && a.getGroupKey().length() > 0
                        && !r.groupSeats.containsKey(a.getGroupKey())) {
                    GroupSlot slot = groups.findById(a.getGroupKey());
                    if (slot != null && slot.getSeats() > 0) {
                        r.groupSeats.put(a.getGroupKey(), Integer.valueOf(slot.getSeats()));
                    }
                }
            }
        }
        return r;
    }

    /// The business's rules with the service's own values in place.
    public static SlotEngine.Rules rules(Business b, Service s, long now) {
        SlotEngine.Rules rules = new SlotEngine.Rules();
        rules.now = now;
        rules.minNoticeMinutes = s.getMinNoticeMinutes() >= 0 ? s.getMinNoticeMinutes()
                : b.getMinNoticeMinutes();
        rules.horizonDays = b.getHorizonDays();
        rules.clientGapMinutes = b.getClientGapMinutes();
        rules.clientPerDay = b.getClientPerDay();
        rules.minUsableGapMinutes = b.getMinUsableGapMinutes();
        rules.tight = Catalog.TIGHT.equals(b.getPacking());
        return rules;
    }

    public static SlotEngine.Need need(Service s, String locationId) {
        SlotEngine.Need need = new SlotEngine.Need();
        need.serviceId = s.getId();
        need.locationId = locationId;
        need.minutes = s.getMinutes();
        need.bufferBefore = s.getBufferBefore();
        need.bufferAfter = s.getBufferAfter();
        need.capacity = s.getCapacity();
        need.providersNeeded = s.getProvidersNeeded();
        need.resourceKind = s.getResourceKind();
        return need;
    }

    // tag::slate-local-starts[]
    /// The start times of a day, read off the local clock every `step`
    /// minutes. A time that does not exist that day -- skipped by a change to
    /// summer time -- is left out rather than moved.
    public static long[] starts(LocalDate day, ZoneId zone, int step) {
        int each = Math.max(SlotEngine.CELL_MINUTES, step);
        List<Long> out = new ArrayList<Long>();
        for (int minute = 0; minute < 24 * 60; minute += each) {
            LocalTime wanted = LocalTime.of(minute / 60, minute % 60);
            ZonedDateTime at = ZonedDateTime.of(day, wanted, zone);
            if (!at.toLocalTime().equals(wanted) || !at.toLocalDate().equals(day)) {
                continue;
            }
            out.add(Long.valueOf(at.toInstant().toEpochMilli()));
        }
        long[] starts = new long[out.size()];
        for (int iter = 0; iter < starts.length; iter++) {
            starts[iter] = out.get(iter).longValue();
        }
        return starts;
    }
    // end::slate-local-starts[]

    /// The instant `minute` minutes after midnight on the local clock. 1440 is
    /// the next midnight. A minute a change to summer time skips is moved past
    /// the change, which for a shift's edge is what the provider means.
    public static long at(LocalDate day, int minute, ZoneId zone) {
        if (minute >= 24 * 60) {
            return ZonedDateTime.of(day.plusDays(1), LocalTime.MIDNIGHT, zone).toInstant()
                    .toEpochMilli();
        }
        return ZonedDateTime.of(day, LocalTime.of(minute / 60, minute % 60), zone).toInstant()
                .toEpochMilli();
    }

    /// The providers a service can go to. Answers 409 when the client named one
    /// who does not offer it.
    public List<Provider> candidates(Service service, List<String> required) {
        List<Provider> all = catalog.providersOf(service.getId());
        for (int iter = 0; iter < required.size(); iter++) {
            if (!contains(all, required.get(iter))) {
                throw new ResponseStatusException(409, "That person does not offer "
                        + service.getName());
            }
        }
        return all;
    }

    public static LocalDate day(String text) {
        try {
            return LocalDate.parse(text == null ? "" : text.trim());
        } catch (DateTimeException bad) {
            throw new ResponseStatusException(400, "A day is written yyyy-MM-dd");
        }
    }

    public static List<String> csv(String text) {
        List<String> out = new ArrayList<String>();
        if (text == null) {
            return out;
        }
        int at = 0;
        while (at <= text.length()) {
            int comma = text.indexOf(',', at);
            String one = (comma < 0 ? text.substring(at) : text.substring(at, comma)).trim();
            if (one.length() > 0 && !out.contains(one)) {
                out.add(one);
            }
            if (comma < 0) {
                break;
            }
            at = comma + 1;
        }
        return out;
    }

    static List<SlotDto> slots(List<SlotEngine.Slot> found, List<Provider> candidates) {
        List<SlotDto> out = new ArrayList<SlotDto>();
        for (int iter = 0; iter < found.size(); iter++) {
            out.add(slot(found.get(iter), candidates));
        }
        return out;
    }

    static SlotDto slot(SlotEngine.Slot s, List<Provider> candidates) {
        List<String> names = new ArrayList<String>();
        for (int iter = 0; iter < s.providers.size(); iter++) {
            String id = s.providers.get(iter);
            String name = id;
            for (int c = 0; c < candidates.size(); c++) {
                if (candidates.get(c).getUsername().equals(id)) {
                    name = candidates.get(c).getDisplayName();
                }
            }
            names.add(name);
        }
        return new SlotDto(s.start, s.end, new ArrayList<String>(s.providers), names, s.seatsLeft);
    }

    /// The reason most of a day's starts were turned down for, which is what
    /// a client is told about a day with nothing.
    private static String mostCommon(SlotEngine.Result result) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        String best = "";
        int most = 0;
        for (int iter = 0; iter < result.verdicts.size(); iter++) {
            String reason = result.verdicts.get(iter).reason;
            // Too soon says nothing about the day: it is just the morning that
            // has gone.
            if (SlotEngine.OK.equals(reason) || SlotEngine.TOO_SOON.equals(reason)) {
                continue;
            }
            Integer was = counts.get(reason);
            int now = was == null ? 1 : was.intValue() + 1;
            counts.put(reason, Integer.valueOf(now));
            if (now > most) {
                most = now;
                best = reason;
            }
        }
        return best.length() == 0 ? SlotEngine.TOO_SOON : best;
    }

    private ZoneId zoneOf(Map<String, ZoneId> zones, String locationId, ZoneId fallback) {
        ZoneId known = zones.get(locationId);
        if (known == null) {
            Location there = locations.findById(locationId);
            known = there == null ? fallback : ZoneId.of(there.getTimeZone());
            zones.put(locationId, known);
        }
        return known;
    }

    private static Business copy(Business b) {
        Business out = new Business();
        out.setId(b.getId());
        out.setTimeZone(b.getTimeZone());
        out.setStepMinutes(b.getStepMinutes());
        out.setMinNoticeMinutes(b.getMinNoticeMinutes());
        out.setHorizonDays(b.getHorizonDays());
        out.setHoldMinutes(b.getHoldMinutes());
        out.setCancelHours(b.getCancelHours());
        out.setRescheduleHours(b.getRescheduleHours());
        out.setDepositPercent(b.getDepositPercent());
        out.setNoShowFeePercent(b.getNoShowFeePercent());
        out.setClientGapMinutes(b.getClientGapMinutes());
        out.setClientPerDay(b.getClientPerDay());
        out.setMinUsableGapMinutes(b.getMinUsableGapMinutes());
        out.setPacking(b.getPacking());
        out.setReminderHours(b.getReminderHours());
        return out;
    }

    private static boolean contains(List<Provider> list, String username) {
        for (int iter = 0; iter < list.size(); iter++) {
            if (list.get(iter).getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    private static void add(Map<String, List<SlotEngine.Busy>> map, String key,
            SlotEngine.Busy busy) {
        List<SlotEngine.Busy> list = map.get(key);
        if (list == null) {
            list = new ArrayList<SlotEngine.Busy>();
            map.put(key, list);
        }
        list.add(busy);
    }

    private static void addAll(List<SlotEngine.Busy> to, List<SlotEngine.Busy> from) {
        if (from != null) {
            to.addAll(from);
        }
    }
}
