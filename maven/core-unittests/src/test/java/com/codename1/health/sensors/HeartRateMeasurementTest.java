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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Heart Rate Measurement characteristic (0x2A37) is the single most
 * widely implemented health profile, and its variable-length layout is the
 * one most often decoded wrongly. These cases pin the four traps: unsigned
 * reads, the contact-supported/detected pair, kilojoule energy units, and
 * variable-count RR intervals.
 */
class HeartRateMeasurementTest {

    @Test
    void parsesUint8HeartRate() {
        HeartRateMeasurement m =
                HeartRateMeasurement.parse(new byte[] { 0x00, 60 });
        assertNotNull(m);
        assertEquals(60, m.getHeartRate());
        assertEquals(0, m.getRrIntervalCount());
        assertFalse(m.hasEnergyExpended());
    }

    /**
     * A heart rate above 127 is negative when read as a signed Java byte.
     * An unguarded parser reports -56 bpm for a sprinting athlete.
     */
    @Test
    void uint8HeartRateAboveSignedByteRangeIsReadUnsigned() {
        HeartRateMeasurement m = HeartRateMeasurement.parse(
                new byte[] { 0x00, (byte) 200 });
        assertNotNull(m);
        assertEquals(200, m.getHeartRate());
    }

    @Test
    void parsesUint16HeartRateLittleEndian() {
        // flags bit0 set -> uint16; 0x012C = 300
        HeartRateMeasurement m = HeartRateMeasurement.parse(
                new byte[] { 0x01, 0x2C, 0x01 });
        assertNotNull(m);
        assertEquals(300, m.getHeartRate());
    }

    /**
     * Both contact bits clear means the strap does not implement contact
     * detection at all. Reporting that as "not touching skin" is a very
     * common bug and produces a permanent false warning on such devices.
     */
    @Test
    void contactUnsupportedIsNotTheSameAsContactAbsent() {
        HeartRateMeasurement m =
                HeartRateMeasurement.parse(new byte[] { 0x00, 70 });
        assertNotNull(m);
        assertFalse(m.isSensorContactSupported());
        assertFalse(m.isSensorContactDetected());
    }

    @Test
    void contactSupportedAndDetectedAreReportedSeparately() {
        // bit2 supported, bit1 detected
        HeartRateMeasurement both =
                HeartRateMeasurement.parse(new byte[] { 0x06, 70 });
        assertNotNull(both);
        assertTrue(both.isSensorContactSupported());
        assertTrue(both.isSensorContactDetected());

        // bit2 supported, bit1 clear -> genuinely not touching skin
        HeartRateMeasurement supportedOnly =
                HeartRateMeasurement.parse(new byte[] { 0x04, 70 });
        assertNotNull(supportedOnly);
        assertTrue(supportedOnly.isSensorContactSupported());
        assertFalse(supportedOnly.isSensorContactDetected());
    }

    /**
     * Detected-without-supported is a malformed combination; the parser must
     * not report contact on the strength of a bit the profile says is
     * meaningless.
     */
    @Test
    void contactDetectedWithoutSupportedIsIgnored() {
        HeartRateMeasurement m =
                HeartRateMeasurement.parse(new byte[] { 0x02, 70 });
        assertNotNull(m);
        assertFalse(m.isSensorContactSupported());
        assertFalse(m.isSensorContactDetected());
    }

    /** Energy expended is transmitted in kilojoules, not kilocalories. */
    @Test
    void energyExpendedIsKilojoulesAndConvertsToKilocalories() {
        // flags bit3, uint8 rate, energy 0x0064 = 100 kJ
        HeartRateMeasurement m = HeartRateMeasurement.parse(
                new byte[] { 0x08, 70, 0x64, 0x00 });
        assertNotNull(m);
        assertTrue(m.hasEnergyExpended());
        assertEquals(100, m.getEnergyExpendedKilojoules());
        assertEquals(23.9, m.getEnergyExpendedKilocalories(), 0.1);
    }

    /**
     * The RR field has no count: it runs to the end of the payload. A strap
     * notifying once a second while the heart beats faster sends several per
     * notification, and dropping any of them corrupts every HRV metric.
     */
    @Test
    void allRrIntervalsInAPayloadAreReported() {
        // flags bit4, rate 60, three intervals: 1024, 512, 2048
        HeartRateMeasurement m = HeartRateMeasurement.parse(new byte[] {
                0x10, 60,
                0x00, 0x04,
                0x00, 0x02,
                0x00, 0x08 });
        assertNotNull(m);
        assertEquals(3, m.getRrIntervalCount());
        assertEquals(1000.0, m.getRrIntervalMillis(0), 1e-6);
        assertEquals(500.0, m.getRrIntervalMillis(1), 1e-6);
        assertEquals(2000.0, m.getRrIntervalMillis(2), 1e-6);
    }

    /** RR intervals are 1/1024-second units, not milliseconds. */
    @Test
    void rrIntervalsConvertFrom1024thsOfASecond() {
        HeartRateMeasurement m = HeartRateMeasurement.parse(
                new byte[] { 0x10, 60, (byte) 0x00, 0x03 });
        assertNotNull(m);
        assertEquals(1, m.getRrIntervalCount());
        assertEquals(768 * 1000.0 / 1024.0, m.getRrIntervalMillis(0), 1e-9);
    }

    @Test
    void parsesEnergyAndRrTogetherInTheRightOrder() {
        // flags bits 3 and 4, uint16 rate 0x004B = 75, energy 50, one RR
        HeartRateMeasurement m = HeartRateMeasurement.parse(new byte[] {
                0x19, 0x4B, 0x00,
                0x32, 0x00,
                0x00, 0x04 });
        assertNotNull(m);
        assertEquals(75, m.getHeartRate());
        assertEquals(50, m.getEnergyExpendedKilojoules());
        assertEquals(1, m.getRrIntervalCount());
        assertEquals(1000.0, m.getRrIntervalMillis(0), 1e-6);
    }

    /** Reserved flag bits are ignored rather than treated as an error. */
    @Test
    void reservedFlagBitsAreIgnored() {
        HeartRateMeasurement m = HeartRateMeasurement.parse(
                new byte[] { (byte) 0xE0, 65 });
        assertNotNull(m);
        assertEquals(65, m.getHeartRate());
    }

    /**
     * A short packet must yield null, not an exception: this runs inside a
     * notification callback, and one misbehaving strap must not crash the app.
     */
    @Test
    void truncatedPayloadsReturnNullAndNeverThrow() {
        assertNull(HeartRateMeasurement.parse(null));
        assertNull(HeartRateMeasurement.parse(new byte[0]));
        assertNull(HeartRateMeasurement.parse(new byte[] { 0x00 }));
        // claims uint16 but only one value byte follows
        assertNull(HeartRateMeasurement.parse(new byte[] { 0x01, 0x2C }));
        // claims energy expended but the field is missing
        assertNull(HeartRateMeasurement.parse(new byte[] { 0x08, 70 }));
        // claims energy expended but only half of it is present
        assertNull(HeartRateMeasurement.parse(
                new byte[] { 0x08, 70, 0x64 }));
    }

    /**
     * An odd trailing byte in the RR region is dropped rather than read past
     * the end of the array.
     */
    @Test
    void oddTrailingByteInRrRegionIsIgnoredSafely() {
        HeartRateMeasurement m = HeartRateMeasurement.parse(
                new byte[] { 0x10, 60, 0x00, 0x04, 0x11 });
        assertNotNull(m);
        assertEquals(1, m.getRrIntervalCount());
        assertEquals(1000.0, m.getRrIntervalMillis(0), 1e-6);
    }
}
