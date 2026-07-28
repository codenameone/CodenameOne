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

import com.codename1.impl.health.LocalHealthStore;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end behaviour of the local store, which is what the desktop,
 * JavaScript and simulator ports return. These cases also pin the shared
 * aggregation rules, since the local store is the only implementation that
 * exercises them without a platform behind it.
 */
class LocalHealthStoreTest extends UITestBase {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone LA =
            TimeZone.getTimeZone("America/Los_Angeles");

    private LocalHealthStore store;

    @BeforeEach
    void createStore() {
        store = new LocalHealthStore();
    }

    private static long utc(int year, int month, int day, int hour) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(year, month - 1, day, hour, 0, 0);
        return c.getTimeInMillis();
    }

    private void writeSteps(long start, long end, double count) {
        QuantitySample s = QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(count, HealthUnit.COUNT), start, end);
        assertNull(errorOf(store.write(s)), "write should succeed");
    }

    @Test
    void writeThenReadRoundTrips() {
        writeSteps(utc(2026, 1, 1, 9), utc(2026, 1, 1, 10), 1200);
        List<HealthSample> read = store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(utc(2026, 1, 1, 0),
                        utc(2026, 1, 2, 0)))).get();
        assertEquals(1, read.size());
        assertEquals(1200,
                ((QuantitySample) read.get(0)).getValue(HealthUnit.COUNT),
                1e-9);
        assertNotNull(read.get(0).getId(), "a written sample gets an id");
    }

    /**
     * Values come back in the type's canonical unit no matter what unit
     * they were written in, so cross-platform code never has to ask.
     */
    @Test
    void readsAreNormalizedToTheCanonicalUnit() {
        QuantitySample lbs = QuantitySample.create(HealthDataType.BODY_MASS,
                new HealthQuantity(154, HealthUnit.POUND), utc(2026, 1, 1, 8));
        store.write(lbs).get();

        List<HealthSample> read = store.readSamples(new SampleQuery()
                .addType(HealthDataType.BODY_MASS)
                .setTimeRange(HealthTimeRange.between(utc(2026, 1, 1, 0),
                        utc(2026, 1, 2, 0)))).get();
        assertEquals(1, read.size());
        QuantitySample q = (QuantitySample) read.get(0);
        assertSame(HealthUnit.KILOGRAM, q.getQuantity().getUnit());
        assertEquals(69.85, q.getValue(HealthUnit.KILOGRAM), 1e-2);
    }

    @Test
    void requestedUnitOverridesTheCanonicalOne() {
        QuantitySample kg = QuantitySample.create(HealthDataType.BODY_MASS,
                new HealthQuantity(70, HealthUnit.KILOGRAM),
                utc(2026, 1, 1, 8));
        store.write(kg).get();

        List<HealthSample> read = store.readSamples(new SampleQuery()
                .addType(HealthDataType.BODY_MASS)
                .setUnit(HealthUnit.POUND)
                .setTimeRange(HealthTimeRange.between(utc(2026, 1, 1, 0),
                        utc(2026, 1, 2, 0)))).get();
        assertSame(HealthUnit.POUND,
                ((QuantitySample) read.get(0)).getQuantity().getUnit());
    }

    /**
     * Steps accumulate over time, so an instantaneous step sample is
     * meaningless. It is rejected here rather than by the platform later.
     */
    @Test
    void instantaneousWriteOfACumulativeTypeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> QuantitySample.create(HealthDataType.STEPS,
                        new HealthQuantity(100, HealthUnit.COUNT),
                        utc(2026, 1, 1, 9)));
    }

    @Test
    void writeWithAWrongDimensionUnitFailsBeforeReachingTheStore() {
        QuantitySample bad = QuantitySample.create(HealthDataType.BODY_MASS,
                new HealthQuantity(70, HealthUnit.METER),
                utc(2026, 1, 1, 8));
        Throwable err = errorOf(store.write(bad));
        assertNotNull(err);
        assertEquals(HealthError.UNIT_MISMATCH,
                ((HealthException) err).getError());
    }

    @Test
    void deleteByIdRemovesOnlyThatSample() {
        writeSteps(utc(2026, 1, 1, 9), utc(2026, 1, 1, 10), 100);
        writeSteps(utc(2026, 1, 1, 11), utc(2026, 1, 1, 12), 200);
        List<HealthSample> all = store.readSamples(allStepsIn(2026, 1, 1))
                .get();
        assertEquals(2, all.size());

        int removed = store.delete(
                HealthDeleteRequest.byId(HealthDataType.STEPS,
                        all.get(0).getId())).get()
                .intValue();
        assertEquals(1, removed);
        assertEquals(1, store.readSamples(allStepsIn(2026, 1, 1)).get().size());
    }

    private static SampleQuery allStepsIn(int y, int m, int d) {
        return new SampleQuery().addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(utc(y, m, d, 0),
                        utc(y, m, d + 1, 0)));
    }

    @Test
    void cumulativeAggregationSumsPerBucket() {
        writeSteps(utc(2026, 1, 1, 9), utc(2026, 1, 1, 10), 1000);
        writeSteps(utc(2026, 1, 1, 11), utc(2026, 1, 1, 12), 500);
        writeSteps(utc(2026, 1, 2, 9), utc(2026, 1, 2, 10), 300);

        List<AggregateResult> buckets = store.aggregate(new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.between(utc(2026, 1, 1, 0),
                        utc(2026, 1, 3, 0)))
                .setBucket(HealthInterval.calendarDays(1, UTC))).get();

        assertEquals(2, buckets.size());
        assertEquals(1500, buckets.get(0)
                .get(HealthDataType.STEPS, AggregateMetric.TOTAL)
                .getValue(HealthUnit.COUNT), 1e-9);
        assertEquals(300, buckets.get(1)
                .get(HealthDataType.STEPS, AggregateMetric.TOTAL)
                .getValue(HealthUnit.COUNT), 1e-9);
    }

    /**
     * The single most consequential aggregation rule: an empty bucket
     * reports null, not zero. A day with no data and a day on which the
     * user took no steps are different facts, and conflating them draws a
     * flat line through every day the phone stayed home.
     */
    @Test
    void emptyBucketsReportNullRatherThanZero() {
        writeSteps(utc(2026, 1, 1, 9), utc(2026, 1, 1, 10), 1000);

        List<AggregateResult> buckets = store.aggregate(new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.between(utc(2026, 1, 1, 0),
                        utc(2026, 1, 3, 0)))
                .setBucket(HealthInterval.calendarDays(1, UTC))).get();

        assertEquals(2, buckets.size());
        assertNotNull(buckets.get(0)
                .get(HealthDataType.STEPS, AggregateMetric.TOTAL));
        assertNull(buckets.get(1)
                .get(HealthDataType.STEPS, AggregateMetric.TOTAL),
                "a day with no data must not report zero steps");
        assertTrue(buckets.get(1).isEmpty());
        assertEquals(0, buckets.get(1).getSampleCount(HealthDataType.STEPS));
    }

    /**
     * A calendar day is 23 hours across the spring-forward transition. A
     * fixed 86_400_000 bucket would drift a day's worth of data into the
     * neighbouring bucket, silently, twice a year.
     */
    @Test
    void calendarDayBucketsSurviveTheSpringForwardTransition() {
        // 2026-03-08 is the US spring-forward date; that local day is 23h.
        Calendar c = Calendar.getInstance(LA);
        c.clear();
        c.set(2026, Calendar.MARCH, 8, 0, 0, 0);
        long dayStart = c.getTimeInMillis();
        c.add(Calendar.DAY_OF_MONTH, 1);
        long nextDayStart = c.getTimeInMillis();

        assertEquals(23 * 3600000L, nextDayStart - dayStart,
                "2026-03-08 in Los Angeles is a 23-hour day");

        HealthInterval day = HealthInterval.calendarDays(1, LA);
        assertEquals(nextDayStart, day.nextBoundary(dayStart),
                "the bucket boundary must follow the calendar, not a fixed"
                        + " 24 hours");

        // A sample late on the short day must land in that day's bucket.
        writeSteps(nextDayStart - 3600000L, nextDayStart - 1800000L, 700);
        List<AggregateResult> buckets = store.aggregate(new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.between(dayStart, nextDayStart))
                .setBucket(day)).get();
        assertEquals(1, buckets.size());
        assertEquals(700, buckets.get(0)
                .get(HealthDataType.STEPS, AggregateMetric.TOTAL)
                .getValue(HealthUnit.COUNT), 1e-9);
    }

    /**
     * The average is weighted by duration, so a heart rate sustained for
     * ten minutes outweighs a single spot reading. An unweighted mean over
     * irregularly sampled data is how a chart ends up disagreeing with the
     * platform's own summary.
     */
    @Test
    void discreteAverageIsDurationWeighted() {
        long base = utc(2026, 1, 1, 9);
        // 60 bpm held for 10 minutes, then 120 bpm for 1 minute.
        store.write(QuantitySample.create(HealthDataType.HEART_RATE,
                new HealthQuantity(60, HealthUnit.COUNT_PER_MINUTE),
                base, base + 600000L)).get();
        store.write(QuantitySample.create(HealthDataType.HEART_RATE,
                new HealthQuantity(120, HealthUnit.COUNT_PER_MINUTE),
                base + 600000L, base + 660000L)).get();

        List<AggregateResult> buckets = store.aggregate(new AggregateQuery()
                .addType(HealthDataType.HEART_RATE)
                .addMetric(AggregateMetric.AVERAGE)
                .addMetric(AggregateMetric.MINIMUM)
                .addMetric(AggregateMetric.MAXIMUM)
                .setTimeRange(HealthTimeRange.between(utc(2026, 1, 1, 0),
                        utc(2026, 1, 2, 0)))).get();

        assertEquals(1, buckets.size());
        AggregateResult b = buckets.get(0);
        // Unweighted this would be 90; duration-weighted it is ~65.5.
        assertEquals(65.45,
                b.get(HealthDataType.HEART_RATE, AggregateMetric.AVERAGE)
                        .getValue(HealthUnit.COUNT_PER_MINUTE), 0.1);
        assertEquals(60,
                b.get(HealthDataType.HEART_RATE, AggregateMetric.MINIMUM)
                        .getValue(HealthUnit.COUNT_PER_MINUTE), 1e-9);
        assertEquals(120,
                b.get(HealthDataType.HEART_RATE, AggregateMetric.MAXIMUM)
                        .getValue(HealthUnit.COUNT_PER_MINUTE), 1e-9);
    }

    @Test
    void meaninglessMetricForATypeIsRejected() {
        Throwable err = errorOf(store.aggregate(new AggregateQuery()
                .addType(HealthDataType.BODY_MASS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.lastDays(1))));
        assertNotNull(err, "totalling every weight ever recorded is not a"
                + " number anyone should be given");
        assertEquals(HealthError.INVALID_ARGUMENT,
                ((HealthException) err).getError());
    }

    @Test
    void limitTruncatesAndSaysSo() {
        for (int i = 0; i < 5; i++) {
            writeSteps(utc(2026, 1, 1, 9) + i * 60000L,
                    utc(2026, 1, 1, 9) + i * 60000L + 30000L, 10);
        }
        SamplePage page = store.readSamplePage(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setLimit(3)
                .setTimeRange(HealthTimeRange.between(utc(2026, 1, 1, 0),
                        utc(2026, 1, 2, 0)))).get();
        assertEquals(3, page.size());
        assertTrue(page.isTruncated());
    }

    @Test
    void descendingSortReturnsNewestFirst() {
        writeSteps(utc(2026, 1, 1, 9), utc(2026, 1, 1, 10), 100);
        writeSteps(utc(2026, 1, 1, 11), utc(2026, 1, 1, 12), 200);
        List<HealthSample> read = store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setSortDescending(true)
                .setTimeRange(HealthTimeRange.between(utc(2026, 1, 1, 0),
                        utc(2026, 1, 2, 0)))).get();
        assertEquals(200,
                ((QuantitySample) read.get(0)).getValue(HealthUnit.COUNT),
                1e-9);
    }

    /**
     * Series samples are flattened by default so both platforms hand back
     * the same thing; turning it off preserves record identity.
     */
    @Test
    void seriesAreFlattenedByDefault() {
        long base = utc(2026, 1, 1, 9);
        SeriesSample series = SeriesSample.create(HealthDataType.HEART_RATE,
                base, base + 2000,
                new long[] { base, base + 1000, base + 2000 },
                new long[] { base, base + 1000, base + 2000 },
                new double[] { 60, 62, 64 }, HealthUnit.COUNT_PER_MINUTE);
        store.write(series).get();

        SampleQuery q = new SampleQuery().addType(HealthDataType.HEART_RATE)
                .setTimeRange(HealthTimeRange.between(utc(2026, 1, 1, 0),
                        utc(2026, 1, 2, 0)));
        List<HealthSample> flattened = store.readSamples(q).get();
        assertEquals(3, flattened.size());
        for (HealthSample s : flattened) {
            assertTrue(s instanceof QuantitySample);
        }

        List<HealthSample> grouped =
                store.readSamples(q.setFlattenSeries(false)).get();
        assertEquals(1, grouped.size());
        assertTrue(grouped.get(0) instanceof SeriesSample);
        assertEquals(3, ((SeriesSample) grouped.get(0)).size());
    }

    @Test
    void sourceFilterExcludesOtherApps() {
        QuantitySample mine = QuantitySample.create(HealthDataType.BODY_MASS,
                new HealthQuantity(70, HealthUnit.KILOGRAM),
                utc(2026, 1, 1, 8));
        mine.setSource(new HealthSource("com.example.mine", "Mine", null,
                null, null));
        QuantitySample theirs = QuantitySample.create(
                HealthDataType.BODY_MASS,
                new HealthQuantity(71, HealthUnit.KILOGRAM),
                utc(2026, 1, 1, 9));
        theirs.setSource(new HealthSource("com.example.theirs", "Theirs",
                null, null, null));
        List<HealthSample> both = new ArrayList<HealthSample>();
        both.add(mine);
        both.add(theirs);
        store.write(both).get();

        List<HealthSample> filtered = store.readSamples(new SampleQuery()
                .addType(HealthDataType.BODY_MASS)
                .addSource("com.example.mine")
                .setTimeRange(HealthTimeRange.between(utc(2026, 1, 1, 0),
                        utc(2026, 1, 2, 0)))).get();
        assertEquals(1, filtered.size());
        assertEquals("com.example.mine",
                filtered.get(0).getSource().getBundleId());
    }

    /**
     * An {@code except} callback on an already-settled resource fires
     * synchronously, so the error can be read without waiting.
     */
    private static Throwable errorOf(com.codename1.util.AsyncResource<?> r) {
        final Throwable[] err = new Throwable[1];
        r.except(new com.codename1.util.SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err[0] = t;
            }
        });
        return err[0];
    }

    /**
     * Every sample shape is copied on the way in and on the way out.
     *
     * <p>Only {@link QuantitySample} used to be, so writing a workout, a
     * sleep session or a nutrition entry left the store holding the
     * caller's object -- editing it afterwards silently rewrote what was
     * stored, and clearing its identifier broke deletion by id. A read
     * handed the same object straight back, so the same edit worked in
     * reverse.</p>
     */
    @Test
    void everySampleShapeIsSnapshotOnWriteAndOnRead() throws Exception {
        LocalHealthStore store = new LocalHealthStore();

        WorkoutSample workout = WorkoutSample.create(
                WorkoutActivityType.RUNNING, 1000L, 5000L);
        workout.setId("workout-1");
        workout.setActiveDurationMillis(4000L);
        // The store assigns the identifier, as a platform would.
        String assigned = store.write(workout).get().getSampleIds().get(0);
        workout.setId("tampered");
        workout.setActiveDurationMillis(0L);

        List<HealthSample> read = store.readSamples(new SampleQuery()
                .addType(HealthDataType.WORKOUT)
                .setTimeRange(HealthTimeRange.between(0L, 10_000L))).get();
        assertEquals(1, read.size());
        WorkoutSample stored = (WorkoutSample) read.get(0);
        assertNotSame(workout, stored, "the store must not alias the caller");
        assertEquals(assigned, stored.getId(),
                "an edit after the write must not reach the store");
        assertEquals(4000L, stored.getActiveDurationMillis(),
                "shape-specific state survives the write");

        // And the object handed back is a copy too.
        stored.setId("tampered-again");
        stored.setActiveDurationMillis(0L);
        WorkoutSample again = (WorkoutSample) store.readSamples(
                new SampleQuery().addType(HealthDataType.WORKOUT)
                        .setTimeRange(HealthTimeRange.between(0L, 10_000L)))
                .get().get(0);
        assertEquals(assigned, again.getId(),
                "editing a read result must not reach the store either");
        assertEquals(4000L, again.getActiveDurationMillis());
    }

    /**
     * A write does not stamp an identifier onto the caller's sample.
     *
     * <p>This was the one store that mutated its input. {@code
     * HealthSample.hashCode()} switches from identity to the id once one
     * is set, so a sample already held in a {@code HashSet} or used as a
     * map key became unreachable the moment it was written. The id reaches
     * the caller through {@link HealthWriteResult}, as on every other
     * platform.</p>
     */
    @Test
    void writingDoesNotStampAnIdOntoTheCallersSample() throws Exception {
        LocalHealthStore store = new LocalHealthStore();
        QuantitySample sample = QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(100, HealthUnit.COUNT), 1000L, 2000L);

        java.util.Set<HealthSample> held =
                new java.util.HashSet<HealthSample>();
        held.add(sample);

        String id = store.write(sample).get().getSampleIds().get(0);
        assertNull(sample.getId(),
                "the caller's sample must come back untouched");
        assertTrue(held.contains(sample),
                "a sample used as a key must stay reachable after a write");

        // And the stored record does carry the identifier the result named.
        List<HealthSample> read = store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(0L, 5000L))).get();
        assertEquals(id, read.get(0).getId());
    }

    /**
     * An id-based delete honours the type it was given.
     *
     * <p>Health Connect deletes against a record class, so a stale or
     * mispaired type and id removes nothing there. Matching on the
     * identifier alone made the local store more destructive than the
     * platform it stands in for.</p>
     */
    @Test
    void deletingByIdRespectsTheRequestedType() throws Exception {
        LocalHealthStore store = new LocalHealthStore();
        QuantitySample steps = QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(100, HealthUnit.COUNT), 1000L, 2000L);
        String id = store.write(steps).get().getSampleIds().get(0);

        // The right id, the wrong type: nothing is removed.
        assertEquals(0, store.delete(HealthDeleteRequest.byId(
                HealthDataType.BODY_MASS, id)).get().intValue());
        assertEquals(1, store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(0L, 5000L)))
                .get().size());

        // The pair the write actually produced does remove it.
        assertEquals(1, store.delete(HealthDeleteRequest.byId(
                HealthDataType.STEPS, id)).get().intValue());
        assertEquals(0, store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(0L, 5000L)))
                .get().size());
    }
}
