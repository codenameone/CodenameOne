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

import java.util.concurrent.CountDownLatch;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The subscription cursor.
 *
 * <p>This is the piece that has broken most often, and always in the same
 * shape: a fix that stops data being skipped starts data being repeated,
 * or the reverse. The invariant underneath both is simple and is what these
 * tests state directly -- <b>every sample is delivered exactly once, and
 * the cursor advances only after the last of them has been handed over.</b>
 * </p>
 */
class HealthSubscriptionCursorTest extends UITestBase {

    private static final long T0 = 1767268800000L;

    private static List<HealthSample> samples(int n) {
        List<HealthSample> out = new ArrayList<HealthSample>();
        for (int i = 0; i < n; i++) {
            out.add(FakeHealthStore.sample(HealthDataType.STEPS,
                    T0 + i * 1000L, T0 + i * 1000L + 500L, i + 1));
        }
        return out;
    }

    /// Collects everything a listener is handed, in order.
    ///
    /// The latch is what the test waits on: delivery hops through
    /// callSerially, so counting down here is the only honest signal that
    /// the batch actually arrived.
    private static final class Collector implements HealthChangeListener {
        final List<HealthSample> seen = new ArrayList<HealthSample>();
        final List<HealthAnchor> anchors = new ArrayList<HealthAnchor>();
        final CountDownLatch latch;
        int batches;

        Collector(int expectedBatches) {
            this.latch = new CountDownLatch(expectedBatches);
        }

        public void healthDataChanged(HealthChangeBatch batch) {
            batches++;
            seen.addAll(batch.getAdded());
            anchors.add(batch.getAnchor());
            latch.countDown();
        }
    }

    /**
     * A batch larger than the cap is delivered in full across several
     * batches, and only the final one carries the anchor.
     *
     * <p>Truncating and keeping the anchor lost the tail for good;
     * truncating and withholding the anchor re-read the same page forever
     * and never reached the tail either. Both passed a naive "does it
     * deliver something" check, which is why this asserts on the whole
     * sequence.</p>
     */
    @Test
    void aCappedBatchDeliversEverySampleAndAdvancesOnce() {
        FakeHealthStore store = new FakeHealthStore();
        Collector listener = new Collector(3);
        SubscriptionRequest req = new SubscriptionRequest("cap-test")
                .addType(HealthDataType.STEPS)
                .setMaxSamplesPerBatch(2);
        store.subscribe(req, listener);

        List<HealthSample> all = samples(5);
        store.batchesToFire.add(new HealthChangeBatch("cap-test",
                req.getTypes(), all, null, false,
                HealthAnchor.of("cursor-after-page"), 0L, false));
        store.drainChanges();
        waitFor(listener.latch, 5000);

        assertEquals(5, listener.seen.size(),
                "every sample in the page must reach the listener");
        assertEquals(3, listener.batches,
                "5 samples at a cap of 2 is three batches");

        int withAnchor = 0;
        for (HealthAnchor a : listener.anchors) {
            if (a != null) {
                withAnchor++;
            }
        }
        assertEquals(1, withAnchor,
                "exactly one batch may advance the cursor");
        assertNotNull(listener.anchors.get(listener.anchors.size() - 1),
                "and it must be the last one");
    }

    /** An uncapped batch is delivered as one, carrying its anchor. */
    @Test
    void anUncappedBatchIsDeliveredWhole() {
        FakeHealthStore store = new FakeHealthStore();
        Collector listener = new Collector(1);
        SubscriptionRequest req = new SubscriptionRequest("plain")
                .addType(HealthDataType.STEPS);
        store.subscribe(req, listener);

        store.batchesToFire.add(new HealthChangeBatch("plain",
                req.getTypes(), samples(4), null, false,
                HealthAnchor.of("c1"), 0L, false));
        store.drainChanges();
        waitFor(listener.latch, 5000);

        assertEquals(1, listener.batches);
        assertEquals(4, listener.seen.size());
        assertNotNull(listener.anchors.get(0));
    }

    /**
     * A notify-only subscription gets the notification without the
     * payload, and still advances.
     */
    @Test
    void notifyOnlySubscriptionsDropSamplesButKeepTheCursor() {
        FakeHealthStore store = new FakeHealthStore();
        Collector listener = new Collector(1);
        SubscriptionRequest req = new SubscriptionRequest("quiet")
                .addType(HealthDataType.STEPS)
                .setDeliverSamples(false);
        store.subscribe(req, listener);

        store.batchesToFire.add(new HealthChangeBatch("quiet",
                req.getTypes(), samples(3), null, false,
                HealthAnchor.of("c1"), 0L, false));
        store.drainChanges();
        waitFor(listener.latch, 5000);

        assertEquals(1, listener.batches);
        assertEquals(0, listener.seen.size());
        assertNotNull(listener.anchors.get(0),
                "withholding the payload must not stall the cursor");
    }

    /**
     * A subscription for a type this store cannot service is refused
     * rather than registered and left to fail on every drain.
     */
    @Test
    void unsupportedStoreRefusesSubscriptions() {
        FakeHealthStore store = new FakeHealthStore();
        store.supported = false;
        assertThrows(IllegalStateException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        store.subscribe(new SubscriptionRequest("nope")
                                .addType(HealthDataType.STEPS),
                                new Collector(1));
                    }
                });
    }
}
