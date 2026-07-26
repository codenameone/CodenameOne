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

/// A decoded Cycling Power Measurement, characteristic `0x2A63`.
///
/// #### Wire format
///
/// Unlike most SIG characteristics the flags field here is a **`uint16`**,
/// not a single byte, because the profile defines sixteen optional fields.
/// It is followed immediately by the instantaneous power.
///
/// #### Instantaneous power is signed
///
/// The power field is `sint16`, and negative values are real: a power
/// meter reports negative watts when the rider is back-pedalling or the
/// drivetrain is driving the cranks on a descent. Reading it as unsigned
/// turns a brief -5 W into 65531 W, which then poisons every average and
/// maximum for the whole ride.
///
/// This parser exposes the common fields -- instantaneous power,
/// accumulated energy, pedal balance, crank revolutions. The remaining
/// optional fields are skipped in order so that the fields after them
/// still decode correctly.
public final class CyclingPowerMeasurement {

    private static final int FLAG_PEDAL_BALANCE = 0x0001;
    private static final int FLAG_PEDAL_BALANCE_REFERENCE = 0x0002;
    private static final int FLAG_ACCUMULATED_TORQUE = 0x0004;
    private static final int FLAG_ACCUMULATED_TORQUE_SOURCE = 0x0008;
    private static final int FLAG_WHEEL_REVOLUTIONS = 0x0010;
    private static final int FLAG_CRANK_REVOLUTIONS = 0x0020;
    private static final int FLAG_EXTREME_FORCE = 0x0040;
    private static final int FLAG_EXTREME_TORQUE = 0x0080;
    private static final int FLAG_EXTREME_ANGLES = 0x0100;
    private static final int FLAG_TOP_DEAD_SPOT = 0x0200;
    private static final int FLAG_BOTTOM_DEAD_SPOT = 0x0400;
    private static final int FLAG_ACCUMULATED_ENERGY = 0x0800;

    private final int instantaneousPowerWatts;
    private final double pedalPowerBalancePercent;
    private final long wheelRevolutions;
    private final int lastWheelEventTime;
    private final int crankRevolutions;
    private final int lastCrankEventTime;
    private final int accumulatedEnergyKilojoules;

    private CyclingPowerMeasurement(int instantaneousPowerWatts,
            double pedalPowerBalancePercent, long wheelRevolutions,
            int lastWheelEventTime, int crankRevolutions,
            int lastCrankEventTime, int accumulatedEnergyKilojoules) {
        this.instantaneousPowerWatts = instantaneousPowerWatts;
        this.pedalPowerBalancePercent = pedalPowerBalancePercent;
        this.wheelRevolutions = wheelRevolutions;
        this.lastWheelEventTime = lastWheelEventTime;
        this.crankRevolutions = crankRevolutions;
        this.lastCrankEventTime = lastCrankEventTime;
        this.accumulatedEnergyKilojoules = accumulatedEnergyKilojoules;
    }

    /// Decodes a `0x2A63` value. Returns `null` for a malformed or
    /// truncated payload; never throws.
    public static CyclingPowerMeasurement parse(byte[] value) {
        if (value == null || value.length < 4) {
            return null;
        }
        GattReader r = new GattReader(value);
        int flags = r.uint16();
        int power = r.sint16();

        double balance = Double.NaN;
        if ((flags & FLAG_PEDAL_BALANCE) != 0) {
            // Transmitted in halves of a percent.
            balance = r.uint8() * 0.5;
        }
        if ((flags & FLAG_ACCUMULATED_TORQUE) != 0) {
            r.skip(2);
        }
        long wheelRevs = -1;
        int wheelTime = -1;
        if ((flags & FLAG_WHEEL_REVOLUTIONS) != 0) {
            wheelRevs = r.uint32();
            wheelTime = r.uint16();
        }
        int crankRevs = -1;
        int crankTime = -1;
        if ((flags & FLAG_CRANK_REVOLUTIONS) != 0) {
            crankRevs = r.uint16();
            crankTime = r.uint16();
        }
        if ((flags & FLAG_EXTREME_FORCE) != 0) {
            r.skip(4);
        }
        if ((flags & FLAG_EXTREME_TORQUE) != 0) {
            r.skip(4);
        }
        if ((flags & FLAG_EXTREME_ANGLES) != 0) {
            r.skip(3);
        }
        if ((flags & FLAG_TOP_DEAD_SPOT) != 0) {
            r.skip(2);
        }
        if ((flags & FLAG_BOTTOM_DEAD_SPOT) != 0) {
            r.skip(2);
        }
        int energy = -1;
        if ((flags & FLAG_ACCUMULATED_ENERGY) != 0) {
            energy = r.uint16();
        }

        if (!r.isValid()) {
            return null;
        }
        return new CyclingPowerMeasurement(power, balance, wheelRevs,
                wheelTime, crankRevs, crankTime, energy);
    }

    /// Instantaneous power in watts. **May be negative** -- see the class
    /// documentation.
    public int getInstantaneousPowerWatts() {
        return instantaneousPowerWatts;
    }

    /// The share of power contributed by one pedal, as a percentage, or
    /// `Double.NaN` when absent. Which pedal it refers to depends on the
    /// reference bit; most meters report the left.
    public double getPedalPowerBalancePercent() {
        return pedalPowerBalancePercent;
    }

    /// Cumulative wheel revolutions since the meter powered on, or `-1`
    /// when absent. Wraps at 2^32.
    public long getWheelRevolutions() {
        return wheelRevolutions;
    }

    /// The wheel-event timestamp in 1/2048-second units, or `-1` when
    /// absent. Wraps roughly every 32 seconds.
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

    /// Total energy in kilojoules since the meter powered on, or `-1` when
    /// absent.
    public int getAccumulatedEnergyKilojoules() {
        return accumulatedEnergyKilojoules;
    }

    /// `true` when this notification carried crank data, from which
    /// cadence can be derived.
    public boolean hasCrankData() {
        return crankRevolutions >= 0;
    }

    public String toString() {
        return "CyclingPowerMeasurement[" + instantaneousPowerWatts + " W]";
    }
}
