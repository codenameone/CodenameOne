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
 * The remaining Bluetooth SIG health profiles. Each case targets a trap
 * that produces plausible-looking wrong numbers rather than an obvious
 * failure -- which is what makes them dangerous.
 */
class SensorParserTest {

    // ---- Cycling Power (0x2A63) ----

    /**
     * Instantaneous power is sint16. A power meter reports negative watts
     * while back-pedalling or coasting downhill; reading it unsigned turns
     * a brief -5 W into 65531 W and poisons the ride's average and maximum.
     */
    @Test
    void cyclingPowerIsSignedAndCanBeNegative() {
        // flags 0x0000, power 0xFFFB = -5
        CyclingPowerMeasurement m = CyclingPowerMeasurement.parse(
                new byte[] { 0x00, 0x00, (byte) 0xFB, (byte) 0xFF });
        assertNotNull(m);
        assertEquals(-5, m.getInstantaneousPowerWatts());
    }

    @Test
    void cyclingPowerParsesPositiveValues() {
        // 0x00FA = 250 W
        CyclingPowerMeasurement m = CyclingPowerMeasurement.parse(
                new byte[] { 0x00, 0x00, (byte) 0xFA, 0x00 });
        assertNotNull(m);
        assertEquals(250, m.getInstantaneousPowerWatts());
        assertFalse(m.hasCrankData());
    }

    /**
     * The optional fields are positional, so a parser that skips the wrong
     * number of bytes reads crank data out of the middle of another field.
     */
    @Test
    void cyclingPowerSkipsOptionalFieldsInOrder() {
        // flags: pedal balance (0x01) + accumulated torque (0x04)
        //        + crank revolutions (0x20) = 0x0025
        byte[] payload = new byte[] {
            0x25, 0x00,             // flags
            (byte) 0xC8, 0x00,      // power 200
            50,                     // pedal balance (25%)
            0x00, 0x00,             // accumulated torque (skipped)
            0x0A, 0x00,             // crank revolutions 10
            0x00, 0x04              // crank event time 1024
        };
        CyclingPowerMeasurement m = CyclingPowerMeasurement.parse(payload);
        assertNotNull(m);
        assertEquals(200, m.getInstantaneousPowerWatts());
        assertEquals(25.0, m.getPedalPowerBalancePercent(), 1e-9);
        assertTrue(m.hasCrankData());
        assertEquals(10, m.getCrankRevolutions());
        assertEquals(1024, m.getLastCrankEventTime());
    }

    @Test
    void cyclingPowerRejectsTruncatedPayloads() {
        assertNull(CyclingPowerMeasurement.parse(null));
        assertNull(CyclingPowerMeasurement.parse(new byte[] { 0x00, 0x00 }));
        // claims crank data but the fields are missing
        assertNull(CyclingPowerMeasurement.parse(
                new byte[] { 0x20, 0x00, 0x00, 0x00 }));
    }

    // ---- Cycling Speed and Cadence (0x2A5B) ----

    @Test
    void cscParsesWheelAndCrankBlocksIndependently() {
        // flags 0x03: both present
        byte[] payload = new byte[] {
            0x03,
            0x10, 0x00, 0x00, 0x00, // wheel revs 16 (uint32)
            0x00, 0x04,             // wheel event 1024
            0x05, 0x00,             // crank revs 5 (uint16)
            0x00, 0x08              // crank event 2048
        };
        CscMeasurement m = CscMeasurement.parse(payload);
        assertNotNull(m);
        assertTrue(m.hasWheelData());
        assertTrue(m.hasCrankData());
        assertEquals(16L, m.getWheelRevolutions());
        assertEquals(1024, m.getLastWheelEventTime());
        assertEquals(5, m.getCrankRevolutions());
        assertEquals(2048, m.getLastCrankEventTime());
    }

