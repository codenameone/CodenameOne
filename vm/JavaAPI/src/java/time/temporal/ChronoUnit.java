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
package java.time.temporal;

import java.time.Duration;

/// The units of the ISO calendar, with the JDK's order, names and lengths.
public enum ChronoUnit implements TemporalUnit {
    /// A nanosecond.
    NANOS("Nanos", 0L, 1),
    /// A microsecond.
    MICROS("Micros", 0L, 1000),
    /// A millisecond.
    MILLIS("Millis", 0L, 1000000),
    /// A second.
    SECONDS("Seconds", 1L, 0),
    /// A minute.
    MINUTES("Minutes", 60L, 0),
    /// An hour.
    HOURS("Hours", 3600L, 0),
    /// Twelve hours.
    HALF_DAYS("HalfDays", 43200L, 0),
    /// A day.
    DAYS("Days", 86400L, 0),
    /// Seven days.
    WEEKS("Weeks", 7 * 86400L, 0),
    /// A month, estimated as a twelfth of a year.
    MONTHS("Months", 31556952L / 12, 0),
    /// A year, estimated as 365.2425 days.
    YEARS("Years", 31556952L, 0),
    /// Ten years.
    DECADES("Decades", 31556952L * 10L, 0),
    /// A hundred years.
    CENTURIES("Centuries", 31556952L * 100L, 0),
    /// A thousand years.
    MILLENNIA("Millennia", 31556952L * 1000L, 0),
    /// An era, estimated as a billion years.
    ERAS("Eras", 31556952L * 1000000000L, 0),
    /// Forever: longer than any other unit.
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
    public boolean isSupportedBy(Temporal temporal) {
        return temporal.isSupported(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Temporal> R addTo(R temporal, long amount) {
        return (R) temporal.plus(amount, this);
    }

    @Override
    public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
        return temporal1Inclusive.until(temporal2Exclusive, this);
    }

    @Override
    public String toString() {
        return label;
    }
}
