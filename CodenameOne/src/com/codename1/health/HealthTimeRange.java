/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.health;

import java.util.Calendar;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.TimeZone;

/// A half-open span of time, `[start, end)`, in epoch milliseconds UTC.
///
/// #### Why epoch millis rather than java.time
///
/// `java.time` is available in Codename One, and the calendar API uses it.
/// Health does not, for three reasons: a month of continuous heart rate is
/// tens of thousands of points and one `Instant` per point is real memory
/// pressure on a phone; `com.codename1.location` and
/// `com.codename1.bluetooth` already speak `long` millis and health data
/// flows between all three; and both platform SDKs are millisecond-based
/// at the boundary anyway.
///
/// Where calendar semantics genuinely matter -- "the last seven days" is
/// not "the last 604800000 milliseconds" across a daylight-saving
/// transition -- the factory takes an explicit [TimeZone]. Nothing in this
/// API silently reads the JVM default.
public final class HealthTimeRange {

    private final long startMillis;
    private final long endMillis;
    private final boolean openEnded;

    private HealthTimeRange(long startMillis, long endMillis,
            boolean openEnded) {
        if (endMillis < startMillis) {
            throw new IllegalArgumentException(
                    "time range ends before it starts: " + startMillis
                            + " .. " + endMillis);
        }
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.openEnded = openEnded;
    }

    /// An explicit span. `end` is exclusive: a sample stamped exactly at
    /// `end` belongs to the next range, which is what makes adjacent
    /// buckets tile without double-counting.
    public static HealthTimeRange between(long startMillis, long endMillis) {
        return new HealthTimeRange(startMillis, endMillis, false);
    }

    /// A range covering exactly one instant.
    ///
    /// One millisecond wide rather than zero: ranges are half-open, so
    /// `[t,t)` contains nothing at all and the factory documented for
    /// querying a single moment returned an empty result for every input,
    /// including a sample stamped exactly at `t`.
    public static HealthTimeRange at(long instantMillis) {
        return new HealthTimeRange(instantMillis, instantMillis + 1, false);
    }

    /// The last `durationMillis` milliseconds, ending now.
    public static HealthTimeRange lastMillis(long durationMillis) {
        long now = System.currentTimeMillis();
        return new HealthTimeRange(now - durationMillis, now, false);
    }

    /// The last `hours` hours, ending now.
    public static HealthTimeRange lastHours(int hours) {
        return lastMillis(hours * 3600000L);
    }

    /// The last `days` times 24 hours, ending now. This is a rolling
    /// window, **not** a calendar span -- for "the last seven calendar
    /// days" use [#calendarDays(int,ZoneId)].
    public static HealthTimeRange lastDays(int days) {
        return lastMillis(days * 86400000L);
    }

    /// Today, from local midnight to now, in `zone`.
    public static HealthTimeRange today(ZoneId zone) {
        Calendar c = calendarIn(zone);
        long now = millisOf(c);
        startOfDay(c);
        return new HealthTimeRange(millisOf(c), now, false);
    }

    /// The last `count` calendar days in `zone`, from local midnight at the
    /// start of the first day through now. Correct across daylight-saving
    /// transitions, where a day is 23 or 25 hours.
    public static HealthTimeRange calendarDays(int count, ZoneId zone) {
        if (count < 1) {
            throw new IllegalArgumentException(
                    "calendarDays requires a positive count, got " + count);
        }
        Calendar c = calendarIn(zone);
        long now = millisOf(c);
        startOfDay(c);
        c.add(Calendar.DAY_OF_MONTH, -(count - 1));
        return new HealthTimeRange(millisOf(c), now, false);
    }

    /// Everything from `startMillis` onwards. The end is resolved to the
    /// wall clock at the moment the query executes.
    public static HealthTimeRange since(long startMillis) {
        return new HealthTimeRange(startMillis, Long.MAX_VALUE, true);
    }

    /// Everything from `start` onwards.
    public static HealthTimeRange since(Instant start) {
        return since(requireInstant(start, "start").toEpochMilli());
    }

    /// The window between two instants.
    public static HealthTimeRange between(Instant start, Instant end) {
        return between(requireInstant(start, "start").toEpochMilli(),
                requireInstant(end, "end").toEpochMilli());
    }

    /// The instant `at`, with no duration.
    public static HealthTimeRange at(Instant at) {
        return at(requireInstant(at, "at").toEpochMilli());
    }

    /// The last `window`, ending now.
    public static HealthTimeRange last(Duration window) {
        if (window == null) {
            throw new IllegalArgumentException("a window is required");
        }
        return lastMillis(window.toMillis());
    }

    /// The start of this range.
    public Instant getStart() {
        return Instant.ofEpochMilli(startMillis);
    }

    /// The end of this range, or **null** while it is still open-ended --
    /// there is no instant to name until [#resolve(long)] picks one.
    public Instant getEnd() {
        return openEnded ? null : Instant.ofEpochMilli(endMillis);
    }

