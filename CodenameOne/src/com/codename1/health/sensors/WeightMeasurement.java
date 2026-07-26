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

/// A decoded Weight Measurement, characteristic `0x2A9D`.
///
/// #### Wire format
///
/// Byte 0 is a flags field:
///
/// | Bit | Meaning |
/// |-----|---------|
/// | 0 | units: 0 = SI (kg), 1 = Imperial (lb) |
/// | 1 | timestamp present |
/// | 2 | user id present |
/// | 3 | BMI and height present |
///
/// followed by a `uint16` weight and the optional fields.
///
/// #### The resolution depends on the unit
///
/// The raw `uint16` is scaled by **0.005 kg** in SI mode and **0.01 lb**
/// in Imperial mode -- not the same multiplier, and not a simple unit
/// conversion applied afterwards. Getting this wrong yields a weight that
/// is wrong by a factor of about two, which is exactly close enough to
/// look plausible. Height carries the same split: 0.001 m against 0.1 in.
///
/// Like all characteristics here this one is **indicated, not notified**.
public final class WeightMeasurement {

    private static final int FLAG_IMPERIAL = 0x01;
    private static final int FLAG_TIMESTAMP = 0x02;
    private static final int FLAG_USER_ID = 0x04;
    private static final int FLAG_BMI_HEIGHT = 0x08;

    private static final double WEIGHT_RESOLUTION_KG = 0.005;
    private static final double WEIGHT_RESOLUTION_LB = 0.01;
    private static final double HEIGHT_RESOLUTION_M = 0.001;
    private static final double HEIGHT_RESOLUTION_IN = 0.1;

    private static final double LB_TO_KG = 0.45359237;
    private static final double IN_TO_M = 0.0254;

    private final double weightKg;
    private final double heightMeters;
    private final double bmi;
    private final int userId;
    private final long timestampMillis;
    private final boolean imperial;

    private WeightMeasurement(double weightKg, double heightMeters, double bmi,
            int userId, long timestampMillis, boolean imperial) {
        this.weightKg = weightKg;
        this.heightMeters = heightMeters;
        this.bmi = bmi;
        this.userId = userId;
        this.timestampMillis = timestampMillis;
        this.imperial = imperial;
    }

    /// Decodes a `0x2A9D` value. Returns `null` for a malformed or
    /// truncated payload; never throws.
    public static WeightMeasurement parse(byte[] value) {
        if (value == null || value.length < 3) {
            return null;
        }
        GattReader r = new GattReader(value);
        int flags = r.uint8();
        boolean imperial = (flags & FLAG_IMPERIAL) != 0;

        int rawWeight = r.uint16();
        double weightKg = imperial
                ? rawWeight * WEIGHT_RESOLUTION_LB * LB_TO_KG
                : rawWeight * WEIGHT_RESOLUTION_KG;

        long timestamp = -1;
        if ((flags & FLAG_TIMESTAMP) != 0) {
            timestamp = GattDateTime.read(r);
        }
        int user = -1;
        if ((flags & FLAG_USER_ID) != 0) {
            user = r.uint8();
        }
        double bmi = Double.NaN;
        double heightM = Double.NaN;
        if ((flags & FLAG_BMI_HEIGHT) != 0) {
            // BMI is always transmitted with a resolution of 0.1,
            // regardless of the unit flag.
            bmi = r.uint16() * 0.1;
            int rawHeight = r.uint16();
            heightM = imperial
                    ? rawHeight * HEIGHT_RESOLUTION_IN * IN_TO_M
                    : rawHeight * HEIGHT_RESOLUTION_M;
        }

        if (!r.isValid()) {
            return null;
        }
        return new WeightMeasurement(weightKg, heightM, bmi, user, timestamp,
                imperial);
    }

    /// The weight in kilograms, converted from pounds when the scale
    /// reported Imperial units.
    public double getWeightKg() {
        return weightKg;
    }

    /// `true` when the scale transmitted Imperial units. Useful for
    /// matching the user's own display preference; the value returned by
    /// [#getWeightKg()] is already normalized either way.
    public boolean isImperial() {
        return imperial;
    }

    /// The height in metres, or `Double.NaN` when absent.
    public double getHeightMeters() {
        return heightMeters;
    }

    /// The body mass index the scale computed, or `Double.NaN` when
    /// absent.
    public double getBmi() {
        return bmi;
    }

    /// `true` when this reading carried BMI and height.
    public boolean hasBmiAndHeight() {
        return !Double.isNaN(bmi);
    }

    /// The scale's user-profile index, or `-1` when absent. Family scales
    /// use it to attribute a reading.
    public int getUserId() {
        return userId;
    }

    /// The scale's own timestamp in epoch millis, or `-1` when absent.
    public long getTimestampMillis() {
        return timestampMillis;
    }

    @Override
    public String toString() {
        return "WeightMeasurement[" + weightKg + " kg]";
    }
}
