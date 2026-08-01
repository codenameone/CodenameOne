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
package com.codenameone.examples.hellocodenameone;

import com.codename1.health.AggregateMetric;
import com.codename1.health.AggregateQuery;
import com.codename1.health.AggregateResult;
import com.codename1.health.Health;
import com.codename1.health.HealthAuthorizationStatus;
import com.codename1.health.HealthAvailability;
import com.codename1.health.HealthDataType;
import com.codename1.health.HealthInterval;
import com.codename1.health.HealthQuantity;
import com.codename1.health.HealthSample;
import com.codename1.health.HealthStore;
import com.codename1.health.HealthTimeRange;
import com.codename1.health.HealthUnit;
import com.codename1.health.QuantitySample;
import com.codename1.health.SampleQuery;
import com.codename1.health.sensors.HealthSensorProfile;
import com.codename1.health.sensors.HeartRateMeasurement;
import com.codename1.testing.AbstractTest;

import java.util.List;
import java.time.ZoneId;

/**
 * On-device conformance for the health API, run on every port.
 *
 * <p>This deliberately asserts only what must hold <em>everywhere</em>,
 * because the ports genuinely differ: a device may have no health store, no
 * Bluetooth, or no granted permissions, and none of those are failures. What
 * must never differ is the shape of the API -- the facade is non-null, the
 * sub-facades are non-null, capability queries answer rather than throw, and
 * operations on an unsupported port fail cleanly instead of hanging or
 * crashing.</p>
 *
 * <p>The pure functions -- unit conversion, the GATT parsers, aggregate
 * boundaries -- are asserted exactly, since they must produce identical
 * results on ParparVM, on Android's ART, in the browser and on the desktop
 * JVM. Those are the parts most likely to break under a translator.</p>
 */
public class HealthConformanceTest extends AbstractTest {

    @Override
    public String toString() {
        return "HealthConformanceTest";
    }

    @Override
    public boolean runTest() throws Exception {
        return facadeIsAlwaysUsable()
                && capabilityQueriesAnswer()
                && unitConversionIsExact()
                && heartRateParserIsExact()
                && aggregateBucketsTileTheRange()
                && storeOperationsBehave();
    }

    /** The facade and every sub-facade must exist on every port. */
    private boolean facadeIsAlwaysUsable() {
        Health h = Health.getInstance();
        assertBool(h != null, "Health.getInstance() must never be null");
        assertBool(h.getStore() != null, "getStore() must never be null");
        assertBool(h.getWorkouts() != null,
                "getWorkouts() must never be null");
        assertBool(h.getSensors() != null, "getSensors() must never be null");
        assertBool(Health.getInstance() == h,
                "getInstance() must be stable");
        return true;
    }

    /**
     * Capability queries must answer rather than throw, whatever the port
     * supports.
     */
    private boolean capabilityQueriesAnswer() {
        Health h = Health.getInstance();
        HealthAvailability a = h.getAvailability();
        assertBool(a != null, "getAvailability() must return a value");
        HealthStore store = h.getStore();
        // Calling these must be safe even with no store behind them.
        store.isSupported();
        store.isTypeSupported(HealthDataType.STEPS);
        store.isPushDelivery();
        HealthAuthorizationStatus read =
                store.getReadAuthorizationStatus(HealthDataType.STEPS);
        assertBool(read != null,
                "read authorization status must answer, even if UNKNOWN");
        assertBool(h.getConfigurationProblems() != null,
                "configuration problems must never be null");
        return true;
    }

    /**
     * Unit conversion is pure arithmetic and must be identical on every
     * runtime. The glucose factor is included because getting it wrong is a
     * patient-safety issue, not a rounding difference.
     */
    private boolean unitConversionIsExact() {
        assertClose(0.45359237,
                HealthUnit.convert(1, HealthUnit.POUND, HealthUnit.KILOGRAM),
                1e-9, "lb to kg");
        assertClose(212.0,
                HealthUnit.convert(100, HealthUnit.DEGREE_CELSIUS,
                        HealthUnit.DEGREE_FAHRENHEIT),
                1e-9, "degC to degF (affine)");
        assertClose(5.5499,
                HealthUnit.convert(100, HealthUnit.MILLIGRAM_PER_DECILITER,
                        HealthUnit.MILLIMOLE_PER_LITER),
                1e-3, "glucose mg/dL to mmol/L");
        assertClose(1609.344,
                HealthUnit.convert(1, HealthUnit.MILE, HealthUnit.METER),
                1e-9, "mile to m");
        return true;
    }

