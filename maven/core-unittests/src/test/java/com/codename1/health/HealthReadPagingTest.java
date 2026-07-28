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
import com.codename1.util.AsyncResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * How a read pages, and where it does its work.
 *
 * <p>Both are promises this API makes in writing and neither was kept:
 * the documented way to continue a read silently restarted it, and the
 * post-processing the class says runs in the background ran on the
 * EDT.</p>
 */
class HealthReadPagingTest extends UITestBase {

    /**
     * A read resumed from a page token starts where the token points.
     *
     * <p>The documented continuation is to take
     * {@code SamplePage.getNextPageToken()} off a page and hand it back
     * through {@code SampleQuery.setPageToken()}. The paging copy dropped
     * it and paging then seeded itself with null, so the read restarted
     * at the first page -- returning the data the caller already had and
     * never reaching the remainder it asked for.</p>
     */
    @Test
    void aReadResumesFromTheSuppliedPageToken() throws Exception {
        FakeHealthStore store = new FakeHealthStore();
        List<HealthSample> tail = new ArrayList<HealthSample>();
        tail.add(FakeHealthStore.sample(HealthDataType.STEPS,
                4000L, 5000L, 9));
        store.pages.add(new SamplePage(tail, null, false));

        SampleQuery q = new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(0L, 10000L))
                .setPageToken("resume-here");
        store.readSamples(q).get();

        assertEquals(1, store.queriesSeen.size());
        assertEquals("resume-here",
                store.queriesSeen.get(0).getPageToken(),
                "the caller's token must reach the port's first page");
    }

    /**
     * Post-processing does not run on the thread the port completed on.
     *
     * <p>Both mobile ports deliberately complete the raw resource on the
     * EDT, so flattening, unit conversion, source filtering and the sort
     * all ran there -- and this class promises in as many words that they
     * do not. A hundred-thousand-point heart-rate page froze rendering
     * for exactly as long as it took to convert.</p>
     *
     * <p>Observed through where the <em>second</em> page is asked for,
     * which is the one thing here that is not a race: the request for it
     * is made by the code that finishes post-processing the first, so the
     * thread it arrives on is the thread that did that work.</p>
     */
    @Test
    void postProcessingLeavesTheThreadThePortCompletedOn()
            throws Exception {
        FakeHealthStore store = new FakeHealthStore();
        List<HealthSample> first = new ArrayList<HealthSample>();
        first.add(FakeHealthStore.sample(HealthDataType.STEPS,
                1000L, 2000L, 3));
        store.pages.add(new SamplePage(first, "page-2", false));
        List<HealthSample> second = new ArrayList<HealthSample>();
        second.add(FakeHealthStore.sample(HealthDataType.STEPS,
                2000L, 3000L, 4));
        store.pages.add(new SamplePage(second, null, false));

        String caller = Thread.currentThread().getName();
        List<HealthSample> all = store.readSamples(new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(0L, 10000L))).get();

        assertEquals(2, all.size(), "both pages must be collected");
        assertEquals(2, store.readThreads.size());
        assertEquals(caller, store.readThreads.get(0),
                "the first page is asked for by the caller");
        assertEquals("CN1 Health", store.readThreads.get(1),
                "the second is asked for by whatever post-processed the"
                        + " first, and that must not be the caller");
    }
}
