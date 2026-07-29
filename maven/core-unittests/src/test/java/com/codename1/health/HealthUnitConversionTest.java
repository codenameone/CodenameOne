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
package com.codename1.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit conversion. Health data carries units that differ by region and by
 * device, and a silent factor-of-two or factor-of-18 error is the most
 * consequential bug class in this API -- a mis-converted glucose reading is a
 * patient-safety issue, not a rendering glitch.
 */
class HealthUnitConversionTest {

    private static final double EPS = 1e-9;

    @Test
    void massConvertsBetweenMetricAndImperial() {
        assertEquals(1.0,
                HealthUnit.convert(1000, HealthUnit.GRAM,
                        HealthUnit.KILOGRAM), EPS);
        assertEquals(0.45359237,
                HealthUnit.convert(1, HealthUnit.POUND, HealthUnit.KILOGRAM),
                EPS);
        assertEquals(14.0,
                HealthUnit.convert(1, HealthUnit.STONE, HealthUnit.POUND),
                1e-6);
    }

    /**
     * Temperature is why conversion is affine rather than a plain ratio: a
     * multiplicative-only converter puts freezing point at 0 degrees F.
     */
    @Test
    void temperatureConversionAppliesTheOffset() {
        assertEquals(32.0,
                HealthUnit.convert(0, HealthUnit.DEGREE_CELSIUS,
                        HealthUnit.DEGREE_FAHRENHEIT), 1e-9);
        assertEquals(212.0,
                HealthUnit.convert(100, HealthUnit.DEGREE_CELSIUS,
                        HealthUnit.DEGREE_FAHRENHEIT), 1e-9);
        assertEquals(37.0,
                HealthUnit.convert(98.6, HealthUnit.DEGREE_FAHRENHEIT,
                        HealthUnit.DEGREE_CELSIUS), 1e-9);
        assertEquals(-40.0,
                HealthUnit.convert(-40, HealthUnit.DEGREE_FAHRENHEIT,
                        HealthUnit.DEGREE_CELSIUS), 1e-9);
    }

    /**
     * The clinically important one: 100 mg/dL is 5.55 mmol/L. Getting the
     * 18.0182 factor wrong -- or inverting it -- turns a normal fasting
     * glucose into either a hypoglycaemic emergency or diabetic ketoacidosis.
     */
    @Test
    void glucoseConvertsBetweenMgPerDlAndMmolPerL() {
        assertEquals(5.5499,
                HealthUnit.convert(100, HealthUnit.MILLIGRAM_PER_DECILITER,
                        HealthUnit.MILLIMOLE_PER_LITER), 1e-3);
        assertEquals(126.0,
                HealthUnit.convert(6.9930,
                        HealthUnit.MILLIMOLE_PER_LITER,
                        HealthUnit.MILLIGRAM_PER_DECILITER), 1e-2);
    }

    @Test
    void energyConvertsBetweenCaloriesAndJoules() {
        assertEquals(4.184,
                HealthUnit.convert(1, HealthUnit.KILOCALORIE,
                        HealthUnit.KILOJOULE), 1e-9);
        assertEquals(1000.0,
                HealthUnit.convert(1, HealthUnit.KILOJOULE,
                        HealthUnit.JOULE), 1e-6);
    }

    @Test
    void speedConvertsBetweenMetricAndImperial() {
        assertEquals(3.6,
                HealthUnit.convert(1, HealthUnit.METER_PER_SECOND,
                        HealthUnit.KILOMETER_PER_HOUR), 1e-9);
        assertEquals(26.8224,
                HealthUnit.convert(60, HealthUnit.MILE_PER_HOUR,
                        HealthUnit.METER_PER_SECOND), 1e-6);
    }

    @Test
    void distanceConvertsBetweenMetricAndImperial() {
        assertEquals(1609.344,
                HealthUnit.convert(1, HealthUnit.MILE, HealthUnit.METER),
                1e-9);
        assertEquals(12.0,
                HealthUnit.convert(1, HealthUnit.FOOT, HealthUnit.INCH),
                1e-9);
    }

    @Test
    void convertingAcrossDimensionsIsARejectedProgrammingError() {
        assertThrows(IllegalArgumentException.class,
                () -> HealthUnit.convert(1, HealthUnit.KILOGRAM,
                        HealthUnit.METER));
    }