    @Test
    void cscCrankOnlyPayloadReportsNoWheelData() {
        CscMeasurement m = CscMeasurement.parse(
                new byte[] { 0x02, 0x05, 0x00, 0x00, 0x04 });
        assertNotNull(m);
        assertFalse(m.hasWheelData());
        assertTrue(m.hasCrankData());
        assertEquals(-1L, m.getWheelRevolutions());
    }

    /**
     * A uint32 revolution count above 2^31 must stay positive; read as a
     * signed int it goes negative and the derived cadence flips sign.
     */
    @Test
    void cscWheelRevolutionsStayPositiveAboveTwoToThe31() {
        byte[] payload = new byte[] {
            0x01,
            0x00, 0x00, 0x00, (byte) 0x80, // 2147483648
            0x00, 0x04
        };
        CscMeasurement m = CscMeasurement.parse(payload);
        assertNotNull(m);
        assertEquals(2147483648L, m.getWheelRevolutions());
    }

    // ---- Running Speed and Cadence (0x2A53) ----

    @Test
    void rscParsesSpeedAndCadence() {
        // speed 0x0200 = 512 -> 2 m/s; cadence 180 steps/min
        RscMeasurement m = RscMeasurement.parse(
                new byte[] { 0x04, 0x00, 0x02, (byte) 180 });
        assertNotNull(m);
        assertEquals(2.0, m.getSpeedMetersPerSecond(), 1e-9);
        assertEquals(180, m.getCadenceStepsPerMinute());
        assertTrue(m.isRunning());
    }

    /**
     * Cadence is transmitted in steps per minute, not strides. Halving it
     * is a UI decision, made explicit rather than applied silently.
     */
    @Test
    void rscExposesBothStepAndStrideConventions() {
        RscMeasurement m = RscMeasurement.parse(
                new byte[] { 0x00, 0x00, 0x02, (byte) 180 });
        assertNotNull(m);
        assertEquals(180, m.getCadenceStepsPerMinute());
        assertEquals(90.0, m.getStrideRatePerMinute(), 1e-9);
    }

    @Test
    void rscTotalDistanceIsTenthsOfAMetre() {
        // flags 0x02: total distance present; 0x000003E8 = 1000 -> 100 m
        byte[] payload = new byte[] {
            0x02, 0x00, 0x02, (byte) 180,
            (byte) 0xE8, 0x03, 0x00, 0x00
        };
        RscMeasurement m = RscMeasurement.parse(payload);
        assertNotNull(m);
        assertEquals(100.0, m.getTotalDistanceMeters(), 1e-9);
    }

    // ---- Health Thermometer (0x2A1C) ----

    /**
     * Temperature uses the 32-bit IEEE-11073 FLOAT, unlike blood pressure
     * and glucose which use the 16-bit SFLOAT. Decoding one as the other
     * yields plausible nonsense.
     */
    @Test
    void temperatureDecodesIeee11073Float32() {
        // mantissa 365, exponent -1 -> 36.5 degC
        byte[] payload = new byte[] {
            0x00,
            0x6D, 0x01, 0x00,       // mantissa 365 (24-bit LE)
            (byte) 0xFF             // exponent -1
        };
        TemperatureMeasurement m = TemperatureMeasurement.parse(payload);
        assertNotNull(m);
        assertEquals(36.5, m.getCelsius(), 1e-9);
        assertEquals(97.7, m.getFahrenheit(), 1e-6);
    }

    @Test
    void temperatureConvertsFromFahrenheitWhenFlagged() {
        // flags 0x01 = Fahrenheit; mantissa 986, exponent -1 -> 98.6F
        byte[] payload = new byte[] {
            0x01,
            (byte) 0xDA, 0x03, 0x00,
            (byte) 0xFF
        };
        TemperatureMeasurement m = TemperatureMeasurement.parse(payload);
        assertNotNull(m);
        assertEquals(37.0, m.getCelsius(), 1e-6);
    }

