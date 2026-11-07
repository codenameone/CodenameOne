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

import com.codenameone.examples.slate.booking.SlotEngine.Busy;
import com.codenameone.examples.slate.booking.SlotEngine.Party;
import com.codenameone.examples.slate.booking.SlotEngine.Request;
import com.codenameone.examples.slate.booking.SlotEngine.Res;
import com.codenameone.examples.slate.booking.SlotEngine.Result;
import com.codenameone.examples.slate.booking.SlotEngine.Shift;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The rules of the slot engine, one at a time, on a day built by hand.
///
/// Every test has a working day from 09:00 to 17:00 UTC and asks about a
/// 60 minute service; each adds the one thing its rule is about.
class SlotEngineTest {
    private static final long MIN = SlotEngine.MINUTE;
    /// 2030-01-07 00:00 UTC, a Monday.
    private static final long DAY = LocalDate.of(2030, 1, 7).atStartOfDay(ZoneId.of("UTC"))
            .toInstant().toEpochMilli();
    private static final String HERE = "loc-main";
    private static final String THERE = "loc-north";

    private static long at(int hour, int minute) {
        return DAY + (hour * 60L + minute) * MIN;
    }

    private static Party party(String id) {
        Party p = new Party();
        p.id = id;
        p.shifts.add(new Shift(at(9, 0), at(17, 0), HERE));
        return p;
    }

    private static Busy booked(long start, long end) {
        return new Busy(start, end, SlotEngine.KIND_APPOINTMENT, HERE, "");
    }

    private static Request request(Party... parties) {
        Request r = new Request();
        r.rules.now = DAY - 86400000L;
        r.rules.horizonDays = 60;
        r.need.serviceId = "svc";
        r.need.locationId = HERE;
        r.need.minutes = 60;
        for (int iter = 0; iter < parties.length; iter++) {
            r.providers.add(parties[iter]);
        }
        return r;
    }

    private static String reason(Request r, long start) {
        r.starts = new long[] {start};
        return SlotEngine.evaluate(r).reason(start);
    }

    @Test
    void aStartInsideTheShiftIsOffered() {
        Request r = request(party("a"));
        assertEquals(SlotEngine.OK, reason(r, at(9, 0)));
        assertEquals(SlotEngine.OK, reason(r, at(16, 0)));
        // Ending a minute past the shift is outside it.
        assertEquals(SlotEngine.OUTSIDE_HOURS, reason(r, at(16, 5)));
        assertEquals(SlotEngine.OUTSIDE_HOURS, reason(r, at(8, 30)));
    }

    @Test
    void noticeAndHorizonBoundTheStart() {
        Request r = request(party("a"));
        r.rules.now = at(9, 0);
        r.rules.minNoticeMinutes = 120;
        assertEquals(SlotEngine.TOO_SOON, reason(r, at(10, 55)));
        assertEquals(SlotEngine.OK, reason(r, at(11, 0)));
        r.rules.now = DAY - 3 * 86400000L;
        r.rules.minNoticeMinutes = 0;
        r.rules.horizonDays = 2;
        assertEquals(SlotEngine.TOO_FAR, reason(r, at(10, 0)));
    }

    @Test
    void busyTimeSaysWhatKindItIs() {
        Party a = party("a");
        a.busy.add(booked(at(10, 0), at(11, 0)));
        a.busy.add(new Busy(at(12, 0), at(13, 0), SlotEngine.KIND_TIME_OFF, "", ""));
        a.busy.add(new Busy(at(14, 0), at(15, 0), SlotEngine.KIND_CALENDAR, "", ""));
        Request r = request(a);
        assertEquals(SlotEngine.BOOKED, reason(r, at(10, 30)));
        assertEquals(SlotEngine.TIME_OFF, reason(r, at(11, 30)));
        assertEquals(SlotEngine.CALENDAR, reason(r, at(14, 59)));
        // Touching is not overlapping.
        assertEquals(SlotEngine.OK, reason(r, at(11, 0)));
        assertEquals(SlotEngine.OK, reason(r, at(13, 0)));
    }

