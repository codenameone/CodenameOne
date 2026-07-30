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

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An open-ended range is closed once, when the operation is admitted.
 *
 * <p>{@code HealthTimeRange.since(t)} means "until now", and {@code resolve}
 * reads the clock to say when that is. Carrying the open range into the
 * operation meant every place that needed a concrete end read the clock for
 * itself: an aggregate resolved it three times -- computing bucket boundaries,
 * reading samples in the fallback, and rolling them up -- and a paged read
 * resolved it once per page plus again inside the port. Any two of those
 * landing either side of a bucket boundary, or of an incoming sample, produce
 * an answer built from two different windows.</p>
 *
 * <p>Pinning is asserted rather than the race, which needs the clock to tick
 * mid-operation. Once the range reaching the port is closed, the later
 * {@code resolve} calls return it unchanged and there is nothing left to
 * disagree about.</p>
 */
class HealthOpenRangePinningTest extends UITestBase {

    @Test
    void anAggregateIsHandedAClosedRange() {
        FakeHealthStore store = new FakeHealthStore();
        long start = 1_000_000L;

        store.aggregate(new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.since(start)));

        assertEquals(1, store.aggregatesSeen.size());
        HealthTimeRange range = store.aggregatesSeen.get(0).getTimeRange();
        assertFalse(range.isOpenEnded(),
                "the port must not be asked to resolve 'now' for itself");
        assertEquals(start, range.getStartMillis());
        assertTrue(range.getEndMillis() >= start
                        && range.getEndMillis() < Long.MAX_VALUE,
                "and the end must be a real instant, was "
                        + range.getEndMillis());
    }

    /**
     * The boundaries and the range the port receives describe one window.
     *
     * <p>These are computed from the same pinned query, so the last boundary
     * cannot sit beyond the range's end -- which is the shape of the bug: a
     * read covering time no bucket was created for.</p>
     */
    @Test
    void theBucketBoundariesAgreeWithThatRange() {
        FakeHealthStore store = new FakeHealthStore();
        long start = 2_000_000L;

        store.aggregate(new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.since(start)));

        HealthTimeRange range = store.aggregatesSeen.get(0).getTimeRange();
        long[] bounds = store.aggregateBoundsSeen.get(0);
        assertTrue(bounds.length >= 2, "an aggregate needs a bucket");
        assertEquals(range.getStartMillis(), bounds[0]);
        assertEquals(range.getEndMillis(), bounds[bounds.length - 1],
                "the buckets must span exactly the range the port was given");
    }

    /** Every page of a paged read shares the one closed range. */
    @Test
    void everyPageOfAReadSharesOneClosedRange() throws Exception {
        FakeHealthStore store = new FakeHealthStore();
        List<HealthSample> first = new ArrayList<HealthSample>();
        first.add(FakeHealthStore.sample(HealthDataType.STEPS, 10L, 20L, 3));
        store.pages.add(new SamplePage(first, "next", true));
        List<HealthSample> second = new ArrayList<HealthSample>();
        second.add(FakeHealthStore.sample(HealthDataType.STEPS, 30L, 40L, 4));
        store.pages.add(new SamplePage(second, null, false));

        store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setLimit(10)
                .setTimeRange(HealthTimeRange.since(1L))).get();

        assertTrue(store.queriesSeen.size() >= 2,
                "the read must have paged, saw " + store.queriesSeen.size());
        HealthTimeRange page1 = store.queriesSeen.get(0).getTimeRange();
        HealthTimeRange page2 = store.queriesSeen.get(1).getTimeRange();
        assertFalse(page1.isOpenEnded(), "page one carries a closed range");
        assertFalse(page2.isOpenEnded(), "so does page two");
        assertEquals(page1.getEndMillis(), page2.getEndMillis(),
                "and both pages describe the same window");
    }
}
