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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// tag::slate-slot-engine[]
/// Decides which start times can be booked: every party a booking needs has to
/// be free, and every rule the business set has to hold.
///
/// It reads nothing and writes nothing. What it is given -- the start times to
/// try, each provider's shifts and busy time, the resources, the client's own
/// day -- is assembled by [Availability] from the database, and the same
/// decision is made again by [Bookings] for the one start a client picks, just
/// before the time is claimed. Keeping it pure is what lets the rules be
/// tested one at a time, and lets the owner's preview say, for every start and
/// every provider, which rule turned it down.
///
/// Times are milliseconds since the epoch. The engine knows nothing of zones:
/// the start times it is handed were already chosen on the local clock, and a
/// shift is already the two instants it runs between, so a day that changes to
/// or from daylight saving is the caller's to get right, once.
public final class SlotEngine {
    /// The grid every duration and every buffer is a multiple of, and the
    /// size of one [com.codenameone.examples.slate.domain.SlotClaim].
    public static final int CELL_MINUTES = 5;
    public static final long MINUTE = 60000L;
    public static final long CELL = CELL_MINUTES * MINUTE;

    /// Why a start was turned down. [#OK] is the one that was not.
    public static final String OK = "OK";
    public static final String TOO_SOON = "TOO_SOON";
    public static final String TOO_FAR = "TOO_FAR";
    public static final String OUTSIDE_HOURS = "OUTSIDE_HOURS";
    public static final String BOOKED = "BOOKED";
    public static final String TIME_OFF = "TIME_OFF";
    public static final String CALENDAR = "CALENDAR";
    public static final String TRAVEL = "TRAVEL";
    public static final String DAY_FULL = "DAY_FULL";
    public static final String NEEDS_BREAK = "NEEDS_BREAK";
    public static final String LEAVES_GAP = "LEAVES_GAP";
    public static final String NOT_ADJACENT = "NOT_ADJACENT";
    public static final String NO_RESOURCE = "NO_RESOURCE";
    public static final String CLIENT_BUSY = "CLIENT_BUSY";
    public static final String CLIENT_GAP = "CLIENT_GAP";
    public static final String CLIENT_DAY_FULL = "CLIENT_DAY_FULL";
    public static final String GROUP_FULL = "GROUP_FULL";
    public static final String NOT_ENOUGH_PROVIDERS = "NOT_ENOUGH_PROVIDERS";

    /// What kind of time a [Busy] is.
    public static final String KIND_APPOINTMENT = "APPOINTMENT";
    public static final String KIND_TIME_OFF = "TIME_OFF";
    public static final String KIND_CALENDAR = "CALENDAR";

    private SlotEngine() {
    }

    /// The business's rules, with the service's own values already applied.
    public static final class Rules {
        public long now;
        public int minNoticeMinutes;
        public int horizonDays;
        public int clientGapMinutes;
        public int clientPerDay;
        public int minUsableGapMinutes;
        public boolean tight;
    }

    /// What the service needs.
    public static final class Need {
        public String serviceId = "";
        public String locationId = "";
        public int minutes;
        public int bufferBefore;
        public int bufferAfter;
        public int capacity = 1;
        public int providersNeeded = 1;
        public String resourceKind = "";
    }

    /// Time that is taken. `locationId` is where an appointment is, for the
    /// travel rule; `groupKey` is the group slot it is a seat of, which a
    /// further seat of the same slot does not count as busy.
    public static final class Busy {
        public final long start;
        public final long end;
        public final String kind;
        public final String locationId;
        public final String groupKey;

        public Busy(long start, long end, String kind, String locationId, String groupKey) {
            this.start = start;
            this.end = end;
            this.kind = kind;
            this.locationId = locationId == null ? "" : locationId;
            this.groupKey = groupKey == null ? "" : groupKey;
        }
    }

    /// One stretch of a provider's working day, at one location.
    public static final class Shift {
        public final long start;
        public final long end;
        public final String locationId;

        public Shift(long start, long end, String locationId) {
            this.start = start;
            this.end = end;
            this.locationId = locationId;
        }
    }

    /// A provider as the engine sees them for one day.
    public static final class Party {
        public String id = "";
        public final List<Shift> shifts = new ArrayList<Shift>();
        public final List<Busy> busy = new ArrayList<Busy>();
        /// Appointments already on the day the starts fall in.
        public int appointmentsToday;
        public int maxPerDay;
        public int maxContinuousMinutes;
        public int breakMinutes;
    }

    /// A resource at the location, with the time it is taken.
    public static final class Res {
        public String id = "";
        public final List<Busy> busy = new ArrayList<Busy>();
    }

