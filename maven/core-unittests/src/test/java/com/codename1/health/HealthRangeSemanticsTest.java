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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Range membership, which must mean the same thing everywhere.
 *
 * <p>Ranges are half-open: `[start,end)`. That single sentence has been
 * violated independently in the local store, the aggregate filter, the iOS
 * predicate and the single-instant factory, each time producing data that
 * silently moved between days or got counted twice. These tests pin the
 * rule at the boundary, which is the only place it is ever wrong.</p>
 */
class HealthRangeSemanticsTest {

    private static final long NOON = 1767268800000L;
    private static final long HOUR = 3600000L;

    private static LocalHealthStore store(HealthSample... samples)
            throws Exception {
        LocalHealthStore s = new LocalHealthStore();
        List<HealthSample> in = new java.util.ArrayList<HealthSample>();
        for (HealthSample sample : samples) {
            in.add(sample);
        }
        s.write(in).get();
        return s;
    }

    private static List<HealthSample> read(LocalHealthStore s,
            HealthDataType type, long from, long to) throws Exception {
        return s.readSamples(new SampleQuery().addType(type)
                .setTimeRange(HealthTimeRange.between(from, to))).get();
    }

    /**
     * An interval ending exactly on the start has zero overlap and belongs
     * to the previous range. Steps ending at midnight are yesterday's.
     */
    @Test
    void intervalEndingAtTheStartIsExcluded() throws Exception {
        LocalHealthStore s = store(FakeHealthStore.sample(
                HealthDataType.STEPS, NOON - HOUR, NOON, 100));
        assertEquals(0, read(s, HealthDataType.STEPS, NOON,
                NOON + HOUR).size());
    }

    /** The same interval is inside the range it actually ends within. */
    @Test
    void intervalEndingAtTheStartBelongsToThePreviousRange()
            throws Exception {
        LocalHealthStore s = store(FakeHealthStore.sample(
                HealthDataType.STEPS, NOON - HOUR, NOON, 100));
        assertEquals(1, read(s, HealthDataType.STEPS, NOON - HOUR,
                NOON).size());
    }

    /** An interval straddling the start overlaps and is included. */
    @Test
    void intervalStraddlingTheStartIsIncluded() throws Exception {
        LocalHealthStore s = store(FakeHealthStore.sample(
                HealthDataType.STEPS, NOON - 300000L, NOON + 300000L, 100));
        assertEquals(1, read(s, HealthDataType.STEPS, NOON,
                NOON + HOUR).size());
    }

    /** An instant exactly at the inclusive start is inside. */
    @Test
    void instantAtTheStartIsIncluded() throws Exception {
        LocalHealthStore s = store(FakeHealthStore.sample(
                HealthDataType.BODY_MASS, NOON, NOON, 70));
        assertEquals(1, read(s, HealthDataType.BODY_MASS, NOON,
                NOON + HOUR).size());
    }

    /** An instant at the exclusive end is outside. */
    @Test
    void instantAtTheEndIsExcluded() throws Exception {
        LocalHealthStore s = store(FakeHealthStore.sample(
                HealthDataType.BODY_MASS, NOON + HOUR, NOON + HOUR, 70));
        assertEquals(0, read(s, HealthDataType.BODY_MASS, NOON,
                NOON + HOUR).size());
    }

    /**
     * `at(t)` is documented for querying a single moment, so it has to
     * contain that moment. Built as `[t,t)` it contained nothing at all and
     * every such query came back empty.
     */
    @Test
    void singleInstantRangeContainsItsInstant() throws Exception {
        HealthTimeRange r = HealthTimeRange.at(NOON).resolve(NOON);
        assertTrue(r.getEndMillis() > r.getStartMillis(),
                "a zero-width half-open range can never contain anything");

        LocalHealthStore s = store(FakeHealthStore.sample(
                HealthDataType.BODY_MASS, NOON, NOON, 70));
        assertEquals(1, s.readSamples(new SampleQuery()
                .addType(HealthDataType.BODY_MASS)
                .setTimeRange(HealthTimeRange.at(NOON))).get().size());
    }

    /**
     * Adjacent ranges partition the timeline: every sample lands in exactly
     * one of them. This is the property all the boundary bugs broke.
     */
    @Test
    void adjacentRangesNeitherDropNorDuplicate() throws Exception {
        LocalHealthStore s = store(
                FakeHealthStore.sample(HealthDataType.STEPS,
                        NOON - HOUR, NOON, 10),
                FakeHealthStore.sample(HealthDataType.STEPS,
                        NOON, NOON + HOUR, 20),
                FakeHealthStore.sample(HealthDataType.BODY_MASS,
                        NOON, NOON, 70));
        int first = read(s, HealthDataType.STEPS, NOON - HOUR, NOON).size();
        int second = read(s, HealthDataType.STEPS, NOON, NOON + HOUR).size();
        assertEquals(2, first + second,
                "each interval belongs to exactly one of two adjacent"
                        + " ranges");
    }
}
