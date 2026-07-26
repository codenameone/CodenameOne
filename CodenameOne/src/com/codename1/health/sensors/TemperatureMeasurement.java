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

/// A decoded Temperature Measurement, characteristic `0x2A1C`.
///
/// #### Wire format
///
/// A flags byte -- bit 0 Fahrenheit rather than Celsius, bit 1 timestamp
/// present, bit 2 temperature type present -- followed by a **32-bit
/// IEEE-11073 FLOAT**.
///
/// Note that this is the 32-bit format, whereas blood pressure and glucose
/// use the 16-bit SFLOAT. They are different encodings with different
/// reserved values, and decoding one as the other produces numbers that
/// look plausible enough to ship. A thermometer that cannot obtain a
/// reading sends a reserved value; this parser returns `null` rather than
/// letting it through.
public final class TemperatureMeasurement {

    private static final int FLAG_FAHRENHEIT = 0x01;
    private static final int FLAG_TIMESTAMP = 0x02;
    private static final int FLAG_TYPE = 0x04;

    /// The temperature was measured somewhere this profile does not name.
    public static final int SITE_UNKNOWN = 0;
    /// Armpit.
    public static final int SITE_ARMPIT = 1;
    /// Body, general.
    public static final int SITE_BODY = 2;
    /// Ear.
    public static final int SITE_EAR = 3;
    /// Finger.
    public static final int SITE_FINGER = 4;
    /// Gastrointestinal tract.
    public static final int SITE_GASTROINTESTINAL = 5;
    /// Mouth.
    public static final int SITE_MOUTH = 6;
    /// Rectum.
    public static final int SITE_RECTUM = 7;
    /// Toe.
    public static final int SITE_TOE = 8;
    /// Tympanum, the ear drum.
    public static final int SITE_TYMPANUM = 9;

    private final double celsius;
    private final long timestampMillis;
    private final int site;

    private TemperatureMeasurement(double celsius, long timestampMillis,
            int site) {
        this.celsius = celsius;
        this.timestampMillis = timestampMillis;
        this.site = site;
    }

    /// Decodes a `0x2A1C` value. Returns `null` for a malformed or
    /// truncated payload, or when the device signalled that it could not
    /// obtain a reading. Never throws.
    public static TemperatureMeasurement parse(byte[] value) {
        if (value == null || value.length < 5) {
            return null;
        }
        GattReader r = new GattReader(value);
        int flags = r.uint8();
        double raw = Ieee11073.float32(r.uint32());
        double celsius = (flags & FLAG_FAHRENHEIT) != 0
                ? (raw - 32.0) * 5.0 / 9.0 : raw;

        long timestamp = -1;
        if ((flags & FLAG_TIMESTAMP) != 0) {
            timestamp = GattDateTime.read(r);
        }
        int site = SITE_UNKNOWN;
        if ((flags & FLAG_TYPE) != 0) {
            site = r.uint8();
        }

        if (!r.isValid() || Double.isNaN(celsius)) {
            return null;
        }
        return new TemperatureMeasurement(celsius, timestamp, site);
    }

    /// The temperature in degrees Celsius, converted from Fahrenheit when
    /// the device reported those.
    public double getCelsius() {
        return celsius;
    }

    /// The temperature in degrees Fahrenheit.
    public double getFahrenheit() {
        return celsius * 9.0 / 5.0 + 32.0;
    }

    /// Where on the body the measurement was taken, as a `SITE_` constant.
    public int getSite() {
        return site;
    }

    /// The device's own timestamp in epoch millis, or `-1` when absent.
    public long getTimestampMillis() {
        return timestampMillis;
    }

    @Override
    public String toString() {
        return "TemperatureMeasurement[" + celsius + " degC]";
    }
}
