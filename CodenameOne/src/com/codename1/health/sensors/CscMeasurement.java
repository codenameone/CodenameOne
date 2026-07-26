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

/// A decoded Cycling Speed and Cadence Measurement, characteristic
/// `0x2A5B`.
///
/// #### Wire format
///
/// A single flags byte -- bit 0 wheel data present, bit 1 crank data
/// present -- followed by whichever blocks it selected: a `uint32`
/// revolution count plus a `uint16` event time for the wheel, and a
/// `uint16` count plus `uint16` event time for the crank.
///
/// #### These are cumulative counters, not rates
///
/// Neither speed nor cadence is transmitted. What arrives is a running
/// total and a timestamp, and both wrap -- the event times roughly every
/// 64 seconds. Differencing them correctly is what
/// [CumulativeCounterTracker] exists for, and [SensorSession] applies it
/// automatically, emitting derived speed and cadence samples. Use this
/// parser directly only if you are doing that arithmetic yourself.
public final class CscMeasurement {

    private static final int FLAG_WHEEL = 0x01;
    private static final int FLAG_CRANK = 0x02;

    private final long wheelRevolutions;
    private final int lastWheelEventTime;
    private final int crankRevolutions;
    private final int lastCrankEventTime;

    private CscMeasurement(long wheelRevolutions, int lastWheelEventTime,
            int crankRevolutions, int lastCrankEventTime) {
        this.wheelRevolutions = wheelRevolutions;
        this.lastWheelEventTime = lastWheelEventTime;
        this.crankRevolutions = crankRevolutions;
        this.lastCrankEventTime = lastCrankEventTime;
    }

    /// Decodes a `0x2A5B` value. Returns `null` for a malformed or
    /// truncated payload; never throws.
    public static CscMeasurement parse(byte[] value) {
        if (value == null || value.length < 2) {
            return null;
        }
        GattReader r = new GattReader(value);
        int flags = r.uint8();
        long wheelRevs = -1;
        int wheelTime = -1;
        if ((flags & FLAG_WHEEL) != 0) {
            wheelRevs = r.uint32();
            wheelTime = r.uint16();
        }
        int crankRevs = -1;
        int crankTime = -1;
        if ((flags & FLAG_CRANK) != 0) {
            crankRevs = r.uint16();
            crankTime = r.uint16();
        }
        if (!r.isValid()) {
            return null;
        }
        return new CscMeasurement(wheelRevs, wheelTime, crankRevs, crankTime);
    }

    /// Cumulative wheel revolutions, or `-1` when absent. Wraps at 2^32.
    public long getWheelRevolutions() {
        return wheelRevolutions;
    }

    /// The wheel-event timestamp in 1/1024-second units, or `-1` when
    /// absent. Wraps roughly every 64 seconds.
    public int getLastWheelEventTime() {
        return lastWheelEventTime;
    }

    /// Cumulative crank revolutions, or `-1` when absent. Wraps at 2^16.
    public int getCrankRevolutions() {
        return crankRevolutions;
    }

    /// The crank-event timestamp in 1/1024-second units, or `-1` when
    /// absent. Wraps roughly every 64 seconds.
    public int getLastCrankEventTime() {
        return lastCrankEventTime;
    }

    /// `true` when this notification carried wheel data.
    public boolean hasWheelData() {
        return wheelRevolutions >= 0;
    }

    /// `true` when this notification carried crank data.
    public boolean hasCrankData() {
        return crankRevolutions >= 0;
    }

    @Override
    public String toString() {
        return "CscMeasurement[wheel=" + wheelRevolutions + " crank="
                + crankRevolutions + "]";
    }
}
