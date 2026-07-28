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

import com.codename1.impl.health.HealthChangePage;
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
                HealthDeleteRequest.byId(HealthDataType.STEPS, "abc"));
        assertTrue(byId.contains("\"ids\""));
        assertTrue(byId.contains("abc"));
        assertTrue(byId.contains("\"steps\""),
                "the id form carries its type too: Health Connect deletes"
                        + " by record class and cannot resolve a bare id");

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

    // ------------------------------------------------------------------
    // change pages
    // ------------------------------------------------------------------

    @Test
    void changePageCarriesAddedSamplesAndDeletedIds() {
        String payload = "tok2\t0\t1\n"
                + "+\tid1\tsteps\t1000\t2000\t100\tcount\t\n"
                + "-\tid9\n"
                + "+\tid2\tsteps\t3000\t4000\t200\tcount\t\n";
        HealthChangePage page = HealthWire.decodeChangePage(payload);
        assertNotNull(page);
        assertEquals("tok2", page.getNextToken());
        assertFalse(page.isExpired());
        assertTrue(page.hasMore());
        assertEquals(2, page.getAdded().size());
        assertEquals(1, page.getDeletedIds().size());
        assertEquals("id9", page.getDeletedIds().get(0));
    }

    /**
     * The whole point of the header flag: a token that aged out means the
     * changes are incomplete, and the caller has to resync rather than
     * treat an empty-looking page as "nothing happened".
     */
    @Test
    void expiredTokenIsReported() {
        HealthChangePage page = HealthWire.decodeChangePage("tok\t1\t0\n");
        assertNotNull(page);
        assertTrue(page.isExpired());
        assertTrue(page.getAdded().isEmpty());
    }

    /**
     * An unreadable reply must not decode to an empty page. An empty page
     * would advance the cursor past changes that were never seen, losing
     * them permanently; a null tells the caller to leave the cursor alone.
     */
    @Test
    void unreadableChangePayloadYieldsNullRatherThanAnEmptyPage() {
        assertNull(HealthWire.decodeChangePage(null));
        assertNull(HealthWire.decodeChangePage(""));
        assertNull(HealthWire.decodeChangePage("no-newline-header"));
        assertNull(HealthWire.decodeChangePage("tok\t0\n"),
                "a header missing the hasMore field is not a valid page");
    }

    @Test
    void undecodableChangeLinesAreSkippedButThePageSurvives() {
        String payload = "tok\t0\t0\n"
                + "+\tid1\tblood_unicorn\t1\t2\t7\tcount\t\n"
                + "?\tid2\n"
                + "+\tid3\tsteps\t1000\t2000\t100\tcount\t\n";
        HealthChangePage page = HealthWire.decodeChangePage(payload);
        assertNotNull(page);
        assertEquals(1, page.getAdded().size());
        assertTrue(page.getDeletedIds().isEmpty());
    }

    /**
     * A delete needs a type because Health Connect deletes by record class
     * plus id. Without one the request could only be honoured by guessing,
     * which would delete nothing and still report success.
     */
    @Test
    void deleteWithoutATypeIsRejected() {
        HealthDeleteRequest r = HealthDeleteRequest.byId(null, "abc");
        HealthException ex = assertThrows(HealthException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() throws Throwable {
                        r.validate();
                    }
                });
        assertSame(HealthError.INVALID_ARGUMENT, ex.getError());
    }

    private static SeriesSample heartRate(long base, int n) {
        long[] starts = new long[n];
        long[] ends = new long[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            starts[i] = base + i * 1000L;
            ends[i] = starts[i];
            values[i] = 60 + i;
        }
        return SeriesSample.create(HealthDataType.HEART_RATE, base,
                base + (n - 1) * 1000L, starts, ends, values,
                HealthUnit.COUNT_PER_MINUTE);
    }

    /**
     * A series write reaches the platform as its measurements rather than
     * as nothing at all.
     *
     * <p>The encoder used to skip any sample that was not a
     * {@link QuantitySample}, and both mobile ports hand this payload
     * straight to their bridge -- so a write of nothing but series data
     * produced an empty batch, which completes successfully with no
     * identifiers. The caller was told the write worked while none of it
     * was stored.</p>
     */
    @Test
    void aSeriesWriteCarriesItsMeasurements() {
        List<HealthSample> out = new ArrayList<HealthSample>();
        out.add(heartRate(10_000L, 3));

        SamplePage page = HealthWire.decodeSamplePage(
                HealthWire.encodeSamples(out));

        assertEquals(3, page.size(),
                "every measurement must reach the platform");
        for (int i = 0; i < 3; i++) {
            QuantitySample s = (QuantitySample) page.getSamples().get(i);
            assertSame(HealthDataType.HEART_RATE, s.getType());
            assertEquals(60 + i,
                    s.getValue(HealthUnit.COUNT_PER_MINUTE), 1e-9);
        }
    }

    /**
     * A shape the payload cannot carry is reported rather than dropped.
     *
     * <p>This is what stops the ports reporting a successful write of
     * nothing: they refuse the write instead.</p>
     */
    @Test
    void shapesTheWireCannotCarryAreReported() {
        List<HealthSample> series = new ArrayList<HealthSample>();
        series.add(heartRate(10_000L, 2));
        series.add(steps(1000L, 2000L, 10));
        assertNull(HealthWire.unsupportedForWrite(series),
                "quantities and series both encode");

        List<HealthSample> withSleep = new ArrayList<HealthSample>();
        withSleep.add(steps(1000L, 2000L, 10));
        SleepSample sleep = SleepSample.create(1000L, 2000L);
        withSleep.add(sleep);
        HealthSample rejected = HealthWire.unsupportedForWrite(withSleep);
        assertNotNull(rejected, "a shape with no line must be reported");
        assertSame(sleep, rejected);
    }

    /**
     * A series survives the wire whole when the caller asked to keep it.
     *
     * <p>`setFlattenSeries(false)` promises record identity, and the
     * descriptor never carried the option so the bridge flattened
     * regardless -- the option was honoured nowhere.</p>
     */
    @Test
    void anUnflattenedSeriesRoundTripsAsOneRecord() {
        SeriesSample original = heartRate(50_000L, 4);
        original.setId("hc-record-1");
        original.setRecordingMethod(RecordingMethod.AUTOMATIC);
        StringBuilder sb = new StringBuilder();
        HealthWire.appendSeries(sb, original, "com.example.app", "Example",
                "Strap");

        SamplePage page = HealthWire.decodeSamplePage(sb.toString());
        assertEquals(1, page.size(), "one record, not four samples");
        SeriesSample s = (SeriesSample) page.getSamples().get(0);
        assertEquals("hc-record-1", s.getId(),
                "record identity is the reason for asking");
        assertEquals(4, s.size());
        assertSame(RecordingMethod.AUTOMATIC, s.getRecordingMethod());
        assertEquals("com.example.app", s.getSource().getBundleId());
        for (int i = 0; i < 4; i++) {
            assertEquals(50_000L + i * 1000L, s.getSampleStartMillis(i));
            assertEquals(60 + i,
                    s.getSampleValue(i, HealthUnit.COUNT_PER_MINUTE), 1e-9);
        }
    }

    /**
     * A series line whose measurements disagree with its count is dropped.
     *
     * <p>Trusting the count would produce a record padded with zeroes,
     * which reads as real data at a plausible-looking timestamp.</p>
     */
    @Test
    void aSeriesLineWithATruncatedTailIsSkipped() {
        String line = "~id\theart_rate\t1000\t3000\t3\tcount/min\t\t\t\t"
                + "UNKNOWN\t1000:1000:60.0,2000:2000:61.0\n";
        assertEquals(0, HealthWire.decodeSamplePage(line).size(),
                "two measurements cannot answer a claim of three");
    }

    /** The query descriptor carries the flattening option. */
    @Test
    void theQueryDescriptorCarriesTheFlatteningOption() {
        SampleQuery q = new SampleQuery()
                .addType(HealthDataType.HEART_RATE)
                .setTimeRange(HealthTimeRange.between(0L, 1000L));
        assertTrue(HealthWire.encodeSampleQuery(q).indexOf(
                "\"flatten\":true") >= 0);
        assertTrue(HealthWire.encodeSampleQuery(q.setFlattenSeries(false))
                .indexOf("\"flatten\":false") >= 0,
                "a bridge cannot honour an option it is never told about");
    }

    /**
     * A series is checked and converted like a scalar write.
     *
     * <p>The wire carries the raw value and the bridges read it in the
     * type's canonical unit, so a series that skipped the conversion was
     * stored as the wrong numbers entirely -- 2 Hz persisted as 2 bpm
     * rather than 120 -- and an incompatible dimension was accepted
     * without complaint.</p>
     */
    @Test
    void aSeriesWriteIsConvertedToTheCanonicalUnit() throws Exception {
        long[] at = {1000L, 2000L};
        long[] ends = {1000L, 2000L};
        double[] hertz = {2.0, 3.0};
        SeriesSample series = SeriesSample.create(HealthDataType.HEART_RATE,
                1000L, 2000L, at, ends, hertz, HealthUnit.COUNT_PER_SECOND);

        FakeHealthStore store = new FakeHealthStore();
        store.write(series).get();

        assertEquals(1, store.writeChunks.size());
        SeriesSample written =
                (SeriesSample) store.writeChunks.get(0).get(0);
        assertSame(HealthUnit.COUNT_PER_MINUTE, written.getUnit(),
                "the port receives the unit it writes in");
        assertEquals(120.0,
                written.getSampleValue(0, HealthUnit.COUNT_PER_MINUTE), 1e-9);
        assertEquals(180.0,
                written.getSampleValue(1, HealthUnit.COUNT_PER_MINUTE), 1e-9);
    }

    /** A series in the wrong dimension is refused, not silently stored. */
    @Test
    void aSeriesInTheWrongDimensionIsRejected() {
        long[] at = {1000L};
        double[] kg = {70.0};
        SeriesSample series = SeriesSample.create(HealthDataType.HEART_RATE,
                1000L, 1000L, at, at, kg, HealthUnit.KILOGRAM);
        FakeHealthStore store = new FakeHealthStore();
        Throwable err = errorOf(store.write(series));
        assertNotNull(err, "an incompatible unit must not reach the port");
        assertSame(HealthError.UNIT_MISMATCH,
                ((HealthException) err).getError());
    }

    /**
     * A series longer than one batch is split so no single platform call
     * exceeds the record cap.
     *
     * <p>The chunker counted a series as one sample while the wire expands
     * it to one record per point, so a 5,000-point series went to Health
     * Connect as 5,000 records in one call -- past its 1,000-record cap,
     * rejecting the whole write that `write` documents as chunked.</p>
     */
    @Test
    void anOversizedSeriesIsSplitAcrossWrites() throws Exception {
        int n = 250;
        long[] at = new long[n];
        long[] ends = new long[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            at[i] = 1000L + i;
            ends[i] = at[i];
            values[i] = 60 + i % 10;
        }
        SeriesSample series = SeriesSample.create(HealthDataType.HEART_RATE,
                at[0], at[n - 1], at, ends, values,
                HealthUnit.COUNT_PER_MINUTE);

        FakeHealthStore store = new FakeHealthStore();
        store.maxWriteBatch = 100;
        store.write(series).get();

        int records = 0;
        for (List<HealthSample> chunk : store.writeChunks) {
            int cost = 0;
            for (HealthSample s : chunk) {
                cost += s instanceof SeriesSample
                        ? ((SeriesSample) s).size() : 1;
            }
            assertTrue(cost <= 100,
                    "no call may exceed the platform record cap, got " + cost);
            records += cost;
        }
        assertEquals(n, records, "every measurement is still written");
    }

    /**
     * Deleting is not writing. The series-shaped types have no
     * single-value write form on Health Connect but delete by record id
     * perfectly well -- which is the whole point of asking for record
     * identity in the first place.
     */
    @Test
    void seriesTypesAreDeletableOnAndroidEvenThoughTheyAreNotWritable() {
        assertFalse(HealthWire.isAndroidWritable(HealthDataType.POWER));
        assertTrue(HealthWire.isAndroidDeletable(HealthDataType.POWER));
        assertTrue(HealthWire.isAndroidDeletable(HealthDataType.SPEED));
        assertTrue(HealthWire.isAndroidDeletable(
                HealthDataType.CYCLING_CADENCE));
        assertTrue(HealthWire.isAndroidDeletable(
                HealthDataType.RUNNING_CADENCE));
        // Still bounded by what the bridge maps at all.
        assertFalse(HealthWire.isAndroidDeletable(
                HealthDataType.SLEEP));
    }

    private static Throwable errorOf(
            com.codename1.util.AsyncResource<?> r) {
        final Throwable[] err = new Throwable[1];
        r.except(new com.codename1.util.SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err[0] = t;
            }
        });
        return err[0];
    }

    /**
     * Nutrition is local and simulator only in this release, and the
     * documentation now says so. This pins the two gates both ports
     * consult, so the claim cannot quietly stop being true.
     */
    @Test
    void nutritionIsNotCarriedToEitherPhone() {
        assertFalse(HealthWire.isAndroidSupported(HealthDataType.NUTRITION),
                "Health Connect has no mapping for the record shape");
        assertFalse(HealthWire.isAndroidWritable(HealthDataType.NUTRITION));

        java.util.List<HealthSample> batch =
                new java.util.ArrayList<HealthSample>();
        batch.add(com.codename1.health.nutrition.NutritionSample.create(
                1767225600000L));
        assertNotNull(HealthWire.unsupportedForWrite(batch),
                "a nutrition entry must be refused, not written as"
                        + " something else");
    }

    /** The dietary quantities that do reach the phones still do. */
    @Test
    void dietaryQuantitiesAreStillCarried() {
        assertTrue(HealthWire.isAndroidSupported(HealthDataType.HYDRATION));
    }
}
