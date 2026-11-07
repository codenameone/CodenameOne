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

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/// A calendar that is busy at made-up but steady times, for a server with
/// no OAuth clients set up: the demo, the tests and a developer's laptop. It
/// stands in for Google and Microsoft alike, so connecting either shows its
/// busy time on the schedule without an account anywhere.
///
/// Every weekday it is busy over lunch, 12:30 to 13:30, and on the days whose
/// number the credential picks, from 16:00 to 17:00 too.
final class SimulatedCalendar implements ExternalCalendar {
    private final String kind;
    private final ZoneId zone;

    SimulatedCalendar(String kind, ZoneId zone) {
        this.kind = kind;
        this.zone = zone;
    }

    @Override
    public String kind() {
        return kind;
    }

    @Override
    public String connect(String given, String redirectUri) throws IOException {
        if (given == null || given.trim().length() == 0) {
            throw new IOException("The sign-in was cancelled");
        }
        return "simulated:" + given.trim();
    }

    @Override
    public List<long[]> busy(String credential, String calendars, long from, long to) {
        List<long[]> out = new ArrayList<long[]>();
        int pick = Math.abs(credential.hashCode() % 3);
        LocalDate day = Instant.ofEpochMilli(from).atZone(zone).toLocalDate();
        LocalDate last = Instant.ofEpochMilli(to).atZone(zone).toLocalDate();
        while (!day.isAfter(last)) {
            if (day.getDayOfWeek().getValue() <= 5) {
                add(out, day, 12 * 60 + 30, 13 * 60 + 30, from, to);
                if (day.getDayOfMonth() % 3 == pick) {
                    add(out, day, 16 * 60, 17 * 60, from, to);
                }
            }
            day = day.plusDays(1);
        }
        return out;
    }

    private void add(List<long[]> out, LocalDate day, int startMinute, int endMinute, long from,
            long to) {
        long start = day.atStartOfDay(zone).plusMinutes(startMinute).toInstant().toEpochMilli();
        long end = day.atStartOfDay(zone).plusMinutes(endMinute).toInstant().toEpochMilli();
        if (start < to && end > from) {
            out.add(new long[] {start, end});
        }
    }
}
