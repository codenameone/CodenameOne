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

    /**
     * `drainChanges` resolves with the number of batches the listener
     * received, so a capped page counts once per delivery.
     *
     * <p>It used to count once per page handed to `fireChanges`, which
     * meant three callbacks were reported as one -- the one number a
     * caller has to reconcile against what its own listener saw.</p>
     */
    @Test
    void theDrainCountMatchesTheDeliveriesTheListenerSaw() {
        FakeHealthStore store = newStore();
        Collector listener = new Collector(3);
        SubscriptionRequest req = new SubscriptionRequest("cap-count")
                .addType(HealthDataType.STEPS)
                .setMaxSamplesPerBatch(2);
        store.subscribe(req, listener);

        store.batchesToFire.add(new HealthChangeBatch("cap-count",
                req.getTypes(), samples(5), null, false,
                HealthAnchor.of("cursor-after-page"), 0L, false));
        AsyncResource<Integer> drained = store.drainChanges();
        waitFor(listener.latch, 5000);

        assertEquals(3, listener.batches);
        assertEquals(Integer.valueOf(3), drained.get(),
                "the drain must report what the listener actually got");
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

    /**
     * Re-registering an id resumes from the persisted cursor.
     *
     * <p>The saved anchor reached `doSubscribe` alone, which neither
     * mobile store overrides, while both drains read `sub.getAnchor()`.
     * Replacing a listener -- or restoring a subscription at launch --
     * therefore started the next drain from a fresh baseline and silently
     * discarded every change accumulated since the last one.</p>
     */
    @Test
    void replacingASubscriptionKeepsItsCursor() {
        FakeHealthStore store = newStore();
        SubscriptionRequest req = new SubscriptionRequest("resumed")
                .addType(HealthDataType.STEPS);
        Collector first = new Collector(1);
        store.subscribe(req, first);
        store.batchesToFire.add(new HealthChangeBatch("resumed",
                req.getTypes(), samples(1), null, false,
                HealthAnchor.of("cursor-1"), 0L, false));
        store.drainChanges();
        waitFor(first.latch, 5000);

        HealthSubscription replaced = store.subscribe(req, new Collector(1));
        assertNotNull(replaced.getAnchor(),
                "a replacement must not start from a fresh baseline");
        assertEquals("cursor-1", replaced.getAnchor().toStorableString());
    }

    /**
     * Replacing a background subscription with an in-memory one drops the
     * persisted binding.
     *
     * <p>Otherwise the next launch restores the old background listener
     * class and delivers the replacement's changes to exactly the listener
     * the app had replaced.</p>
     */
    @Test
    void anInMemoryListenerClearsAStaleBackgroundBinding() {
        FakeHealthStore store = newStore();
        SubscriptionRequest req = new SubscriptionRequest("rebound")
                .addType(HealthDataType.STEPS);
        store.subscribe(req, TestBackgroundListener.class);
        assertNotNull(com.codename1.io.Preferences.get(
                "cn1$health$listener$rebound", null),
                "the background binding is persisted");

        store.subscribe(req, new Collector(1));
        assertNull(com.codename1.io.Preferences.get(
                "cn1$health$listener$rebound", null),
                "an in-memory listener must not leave the old class bound");
    }

    /** A no-op background listener, only ever named. */
    public static class TestBackgroundListener
            implements HealthBackgroundListener {
        public void healthDataChanged(HealthChangeBatch batch) {
        }
    }

    /**
     * Overlapping drains read the change window once.
     *
     * <p>Both callers used to snapshot the same anchors and enter the
     * port's drain, so the platform read the same window twice, every
     * batch was delivered twice, and two callbacks raced to persist the
     * cursor. The second call now resolves alongside the first rather
     * than starting work of its own.</p>
     */
    @Test
    void overlappingDrainsAreCoalesced() throws Exception {
        FakeHealthStore store = newStore();
        SubscriptionRequest req = new SubscriptionRequest("coalesce")
                .addType(HealthDataType.STEPS);
        Collector listener = new Collector(1);
        store.subscribe(req, listener);
        store.batchesToFire.add(new HealthChangeBatch("coalesce",
                req.getTypes(), samples(2), null, false,
                HealthAnchor.of("c1"), 0L, false));

        // The fake completes its drain synchronously, so the second call
        // has to be made from inside the first to overlap it at all.
        store.beforeDrain = new Runnable() {
            public void run() {
                store.drainChanges();
            }
        };
        store.drainChanges();
        waitFor(listener.latch, 5000);

        assertEquals(1, store.drainCount,
                "the second drain must not reach the port");
        assertEquals(2, listener.seen.size(),
                "and the batch is delivered once, not twice");
    }

    /**
     * A cursor seeded at registration reaches the live handle, not only
     * Preferences.
     *
     * <p>The drains read the handle. Persisting alone looked correct and
     * did nothing: the next drain still found a null anchor, took a fresh
     * cursor, and skipped exactly the window the seed existed to
     * cover.</p>
     */
    @Test
    void aSeededCursorReachesTheLiveSubscription() {
        FakeHealthStore store = newStore();
        Collector listener = new Collector(1);
        // A fresh id every run: the persisted cursor outlives the JVM's
        // Preferences, and subscribe() seeds a new handle from it -- so a
        // reused id made this pass on a cursor left by an earlier run
        // rather than on the one seeded here.
        String id = "seeded-" + System.nanoTime();
        SubscriptionRequest req = new SubscriptionRequest(id)
                .addType(HealthDataType.STEPS);
        HealthSubscription sub = store.subscribe(req, listener);
        store.drainChanges();
        assertEquals(1, store.anchorsSeen.size());
        assertNull(store.anchorsSeen.get(0),
                "a new subscription starts with no cursor");

        assertTrue(store.seedForTest(sub, HealthAnchor.of("baseline-token")));
        store.drainChanges();

        assertEquals(1, store.anchorsSeen.size());
        assertNotNull(store.anchorsSeen.get(0),
                "the drain must see the seeded cursor on the handle");
        assertEquals("baseline-token",
                store.anchorsSeen.get(0).toStorableString());
        store.unsubscribe(id);
    }

    /**
     * A cursor issued for a subscription that has since been cancelled is
     * dropped, not applied.
     *
     * <p>The platform call that produces it starts before the
     * cancellation and can land arbitrarily late. Applying it then would
     * restore a cursor {@code unsubscribe()} promised to discard, and --
     * worse -- hand it to a fresh subscription that happens to reuse the
     * id, which may be watching an entirely different set of types.</p>
     */
    @Test
    void aCursorSeededForACancelledSubscriptionIsDropped() {
        FakeHealthStore store = newStore();
        String id = "stale-" + System.nanoTime();
        SubscriptionRequest req = new SubscriptionRequest(id)
                .addType(HealthDataType.STEPS);
        HealthSubscription first = store.subscribe(req, new Collector(1));
        store.unsubscribe(id);

        assertFalse(store.seedForTest(first, HealthAnchor.of("stale-token")),
                "a seed for a stopped subscription must be refused");
        assertNull(com.codename1.io.Preferences.get(
                "cn1$health$anchor$" + id, null),
                "and it must not reach the persisted cursor either");

        // The same id, registered again while that answer was in flight.
        HealthSubscription second = store.subscribe(
                new SubscriptionRequest(id).addType(HealthDataType.BODY_MASS),
                new Collector(1));
        assertFalse(store.seedForTest(first, HealthAnchor.of("stale-token")),
                "and the replacement must not inherit it");
        assertNull(second.getAnchor());
        store.unsubscribe(id);
    }

    /**
     * A seed never rewinds a cursor that has already moved.
     *
     * <p>A drain that runs while the starting cursor is being issued
     * establishes one of its own. Letting the late answer overwrite it
     * would send the next drain back to an earlier point and re-deliver
     * changes the app has already been told about.</p>
     */
    @Test
    void aSeedDoesNotRewindACursorThatHasAdvanced() {
        FakeHealthStore store = newStore();
        String id = "advanced-" + System.nanoTime();
        HealthSubscription sub;

        Collector waiting = new Collector(1);
        store.subscribe(new SubscriptionRequest(id)
                .addType(HealthDataType.STEPS), waiting);
        sub = store.getSubscriptions().get(0);
        List<HealthSample> one = new ArrayList<HealthSample>();
        one.add(FakeHealthStore.sample(HealthDataType.STEPS, 1000L, 2000L,
                7));
        store.batchesToFire.add(new HealthChangeBatch(id,
                sub.getTypes(), one, null, false,
                HealthAnchor.of("live-token"), 0L, false));
        store.drainChanges();
        waitFor(waiting.latch, 5000);
        assertEquals("live-token", sub.getAnchor().toStorableString());

        assertFalse(store.seedForTest(sub, HealthAnchor.of("baseline-token")),
                "a baseline that lands late must not rewind the cursor");
        assertEquals("live-token", sub.getAnchor().toStorableString());
        store.unsubscribe(id);
    }
}