    @Test
    void buffersAreTimeTheProviderIsNotFree() {
        Party a = party("a");
        a.busy.add(booked(at(11, 0), at(12, 0)));
        Request r = request(a);
        r.need.bufferAfter = 15;
        // 10:00 to 11:00 plus 15 minutes of clean-up runs into the 11:00.
        assertEquals(SlotEngine.BOOKED, reason(r, at(10, 0)));
        assertEquals(SlotEngine.OK, reason(r, at(9, 45)));
        // And a buffer cannot run past the end of the shift.
        assertEquals(SlotEngine.OUTSIDE_HOURS, reason(r, at(16, 0)));
    }

    @Test
    void aGapTooShortToSellIsNotLeft() {
        Party a = party("a");
        a.busy.add(booked(at(11, 0), at(12, 0)));
        Request r = request(a);
        r.rules.minUsableGapMinutes = 30;
        // 09:45 to 10:45 leaves 09:00-09:45 (fine) and 10:45-11:00 (15 minutes).
        assertEquals(SlotEngine.LEAVES_GAP, reason(r, at(9, 45)));
        assertEquals(SlotEngine.OK, reason(r, at(10, 0)));
        assertEquals(SlotEngine.LEAVES_GAP, reason(r, at(12, 15)));
        assertEquals(SlotEngine.OK, reason(r, at(12, 30)));
    }

    @Test
    void tightPackingOffersOnlyStartsThatTouchSomething() {
        Party a = party("a");
        a.busy.add(booked(at(12, 0), at(13, 0)));
        Request r = request(a);
        r.rules.tight = true;
        assertEquals(SlotEngine.OK, reason(r, at(9, 0)));
        assertEquals(SlotEngine.OK, reason(r, at(11, 0)));
        assertEquals(SlotEngine.OK, reason(r, at(13, 0)));
        assertEquals(SlotEngine.OK, reason(r, at(16, 0)));
        assertEquals(SlotEngine.NOT_ADJACENT, reason(r, at(10, 0)));
    }

    @Test
    void aDailyCapAndAMaximumRunAreKept() {
        Party a = party("a");
        a.maxPerDay = 3;
        a.appointmentsToday = 3;
        assertEquals(SlotEngine.DAY_FULL, reason(request(a), at(10, 0)));

        Party b = party("b");
        b.maxContinuousMinutes = 120;
        b.breakMinutes = 15;
        b.busy.add(booked(at(9, 0), at(10, 0)));
        b.busy.add(booked(at(10, 0), at(11, 0)));
        Request r = request(b);
        // A third hour back to back is three hours without a break.
        assertEquals(SlotEngine.NEEDS_BREAK, reason(r, at(11, 0)));
        assertEquals(SlotEngine.NEEDS_BREAK, reason(r, at(11, 10)));
        assertEquals(SlotEngine.OK, reason(r, at(11, 15)));
    }

    @Test
    void travelBetweenLocationsIsTimeTheProviderIsNotFree() {
        Party a = party("a");
        a.shifts.clear();
        a.shifts.add(new Shift(at(9, 0), at(12, 0), THERE));
        a.shifts.add(new Shift(at(12, 0), at(17, 0), HERE));
        a.busy.add(new Busy(at(11, 0), at(12, 0), SlotEngine.KIND_APPOINTMENT, THERE, ""));
        Request r = request(a);
        r.travel(HERE, THERE, 20);
        assertEquals(SlotEngine.TRAVEL, reason(r, at(12, 0)));
        assertEquals(SlotEngine.TRAVEL, reason(r, at(12, 15)));
        assertEquals(SlotEngine.OK, reason(r, at(12, 20)));
    }

    @Test
    void theClientsOwnDayCounts() {
        Request r = request(party("a"));
        r.rules.clientGapMinutes = 30;
        r.client.add(new Busy(at(12, 0), at(13, 0), SlotEngine.KIND_APPOINTMENT, "", ""));
        assertEquals(SlotEngine.CLIENT_BUSY, reason(r, at(12, 30)));
        assertEquals(SlotEngine.CLIENT_GAP, reason(r, at(13, 15)));
        assertEquals(SlotEngine.OK, reason(r, at(13, 30)));
        r.rules.clientPerDay = 1;
        r.clientAppointmentsToday = 1;
        assertEquals(SlotEngine.CLIENT_DAY_FULL, reason(r, at(15, 0)));
    }