    /**
     * A thermometer that cannot obtain a reading sends a reserved value.
     * Letting it through would report a patient temperature of 8388607.
     */
    @Test
    void temperatureRejectsTheReservedNaNValue() {
        byte[] payload = new byte[] {
            0x00,
            (byte) 0xFF, (byte) 0xFF, 0x7F, // 0x007FFFFF = NaN
            0x00
        };
        assertNull(TemperatureMeasurement.parse(payload));
    }

    // ---- Blood Pressure (0x2A35) ----

    @Test
    void bloodPressureDecodesThreeSfloatValues() {
        // 120, 80, 93 mmHg as SFLOAT with exponent 0
        byte[] payload = new byte[] {
            0x00,
            0x78, 0x00,   // systolic 120
            0x50, 0x00,   // diastolic 80
            0x5D, 0x00    // mean 93
        };
        BloodPressureMeasurement m = BloodPressureMeasurement.parse(payload);
        assertNotNull(m);
        assertEquals(120.0, m.getSystolicMmHg(), 1e-9);
        assertEquals(80.0, m.getDiastolicMmHg(), 1e-9);
        assertEquals(93.0, m.getMeanArterialMmHg(), 1e-9);
        assertFalse(m.hasPulse());
    }

    /**
     * A cuff that fails mid-inflation sends the reserved SFLOAT NaN.
     * Without this check the reading surfaces as 2047 mmHg.
     */
    @Test
    void bloodPressureRejectsAFailedMeasurement() {
        byte[] payload = new byte[] {
            0x00,
            (byte) 0xFF, 0x07,   // 0x07FF = NaN
            0x50, 0x00,
            0x5D, 0x00
        };
        assertNull(BloodPressureMeasurement.parse(payload));
    }

    @Test
    void bloodPressureConvertsFromKilopascals() {
        // flags 0x01 = kPa; 16 kPa is about 120 mmHg
        byte[] payload = new byte[] {
            0x01,
            0x10, 0x00,
            0x0A, 0x00,
            0x0C, 0x00
        };
        BloodPressureMeasurement m = BloodPressureMeasurement.parse(payload);
        assertNotNull(m);
        assertEquals(120.0, m.getSystolicMmHg(), 0.1);
    }

    @Test
    void bloodPressureRejectsTruncatedPayloads() {
        assertNull(BloodPressureMeasurement.parse(null));
        assertNull(BloodPressureMeasurement.parse(new byte[] { 0x00, 0x78 }));
    }

    // ---- Weight Scale (0x2A9D) ----

    /**
     * The raw uint16 is scaled by 0.005 kg in SI mode but 0.01 lb in
     * Imperial -- a different multiplier, not a unit conversion applied
     * afterwards. Confusing them is wrong by roughly a factor of two,
     * which is close enough to look plausible.
     */
    @Test
    void weightUsesADifferentResolutionPerUnitSystem() {
        // SI: 14000 * 0.005 = 70 kg
        WeightMeasurement si = WeightMeasurement.parse(
                new byte[] { 0x00, (byte) 0xB0, 0x36 });
        assertNotNull(si);
        assertFalse(si.isImperial());
        assertEquals(70.0, si.getWeightKg(), 1e-9);

        // Imperial: 15400 * 0.01 lb = 154 lb = 69.85 kg
        WeightMeasurement imperial = WeightMeasurement.parse(
                new byte[] { 0x01, 0x28, 0x3C });
        assertNotNull(imperial);
        assertTrue(imperial.isImperial());
        assertEquals(69.85, imperial.getWeightKg(), 1e-2);
    }

    @Test
    void weightParsesOptionalBmiAndHeight() {
        // flags 0x08: BMI and height present, SI units
        byte[] payload = new byte[] {
            0x08,
            (byte) 0xB0, 0x36,   // 70 kg
            (byte) 0xE6, 0x00,   // BMI 23.0
            (byte) 0xEA, 0x06    // height 1770 * 0.001 = 1.77 m
        };
        WeightMeasurement m = WeightMeasurement.parse(payload);
        assertNotNull(m);
        assertTrue(m.hasBmiAndHeight());
        assertEquals(23.0, m.getBmi(), 1e-9);
        assertEquals(1.77, m.getHeightMeters(), 1e-9);
    }