    /// How long this range covers, or **null** while it is open-ended.
    public Duration getDuration() {
        return openEnded ? null
                : Duration.ofMillis(endMillis - startMillis);
    }

    private static Instant requireInstant(Instant value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    /// A calendar in `zone`.
    ///
    /// `ZoneId` is the type this API speaks, because it is immutable and
    /// because [com.codename1.calendar] already speaks it; `TimeZone` is
    /// mutable in principle -- `SimpleTimeZone` has setters -- which is why
    /// holding one meant reasoning about defensive copies that the CLDC 1.1
    /// subset cannot make cleanly. The bucket arithmetic below is still
    /// `Calendar`, which is correct across daylight-saving transitions and
    /// well covered, so the zone is handed over by id rather than the
    /// arithmetic being rewritten.
    static Calendar calendarIn(ZoneId zone) {
        if (zone == null) {
            throw new IllegalArgumentException(
                    "a calendar-based range requires an explicit ZoneId");
        }
        return Calendar.getInstance(TimeZone.getTimeZone(zoneIdToTz(
                zone.getId())));
    }

    /// A `TimeZone` id for `zoneId`.
    ///
    /// A fixed-offset zone -- `ZoneOffset.ofHours(2)`, or
    /// `ZoneId.of("+02:00")` -- has the id `+02:00`, and
    /// `TimeZone.getTimeZone("+02:00")` does not recognise that form: it
    /// falls back to GMT and says nothing. Every calendar bucket then aligned
    /// two hours early and samples landed in the wrong local day, which is
    /// precisely the drift this API takes an explicit zone to avoid.
    ///
    /// The offset forms are translated to the `GMT+02:00` spelling
    /// `TimeZone` does understand; `Z` is GMT. Named zones pass through, and
    /// those `TimeZone` resolves correctly. Same rule as the CLDC
    /// `ZoneId.toTimeZone()`, which is package-private in `java.time` and so
    /// cannot be called from here.
    static String zoneIdToTz(String zoneId) {
        if (zoneId == null || zoneId.length() == 0) {
            return "GMT";
        }
        if ("Z".equals(zoneId)) {
            return "GMT";
        }
        char c = zoneId.charAt(0);
        if (c == '+' || c == '-') {
            return "GMT" + zoneId;
        }
        // The prefixed spellings too. A standard java.time runtime keeps
        // ZoneId.of("UTC+02:00") and ZoneId.of("UT-05:00") with those ids
        // rather than normalising them to a bare offset, and TimeZone knows
        // neither -- it answers GMT for both, silently, which is the same
        // wrong-local-midnight this method exists to prevent.
        //
        // GMT+02:00 is already the form TimeZone wants, so it passes through
        // the final return along with the named zones.
        if (startsWithOffset(zoneId, "UTC")) {
            return "GMT" + zoneId.substring(3);
        }
        if (startsWithOffset(zoneId, "UT")) {
            return "GMT" + zoneId.substring(2);
        }
        return zoneId;
    }

    /// Whether `zoneId` is `prefix` followed by a signed offset.
    private static boolean startsWithOffset(String zoneId, String prefix) {
        if (!zoneId.startsWith(prefix) || zoneId.length() <= prefix.length()) {
            return false;
        }
        char next = zoneId.charAt(prefix.length());
        return next == '+' || next == '-';
    }

    /// Reads a calendar's instant.
    ///
    /// `Calendar.getTimeInMillis()` is protected in the CLDC profile this
    /// framework targets, so the value has to come through `getTime()`.
    /// The desktop JDK exposes it publicly, which is why this only shows up
    /// in a device build.
    private static long millisOf(Calendar c) {
        return c.getTime().getTime();
    }

    private static void startOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    /// Inclusive start, epoch millis UTC.
    public long getStartMillis() {
        return startMillis;
    }

    /// Exclusive end, epoch millis UTC. `Long.MAX_VALUE` when
    /// [#isOpenEnded()].
    public long getEndMillis() {
        return endMillis;
    }

    /// `true` when this range was built by [#since(long)] and its end is
    /// resolved at query time rather than fixed.
    public boolean isOpenEnded() {
        return openEnded;
    }

    /// The span in milliseconds, or `Long.MAX_VALUE` when open-ended.
    public long getDurationMillis() {
        return openEnded ? Long.MAX_VALUE : endMillis - startMillis;
    }

    /// `true` when `millis` falls in `[start, end)`. A zero-width range
    /// contains nothing.
    public boolean contains(long millis) {
        return millis >= startMillis && millis < endMillis;
    }

    /// This range with its open end resolved to `nowMillis`. Returns
    /// `this` when the range is already closed.
    public HealthTimeRange resolve(long nowMillis) {
        if (!openEnded) {
            return this;
        }
        return new HealthTimeRange(startMillis,
                Math.max(startMillis, nowMillis), false);
    }

    @Override
    public String toString() {
        return "[" + startMillis + ", "
                + (openEnded ? "now" : String.valueOf(endMillis)) + ")";
    }
}
