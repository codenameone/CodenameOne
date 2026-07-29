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

    /**
     * A write that fails on its first chunk reports no partial result.
     *
     * <p>{@code getPartialResult()} is documented as null unless an
     * earlier chunk was committed, and it exists so a caller can retry
     * without duplicating what is already stored. An empty-but-present
     * result reads as "some of this went in", so a caller written to
     * avoid duplicates suppressed a retry that was perfectly safe --
     * and every ordinary single-sample write took that branch, because
     * the first chunk is the only chunk.</p>
     */
    @Test
    void aWriteThatFailsOnItsFirstChunkHasNoPartialResult() {
        FakeHealthStore store = new FakeHealthStore();
        store.failWriteChunk = 0;
        List<HealthSample> one = new ArrayList<HealthSample>();
        one.add(FakeHealthStore.sample(HealthDataType.STEPS,
                1000L, 2000L, 5));

        HealthException failed = assertThrows(HealthException.class,
                () -> {
                    try {
                        store.write(one).get();
                    } catch (Throwable t) {
                        throw t.getCause() instanceof HealthException
                                ? (HealthException) t.getCause() : t;
                    }
                });
        assertNull(failed.getPartialResult(),
                "nothing was committed, so there is nothing partial");
    }

    /**
     * A write that fails after a chunk went in reports what did.
     *
     * <p>The other half of the same contract: without this the caller
     * sees only the failure, retries the whole batch, and writes the
     * committed samples a second time.</p>
     */
    @Test
    void aWriteThatFailsLaterReportsTheCommittedChunk() {
        FakeHealthStore store = new FakeHealthStore();
        store.maxWriteBatch = 1;
        store.failWriteChunk = 1;
        List<HealthSample> two = new ArrayList<HealthSample>();
        two.add(FakeHealthStore.sample(HealthDataType.STEPS,
                1000L, 2000L, 5));
        two.add(FakeHealthStore.sample(HealthDataType.STEPS,
                2000L, 3000L, 6));

        HealthException failed = assertThrows(HealthException.class,
                () -> {
                    try {
                        store.write(two).get();
                    } catch (Throwable t) {
                        throw t.getCause() instanceof HealthException
                                ? (HealthException) t.getCause() : t;
                    }
                });
        assertNotNull(failed.getPartialResult(),
                "the first chunk is in the store and must be reported");
        assertEquals(1,
                failed.getPartialResult().getSampleIds().size());
    }

    /**
     * A blood-pressure reading is converted into the unit the query
     * asked for.
     *
     * <p>It is not a {@code QuantitySample} -- it carries two quantities
     * rather than one -- so it fell straight through the normalizer and
     * came back in whatever unit it was stored in. {@code readSamples}
     * documents its results as normalized, and validation accepts a
     * pressure unit on the query because {@code BLOOD_PRESSURE} is a
     * pressure type, so a caller asking for mmHg against a store holding
     * kPa was answered in kPa with nothing to say so.</p>
     */
    @Test
    void aBloodPressureReadingIsNormalizedToTheQueryUnit() throws Exception {
        FakeHealthStore store = new FakeHealthStore();
        BloodPressureSample stored = BloodPressureSample.create(
                new HealthQuantity(16.0, HealthUnit.KILOPASCAL),
                new HealthQuantity(10.6666, HealthUnit.KILOPASCAL),
                1000L);
        stored.setBodyPosition(BloodPressureSample.POSITION_SITTING);
        stored.putMetadata("cuff", "left");
        List<HealthSample> page = new ArrayList<HealthSample>();
        page.add(stored);
        store.pages.add(new SamplePage(page, null, false));

        List<HealthSample> read = store.readSamples(new SampleQuery()
                .addType(HealthDataType.BLOOD_PRESSURE)
                .setUnit(HealthUnit.MILLIMETER_OF_MERCURY)
                .setTimeRange(HealthTimeRange.between(0L, 10000L))).get();

        assertEquals(1, read.size());
        BloodPressureSample bp = (BloodPressureSample) read.get(0);
        assertEquals(120.0,
                bp.getSystolic().getValue(HealthUnit.MILLIMETER_OF_MERCURY),
                0.01, "16 kPa is 120 mmHg");
        assertSame(HealthUnit.MILLIMETER_OF_MERCURY,
                bp.getSystolic().getUnit(),
                "and it must be carried in the unit that was asked for");
        assertSame(HealthUnit.MILLIMETER_OF_MERCURY,
                bp.getDiastolic().getUnit());
        assertEquals(BloodPressureSample.POSITION_SITTING,
                bp.getBodyPosition(),
                "converting must not drop the rest of the reading");
        assertEquals("left", bp.getMetadata().get("cuff"));
    }

    /**
     * A platform answer that arrives after the timeout is ignored.
     *
     * <p>{@code AsyncResource} allows a resource to be completed more
     * than once, and every operation here is armed with a timeout -- so a
     * write reported TIMEOUT and then reported success on the same
     * resource. A caller that retried on the timeout had both inserts
     * commit, which is a duplicate record in someone's health data.</p>
     */
    @Test
    void aPlatformAnswerAfterTheTimeoutIsIgnored() throws Exception {
        FakeHealthStore store = new FakeHealthStore();
        store.holdWrite = true;
        store.setOperationTimeoutForTest(120);
        List<HealthSample> one = new ArrayList<HealthSample>();
        one.add(FakeHealthStore.sample(HealthDataType.STEPS,
                1000L, 2000L, 5));

        final Throwable[] first = new Throwable[1];
        final int[] outcomes = new int[1];
        final CountDownLatch timedOut = new CountDownLatch(1);
        store.write(one).onResult(new AsyncResult<HealthWriteResult>() {
            public void onReady(HealthWriteResult value, Throwable err) {
                outcomes[0]++;
                if (first[0] == null) {
                    first[0] = err;
                }
                timedOut.countDown();
            }
        });
        assertTrue(timedOut.await(5, TimeUnit.SECONDS),
                "the timeout must fire");
        assertNotNull(first[0], "and it must arrive as an error");

        // The platform answers late, as a slow bridge does.
        assertNotNull(store.lateWrite);
        store.lateWrite.complete(new HealthWriteResult());
        flushSerialCalls();
        assertEquals(1, outcomes[0],
                "a late answer must not report a second outcome");
    }

    /**
     * Mutating the query after the call does not change the answer.
     *
     * <p>{@code SampleQuery} is a mutable builder and a caller is
     * entitled to reuse it as soon as the call returns -- but the answer
     * arrives later and used to be post-processed against the object as
     * it stood by then, so a unit or source filter changed in between
     * converted, filtered and sorted the page by a query nobody had
     * submitted. A changed source list made it silently empty.</p>
     */
    @Test
    void mutatingTheQueryAfterTheCallDoesNotChangeTheAnswer()
            throws Exception {
        FakeHealthStore store = new FakeHealthStore();
        store.holdRead = true;
        List<HealthSample> one = new ArrayList<HealthSample>();
        one.add(FakeHealthStore.sample(HealthDataType.STEPS,
                1000L, 2000L, 5));

        SampleQuery q = new SampleQuery()
                .addType(HealthDataType.STEPS)
                .setTimeRange(HealthTimeRange.between(0L, 10000L));
        AsyncResource<SamplePage> page = store.readSamplePage(q);

        // The caller reuses its builder for the next request while this
        // one is still in flight.
        q.addSource("com.example.other");
        q.setUnit(HealthUnit.KILOMETER);

        assertNotNull(store.heldRead);
        store.heldRead.complete(new SamplePage(one, null, false));
        SamplePage got = page.get();

        assertEquals(1, got.getSamples().size(),
                "the source filter added after the call must not drop the"
                        + " sample this read had already asked for");
    }
}