    @Test
    void weightRejectsTruncatedPayloads() {
        assertNull(WeightMeasurement.parse(null));
        assertNull(WeightMeasurement.parse(new byte[] { 0x00 }));
        assertNull(WeightMeasurement.parse(
                new byte[] { 0x08, (byte) 0xB0, 0x36 }));
    }

    // ---- Glucose (0x2A18) ----

    @Test
    void glucoseDecodesConcentrationAndSequence() {
        // flags 0x02: concentration present, kg/L units
        // 0x00 base time year 2026-01-01 12:00:00
        byte[] payload = new byte[] {
            0x02,
            0x07, 0x00,                   // sequence 7
            (byte) 0xEA, 0x07, 1, 1, 12, 0, 0,  // 2026-01-01 12:00:00
            0x5A, (byte) 0xB0,            // SFLOAT: 90 * 10^-5 kg/L
            0x11                          // type 1, location 1 (finger)
        };
        GlucoseMeasurement m = GlucoseMeasurement.parse(payload);
        assertNotNull(m);
        assertEquals(7, m.getSequenceNumber());
        assertTrue(m.hasConcentration());
        assertEquals(GlucoseMeasurement.SAMPLE_LOCATION_FINGER,
                m.getSampleLocation());
        // 90 mg/dL is about 5 mmol/L
        assertEquals(90.0, m.getMilligramsPerDeciliter(), 0.5);
        assertEquals(4.995, m.getMillimolesPerLiter(), 0.05);
    }

    /**
     * A control-solution reading is a calibration check, not the user's
     * blood glucose. Storing it would put a fictitious value into their
     * medical history.
     */
    @Test
    void glucoseFlagsControlSolutionReadings() {
        byte[] payload = new byte[] {
            0x02,
            0x08, 0x00,
            (byte) 0xEA, 0x07, 1, 1, 12, 0, 0,
            0x5A, (byte) 0xB0,
            0x41                          // location 4 = control solution
        };
        GlucoseMeasurement m = GlucoseMeasurement.parse(payload);
        assertNotNull(m);
        assertTrue(m.isControlSolution());
    }

    @Test
    void glucoseWithoutConcentrationIsAPlaceholderRecord() {
        byte[] payload = new byte[] {
            0x00,
            0x09, 0x00,
            (byte) 0xEA, 0x07, 1, 1, 12, 0, 0
        };
        GlucoseMeasurement m = GlucoseMeasurement.parse(payload);
        assertNotNull(m);
        assertFalse(m.hasConcentration());
        assertTrue(Double.isNaN(m.getMillimolesPerLiter()));
    }

    /**
     * 0x8000 is the profile's "offset unknown" value. Read as a number it
     * is -32768 minutes, which moved the reading back about 22.8 days --
     * a real glucose value published, and on some paths stored, under a
     * date the meter never reported.
     */
    @Test
    void glucoseIgnoresTheUnknownTimeOffsetSentinel() {
        byte[] withSentinel = new byte[] {
            0x03,                         // time offset + concentration
            0x0A, 0x00,
            (byte) 0xEA, 0x07, 1, 1, 12, 0, 0,  // 2026-01-01 12:00:00
            0x00, (byte) 0x80,            // offset 0x8000 = unknown
            0x5A, (byte) 0xB0,
            0x11
        };
        byte[] noOffset = new byte[] {
            0x02,
            0x0A, 0x00,
            (byte) 0xEA, 0x07, 1, 1, 12, 0, 0,
            0x5A, (byte) 0xB0,
            0x11
        };
        GlucoseMeasurement sentinel = GlucoseMeasurement.parse(withSentinel);
        GlucoseMeasurement plain = GlucoseMeasurement.parse(noOffset);
        assertNotNull(sentinel);
        assertNotNull(plain);
        assertEquals(plain.getTimestampMillis(),
                sentinel.getTimestampMillis());
    }

