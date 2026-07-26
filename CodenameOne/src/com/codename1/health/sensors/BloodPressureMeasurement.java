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

/// A decoded Blood Pressure Measurement, characteristic `0x2A35`, or the
/// Intermediate Cuff Pressure, `0x2A36`, which shares the same layout.
///
/// #### Wire format
///
/// Byte 0 is a flags field:
///
/// | Bit | Meaning |
/// |-----|---------|
/// | 0 | units: 0 = mmHg, 1 = kPa |
/// | 1 | timestamp present |
/// | 2 | pulse rate present |
/// | 3 | user id present |
/// | 4 | measurement status present |
///
/// It is followed by three 16-bit IEEE-11073 SFLOAT values -- systolic,
/// diastolic and mean arterial pressure -- then the optional fields.
///
/// #### Two traps
///
/// **This characteristic is indicated, not notified.** Its client
/// configuration descriptor takes `0x0002`. The Bluetooth layer handles
/// that automatically, but a hand-rolled GATT client that writes `0x0001`
/// will subscribe successfully and then never receive anything.
///
/// **A cuff that fails to get a reading sends a reserved SFLOAT.** Those
/// decode to `Double.NaN` here, and [#parse(byte[])] returns `null` when
/// the systolic or diastolic value is one, so a failed measurement can
/// never reach your UI as 2047 mmHg.
public final class BloodPressureMeasurement {

    private static final int FLAG_KPA = 0x01;
    private static final int FLAG_TIMESTAMP = 0x02;
    private static final int FLAG_PULSE = 0x04;
    private static final int FLAG_USER_ID = 0x08;
    private static final int FLAG_STATUS = 0x10;

    /// kPa to mmHg.
    private static final double KPA_TO_MMHG = 7.50061683;

    private final double systolicMmHg;
    private final double diastolicMmHg;
    private final double meanArterialMmHg;
    private final double pulseBpm;
    private final int userId;
    private final long timestampMillis;

    private BloodPressureMeasurement(double systolicMmHg,
            double diastolicMmHg, double meanArterialMmHg, double pulseBpm,
            int userId, long timestampMillis) {
        this.systolicMmHg = systolicMmHg;
        this.diastolicMmHg = diastolicMmHg;
        this.meanArterialMmHg = meanArterialMmHg;
        this.pulseBpm = pulseBpm;
        this.userId = userId;
        this.timestampMillis = timestampMillis;
    }

    /// Decodes a `0x2A35` or `0x2A36` value.
    ///
    /// Returns `null` for a malformed or truncated payload, and also when
    /// the device signalled a failed measurement through a reserved
    /// SFLOAT. Never throws.
    public static BloodPressureMeasurement parse(byte[] value) {
        if (value == null || value.length < 7) {
            return null;
        }
        GattReader r = new GattReader(value);
        int flags = r.uint8();
        boolean kpa = (flags & FLAG_KPA) != 0;

        double systolic = convert(Ieee11073.sfloat(r.uint16()), kpa);
        double diastolic = convert(Ieee11073.sfloat(r.uint16()), kpa);
        double mean = convert(Ieee11073.sfloat(r.uint16()), kpa);

        long timestamp = -1;
        if ((flags & FLAG_TIMESTAMP) != 0) {
            timestamp = GattDateTime.read(r);
        }
        double pulse = Double.NaN;
        if ((flags & FLAG_PULSE) != 0) {
            pulse = Ieee11073.sfloat(r.uint16());
        }
        int user = -1;
        if ((flags & FLAG_USER_ID) != 0) {
            user = r.uint8();
        }
        if ((flags & FLAG_STATUS) != 0) {
            r.skip(2);
        }

        if (!r.isValid()) {
            return null;
        }
        if (Double.isNaN(systolic) || Double.isNaN(diastolic)) {
            // The cuff reported that it could not obtain a reading.
            return null;
        }
        return new BloodPressureMeasurement(systolic, diastolic, mean, pulse,
                user, timestamp);
    }

    private static double convert(double v, boolean kpa) {
        return kpa ? v * KPA_TO_MMHG : v;
    }

    /// Systolic pressure in mmHg, converted from kPa when the device
    /// reported those.
    public double getSystolicMmHg() {
        return systolicMmHg;
    }

    /// Diastolic pressure in mmHg.
    public double getDiastolicMmHg() {
        return diastolicMmHg;
    }

    /// Mean arterial pressure in mmHg, or `Double.NaN` when the device did
    /// not compute one.
    public double getMeanArterialMmHg() {
        return meanArterialMmHg;
    }

    /// Pulse in beats per minute, or `Double.NaN` when absent.
    public double getPulseBpm() {
        return pulseBpm;
    }

    /// `true` when this reading carried a pulse.
    public boolean hasPulse() {
        return !Double.isNaN(pulseBpm);
    }

    /// The device's user-profile index, or `-1` when absent. Cuffs shared
    /// by a household use it to attribute a reading.
    public int getUserId() {
        return userId;
    }

    /// The device's own timestamp for this reading in epoch millis, or
    /// `-1` when absent.
    ///
    /// Worth preferring over the time of receipt: cuffs store readings and
    /// upload them in a burst when they next connect, so several
    /// measurements taken hours apart can arrive within a second.
    public long getTimestampMillis() {
        return timestampMillis;
    }

    @Override
    public String toString() {
        return "BloodPressureMeasurement[" + (int) systolicMmHg + "/"
                + (int) diastolicMmHg + " mmHg]";
    }
}
