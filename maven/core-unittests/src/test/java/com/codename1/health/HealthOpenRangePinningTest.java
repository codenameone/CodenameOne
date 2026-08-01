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
import com.codename1.util.AsyncResource;
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

    /**
     * A read never returns more than its limit, even when the port
     * overshoots and still has a token.
     *
     * <p>A port may budget per type rather than per query -- the Health
     * Connect bridge does, because a top-N has to be correct within each type
     * before the types are merged -- so a limit of two over two types comes
     * back as four with a continuation token attached. The accumulation used
     * to trim only when the token was null, so that reply was handed to the
     * caller whole: the documented query-wide limit broken by a factor of the
     * number of types asked for.</p>
     *
     * <p>Trimming is safe here specifically because {@code readSamples}
     * returns a {@code List} and never hands the token back. Nothing can
     * resume, so nothing is stranded by the cut. {@code readSamplePage} is
     * left alone for the opposite reason.</p>
     */
    @Test
    void aReadIsTrimmedToItsLimitEvenWithATokenOutstanding() throws Exception {
        FakeHealthStore store = new FakeHealthStore();
        List<HealthSample> over = new ArrayList<HealthSample>();
        for (int i = 0; i < 4; i++) {
            over.add(FakeHealthStore.sample(HealthDataType.STEPS,
                    i * 10L, i * 10L + 5L, i));
        }
        // Oversized and still carrying a continuation, which is exactly the
        // shape a per-type budget produces.
        store.pages.add(new SamplePage(over, "more", true));

        List<HealthSample> read = store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .addType(HealthDataType.BODY_MASS)
                .setTimeRange(HealthTimeRange.between(0L, 10_000L))
                .setLimit(2)).get();

        assertEquals(2, read.size(),
                "the query-wide limit has to hold whatever the port sent");
    }

    /**
     * Asking for de-duplication where it does not exist fails loudly.
     *
     * <p>The alternative is the quiet wrongness this API is written to avoid:
     * ordinary-looking totals that are wrong by however many devices recorded
     * the same activity. A caller who asked for overlapping sources to be
     * counted once, and silently got them counted twice, has no way to tell.
     * The probe exists so the refusal is avoidable.</p>
     */
    @Test
    void askingForDeduplicationWhereItIsUnsupportedFails() throws Exception {
        FakeHealthStore store = new FakeHealthStore();
        assertFalse(store.isSourceDeduplicationSupported(),
                "the shared rollup cannot de-duplicate");

        AsyncResource<List<AggregateResult>> res = store.aggregate(
                new AggregateQuery()
                        .addType(HealthDataType.STEPS)
                        .addMetric(AggregateMetric.TOTAL)
                        .setTimeRange(HealthTimeRange.between(0L, 1000L))
                        .setDeduplicateSources(true));
        HealthAwait.settled(res);

        assertEquals(0, store.aggregatesSeen.size(),
                "the port must not be asked to do what it cannot");
        Throwable err = null;
        try {
            res.get();
        } catch (Throwable t) {
            err = t;
        }
        assertNotNull(err, "the aggregate must fail rather than answer");
    }

    /** And the plain aggregate is unaffected. */
    @Test
    void anAggregateWithoutDeduplicationStillRuns() {
        FakeHealthStore store = new FakeHealthStore();
        store.aggregate(new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.between(0L, 1000L)));
        assertEquals(1, store.aggregatesSeen.size());
        assertFalse(store.aggregatesSeen.get(0).isDeduplicateSources());
    }

    /** A store that can de-duplicate is asked to, and the flag survives. */
    @Test
    void aCapableStoreReceivesTheDeduplicationRequest() {
        FakeHealthStore store = new FakeHealthStore() {
            @Override
            public boolean isSourceDeduplicationSupported() {
                return true;
            }
        };
        store.aggregate(new AggregateQuery()
                .addType(HealthDataType.STEPS)
                .addMetric(AggregateMetric.TOTAL)
                .setTimeRange(HealthTimeRange.between(0L, 1000L))
                .setDeduplicateSources(true));

        assertEquals(1, store.aggregatesSeen.size());
        assertTrue(store.aggregatesSeen.get(0).isDeduplicateSources(),
                "the snapshot must carry the request to the port");
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