    /** Every other sint16 is a real offset, including large ones: a meter
     *  that stamps one base time and offsets months of records from it is
     *  doing what the profile describes. */
    @Test
    void glucoseAppliesAnOrdinaryTimeOffset() {
        byte[] payload = new byte[] {
            0x03,
            0x0B, 0x00,
            (byte) 0xEA, 0x07, 1, 1, 12, 0, 0,
            0x1E, 0x00,                   // +30 minutes
            0x5A, (byte) 0xB0,
            0x11
        };
        byte[] noOffset = new byte[] {
            0x02,
            0x0B, 0x00,
            (byte) 0xEA, 0x07, 1, 1, 12, 0, 0,
            0x5A, (byte) 0xB0,
            0x11
        };
        GlucoseMeasurement offset = GlucoseMeasurement.parse(payload);
        GlucoseMeasurement plain = GlucoseMeasurement.parse(noOffset);
        assertNotNull(offset);
        assertNotNull(plain);
        assertEquals(plain.getTimestampMillis() + 30L * 60000L,
                offset.getTimestampMillis());
    }

    @Test
    void glucoseRejectsTruncatedPayloads() {
        assertNull(GlucoseMeasurement.parse(null));
        assertNull(GlucoseMeasurement.parse(new byte[] { 0x02, 0x07 }));
    }

    // ---- Profiles ----

    @Test
    void profilesDeclareTheirServiceAndStreamingNature() {
        assertEquals(0x180D,
                HealthSensorProfile.HEART_RATE.getServiceUuid()
                        .getShortValue());
        assertTrue(HealthSensorProfile.HEART_RATE.isStreaming());
        // Episodic devices report one reading per measurement, so UI should
        // say "waiting" rather than showing a stale live value.
        assertFalse(HealthSensorProfile.WEIGHT_SCALE.isStreaming());
        assertFalse(HealthSensorProfile.BLOOD_PRESSURE.isStreaming());
        assertFalse(HealthSensorProfile.GLUCOSE.isStreaming());
    }

    @Test
    void everyProfileDeclaresAtLeastOneProducedType() {
        for (HealthSensorProfile p : HealthSensorProfile.values()) {
            assertFalse(p.getProducedTypes().isEmpty(),
                    p.getName() + " should produce at least one type");
            assertNotNull(p.getMeasurementUuid());
        }
    }

    // ------------------------------------------------------------------
    // cumulative counters
    // ------------------------------------------------------------------

    /**
     * A stopped wheel repeats its counter and timestamp forever.
     *
     * <p>Returning NaN for every repeat left a live cadence display frozen
     * at the last moving value, so a rider waiting at a light still read
     * 90 rpm. After enough repeats to rule out a duplicate packet the
     * tracker reports zero, which is what is actually happening.</p>
     */
    @Test
    void aStoppedSensorEventuallyReportsZero() {
        CumulativeCounterTracker t = new CumulativeCounterTracker(
                0x10000L, 1024);
        assertTrue(Double.isNaN(t.update(100, 0)),
                "the first notification has no baseline");
        double moving = t.update(110, 1024);
        assertEquals(600.0, moving, 0.1, "10 revolutions in one second");

        // The sensor stops: same counter, same event time, repeatedly.
        assertTrue(Double.isNaN(t.update(110, 1024)),
                "one repeat could still be a duplicate packet");
        assertEquals(0.0, t.update(110, 1024), 0.001,
                "a sustained repeat means stopped, not unknown");
        assertEquals(0.0, t.update(110, 1024), 0.001);
    }

    /** Movement after a stop resumes reporting a real rate. */
    @Test
    void movementAfterAStopReportsAgain() {
        CumulativeCounterTracker t = new CumulativeCounterTracker(
                0x10000L, 1024);
        t.update(100, 0);
        t.update(110, 1024);
        t.update(110, 1024);
        assertEquals(0.0, t.update(110, 1024), 0.001);
        assertEquals(300.0, t.update(115, 2048), 0.1,
                "five revolutions in the next second");
    }
}
