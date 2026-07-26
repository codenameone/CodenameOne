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
package com.codename1.health.sensors;

/// Turns the cumulative revolution counters that cycling and running
/// sensors broadcast into an instantaneous rate.
///
/// #### Why this is not left to the app
///
/// Speed and cadence sensors do not transmit speed or cadence. They
/// transmit a running total of revolutions and the timestamp of the most
/// recent one, and the reader is expected to difference consecutive
/// notifications. Doing that correctly means handling three things that
/// are individually easy to miss and collectively guarantee wrong numbers:
///
/// 1. **The event timer wraps.** It is a `uint16` counting 1/1024- or
///    1/2048-second ticks, so it rolls over roughly every 64 or 32
///    seconds -- during every single ride, many times. An unguarded
///    subtraction produces a large negative interval and therefore a
///    spike of tens of thousands of rpm.
/// 2. **The revolution counter wraps too**, at 2^16 or 2^32.
/// 3. **The first notification has no predecessor.** Differencing against
///    zero reports the sensor's lifetime total as though it happened in
///    one instant.
///
/// Shipping this wrong in every app that touches a cadence sensor is not
/// an acceptable outcome, so it lives here and is exercised by tests.
///
/// Not thread-safe; each instance belongs to one sensor session.
final class CumulativeCounterTracker {

    private final long revolutionModulus;
    private final int timeTicksPerSecond;

    private long lastRevolutions = -1;
    private int lastEventTime = -1;

    /// Creates a tracker.
    ///
    /// - `revolutionModulus`: `0x10000` for a `uint16` counter (cranks),
    ///   `0x100000000L` for a `uint32` one (wheels).
    /// - `timeTicksPerSecond`: 1024 for cycling speed and cadence and for
    ///   crank data, 2048 for cycling-power wheel data.
    CumulativeCounterTracker(long revolutionModulus, int timeTicksPerSecond) {
        this.revolutionModulus = revolutionModulus;
        this.timeTicksPerSecond = timeTicksPerSecond;
    }

    /// Feeds one notification and returns the rate in revolutions per
    /// minute, or `Double.NaN` when no rate can be derived yet.
    ///
    /// `NaN` is returned for the first notification (no baseline), when
    /// the event time has not advanced (a duplicate notification, or a
    /// stationary sensor), and when the input is absent. It is deliberately
    /// not zero: "we cannot tell yet" and "the rider has stopped" are
    /// different, and only the sensor's own repeated identical timestamps
    /// establish the latter.
    double update(long revolutions, int eventTime) {
        if (revolutions < 0 || eventTime < 0) {
            return Double.NaN;
        }
        if (lastRevolutions < 0) {
            lastRevolutions = revolutions;
            lastEventTime = eventTime;
            return Double.NaN;
        }

        long revDelta = revolutions - lastRevolutions;
        if (revDelta < 0) {
            revDelta += revolutionModulus;
        }
        int timeDelta = eventTime - lastEventTime;
        if (timeDelta < 0) {
            timeDelta += 0x10000;
        }

        lastRevolutions = revolutions;
        lastEventTime = eventTime;

        if (timeDelta == 0) {
            return Double.NaN;
        }
        double seconds = timeDelta / (double) timeTicksPerSecond;
        return revDelta / seconds * 60.0;
    }

    /// Forgets the baseline, so the next notification establishes a new
    /// one. Called on reconnect: a sensor that power-cycled restarts its
    /// counters from zero, and differencing across that gap would report a
    /// single enormous negative-then-wrapped interval.
    void reset() {
        lastRevolutions = -1;
        lastEventTime = -1;
    }
}