    /// Everything one evaluation needs.
    public static final class Request {
        public long[] starts = new long[0];
        public Rules rules = new Rules();
        public Need need = new Need();
        public final List<Party> providers = new ArrayList<Party>();
        /// Providers the client asked for by name; every one of them has to be
        /// free. Empty for whoever is.
        public final List<String> required = new ArrayList<String>();
        public final List<Res> resources = new ArrayList<Res>();
        /// The client's own appointments around the day, of any service.
        public final List<Busy> client = new ArrayList<Busy>();
        public int clientAppointmentsToday;
        /// Seats already taken in each group slot, by group key.
        public final Map<String, Integer> groupSeats = new HashMap<String, Integer>();
        /// Asks for a verdict per start and provider, for the owner's preview.
        public boolean explain;
        /// Travel minutes between two locations, both ways; see [#travel].
        public final Map<String, Integer> travel = new HashMap<String, Integer>();

        public void travel(String from, String to, int minutes) {
            travel.put(from + "|" + to, Integer.valueOf(minutes));
        }
    }

    /// A start that can be booked, and who it would be with.
    public static final class Slot {
        public final long start;
        public final long end;
        public final List<String> providers;
        public final String resourceId;
        public final int seatsLeft;

        Slot(long start, long end, List<String> providers, String resourceId, int seatsLeft) {
            this.start = start;
            this.end = end;
            this.providers = providers;
            this.resourceId = resourceId;
            this.seatsLeft = seatsLeft;
        }
    }

    /// One start for one provider, and the rule that decided it. A verdict
    /// with an empty provider is about the start itself (too soon, the
    /// client's own day).
    public static final class Verdict {
        public final long start;
        public final String provider;
        public final String reason;

        Verdict(long start, String provider, String reason) {
            this.start = start;
            this.provider = provider;
            this.reason = reason;
        }
    }

    public static final class Result {
        public final List<Slot> slots = new ArrayList<Slot>();
        public final List<Verdict> verdicts = new ArrayList<Verdict>();

        /// The slot at `start`, or null when it cannot be booked.
        public Slot at(long start) {
            for (int iter = 0; iter < slots.size(); iter++) {
                if (slots.get(iter).start == start) {
                    return slots.get(iter);
                }
            }
            return null;
        }

        /// The first reason given for `start`, preferring one about the start
        /// itself; [#OK] when it can be booked, empty when it was not tried.
        public String reason(long start) {
            if (at(start) != null) {
                return OK;
            }
            String any = "";
            for (int iter = 0; iter < verdicts.size(); iter++) {
                Verdict v = verdicts.get(iter);
                if (v.start == start) {
                    if (v.provider.length() == 0) {
                        return v.reason;
                    }
                    if (any.length() == 0 && !OK.equals(v.reason)) {
                        any = v.reason;
                    }
                }
            }
            return any;
        }
    }

    public static Result evaluate(Request r) {
        Result out = new Result();
        Need need = r.need;
        Rules rules = r.rules;
        long length = need.minutes * MINUTE;
        long before = need.bufferBefore * MINUTE;
        long after = need.bufferAfter * MINUTE;
        long earliest = rules.now + rules.minNoticeMinutes * MINUTE;
        long latest = rules.horizonDays <= 0 ? Long.MAX_VALUE
                : rules.now + rules.horizonDays * 86400000L;
        boolean group = need.capacity > 1;
        int needed = Math.max(1, need.providersNeeded);
        for (int iter = 0; iter < r.starts.length; iter++) {
            long start = r.starts[iter];
            long end = start + length;
            String startReason = startRule(r, start, end, earliest, latest);
            if (startReason != null) {
                // Recorded even without explain: the client is told why a day
                // has nothing, and the reason for its first start is that.
                out.verdicts.add(new Verdict(start, "", startReason));
                continue;
            }
            List<Party> free = new ArrayList<Party>();
            boolean requiredMissing = false;
            Party joining = null;
            int groupSeatsLeft = need.capacity;
            for (int p = 0; p < r.providers.size(); p++) {
                Party party = r.providers.get(p);
                String groupKey = group ? groupKey(need.serviceId, party.id, start) : "";
                Integer taken = group ? r.groupSeats.get(groupKey) : null;
                String reason;
                if (taken != null) {
                    // A group slot that is already running with this provider
                    // is joined, not checked again: its time was decided when
                    // its first seat was booked.
                    reason = taken.intValue() >= need.capacity ? GROUP_FULL : OK;
                } else {
                    reason = providerRule(r, party, start - before, end + after, groupKey);
                }
                if (r.explain) {
                    out.verdicts.add(new Verdict(start, party.id, reason));
                }
                if (OK.equals(reason)) {
                    free.add(party);
                    if (taken != null && joining == null) {
                        joining = party;
                        groupSeatsLeft = need.capacity - taken.intValue();
                    }
                } else if (r.required.contains(party.id)) {
                    requiredMissing = true;
                }
            }
            if (requiredMissing) {
                continue;
            }
            if (joining != null && (r.required.size() == 0 || r.required.contains(joining.id))) {
                // A seat in a group slot that is already running goes with it:
                // its provider and its resource were settled by the first seat.
                List<String> with = new ArrayList<String>();
                with.add(joining.id);
                out.slots.add(new Slot(start, end, with, "", groupSeatsLeft));
                continue;
            }
            List<String> chosen = choose(r, free, needed);
            if (chosen == null) {
                if (!r.explain && r.providers.size() > 0) {
                    out.verdicts.add(new Verdict(start, "", free.size() == 0
                            ? firstProviderReason(r, start - before, end + after, start)
                            : NOT_ENOUGH_PROVIDERS));
                }
                continue;
            }
            String resource = "";
            if (need.resourceKind.length() > 0) {
                resource = freeResource(r, start - before, end + after);
                if (resource == null) {
                    out.verdicts.add(new Verdict(start, "", NO_RESOURCE));
                    continue;
                }
            }
            out.slots.add(new Slot(start, end, chosen, resource, group ? groupSeatsLeft : 1));
        }
        return out;
    }
    // end::slate-slot-engine[]