    @Test
    void nullUnitsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> HealthUnit.convert(1, null, HealthUnit.METER));
    }

    /**
     * The mass and glucose dimensions are deliberately separate. If they
     * shared a "concentration" dimension, adding cholesterol in mg/dL later
     * would silently reuse glucose's molar mass.
     */
    @Test
    void glucoseConcentrationIsNotCompatibleWithPlainMass() {
        assertFalse(HealthUnit.MILLIGRAM_PER_DECILITER
                .isCompatibleWith(HealthUnit.MILLIGRAM));
        assertNotSame(HealthUnit.MILLIGRAM_PER_DECILITER.getDimension(),
                HealthUnit.MILLIGRAM.getDimension());
    }

    @Test
    void symbolsAreTheHealthKitWireFormat() {
        assertEquals("count/min", HealthUnit.COUNT_PER_MINUTE.getSymbol());
        assertEquals("kg", HealthUnit.KILOGRAM.getSymbol());
        assertEquals("kcal", HealthUnit.KILOCALORIE.getSymbol());
        assertEquals("mmHg",
                HealthUnit.MILLIMETER_OF_MERCURY.getSymbol());
        assertEquals("degC", HealthUnit.DEGREE_CELSIUS.getSymbol());
        assertEquals("mg/dL",
                HealthUnit.MILLIGRAM_PER_DECILITER.getSymbol());
        assertEquals("mL/(kg*min)",
                HealthUnit.ML_PER_KG_PER_MINUTE.getSymbol());
    }

    @Test
    void lookupBySymbolRoundTripsAndIsTotal() {
        for (HealthUnit u : HealthUnit.values()) {
            assertSame(u, HealthUnit.forSymbol(u.getSymbol()));
        }
        assertNull(HealthUnit.forSymbol("furlongs/fortnight"));
        assertNull(HealthUnit.forSymbol(null));
    }

    /**
     * Reading a value out of a quantity requires naming the unit, which is
     * what removes the whole pounds-read-as-kilograms bug class.
     */
    @Test
    void quantityConvertsOnRead() {
        HealthQuantity weight =
                new HealthQuantity(154, HealthUnit.POUND);
        assertEquals(69.85, weight.getValue(HealthUnit.KILOGRAM), 1e-2);
        assertEquals(154, weight.getRawValue(), EPS);
        assertSame(HealthUnit.POUND, weight.getUnit());
    }

    @Test
    void quantityInReturnsSelfWhenUnitAlreadyMatches() {
        HealthQuantity q = new HealthQuantity(70, HealthUnit.KILOGRAM);
        assertSame(q, q.in(HealthUnit.KILOGRAM));
    }

    @Test
    void quantityRequiresAUnit() {
        assertThrows(IllegalArgumentException.class,
                () -> new HealthQuantity(1, null));
    }

    /**
     * A series must be in chronological order, and says so rather than
     * being quietly accepted.
     *
     * <p>Readers rely on it: one asked for the newest measurements of a
     * long record takes them from the end rather than sorting half a
     * million of them to find out where they are, so an unordered series
     * would be answered with the wrong points and nothing would look
     * wrong.</p>
     */
    /**
     * Blood pressure is not a quantity type.
     *
     * <p>Classifying it as one let generic code dispatch on the kind,
     * cast a stored reading to {@link QuantitySample} and fail with a
     * ClassCastException -- and let a caller build a one-number quantity
     * for a reading that is a systolic *and* a diastolic.</p>
     */
    @Test
    void bloodPressureIsACompositeRatherThanAQuantity() {
        assertEquals(HealthDataKind.COMPOSITE,
                HealthDataType.BLOOD_PRESSURE.getKind(),
                "a reading made of two numbers is not a quantity");
        assertThrows(IllegalArgumentException.class,
                () -> QuantitySample.create(HealthDataType.BLOOD_PRESSURE,
                        new HealthQuantity(120,
                                HealthUnit.MILLIMETER_OF_MERCURY),
                        1000L),
                "and the quantity factory must refuse it");
        // The composite factory is unaffected.
        assertNotNull(BloodPressureSample.create(120, 80, 1000L));
    }

    @Test
    void anOutOfOrderSeriesIsRefused() {
        long[] starts = {2000L, 1000L};
        long[] ends = {2000L, 1000L};
        double[] values = {60, 62};
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SeriesSample.create(HealthDataType.HEART_RATE,
                        1000L, 2000L, starts, ends, values,
                        HealthUnit.COUNT_PER_MINUTE));
        assertTrue(ex.getMessage().contains("chronological"),
                "the refusal must name the contract, got: "
                        + ex.getMessage());

        // Equal timestamps are still fine: two readings can share one.
        long[] same = {1000L, 1000L};
        assertNotNull(SeriesSample.create(HealthDataType.HEART_RATE,
                1000L, 1000L, same, same, values,
                HealthUnit.COUNT_PER_MINUTE));
    }

    /**
     * A series the caller asked to keep whole still answers in the unit
     * the query asked for.
     *
     * <p>Only {@link QuantitySample} was normalized, so turning flattening
     * off -- an option about record shape, not about units -- silently
     * changed which unit the values came back in. The same query then
     * meant two different things depending on a flag that has nothing to
     * do with measurement.</p>
     */
    @Test
    void anUnflattenedSeriesIsConvertedToTheRequestedUnit()
            throws Exception {
        long[] at = {1000L, 2000L};
        long[] ends = {1000L, 2000L};
        double[] metresPerSecond = {10.0, 20.0};
        SeriesSample series = SeriesSample.create(HealthDataType.SPEED,
                1000L, 2000L, at, ends, metresPerSecond,
                HealthUnit.METER_PER_SECOND);

        FakeHealthStore store = new FakeHealthStore();
        java.util.List<HealthSample> page =
                new java.util.ArrayList<HealthSample>();
        page.add(series);
        store.pages.add(new SamplePage(page, null, false));

        java.util.List<HealthSample> read = store.readSamples(
                new SampleQuery().addType(HealthDataType.SPEED)
                        .setFlattenSeries(false)
                        .setUnit(HealthUnit.KILOMETER_PER_HOUR)
                        .setTimeRange(HealthTimeRange.between(0L, 5000L)))
                .get();

        assertEquals(1, read.size(), "the record stays whole");
        SeriesSample out = (SeriesSample) read.get(0);
        assertSame(HealthUnit.KILOMETER_PER_HOUR, out.getUnit(),
                "the series reports the unit that was asked for");
        assertEquals(36.0, out.getSampleValue(0,
                HealthUnit.KILOMETER_PER_HOUR), 1e-9);
        assertEquals(72.0, out.getSampleValue(1,
                HealthUnit.KILOMETER_PER_HOUR), 1e-9);
    }
}
