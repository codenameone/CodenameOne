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

/// `java.util.concurrent.TimeUnit` for the Codename One runtime: a duration
/// granularity and the conversions between granularities. Conversions to a
/// coarser unit truncate; conversions to a finer one saturate at
/// `Long.MIN_VALUE` and `Long.MAX_VALUE` instead of overflowing, as the JDK's
/// do.
///
/// `timedWait` and `timedJoin` are not provided: they exist to coordinate
/// threads through a monitor, and Codename One application code is confined
/// to one thread.
public enum TimeUnit {
    NANOSECONDS(1L),
    MICROSECONDS(1000L),
    MILLISECONDS(1000L * 1000L),
    SECONDS(1000L * 1000L * 1000L),
    MINUTES(60L * 1000L * 1000L * 1000L),
    HOURS(60L * 60L * 1000L * 1000L * 1000L),
    DAYS(24L * 60L * 60L * 1000L * 1000L * 1000L);

    /// Nanoseconds in one of this unit. Each unit's value is an exact
    /// multiple of every finer unit's, and never zero, so the divisions below
    /// are exact and cannot divide by zero.
    private final long nanos;

    TimeUnit(long nanos) {
        this.nanos = nanos;
    }

    private static long scale(long duration, long from, long to) {
        if (from == to) {
            return duration;
        }
        if (from < to) {
            return duration / (to / from);
        }
        long ratio = from / to;
        long limit = Long.MAX_VALUE / ratio;
        if (duration > limit) {
            return Long.MAX_VALUE;
        }
        if (duration < -limit) {
            return Long.MIN_VALUE;
        }
        return duration * ratio;
    }

    public long convert(long sourceDuration, TimeUnit sourceUnit) {
        return scale(sourceDuration, sourceUnit.nanos, nanos);
    }

    public long toNanos(long duration) {
        return scale(duration, nanos, NANOSECONDS.nanos);
    }

    public long toMicros(long duration) {
        return scale(duration, nanos, MICROSECONDS.nanos);
    }

    public long toMillis(long duration) {
        return scale(duration, nanos, MILLISECONDS.nanos);
    }

    public long toSeconds(long duration) {
        return scale(duration, nanos, SECONDS.nanos);
    }

    public long toMinutes(long duration) {
        return scale(duration, nanos, MINUTES.nanos);
    }

    public long toHours(long duration) {
        return scale(duration, nanos, HOURS.nanos);
    }

    public long toDays(long duration) {
        return scale(duration, nanos, DAYS.nanos);
    }

    /// Sleeps for the duration, rounded down to whole milliseconds; a
    /// duration of zero or less returns at once.
    public void sleep(long timeout) throws InterruptedException {
        long millis = toMillis(timeout);
        if (millis > 0) {
            Thread.sleep(millis);
        }
    }
}