    /// The key every seat of one group slot shares.
    public static String groupKey(String serviceId, String provider, long start) {
        return serviceId + "|" + provider + "|" + start;
    }

    /// The rules about the start itself and the client's own day, or null when
    /// none turns it down.
    private static String startRule(Request r, long start, long end, long earliest,
            long latest) {
        Rules rules = r.rules;
        if (start < earliest) {
            return TOO_SOON;
        }
        if (start > latest) {
            return TOO_FAR;
        }
        if (rules.clientPerDay > 0 && r.clientAppointmentsToday >= rules.clientPerDay) {
            return CLIENT_DAY_FULL;
        }
        long gap = rules.clientGapMinutes * MINUTE;
        for (int iter = 0; iter < r.client.size(); iter++) {
            Busy own = r.client.get(iter);
            if (own.start < end && start < own.end) {
                return CLIENT_BUSY;
            }
            if (own.start < end + gap && start - gap < own.end) {
                return CLIENT_GAP;
            }
        }
        return null;
    }

    /// Whether one provider can take `[blockStart, blockEnd)`, the
    /// appointment with its buffers, and if not, why.
    static String providerRule(Request r, Party party, long blockStart, long blockEnd,
            String groupKey) {
        Shift shift = null;
        for (int iter = 0; iter < party.shifts.size(); iter++) {
            Shift s = party.shifts.get(iter);
            if (s.locationId.equals(r.need.locationId) && s.start <= blockStart
                    && blockEnd <= s.end) {
                shift = s;
                break;
            }
        }
        if (shift == null) {
            return OUTSIDE_HOURS;
        }
        // Busy time, nearest neighbours on each side within the shift, and
        // the travel rule, in one pass.
        long previousEnd = shift.start;
        long nextStart = shift.end;
        for (int iter = 0; iter < party.busy.size(); iter++) {
            Busy b = party.busy.get(iter);
            if (groupKey.length() > 0 && groupKey.equals(b.groupKey)) {
                continue;
            }
            if (b.start < blockEnd && blockStart < b.end) {
                return busyReason(b);
            }
            if (b.end <= blockStart) {
                if (KIND_APPOINTMENT.equals(b.kind) && b.locationId.length() > 0
                        && !b.locationId.equals(r.need.locationId)
                        && b.end + travel(r, b.locationId, r.need.locationId) * MINUTE
                            > blockStart) {
                    return TRAVEL;
                }
                if (b.end > previousEnd) {
                    previousEnd = b.end;
                }
            } else {
                if (KIND_APPOINTMENT.equals(b.kind) && b.locationId.length() > 0
                        && !b.locationId.equals(r.need.locationId)
                        && blockEnd + travel(r, r.need.locationId, b.locationId) * MINUTE
                            > b.start) {
                    return TRAVEL;
                }
                if (b.start < nextStart) {
                    nextStart = b.start;
                }
            }
        }
        if (party.maxPerDay > 0 && party.appointmentsToday >= party.maxPerDay) {
            return DAY_FULL;
        }
        if (party.maxContinuousMinutes > 0 && continuousRun(party, blockStart, blockEnd)
                > party.maxContinuousMinutes * MINUTE) {
            return NEEDS_BREAK;
        }
        long usable = r.rules.minUsableGapMinutes * MINUTE;
        if (usable > 0) {
            long gapBefore = blockStart - Math.max(previousEnd, shift.start);
            long gapAfter = Math.min(nextStart, shift.end) - blockEnd;
            if (gapBefore > 0 && gapBefore < usable || gapAfter > 0 && gapAfter < usable) {
                return LEAVES_GAP;
            }
        }
        // Touching anything counts: a shift's edge, a booking, time off.
        if (r.rules.tight && blockStart != previousEnd && blockEnd != nextStart) {
            return NOT_ADJACENT;
        }
        return OK;
    }

