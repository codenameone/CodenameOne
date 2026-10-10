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
package com.codename1.compat.jdk;

import java.time.Duration;
import java.time.temporal.TemporalAccessor;

/// `java.time.temporal.ChronoUnit`: the units of the ISO calendar, with
/// the JDK's order, names and lengths.
public enum ChronoUnit implements TemporalUnit {
    NANOS("Nanos", 0L, 1),
    MICROS("Micros", 0L, 1000),
    MILLIS("Millis", 0L, 1000000),
    SECONDS("Seconds", 1L, 0),
    MINUTES("Minutes", 60L, 0),
    HOURS("Hours", 3600L, 0),
    HALF_DAYS("HalfDays", 43200L, 0),
    DAYS("Days", 86400L, 0),
    WEEKS("Weeks", 7 * 86400L, 0),
    MONTHS("Months", 31556952L / 12, 0),
    YEARS("Years", 31556952L, 0),
    DECADES("Decades", 31556952L * 10L, 0),
    CENTURIES("Centuries", 31556952L * 100L, 0),
    MILLENNIA("Millennia", 31556952L * 1000L, 0),
    ERAS("Eras", 31556952L * 1000000000L, 0),
    FOREVER("Forever", Long.MAX_VALUE, 999999999);

    private final String label;
    private final long seconds;
    private final int nanos;

    ChronoUnit(String label, long seconds, int nanos) {
        this.label = label;
        this.seconds = seconds;
        this.nanos = nanos;
    }

    @Override
    public Duration getDuration() {
        return Duration.ofSeconds(seconds, nanos);
    }

    @Override
    public boolean isDurationEstimated() {
        return compareTo(DAYS) >= 0;
    }

    @Override
    public boolean isDateBased() {
        return compareTo(DAYS) >= 0 && this != FOREVER;
    }

    @Override
    public boolean isTimeBased() {
        return compareTo(DAYS) < 0;
    }

    @Override
    public long between(TemporalAccessor startInclusive, TemporalAccessor endExclusive) {
        return JdkDates.until(startInclusive, endExclusive, this);
    }

    /// The length in seconds, without the fraction.
    long seconds() {
        return seconds;
    }

    /// The length in nanoseconds of a unit shorter than a second.
    int nanos() {
        return nanos;
    }

    @Override
    public String toString() {
        return label;
    }
}
