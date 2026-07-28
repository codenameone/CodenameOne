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
import org.junit.jupiter.api.AfterEach;
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

    /// Subscriptions persist into Preferences, which is process-global, so
    /// a test that leaves one behind is seen by every later test -- the
    /// fallback store restores it and stops reporting an empty registry.
    private final List<HealthStore> touched = new ArrayList<HealthStore>();

    @AfterEach
    void forgetSubscriptions() {
        for (HealthStore store : touched) {
            for (HealthSubscription sub : store.getSubscriptions()) {
                store.unsubscribe(sub.getId());
            }
        }
        touched.clear();
    }

    private FakeHealthStore newStore() {
        FakeHealthStore store = new FakeHealthStore();
        touched.add(store);
        return store;
    }

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
    private static class Collector implements HealthChangeListener {
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
        FakeHealthStore store = newStore();
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
        FakeHealthStore store = newStore();
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
        FakeHealthStore store = newStore();
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
        FakeHealthStore store = newStore();
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

    /**
     * A listener that throws on an early chunk stops the whole page.
     *
     * <p>Queuing every chunk up front meant the final one still ran and
     * persisted the page anchor, so the chunk the app failed on was
     * skipped for good. The cursor must not move past data the listener
     * could not handle.</p>
     */
    @Test
    void aThrowingListenerStopsTheRestOfThePage() {
        FakeHealthStore store = newStore();
        final CountDownLatch first = new CountDownLatch(1);
        final int[] calls = new int[1];
        HealthChangeListener thrower = new HealthChangeListener() {
            public void healthDataChanged(HealthChangeBatch batch) {
                calls[0]++;
                first.countDown();
                throw new IllegalStateException("app cannot handle this");
            }
        };
        SubscriptionRequest req = new SubscriptionRequest("throwing")
                .addType(HealthDataType.STEPS)
                .setMaxSamplesPerBatch(2);
        store.subscribe(req, thrower);

        store.batchesToFire.add(new HealthChangeBatch("throwing",
                req.getTypes(), samples(5), null, false,
                HealthAnchor.of("after-page"), 0L, false));
        store.drainChanges();
        waitFor(first, 5000);
        // Then settle, so a wrongly-queued later chunk would have run.
        com.codename1.testing.TestUtils.waitFor(300);

        assertEquals(1, calls[0],
                "delivery stops at the chunk the listener rejected");
        assertNull(store.getSubscriptions().get(0).getAnchor(),
                "and the cursor must not have advanced");
    }

    /**
     * Delivery options survive a restart. A notify-only subscription that
     * starts delivering full payloads after a relaunch is a privacy
     * surprise, not just a performance one.
     */
    @Test
    void deliveryOptionsSurviveRestore() {
        FakeHealthStore first = newStore();
        first.subscribe(new SubscriptionRequest("persisted")
                .addType(HealthDataType.STEPS)
                .setDeliverSamples(false)
                .setIncludeDeletions(false)
                .setMaxSamplesPerBatch(7), new Collector(1));

        // A fresh store restores from the same preferences.
        FakeHealthStore restored = newStore();
        List<HealthSubscription> subs = restored.getSubscriptions();
        HealthSubscription found = null;
        for (HealthSubscription sub : subs) {
            if ("persisted".equals(sub.getId())) {
                found = sub;
            }
        }
        assertNotNull(found, "the subscription must come back at all");

        Collector listener = new Collector(1);
        restored.subscribe(new SubscriptionRequest("persisted")
                .addType(HealthDataType.STEPS)
                .setDeliverSamples(false)
                .setIncludeDeletions(false)
                .setMaxSamplesPerBatch(7), listener);
        restored.batchesToFire.add(new HealthChangeBatch("persisted",
                new java.util.ArrayList<HealthDataType>(), samples(3), null,
                false, HealthAnchor.of("c"), 0L, false));
        restored.drainChanges();
        waitFor(listener.latch, 5000);
        assertEquals(0, listener.seen.size(),
                "restored notify-only must stay notify-only");
    }

    /**
     * Cancelling a subscription mid-page does not gate every later drain.
     *
     * <p>Delivery is queued one chunk at a time and each chunk reports
     * whether it queued the next, so a drain can wait for the page to
     * finish before it resolves. A chunk that finds its subscription
     * cancelled queues nothing -- but it used to report otherwise, so the
     * chunks behind it stayed counted as outstanding for the life of the
     * process, and every later drain waited on deliveries that would never
     * happen.</p>
     *
     * <p>The listener cancels itself, which places the cancellation
     * exactly between two chunks rather than leaving it to a race. A
     * second subscription stays registered, because a drain with nothing
     * registered resolves without consulting the counter at all and would
     * hide the leak.</p>
     */
    @Test
    void cancellingMidPageDoesNotStallLaterDrains() throws Exception {
        final FakeHealthStore store = newStore();
        store.subscribe(new SubscriptionRequest("keeper")
                .addType(HealthDataType.STEPS), new Collector(1));

        SubscriptionRequest req = new SubscriptionRequest("cancelled")
                .addType(HealthDataType.STEPS)
                .setMaxSamplesPerBatch(1);
        final CountDownLatch first = new CountDownLatch(1);
        store.subscribe(req, new HealthChangeListener() {
            public void healthDataChanged(HealthChangeBatch batch) {
                store.unsubscribe("cancelled");
                first.countDown();
            }
        });

        // Five chunks; the second finds the subscription gone and the
        // three behind it are never queued at all.
        store.batchesToFire.add(new HealthChangeBatch("cancelled",
                req.getTypes(), samples(5), null, false,
                HealthAnchor.of("c1"), 0L, false));
        store.drainChanges();
        assertTrue(first.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "the first chunk must be delivered");

        final CountDownLatch drained = new CountDownLatch(1);
        store.drainChanges().onResult(
                new com.codename1.util.AsyncResult<Integer>() {
                    public void onReady(Integer value, Throwable err) {
                        drained.countDown();
                    }
                });
        assertTrue(drained.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "a drain must not wait on chunks that were never queued");
    }
}
