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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bucket arithmetic.
 *
 * <p>Every one of these assertions corresponds to a way the totals have
 * been wrong: a workout counted whole in two buckets, a spot reading adding
 * a millisecond of "time covered", a calendar bucket reaching outside the
 * query and counting time nobody asked for. The arithmetic is shared by
 * every platform, so getting it wrong is wrong everywhere at once.</p>
 */
class HealthAggregateMathTest {

    private static final long NOON = 1767268800000L;
    private static final long HOUR = 3600000L;
    private static final long MINUTE = 60000L;

    private static LocalHealthStore store(HealthSample... samples)
            throws Exception {
        LocalHealthStore s = new LocalHealthStore();
        List<HealthSample> in = new ArrayList<HealthSample>();
        for (HealthSample sample : samples) {
            in.add(sample);
        }
        s.write(in).get();
        return s;
    }

    private static List<AggregateResult> hourly(LocalHealthStore s,
            HealthDataType type, AggregateMetric metric, long from, long to)
            throws Exception {
        return s.aggregate(new AggregateQuery().addType(type)
                .addMetric(metric)
                .setTimeRange(HealthTimeRange.between(from, to))
                .setBucket(HealthInterval.hours(1))).get();
    }

    private static double value(AggregateResult r, HealthDataType t,
            AggregateMetric m) {
        HealthQuantity q = r.get(t, m);
        return q == null ? -1 : q.getValue(t.getCanonicalUnit());
    }

    /**
     * A two-hour workout across two hourly buckets is one hour in each,
     * not two in both.
     */
    @Test
    void intervalDurationIsClippedToItsBucket() throws Exception {
        LocalHealthStore s = store(FakeHealthStore.sample(
                HealthDataType.EXERCISE_TIME, NOON, NOON + 2 * HOUR, 120));
        List<AggregateResult> b = hourly(s, HealthDataType.EXERCISE_TIME,
                AggregateMetric.DURATION, NOON, NOON + 2 * HOUR);
        assertEquals(2, b.size());
        assertEquals(HOUR, b.get(0).get(HealthDataType.EXERCISE_TIME,
                AggregateMetric.DURATION).getValue(HealthUnit.MILLISECOND),
                1.0);
        assertEquals(HOUR, b.get(1).get(HealthDataType.EXERCISE_TIME,
                AggregateMetric.DURATION).getValue(HealthUnit.MILLISECOND),
                1.0);
    }

    /**
     * A cumulative value spanning two buckets is split in proportion, so
     * the buckets still sum to the original.
     */
    @Test
    void cumulativeValueIsProRatedAcrossBuckets() throws Exception {
        LocalHealthStore s = store(FakeHealthStore.sample(
                HealthDataType.STEPS, NOON, NOON + 2 * HOUR, 100));
        List<AggregateResult> b = hourly(s, HealthDataType.STEPS,
                AggregateMetric.TOTAL, NOON, NOON + 2 * HOUR);
        assertEquals(2, b.size());
        double first = value(b.get(0), HealthDataType.STEPS,
                AggregateMetric.TOTAL);
        double second = value(b.get(1), HealthDataType.STEPS,
                AggregateMetric.TOTAL);
        assertEquals(50.0, first, 0.5);
        assertEquals(50.0, second, 0.5);
        assertEquals(100.0, first + second, 0.5,
                "pro-rating must conserve the total");
    }

    /**
     * An interval ending exactly on a bucket boundary belongs to the
     * earlier bucket only. Counting it in both is how boundary totals
     * inflated.
     */
    @Test
    void intervalEndingOnABoundaryCountsOnce() throws Exception {
        LocalHealthStore s = store(FakeHealthStore.sample(
                HealthDataType.STEPS, NOON, NOON + HOUR, 60));
        List<AggregateResult> b = hourly(s, HealthDataType.STEPS,
                AggregateMetric.TOTAL, NOON, NOON + 2 * HOUR);
        assertEquals(60.0, value(b.get(0), HealthDataType.STEPS,
                AggregateMetric.TOTAL), 0.5);
        assertNull(b.get(1).get(HealthDataType.STEPS,
                AggregateMetric.TOTAL),
                "the second bucket saw none of it");
    }

    /**
     * Instantaneous samples cover no time. The one-millisecond averaging
     * weight is not a duration, and adding it made "total time covered"
     * grow with the number of spot readings.
     */
    @Test
    void instantaneousSamplesContributeNoDuration() throws Exception {
        LocalHealthStore s = store(
                FakeHealthStore.sample(HealthDataType.BODY_MASS,
                        NOON + MINUTE, NOON + MINUTE, 70),
                FakeHealthStore.sample(HealthDataType.BODY_MASS,
                        NOON + 2 * MINUTE, NOON + 2 * MINUTE, 71),
                FakeHealthStore.sample(HealthDataType.BODY_MASS,
                        NOON + 3 * MINUTE, NOON + 3 * MINUTE, 72));
        List<AggregateResult> b = hourly(s, HealthDataType.BODY_MASS,
                AggregateMetric.DURATION, NOON, NOON + HOUR);
        assertEquals(0.0, b.get(0).get(HealthDataType.BODY_MASS,
                AggregateMetric.DURATION).getValue(HealthUnit.MILLISECOND),
                0.001);
    }

    /** Spot readings still average, weighted equally. */
    @Test
    void instantaneousSamplesStillAverage() throws Exception {
        LocalHealthStore s = store(
                FakeHealthStore.sample(HealthDataType.BODY_MASS,
                        NOON + MINUTE, NOON + MINUTE, 70),
                FakeHealthStore.sample(HealthDataType.BODY_MASS,
                        NOON + 2 * MINUTE, NOON + 2 * MINUTE, 72));
        List<AggregateResult> b = hourly(s, HealthDataType.BODY_MASS,
                AggregateMetric.AVERAGE, NOON, NOON + HOUR);
        assertEquals(71.0, value(b.get(0), HealthDataType.BODY_MASS,
                AggregateMetric.AVERAGE), 0.001);
    }

    /**
     * A bucket wider than the query must not count time outside the query.
     * A daily bucket on a one-hour query is the case that caught this.
     */
    @Test
    void overlapIsClippedToTheQueryRangeNotJustTheBucket()
            throws Exception {
        LocalHealthStore s = store(FakeHealthStore.sample(
                HealthDataType.STEPS, NOON - 5 * MINUTE, NOON + 5 * MINUTE,
                10));
        // One bucket covering the whole query, but the query starts at noon
        // while the sample starts five minutes earlier.
        List<AggregateResult> b = s.aggregate(new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.between(NOON,
                        NOON + 10 * MINUTE))).get();
        assertEquals(1, b.size());
        assertEquals(5.0, value(b.get(0), HealthDataType.STEPS,
                AggregateMetric.TOTAL), 0.5,
                "only the five minutes inside the query count");
    }

    /** A bucket with no data stays empty rather than reporting zero. */
    @Test
    void emptyBucketsAreNullNotZero() throws Exception {
        LocalHealthStore s = store(FakeHealthStore.sample(
                HealthDataType.STEPS, NOON, NOON + MINUTE, 10));
        List<AggregateResult> b = hourly(s, HealthDataType.STEPS,
                AggregateMetric.TOTAL, NOON, NOON + 2 * HOUR);
        assertNotNull(b.get(0).get(HealthDataType.STEPS,
                AggregateMetric.TOTAL));
        assertNull(b.get(1).get(HealthDataType.STEPS,
                AggregateMetric.TOTAL),
                "no data and zero data are different facts");
    }
}
