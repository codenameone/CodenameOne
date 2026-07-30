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
import com.codename1.ui.CN;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every caller-facing store result arrives on the EDT, whatever the backend.
 *
 * <p>This used to depend on which store answered. The mobile ports complete on
 * the EDT and the result was handed back there; a local-backed store -- the
 * simulator, the desktop, the JavaScript port -- completed on whichever thread
 * had called, so the result arrived on that thread. The same app code updating
 * a label from a read callback therefore worked on a phone and produced a
 * repaint glitch on the desktop, and it was written down as a known asymmetry
 * rather than fixed.</p>
 *
 * <p>Each operation is started from a worker thread on purpose: with the caller
 * already on the EDT the old code delivered there by accident, so a test that
 * called from the EDT would have passed against the bug.</p>
 */
class HealthEdtDeliveryTest extends UITestBase {

    /** Where one delivery landed. */
    private static final class Landing<T> implements AsyncResult<T> {
        private final AtomicBoolean onEdt = new AtomicBoolean();
        private final AtomicBoolean arrived = new AtomicBoolean();
        private final AtomicBoolean failed = new AtomicBoolean();

        public void onReady(T value, Throwable err) {
            onEdt.set(CN.isEdt());
            failed.set(err != null);
            arrived.set(true);
        }
    }

    /**
     * Runs {@code op} off the EDT and waits for its delivery without blocking
     * the EDT.
     *
     * <p>Worth spelling out, because the obvious harness fails in a way that
     * looks like a product bug. These tests run on the EDT; the delivery being
     * asserted is a {@code callSerially}; so waiting on a latch from the test
     * thread stops the event loop and the runnable carrying the result can
     * never run. {@code invokeAndBlock} is the CN1 answer -- it moves the
     * waiting off the EDT and keeps the loop pumping. My first version of this
     * file used a plain latch and reported the aggregate path as broken when
     * it was not.</p>
     */
    private <T> void assertDeliveredOnEdt(final Landing<T> landing,
            final Runnable op) {
        CN.invokeAndBlock(new Runnable() {
            public void run() {
                assertFalse(CN.isEdt(), "the operation must start off the EDT");
                op.run();
                long deadline = System.currentTimeMillis() + 10_000L;
                while (!landing.arrived.get()
                        && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            }
        });
        assertTrue(landing.arrived.get(), "the callback must arrive");
        assertFalse(landing.failed.get(), "and must not be an error");
        assertTrue(landing.onEdt.get(),
                "a result must arrive on the EDT on every backend");
    }

    @Test
    void aReadDeliversOnTheEdt() {
        final FakeHealthStore store = new FakeHealthStore();
        store.holdRead = true;
        final Landing<List<HealthSample>> landing =
                new Landing<List<HealthSample>>();
        assertDeliveredOnEdt(landing, new Runnable() {
            public void run() {
                store.readSamples(new SampleQuery()
                        .addType(HealthDataType.STEPS)
                        .setTimeRange(HealthTimeRange.between(0L, 1000L)))
                        .onResult(landing);
                store.heldRead.complete(new SamplePage(
                        new ArrayList<HealthSample>(), null, false));
            }
        });
    }

    @Test
    void aPagedReadDeliversOnTheEdt() {
        final FakeHealthStore store = new FakeHealthStore();
        store.holdRead = true;
        final Landing<SamplePage> landing = new Landing<SamplePage>();
        assertDeliveredOnEdt(landing, new Runnable() {
            public void run() {
                store.readSamplePage(new SampleQuery()
                        .addType(HealthDataType.STEPS)
                        .setTimeRange(HealthTimeRange.between(0L, 1000L)))
                        .onResult(landing);
                store.heldRead.complete(new SamplePage(
                        new ArrayList<HealthSample>(), null, false));
            }
        });
    }

    @Test
    void aWriteDeliversOnTheEdt() {
        final FakeHealthStore store = new FakeHealthStore();
        store.holdWrite = true;
        final Landing<HealthWriteResult> landing =
                new Landing<HealthWriteResult>();
        final List<HealthSample> samples = new ArrayList<HealthSample>();
        samples.add(QuantitySample.create(HealthDataType.STEPS,
                new HealthQuantity(120, HealthUnit.COUNT), 0L, 60_000L));
        assertDeliveredOnEdt(landing, new Runnable() {
            public void run() {
                store.write(samples).onResult(landing);
                store.lateWrite.complete(new HealthWriteResult());
            }
        });
    }

    /**
     * A port that answers off the EDT still reaches the caller on it.
     *
     * <p>Held open deliberately. The other tests here start an operation and
     * attach the listener afterwards, which is fine while the store is slower
     * than the attach -- but it is a race, and it is the race that made this
     * very test fail on CI and pass locally: when the EDT delivered first, the
     * resource was already settled and {@code onResult} then fired inline on
     * the attaching thread, recording "not the EDT" for a delivery that had in
     * fact happened there. Holding the port's completion until the listener is
     * attached removes the ordering question entirely.</p>
     */
    @Test
    void anAggregateDeliversOnTheEdt() {
        final FakeHealthStore store = new FakeHealthStore();
        store.holdAggregate = true;
        final Landing<List<AggregateResult>> landing =
                new Landing<List<AggregateResult>>();
        assertDeliveredOnEdt(landing, new Runnable() {
            public void run() {
                store.aggregate(new AggregateQuery()
                        .addType(HealthDataType.STEPS)
                        .addMetric(AggregateMetric.TOTAL)
                        .setTimeRange(HealthTimeRange.between(0L, 1000L)))
                        .onResult(landing);
                // Attached; now let the port answer, from this worker thread.
                store.heldAggregate.complete(
                        new ArrayList<AggregateResult>());
            }
        });
    }

    @Test
    void aDeleteDeliversOnTheEdt() {
        final FakeHealthStore store = new FakeHealthStore();
        store.holdDelete = true;
        final Landing<Integer> landing = new Landing<Integer>();
        assertDeliveredOnEdt(landing, new Runnable() {
            public void run() {
                store.delete(HealthDeleteRequest.byRange(
                        HealthDataType.STEPS,
                        HealthTimeRange.between(0L, 1000L)))
                        .onResult(landing);
                store.heldDelete.complete(Integer.valueOf(0));
            }
        });
    }
}