    @Test
    void aSlotGoesToTheLeastBookedProviderAndANamedOneIsKept() {
        Party a = party("a");
        a.appointmentsToday = 4;
        Party b = party("b");
        b.appointmentsToday = 1;
        Request r = request(a, b);
        r.starts = new long[] {at(10, 0)};
        assertEquals("b", SlotEngine.evaluate(r).at(at(10, 0)).providers.get(0));

        r.required.add("a");
        assertEquals("a", SlotEngine.evaluate(r).at(at(10, 0)).providers.get(0));
        a.busy.add(booked(at(10, 0), at(11, 0)));
        // Someone else free is no answer when the client asked for "a".
        assertNull(SlotEngine.evaluate(r).at(at(10, 0)));
    }

    @Test
    void twoProvidersAreNeededAtOnceForAJointService() {
        Party a = party("a");
        Party b = party("b");
        b.busy.add(booked(at(10, 0), at(11, 0)));
        Request r = request(a, b);
        r.need.providersNeeded = 2;
        assertEquals(SlotEngine.OK, reason(r, at(9, 0)));
        assertEquals(SlotEngine.NOT_ENOUGH_PROVIDERS, reason(r, at(10, 0)));
        r.starts = new long[] {at(9, 0)};
        assertEquals(2, SlotEngine.evaluate(r).at(at(9, 0)).providers.size());
    }

    @Test
    void aResourceIsNeededAndTakenOnce() {
        Request r = request(party("a"));
        r.need.resourceKind = "Chair";
        Res chair = new Res();
        chair.id = "res-1";
        chair.busy.add(booked(at(10, 0), at(11, 0)));
        r.resources.add(chair);
        assertEquals(SlotEngine.NO_RESOURCE, reason(r, at(10, 0)));
        r.starts = new long[] {at(11, 0)};
        assertEquals("res-1", SlotEngine.evaluate(r).at(at(11, 0)).resourceId);
    }

    @Test
    void aGroupSlotIsJoinedUntilItIsFull() {
        Party a = party("a");
        String key = SlotEngine.groupKey("svc", "a", at(10, 0));
        // The first seat's own claim is not busy time for the next seat.
        a.busy.add(new Busy(at(10, 0), at(11, 0), SlotEngine.KIND_APPOINTMENT, HERE, key));
        Request r = request(a);
        r.need.capacity = 4;
        r.groupSeats.put(key, Integer.valueOf(3));
        r.starts = new long[] {at(10, 0)};
        Result result = SlotEngine.evaluate(r);
        assertNotNull(result.at(at(10, 0)));
        assertEquals(1, result.at(at(10, 0)).seatsLeft);
        // Another start in the middle of the class is still booked time.
        assertEquals(SlotEngine.BOOKED, reason(r, at(10, 30)));
        r.groupSeats.put(key, Integer.valueOf(4));
        assertEquals(SlotEngine.GROUP_FULL, reason(r, at(10, 0)));
    }

    @Test
    void explainGivesAVerdictPerProvider() {
        Party a = party("a");
        Party b = party("b");
        b.busy.add(booked(at(10, 0), at(11, 0)));
        Request r = request(a, b);
        r.explain = true;
        r.starts = new long[] {at(10, 0)};
        Result result = SlotEngine.evaluate(r);
        assertEquals(2, result.verdicts.size());
        assertEquals(SlotEngine.OK, result.verdicts.get(0).reason);
        assertEquals(SlotEngine.BOOKED, result.verdicts.get(1).reason);
    }

    @Test
    void startsFollowTheLocalClockAcrossADaylightSavingChange() {
        ZoneId london = ZoneId.of("Europe/London");
        // The clocks go forward at 01:00 on 2030-03-31: 01:00 to 01:59 never happen.
        long[] spring = Availability.starts(LocalDate.of(2030, 3, 31), london, 60);
        assertEquals(23, spring.length);
        // They go back at 02:00 on 2030-10-27, and 01:00 is one start, not two.
        long[] autumn = Availability.starts(LocalDate.of(2030, 10, 27), london, 60);
        assertEquals(24, autumn.length);
        for (int iter = 1; iter < autumn.length; iter++) {
            assertTrue(autumn[iter] > autumn[iter - 1]);
        }
        // 09:00 local in summer is 08:00 UTC.
        assertEquals(Availability.at(LocalDate.of(2030, 7, 1), 9 * 60, london),
                LocalDate.of(2030, 7, 1).atStartOfDay(ZoneId.of("UTC")).toInstant()
                        .toEpochMilli() + 8 * 60 * MIN);
    }
}