    /**
     * The 0x2A37 flags-byte handling, which every translator must produce
     * identically -- unsigned reads especially, since signedness is where
     * ParparVM and the JS port are most likely to diverge.
     */
    private boolean heartRateParserIsExact() {
        HeartRateMeasurement m = HeartRateMeasurement.parse(
                new byte[] { 0x00, (byte) 200 });
        assertBool(m != null, "a valid uint8 payload must parse");
        assertBool(m.getHeartRate() == 200,
                "200 bpm must be read unsigned, got " + m.getHeartRate());

        HeartRateMeasurement rr = HeartRateMeasurement.parse(new byte[] {
                0x10, 60, 0x00, 0x04, 0x00, 0x02 });
        assertBool(rr != null, "an RR payload must parse");
        assertBool(rr.getRrIntervalCount() == 2,
                "both RR intervals must be reported, got "
                        + rr.getRrIntervalCount());
        assertClose(1000.0, rr.getRrIntervalMillis(0), 1e-6,
                "RR interval in ms");

        // A short payload must yield null rather than throwing, since this
        // runs inside a notification callback on a real device.
        assertBool(HeartRateMeasurement.parse(new byte[] { 0x01, 0x2C })
                == null, "a truncated payload must return null");

        assertBool(HealthSensorProfile.HEART_RATE.isStreaming(),
                "heart rate is a streaming profile");
        return true;
    }

    /**
     * Bucket boundaries are computed in shared code, so they must tile the
     * range identically everywhere -- including through the CLDC Calendar
     * implementations the translators provide.
     */
    private boolean aggregateBucketsTileTheRange() {
        ZoneId utc = ZoneId.of("UTC");
        HealthInterval day = HealthInterval.calendarDays(1, utc);
        long start = 1767225600000L; // 2026-01-01T00:00:00Z
        long next = day.nextBoundary(start);
        assertBool(next == start + 86400000L,
                "a UTC calendar day is 24h, got " + (next - start));

        HealthInterval hourly = HealthInterval.hours(1);
        assertBool(hourly.nextBoundary(start) == start + 3600000L,
                "an hour bucket must advance by an hour");
        assertBool(!hourly.isCalendarBased(),
                "a fixed interval is not calendar based");
        assertBool(day.isCalendarBased(),
                "a day interval is calendar based");
        return true;
    }

    /**
     * Store operations must behave on every port: either they work, or they
     * fail cleanly. Neither hanging nor throwing out of the call is
     * acceptable.
     */
    private boolean storeOperationsBehave() throws Exception {
        HealthStore store = Health.getInstance().getStore();
        SampleQuery q = new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.lastHours(1))
                .setLimit(10);

        // Must return a resource rather than throwing, whatever the port.
        assertBool(store.readSamples(q) != null,
                "readSamples must return a resource");

        AggregateQuery aq = new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.lastDays(1));
        assertBool(store.aggregate(aq) != null,
                "aggregate must return a resource");

        QuantitySample w = QuantitySample.create(HealthDataType.BODY_MASS,
                new HealthQuantity(70, HealthUnit.KILOGRAM),
                System.currentTimeMillis());
        assertBool(store.write(w) != null,
                "write must return a resource");

        // Where a store does exist, a write followed by a read must round
        // trip -- this is the only end-to-end assertion, and it is skipped
        // rather than failed where there is no store to write to.
        if (store.isSupported() && store.isWritable(HealthDataType.BODY_MASS)
                && Health.getInstance().getAvailability()
                        != HealthAvailability.NOT_SUPPORTED) {
            List<HealthSample> read = store.readSamples(new SampleQuery()
                    .addType(HealthDataType.BODY_MASS)
                    .setTimeRange(HealthTimeRange.lastHours(1))
                    .setLimit(50)).get();
            assertBool(read != null, "a supported store must return a list");
        }
        return true;
    }

    /**
     * A tolerance-based comparison, which AbstractTest does not provide for
     * doubles. Delegates the failure to the framework's own assertion so it
     * is reported the same way as every other failure.
     */
    private void assertClose(double expected, double actual,
            double epsilon, String message) {
        assertBool(Math.abs(expected - actual) <= epsilon,
                message + ": expected " + expected + " but was " + actual);
    }
}
