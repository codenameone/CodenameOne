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

import com.codename1.impl.health.HealthWire;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The wire format shared by the Android bridge and the iOS native layer.
 *
 * <p>The behaviour that matters most is what happens to a line this build
 * cannot understand: it is skipped, not fatal. An older app reading a store
 * a newer OS has added record types to must lose the unfamiliar rows and
 * keep the familiar ones, rather than failing the whole page.</p>
 */
class HealthWireTest {

    private static QuantitySample steps(long start, long end, double count) {
        return QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(count, HealthUnit.COUNT), start, end);
    }

    @Test
    void samplesRoundTripThroughTheWireFormat() {
        List<HealthSample> out = new ArrayList<HealthSample>();
        out.add(steps(1000L, 2000L, 250));

        String encoded = HealthWire.encodeSamples(out);
        SamplePage page = HealthWire.decodeSamplePage(encoded);

        assertEquals(1, page.size());
        QuantitySample s = (QuantitySample) page.getSamples().get(0);
        assertSame(HealthDataType.STEPS, s.getType());
        assertEquals(250, s.getValue(HealthUnit.COUNT), 1e-9);
        assertEquals(1000L, s.getStartMillis());
        assertEquals(2000L, s.getEndMillis());
    }

    @Test
    void instantaneousSamplesRoundTrip() {
        List<HealthSample> out = new ArrayList<HealthSample>();
        out.add(QuantitySample.create(HealthDataType.BODY_MASS,
                new HealthQuantity(70.5, HealthUnit.KILOGRAM), 5000L));

        SamplePage page = HealthWire.decodeSamplePage(
                HealthWire.encodeSamples(out));
        QuantitySample s = (QuantitySample) page.getSamples().get(0);
        assertTrue(s.isInstantaneous());
        assertEquals(70.5, s.getValue(HealthUnit.KILOGRAM), 1e-9);
    }

    /**
     * A record type this build has never heard of. Skipping the line is
     * what lets an older app keep working against a newer store.
     */
    @Test
    void unknownTypeSkipsTheLineRatherThanFailingThePage() {
        String payload = "id1\tsteps\t1000\t2000\t100\tcount\t\n"
                + "id2\tblood_unicorn\t1000\t2000\t7\tcount\t\n"
                + "id3\tsteps\t3000\t4000\t200\tcount\t\n";
        SamplePage page = HealthWire.decodeSamplePage(payload);
        assertEquals(2, page.size(), "the two known lines must survive");
    }

    @Test
    void unknownUnitSkipsTheLine() {
        String payload = "id1\tsteps\t1000\t2000\t100\tfurlongs\t\n";
        assertEquals(0, HealthWire.decodeSamplePage(payload).size());
    }

    @Test
    void malformedLinesAreSkippedAndNeverThrow() {
        String payload = "\n"
                + "not-enough-fields\n"
                + "id\tsteps\tnot-a-number\t2000\t100\tcount\t\n"
                + "id\tsteps\t1000\t2000\tnot-a-number\tcount\t\n"
                + "id\tsteps\t1000\t2000\t100\tcount\t\n";
        SamplePage page = HealthWire.decodeSamplePage(payload);
        assertEquals(1, page.size());
    }

    /**
     * An interval-only type sent as an instant is rejected by the sample
     * constructor; the decoder must absorb that rather than propagate it.
     */
    @Test
    void invalidTypeShapeSkipsTheLine() {
        String payload = "id\tsteps\t1000\t1000\t100\tcount\t\n";
        assertEquals(0, HealthWire.decodeSamplePage(payload).size(),
                "an instantaneous step count is not a valid sample");
    }

    @Test
    void emptyAndNullPayloadsDecodeToAnEmptyPage() {
        assertEquals(0, HealthWire.decodeSamplePage(null).size());
        assertEquals(0, HealthWire.decodeSamplePage("").size());
    }

    @Test
    void sourceIsCarriedWhenPresent() {
        String payload =
                "id\tsteps\t1000\t2000\t100\tcount\tcom.example.app\tExample"
                + "\tPixel\n";
        SamplePage page = HealthWire.decodeSamplePage(payload);
        assertEquals(1, page.size());
        HealthSource src = page.getSamples().get(0).getSource();
        assertNotNull(src);
        assertEquals("com.example.app", src.getBundleId());
        assertEquals("Example", src.getName());
        assertEquals("Pixel", src.getDeviceName());
    }

    @Test
    void writeResultDecodesOneIdPerLine() {
        HealthWriteResult r = HealthWire.decodeWriteResult("a\nb\n\nc\n");
        assertEquals(3, r.getWrittenCount());
        assertEquals("a", r.getSampleIds().get(0));
        assertEquals("c", r.getSampleIds().get(2));
    }

    @Test
    void sampleQueryEncodesTheRangeAndLimit() {
        String json = HealthWire.encodeSampleQuery(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(100L, 200L))
                .setLimit(42));
        assertTrue(json.contains("\"steps\""));
        assertTrue(json.contains("\"start\":100"));
        assertTrue(json.contains("\"end\":200"));
        assertTrue(json.contains("\"limit\":42"));
    }

    @Test
    void deleteRequestEncodesEitherIdsOrARange() {
        String byId = HealthWire.encodeDeleteRequest(
                HealthDeleteRequest.byId("abc"));
        assertTrue(byId.contains("\"ids\""));
        assertTrue(byId.contains("abc"));

        String byRange = HealthWire.encodeDeleteRequest(
                HealthDeleteRequest.byRange(HealthDataType.STEPS,
                        HealthTimeRange.between(1L, 2L)));
        assertTrue(byRange.contains("\"types\""));
        assertTrue(byRange.contains("\"start\":1"));
    }

    /**
     * Aggregate buckets the platform said nothing about stay empty rather
     * than being filled in, preserving the null-not-zero contract across
     * the native boundary.
     */
    @Test
    void aggregateDecodingLeavesUnreportedBucketsEmpty() {
        long[] boundaries = { 0L, 100L, 200L, 300L };
        AggregateQuery q = new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.between(0L, 300L));
        String payload = "0\tsteps\tTOTAL\t500.0\tcount\n";

        List<AggregateResult> buckets =
                HealthWire.decodeAggregates(payload, q, boundaries);
        assertEquals(3, buckets.size());
        assertEquals(500.0, buckets.get(0)
                .get(HealthDataType.STEPS, AggregateMetric.TOTAL)
                .getValue(HealthUnit.COUNT), 1e-9);
        assertNull(buckets.get(1)
                .get(HealthDataType.STEPS, AggregateMetric.TOTAL));
        assertNull(buckets.get(2)
                .get(HealthDataType.STEPS, AggregateMetric.TOTAL));
    }

    @Test
    void aggregateLinesOutsideTheBucketRangeAreIgnored() {
        long[] boundaries = { 0L, 100L };
        AggregateQuery q = new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.between(0L, 100L));
        String payload = "9\tsteps\tTOTAL\t500.0\tcount\n"
                + "-1\tsteps\tTOTAL\t500.0\tcount\n"
                + "0\tsteps\tNONSENSE\t500.0\tcount\n";
        List<AggregateResult> buckets =
                HealthWire.decodeAggregates(payload, q, boundaries);
        assertEquals(1, buckets.size());
        assertTrue(buckets.get(0).isEmpty());
    }
}
