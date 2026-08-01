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

/// A decoded Heart Rate Measurement, characteristic `0x2A37`.
///
/// Parsers in this package are public and static so they can be unit
/// tested without a radio and reused by apps doing their own GATT work.
///
/// #### Wire format
///
/// Byte 0 is a flags field; the payload that follows is variable-length
/// and entirely determined by it.
///
/// | Bit | Meaning |
/// |-----|---------|
/// | 0 | value format: 0 = `uint8`, 1 = `uint16` little-endian |
/// | 1 | sensor contact **detected** |
/// | 2 | sensor contact **supported** |
/// | 3 | energy expended field present |
/// | 4 | RR-interval field present |
/// | 5-7 | reserved -- ignored, not asserted zero |
///
/// #### Three things that are easy to get wrong
///
/// **Sensor contact.** Bit 1 is meaningful only when bit 2 is set. A strap
/// that does not implement contact detection sends both bits clear, which
/// means *unsupported* -- not *not touching skin*. Reporting "poor contact"
/// for such a device is a very common bug, so
/// [#isSensorContactDetected()] is documented to be read only alongside
/// [#isSensorContactSupported()].
///
/// **Energy expended is kilojoules**, not kilocalories, despite most
/// fitness UIs showing kcal. Divide by 4.184.
///
/// **RR intervals arrive in batches.** The field is not one value but the
/// entire remainder of the payload, as many `uint16` values as fit -- a
/// strap notifying at 1 Hz while the heart beats faster sends two or three
/// per notification. Dropping any of them silently corrupts every
/// heart-rate-variability calculation downstream, so [#getRrIntervalCount()]
/// exists and callers must loop.
public final class HeartRateMeasurement {

    private static final int FLAG_UINT16 = 0x01;
    private static final int FLAG_CONTACT_DETECTED = 0x02;
    private static final int FLAG_CONTACT_SUPPORTED = 0x04;
    private static final int FLAG_ENERGY_EXPENDED = 0x08;
    private static final int FLAG_RR_INTERVALS = 0x10;

    /// RR intervals are transmitted in units of 1/1024 second.
    private static final double RR_UNIT_MILLIS = 1000.0 / 1024.0;

    private final int heartRate;
    private final boolean contactSupported;
    private final boolean contactDetected;
    private final boolean hasEnergyExpended;
    private final int energyExpendedKilojoules;
    private final int[] rrIntervalsRaw;

    private HeartRateMeasurement(int heartRate, boolean contactSupported,
            boolean contactDetected, boolean hasEnergyExpended,
            int energyExpendedKilojoules, int[] rrIntervalsRaw) {
        this.heartRate = heartRate;
        this.contactSupported = contactSupported;
        this.contactDetected = contactDetected;
        this.hasEnergyExpended = hasEnergyExpended;
        this.energyExpendedKilojoules = energyExpendedKilojoules;
        this.rrIntervalsRaw = rrIntervalsRaw;
    }

    /// Decodes a `0x2A37` value.
    ///
    /// Returns `null` for a null, empty, truncated or otherwise malformed
    /// payload -- **never throws**. A notification callback is the wrong
    /// place to discover that a device sent a short packet, and one
    /// misbehaving strap should not take down the app.
    public static HeartRateMeasurement parse(byte[] value) {
        if (value == null || value.length < 2) {
            return null;
        }
        GattReader r = new GattReader(value);
        int flags = r.uint8();
        int bpm = (flags & FLAG_UINT16) != 0 ? r.uint16() : r.uint8();

        boolean supported = (flags & FLAG_CONTACT_SUPPORTED) != 0;
        boolean detected = supported && (flags & FLAG_CONTACT_DETECTED) != 0;

        boolean hasEnergy = (flags & FLAG_ENERGY_EXPENDED) != 0;
        int energy = hasEnergy ? r.uint16() : 0;

        int[] rr;
        if ((flags & FLAG_RR_INTERVALS) != 0) {
            // The count is not transmitted: whatever remains is RR data.
            // An odd remainder is a truncated notification, not a run of
            // pairs with a spare byte. Rounding down accepted the packet
            // and silently dropped half an interval -- which this parser
            // exists to feed into an HRV calculation, where a missing beat
            // interval is not a rounding error.
            if ((r.remaining() & 1) != 0) {
                return null;
            }
            int count = r.remaining() / 2;
            rr = new int[count];
            for (int i = 0; i < count; i++) {
                rr[i] = r.uint16();
            }
        } else {
            rr = new int[0];
        }

        if (!r.isValid()) {
            return null;
        }
        return new HeartRateMeasurement(bpm, supported, detected, hasEnergy,
                energy, rr);
    }

    /// The heart rate in beats per minute.
    public int getHeartRate() {
        return heartRate;
    }

    /// Whether this device reports skin contact at all. When `false`,
    /// [#isSensorContactDetected()] carries no information.
    public boolean isSensorContactSupported() {
        return contactSupported;
    }

    /// Whether the sensor is in contact with skin. Only meaningful when
    /// [#isSensorContactSupported()] is `true`; always `false` otherwise,
    /// which must not be shown to the user as poor contact.
    public boolean isSensorContactDetected() {
        return contactDetected;
    }

    /// Whether this notification carried the energy-expended field.
    /// Devices typically include it only every few notifications.
    public boolean hasEnergyExpended() {
        return hasEnergyExpended;
    }

    /// Energy expended since the last reset, in **kilojoules**. Zero when
    /// [#hasEnergyExpended()] is false.
    ///
    /// Reset with [SensorSession#resetEnergyExpended()].
    public int getEnergyExpendedKilojoules() {
        return energyExpendedKilojoules;
    }

    /// Energy expended since the last reset, in kilocalories -- the
    /// kilojoule value divided by 4.184.
    public double getEnergyExpendedKilocalories() {
        return energyExpendedKilojoules / 4.184;
    }

    /// How many RR intervals this notification carried. Often more than
    /// one, and zero when the device does not report them.
    public int getRrIntervalCount() {
        return rrIntervalsRaw.length;
    }

    /// RR interval `i` in milliseconds, converted from the wire's
    /// 1/1024-second units.
    ///
    /// #### Throws
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `i` is outside
    ///   `[0, getRrIntervalCount())`.
    public double getRrIntervalMillis(int i) {
        return rrIntervalsRaw[i] * RR_UNIT_MILLIS;
    }

    @Override
    public String toString() {
        return "HeartRateMeasurement[" + heartRate + " bpm, rr="
                + rrIntervalsRaw.length
                + (contactSupported ? (contactDetected ? ", contact"
                        : ", no contact") : "") + "]";
    }
}