    private static String busyReason(Busy b) {
        if (KIND_TIME_OFF.equals(b.kind)) {
            return TIME_OFF;
        }
        if (KIND_CALENDAR.equals(b.kind)) {
            return CALENDAR;
        }
        return BOOKED;
    }

    /// The length of the run of work the new block would be part of: it and
    /// every appointment joined to it by less than the provider's break.
    private static long continuousRun(Party party, long blockStart, long blockEnd) {
        long gap = party.breakMinutes * MINUTE;
        long runStart = blockStart;
        long runEnd = blockEnd;
        boolean grew = true;
        while (grew) {
            grew = false;
            for (int iter = 0; iter < party.busy.size(); iter++) {
                Busy b = party.busy.get(iter);
                if (!KIND_APPOINTMENT.equals(b.kind)) {
                    continue;
                }
                if (b.end <= runStart && runStart - b.end < Math.max(gap, 1) && b.start < runStart) {
                    runStart = b.start;
                    grew = true;
                } else if (b.start >= runEnd && b.start - runEnd < Math.max(gap, 1)
                        && b.end > runEnd) {
                    runEnd = b.end;
                    grew = true;
                }
            }
        }
        return runEnd - runStart;
    }

    /// Minutes from one location to another, read both ways.
    static int travel(Request r, String from, String to) {
        Integer there = r.travel.get(from + "|" + to);
        if (there == null) {
            there = r.travel.get(to + "|" + from);
        }
        return there == null ? 0 : there.intValue();
    }

    /// The providers a slot goes to: every one the client named and then the
    /// least booked of the rest, or null when there are not enough.
    private static List<String> choose(Request r, List<Party> free, int needed) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < r.required.size(); iter++) {
            out.add(r.required.get(iter));
        }
        List<Party> rest = new ArrayList<Party>();
        for (int iter = 0; iter < free.size(); iter++) {
            if (!out.contains(free.get(iter).id)) {
                rest.add(free.get(iter));
            }
        }
        // The least busy first, so that work spreads across the team; by name
        // when two are equal, so the same request always gets the same answer.
        for (int i = 1; i < rest.size(); i++) {
            Party moving = rest.get(i);
            int j = i - 1;
            while (j >= 0 && (rest.get(j).appointmentsToday > moving.appointmentsToday
                    || rest.get(j).appointmentsToday == moving.appointmentsToday
                        && rest.get(j).id.compareTo(moving.id) > 0)) {
                rest.set(j + 1, rest.get(j));
                j--;
            }
            rest.set(j + 1, moving);
        }
        for (int iter = 0; iter < rest.size() && out.size() < needed; iter++) {
            out.add(rest.get(iter).id);
        }
        return out.size() < needed ? null : out;
    }

    private static String firstProviderReason(Request r, long blockStart, long blockEnd,
            long start) {
        String reason = OUTSIDE_HOURS;
        for (int p = 0; p < r.providers.size(); p++) {
            Party party = r.providers.get(p);
            String groupKey = r.need.capacity > 1 ? groupKey(r.need.serviceId, party.id, start) : "";
            Integer taken = groupKey.length() > 0 ? r.groupSeats.get(groupKey) : null;
            // A running group slot is judged by its seats, as evaluate judged
            // it; its own claim would otherwise read as free time and answer OK.
            String one = taken != null
                    ? (taken.intValue() >= r.need.capacity ? GROUP_FULL : OK)
                    : providerRule(r, party, blockStart, blockEnd, groupKey);
            // The most telling reason wins: being booked says more than being
            // outside the hours, which every provider off that day also is.
            if (!OUTSIDE_HOURS.equals(one)) {
                return one;
            }
        }
        return reason;
    }

    private static String freeResource(Request r, long blockStart, long blockEnd) {
        for (int iter = 0; iter < r.resources.size(); iter++) {
            Res res = r.resources.get(iter);
            boolean taken = false;
            for (int b = 0; b < res.busy.size() && !taken; b++) {
                Busy busy = res.busy.get(b);
                taken = busy.start < blockEnd && blockStart < busy.end;
            }
            if (!taken) {
                return res.id;
            }
        }
        return null;
    }

    /// The cell `millis` falls in.
    public static long cell(long millis) {
        return millis >= 0 ? millis / CELL : (millis - CELL + 1) / CELL;
    }
}
