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
package com.codename1.continuity;

import com.codename1.continuity.sync.SyncedStore;
import com.codename1.continuity.sync.SyncedStoreListener;
import com.codename1.impl.continuity.LocalContinuityBridge;
import com.codename1.io.Storage;
import com.codename1.junit.EdtTest;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The framework against the simulated platform every non-Apple port and the simulator use.
 *
 * <p>Everything here is real code: a real {@link Storage}, the real checkpoint, the real
 * dedup and the real inbound dispatch. Only the operating system is simulated, which is exactly
 * the split the {@link LocalContinuityBridge} exists to create.</p>
 */
public class LocalContinuityTest extends UITestBase {

    private LocalContinuityBridge bridge;

    /// Store listeners this test registered. Removed rather than reset wholesale: the framework
    /// deliberately offers no public way to clear them, so a test has to unwind exactly what it
    /// did -- which is also what an application has to do.
    private final List<SyncedStoreListener> registered = new ArrayList<SyncedStoreListener>();

    @BeforeEach
    public void installBridge() {
        Continuity.reset();
        Storage.getInstance().clearStorage();
        // The delivery high-water marks are DURABLE now, so they outlive reset() by design --
        // which is the whole point of them, and which makes them leak from one test into the
        // next unless each starts from a clean slate.
        com.codename1.io.Preferences.delete(Continuity.PREF_SEEN);
        bridge = new LocalContinuityBridge();
        Continuity.setBridge(bridge);
        // A running application has a form on screen, and the framework deliberately holds an
        // arriving state until one exists -- a continuation can cold-launch the app, and both
        // Apple delegates hand it over while init/start are still queued. Without this every
        // inbound test would exercise the cold-launch hold rather than the delivery it means to.
        new Form("continuity").show();
    }

    @AfterEach
    public void clearFramework() {
        for (int i = 0; i < registered.size(); i++) {
            SyncedStore.removeChangeListener(registered.get(i));
        }
        registered.clear();
        Continuity.reset();
        Storage.getInstance().clearStorage();
    }

    // ------------------------------------------------------------------
    // Nothing happens until the application opts in
    // ------------------------------------------------------------------

    /**
     * The single most important property of this feature: an app that never touches it behaves
     * exactly as it always did.
     */
    @EdtTest
    public void nothingIsSavedUntilTheApplicationEnablesTheFramework() {
        assertFalse(Continuity.isEnabled());

        Continuity.routeStackChanged();
        Continuity.checkpoint();
        flushSerialCalls();

        assertFalse(Storage.getInstance().exists(Continuity.STORAGE_KEY));
        assertNull(Continuity.getRestorableState());
        assertFalse(Continuity.restore());
    }

    @EdtTest
    public void settingAStateProviderEnablesTheFramework() {
        Continuity.setStateProvider(new RecordingProvider());

        assertTrue(Continuity.isEnabled());
    }

    // ------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------

    @EdtTest
    public void aCheckpointWritesThePayloadAndCanBeReadBack() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("draft", "half a sentence");
        Continuity.setStateProvider(provider);

        Continuity.checkpoint();

        AppState stored = Continuity.getRestorableState();
        assertNotNull(stored);
        assertEquals("half a sentence", stored.getPayload().get("draft"));
        assertEquals(Continuity.getDeviceId(), stored.getDeviceId());
        assertTrue(stored.getTimestamp() > 0);
    }

    /**
     * The coalescing rule: a burst of navigations costs one write, not one per navigation.
     */
    @EdtTest
    public void aBurstOfRouteChangesCollapsesIntoOneCheckpoint() {
        CountingProvider provider = new CountingProvider();
        Continuity.setStateProvider(provider);

        Continuity.routeStackChanged();
        Continuity.routeStackChanged();
        Continuity.routeStackChanged();
        flushSerialCalls();

        assertEquals(1, provider.saves);
    }

    /**
     * The sequence increases with every state, which is what lets a receiver tell a state it has
     * already acted on from a new one. Two states can share a timestamp -- clocks are coarse --
     * so the timestamp cannot carry this on its own.
     */
    @EdtTest
    public void everyCheckpointGetsAHigherSequence() {
        Continuity.setStateProvider(new RecordingProvider());

        Continuity.checkpoint();
        long first = Continuity.getRestorableState().getSequence();
        Continuity.checkpoint();
        long second = Continuity.getRestorableState().getSequence();

        assertTrue(second > first, second + " should be greater than " + first);
    }

    /**
     * A provider that throws must not take down the navigation that triggered the checkpoint.
     * The routes are still saved; only the payload is absent from that one state.
     */
    @EdtTest
    public void aProviderThatThrowsCostsOnlyItsOwnPayload() {
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                throw new IllegalStateException("boom");
            }

            public void restoreState(Map<String, Object> payload) {
            }
        });

        Continuity.checkpoint();

        AppState stored = Continuity.getRestorableState();
        assertNotNull(stored);
        assertTrue(stored.getPayload().isEmpty());
    }

    /**
     * An unrepresentable payload is NOT swallowed. It is a programming error with exactly one
     * useful moment to surface -- here, naming the key -- rather than a value that silently stops
     * arriving on the other device.
     */
    @EdtTest
    public void anUnrepresentablePayloadFailsTheCheckpointLoudly() {
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("when", new java.util.Date());
                return m;
            }

            public void restoreState(Map<String, Object> payload) {
            }
        });

        try {
            Continuity.checkpoint();
            org.junit.jupiter.api.Assertions.fail("expected the unrepresentable value to be "
                    + "refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("when"), expected.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Restoring
    // ------------------------------------------------------------------

    @EdtTest
    public void restoringHandsThePayloadBackToTheProvider() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("draft", "half a sentence");
        Continuity.setStateProvider(provider);
        Continuity.checkpoint();

        boolean shownAForm = Continuity.restore();

        // No routes were recorded, so the framework showed nothing and says so -- which is what
        // lets "restore, or else begin" work for an app that does not use @Route.
        assertFalse(shownAForm);
        assertEquals("half a sentence", provider.restored.get("draft"));
    }

    @EdtTest
    public void aStateOlderThanTheMaxAgeIsNotOffered() {
        Continuity.setStateProvider(new RecordingProvider());
        Continuity.checkpoint();
        assertNotNull(Continuity.getRestorableState());

        Continuity.setMaxAge(1L);
        // The stored state's timestamp is now, so age it rather than waiting.
        AppState aged = Continuity.getRestorableState().setTimestamp(
                System.currentTimeMillis() - 5000L);
        Storage.getInstance().writeObject(Continuity.STORAGE_KEY, aged);

        assertNull(Continuity.getRestorableState());
    }

    @EdtTest
    public void clearForgetsTheStoredStateAndTheAdvertisedActivity() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("draft", "something");
        Continuity.setStateProvider(provider);
        Continuity.checkpoint();
        assertNotNull(bridge.getPublishedInfo());

        Continuity.clear();

        assertNull(Continuity.getRestorableState());
        assertNull(bridge.getPublishedType());
    }

    // ------------------------------------------------------------------
    // Continuation to and from another device
    // ------------------------------------------------------------------

    @EdtTest
    public void aCheckpointAdvertisesTheStateUnderThisAppsActivityType() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("draft", "hello");
        Continuity.setStateProvider(provider);
        Continuity.setTitle("Editing a draft");

        Continuity.checkpoint();

        assertEquals(Continuity.getActivityType(), bridge.getPublishedType());
        assertEquals("Editing a draft", bridge.getPublishedTitle());
        AppState advertised = StateCodec.fromMap(bridge.getPublishedInfo());
        assertNotNull(advertised);
        assertEquals("hello", advertised.getPayload().get("draft"));
    }

    /**
     * This device's own echo is never acted on. A relay returns the state this device just
     * published as a matter of course, and restoring it would move the user to where they
     * already are -- repeatedly.
     */
    @EdtTest
    public void thisDevicesOwnEchoIsIgnored() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("draft", "something");
        Continuity.setStateProvider(provider);
        Continuity.checkpoint();

        boolean claimed = bridge.simulateArrival(Continuity.getActivityType(),
                bridge.getPublishedInfo());
        flushSerialCalls();

        assertTrue(claimed);
        assertNull(provider.restored);
    }

    @EdtTest
    public void aStateFromAnotherDeviceReachesTheListener() {
        Continuity.setStateProvider(new RecordingProvider());
        RecordingListener listener = new RecordingListener();
        Continuity.addContinuationListener(listener);

        deliverFromElsewhere("welcome back", 1L);

        assertNotNull(listener.seen);
        assertEquals("welcome back", listener.seen.getPayload().get("note"));
    }

    /**
     * The same state delivered twice acts once. A continuation and a relay routinely carry the
     * same one.
     */
    @EdtTest
    public void thesameStateDeliveredTwiceActsOnce() {
        Continuity.setStateProvider(new RecordingProvider());
        RecordingListener listener = new RecordingListener();
        Continuity.addContinuationListener(listener);

        deliverFromElsewhere("first", 4L);
        deliverFromElsewhere("first", 4L);

        assertEquals(1, listener.calls);
    }

    @EdtTest
    public void aStateOlderThanOneAlreadySeenFromThatDeviceIsIgnored() {
        Continuity.setStateProvider(new RecordingProvider());
        RecordingListener listener = new RecordingListener();
        Continuity.addContinuationListener(listener);

        deliverFromElsewhere("newer", 9L);
        deliverFromElsewhere("older", 2L);

        assertEquals(1, listener.calls);
        assertEquals("newer", listener.seen.getPayload().get("note"));
    }

    /**
     * A listener that returns false has consumed the state: nothing is restored, and no other
     * listener is asked. This is how an app prompts before moving the user.
     */
    @EdtTest
    public void aListenerThatDeclinesStopsTheRestore() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                return false;
            }
        });
        RecordingListener second = new RecordingListener();
        Continuity.addContinuationListener(second);

        deliverFromElsewhere("ignored", 1L);

        assertEquals(0, second.calls);
        assertNull(provider.restored);
    }

    @EdtTest
    public void anActivityTypeThisAppNeverPublishedIsNotClaimed() {
        Continuity.setStateProvider(new RecordingProvider());
        RecordingListener listener = new RecordingListener();
        Continuity.addContinuationListener(listener);

        boolean claimed = bridge.simulateArrival("com.someone.else.activity",
                new HashMap<String, Object>());
        flushSerialCalls();

        assertFalse(claimed);
        assertEquals(0, listener.calls);
    }

    @EdtTest
    public void autoRestoreOffLeavesTheStateForTheApplication() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(false);

        deliverFromElsewhere("later", 1L);

        assertNull(provider.restored);
        AppState waiting = Continuity.getRestorableState();
        assertNotNull(waiting);
        assertEquals("later", waiting.getPayload().get("note"));
    }

    /**
     * Recording the high-water mark and reaching the event queue are two steps, and two channels
     * deliver on threads of their own -- so an older state could pass the dedup, pause, and be
     * queued BEHIND the newer one that overtook it. The event thread then restored the newer
     * state and overwrote it with the stale one.
     *
     * <p>Simulated by delivering the newer state from inside the older one's dispatch window,
     * which is the same ordering without needing two real threads.</p>
     */
    @EdtTest
    public void aStateSupersededWhileQueuedIsDropped() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        final RecordingListener listener = new RecordingListener();
        Continuity.addContinuationListener(listener);

        // Both enqueued before either runs: deliver() records the mark and posts to the EDT, and
        // nothing here drains the queue in between.
        Continuity.deliver(fromElsewhere("older", 1L));
        Continuity.deliver(fromElsewhere("newer", 2L));
        flushSerialCalls();

        assertEquals(1, listener.calls, "the superseded delivery still ran");
        assertEquals("newer", listener.seen.getPayload().get("note"));
    }

    /** An empty document is not a state, so nothing is claimed and no listener runs. */
    @EdtTest
    public void anEmptyActivityPayloadIsNotDeliveredToListeners() {
        Continuity.setStateProvider(new RecordingProvider());
        RecordingListener listener = new RecordingListener();
        Continuity.addContinuationListener(listener);

        boolean claimed = bridge.simulateArrival(Continuity.getActivityType(),
                new HashMap<String, Object>());
        flushSerialCalls();

        assertFalse(claimed, "an activity carrying no state must not be claimed");
        assertEquals(0, listener.calls);
    }

    /**
     * A relay hands back whatever it still holds, which can be days old. Auto-restoring an
     * expired checkout or booking hold is the exact harm setMaxAge exists to prevent, and the
     * stored-state check alone never saw this path.
     */
    @EdtTest
    public void anExpiredStateArrivingFromElsewhereIsIgnored() {
        Continuity.setStateProvider(new RecordingProvider());
        RecordingListener listener = new RecordingListener();
        Continuity.addContinuationListener(listener);
        Continuity.setMaxAge(60000L);

        deliverFromElsewhereAged("stale", 1L, System.currentTimeMillis() - 300000L);

        assertEquals(0, listener.calls);
    }

    /** The same delivery inside the window still arrives, so the check is not simply off. */
    @EdtTest
    public void aFreshStateArrivingFromElsewhereStillArrivesWithMaxAgeSet() {
        Continuity.setStateProvider(new RecordingProvider());
        RecordingListener listener = new RecordingListener();
        Continuity.addContinuationListener(listener);
        Continuity.setMaxAge(60000L);

        deliverFromElsewhereAged("fresh", 2L, System.currentTimeMillis());

        assertEquals(1, listener.calls);
    }

    /**
     * Dropping an expired state must not consume its sequence, or a fresher state from the same
     * device would be mistaken for one already seen.
     */
    @EdtTest
    public void anExpiredStateDoesNotConsumeTheSequenceOfAFresherOne() {
        Continuity.setStateProvider(new RecordingProvider());
        RecordingListener listener = new RecordingListener();
        Continuity.addContinuationListener(listener);
        Continuity.setMaxAge(60000L);

        deliverFromElsewhereAged("stale", 5L, System.currentTimeMillis() - 300000L);
        deliverFromElsewhereAged("fresh", 5L, System.currentTimeMillis());

        assertEquals(1, listener.calls);
        assertEquals("fresh", listener.seen.getPayload().get("note"));
    }

    /**
     * A publish REPLACES what the relay holds, so two checkpoints racing to the endpoint could
     * land in reverse order and leave the user's other device fetching work they had moved past.
     * Nothing failed and nothing was logged, which is what made it worth pinning.
     */
    @EdtTest
    public void relayPublishesArriveInCheckpointOrder() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        OrderRecordingRelay r = new OrderRecordingRelay();
        Continuity.setRelay(r);

        for (int i = 1; i <= 6; i++) {
            provider.saved.put("n", Integer.valueOf(i));
            Continuity.checkpoint();
        }
        long newest = Continuity.getRestorableState().getSequence();
        r.awaitQuiet();

        assertFalse(r.published.isEmpty(), "the relay saw nothing at all");
        // Coalescing is allowed and expected -- what is not allowed is going backwards.
        for (int i = 1; i < r.published.size(); i++) {
            assertTrue(r.published.get(i).longValue() > r.published.get(i - 1).longValue(),
                    "relay saw " + r.published + ", which goes backwards");
        }
        assertEquals(Long.valueOf(newest), r.published.get(r.published.size() - 1),
                "the newest checkpoint has to be the relay's final value");
    }

    /**
     * StateRelay.publish documents that a failed state is kept for the next attempt. Dropping it
     * meant the last checkpoint before the network went away -- the one most worth having --
     * never reached the other device at all.
     */
    @EdtTest
    public void aFailedPublishKeepsTheStateForTheNextAttempt() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        FailingThenWorkingRelay r = new FailingThenWorkingRelay();
        Continuity.setRelay(r);

        Continuity.checkpoint();
        long failed = Continuity.getRestorableState().getSequence();
        r.awaitAttempts(1);
        assertEquals(0, r.delivered.size(), "the first attempt was supposed to fail");

        // Deliberately NOT another checkpoint. A checkpoint overwrites the pending slot with its
        // own newer state, so asserting after one proves only that the SECOND state was sent --
        // which happens whether or not the first was retained. That is what an earlier version of
        // this test did, and it passed with the retention removed. pollRelay is the reconnect an
        // application actually makes, and it is what has to send what is owed.
        //
        // Polled in a loop rather than once: awaitAttempts returns when publish() is ENTERED, so
        // the worker may not have finished re-queuing and standing down yet, and a single poll
        // arriving in that window sees publishing==true and correctly does nothing. A reconnect
        // that happens twice is what an application does anyway.
        r.fail = false;
        long deadline = System.currentTimeMillis() + 3000L;
        while (r.delivered.isEmpty() && System.currentTimeMillis() < deadline) {
            Continuity.pollRelay();
            try {
                Thread.sleep(40);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        assertEquals(1, r.delivered.size(), "the retained state never reached the relay");
        assertEquals(Long.valueOf(failed), r.delivered.get(0),
                "a different state was sent, so the failed one was not the one retained");
    }

    /**
     * An app that only registers a store listener keeps continuity OFF by design -- a key/value
     * store is not consent to broadcast a route stack. refreshBridge() tested `enabled` alone, so
     * the simulator's capability menu, which swaps the bridge and calls it, left the replacement
     * with no callback and every later "Change the Synced Store" silently did nothing.
     */
    @EdtTest
    public void swappingTheBridgeKeepsASyncOnlyListenerWorking() {
        SyncedStoreListener l = new SyncedStoreListener() {
            public void storeChanged() {
            }
        };
        registered.add(l);
        SyncedStore.addChangeListener(l);
        assertFalse(Continuity.isEnabled(), "a store listener must not turn continuity on");

        // What the simulator's capability menu does.
        CountingStoreBridge swapped = new CountingStoreBridge();
        Continuity.setBridge(swapped);
        Continuity.refreshBridge();

        assertTrue(swapped.callbackInstalls() > 0,
                "the replacement bridge got no callback, so a change on another device can no "
                        + "longer reach the listener");
    }

    /**
     * Every device's mark has to survive a restart, not just one. An earlier shape reconstructed a
     * single id from the stored checkpoint, so a second foreign device -- or any foreign device
     * once a local navigation had overwritten the checkpoint -- was delivered and acted on again.
     */
    @EdtTest
    public void everyDevicesHighWaterMarkSurvivesARestart() {
        Continuity.enable();
        AppState fromA = foreign("device-a", 4);
        AppState fromB = foreign("device-b", 9);
        bridge.simulateArrival(Continuity.getActivityType(), StateCodec.toMap(fromA));
        bridge.simulateArrival(Continuity.getActivityType(), StateCodec.toMap(fromB));
        // This device then navigates, so the stored checkpoint is OUR state and carries neither id.
        Continuity.checkpoint();

        Continuity.reset();
        Continuity.setBridge(bridge);
        Continuity.enable();

        final int[] seen = new int[1];
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                seen[0]++;
                return true;
            }
        });
        Continuity.deliver(fromA);
        Continuity.deliver(fromB);
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        assertEquals(0, seen[0],
                "a device's mark was lost across the restart, so its state was acted on twice");
    }

    /** A foreign state, ready to deliver. */
    private static AppState foreign(String device, long sequence) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("k", "v");
        return new AppState().setPayload(payload).setDeviceId(device).setSequence(sequence)
                .setTimestamp(System.currentTimeMillis());
    }

    /**
     * A reconnect that lands while a publish is in flight has to be honoured. startPublisher()
     * saw publishing == true and left the work to the live worker -- correct for ordering -- but
     * if that attempt then FAILED the worker requeued and stood down, forgetting the request. A
     * single reconnect after a failed send left the retained state unsent until some later
     * checkpoint happened to restart the publisher.
     */
    @EdtTest
    public void aReconnectDuringAFailedPublishIsRetried() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        BlockingFailingRelay r = new BlockingFailingRelay();
        Continuity.setRelay(r);

        Continuity.checkpoint();
        r.awaitInPublish();
        // The application reconnects while the first attempt is still on the wire.
        Continuity.pollRelay();
        // Now let that attempt fail; the next one is allowed to succeed.
        r.fail = false;
        r.release();

        long deadline = System.currentTimeMillis() + 3000L;
        while (r.delivered() == 0 && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        assertTrue(r.delivered() > 0,
                "the reconnect was forgotten, so the retained state was never sent");
    }

    /** Blocks inside publish() until released, and fails the attempt it was holding. */
    static class BlockingFailingRelay implements StateRelay {
        volatile boolean fail = true;
        private final java.util.concurrent.CountDownLatch gate =
                new java.util.concurrent.CountDownLatch(1);
        private final java.util.concurrent.atomic.AtomicInteger inPublish =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger sent =
                new java.util.concurrent.atomic.AtomicInteger();

        public void publish(AppState state) throws java.io.IOException {
            if (fail) {
                inPublish.incrementAndGet();
                try {
                    gate.await(2, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                throw new java.io.IOException("no network");
            }
            sent.incrementAndGet();
        }

        public AppState fetch() {
            return null;
        }

        void release() {
            gate.countDown();
        }

        int delivered() {
            return sent.get();
        }

        void awaitInPublish() {
            long deadline = System.currentTimeMillis() + 2000L;
            while (inPublish.get() == 0 && System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * A poll coalesced behind a setRelay() must use the NEW relay. The worker kept the one it was
     * started with and refreshed only the era, so the second attempt fetched from the endpoint
     * that had just been replaced and then stamped the answer with the new era -- which made the
     * era check, whose whole job is to stop exactly that, wave it through.
     */
    @EdtTest
    public void aCoalescedPollUsesTheReplacementRelay() {
        BlockingFetchRelay old = new BlockingFetchRelay();
        Continuity.enable();
        Continuity.setRelay(old);
        old.awaitInFlight();

        // Queued while the old relay's fetch is still held, which is what makes it coalesce.
        BlockingFetchRelay replacement = new BlockingFetchRelay();
        replacement.release();
        Continuity.setRelay(replacement);
        old.release();

        long deadline = System.currentTimeMillis() + 3000L;
        while (replacement.fetches() == 0 && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        assertEquals(1, old.fetches(),
                "the replaced relay was asked a second time, so its answer could still be "
                        + "restored under the new relay's era");
        assertTrue(replacement.fetches() > 0, "the replacement relay was never asked");
    }

    /**
     * A relay holds ONE document per user, so two overlapping GETs can return DIFFERENT states --
     * the other device may replace it between them. Nothing downstream re-orders the answers:
     * lastSeen is keyed by the ORIGINATING device, so a response that left first and came back
     * second passes deduplication on its own key and puts the older screen over the newer one.
     */
    @EdtTest
    public void overlappingPollsNeverRunTwoFetchesAtOnce() {
        BlockingFetchRelay r = new BlockingFetchRelay();
        Continuity.enable();
        Continuity.setRelay(r);

        // Six, and all of them while the first fetch is still held: this is the Android resume
        // poll landing on top of an application that also polls on reconnect.
        for (int i = 0; i < 6; i++) {
            Continuity.pollRelay();
        }
        r.awaitInFlight();
        r.release();
        r.awaitQuiet();

        assertEquals(1, r.maxConcurrent(),
                "two relay fetches overlapped, so an older response can land after a newer one");
        // Coalesced, not discarded. A poll asked for while one is in flight is a real question --
        // the application just reconnected -- and answering it with silence is the lost-request
        // bug the publisher already had once.
        assertTrue(r.fetches() >= 2,
                "the polls requested during the first fetch were dropped rather than coalesced");
    }

    /**
     * com.codename1.continuity.sync is a package of its own so that its cost is earned
     * separately. Enabling the whole framework to register a store listener made every route
     * change checkpoint -- which on iOS advertises the app's navigation to the devices around it
     * -- so an application that wanted a key/value store its user's devices share was opted into
     * broadcasting its route stack. A key/value store is not consent to publish where the user is.
     */
    @EdtTest
    public void registeringAStoreListenerDoesNotEnableContinuity() {
        CountingStoreBridge counting = new CountingStoreBridge();
        Continuity.setBridge(counting);
        SyncedStoreListener l = new SyncedStoreListener() {
            public void storeChanged() {
            }
        };
        registered.add(l);

        SyncedStore.addChangeListener(l);

        assertFalse(Continuity.isEnabled(),
                "registering a store listener turned continuity on, so route changes now "
                        + "checkpoint and Handoff advertises them");
        // The listener still has to be reachable, which is the whole reason the old code enabled.
        assertTrue(counting.callbackInstalls() > 0,
                "no callback was installed, so a change on another device could never arrive");
    }

    /**
     * A different endpoint is a different destination. A state retained after a failed send was
     * published to whatever relay replaced the one it was captured for -- an application's data
     * sent somewhere it was never handed to.
     */
    @EdtTest
    public void replacingTheRelayDropsWorkQueuedForTheOldOne() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        FailingThenWorkingRelay old = new FailingThenWorkingRelay();
        Continuity.setRelay(old);

        Continuity.checkpoint();
        long stranded = Continuity.getRestorableState().getSequence();
        old.awaitAttempts(1);
        assertEquals(0, old.delivered.size(), "the first attempt was supposed to fail");

        FailingThenWorkingRelay replacement = new FailingThenWorkingRelay();
        replacement.fail = false;
        Continuity.setRelay(replacement);

        // Polled the way an application reconnects. Nothing owed to the previous endpoint may
        // come out of this.
        long deadline = System.currentTimeMillis() + 1200L;
        while (System.currentTimeMillis() < deadline) {
            Continuity.pollRelay();
            try {
                Thread.sleep(40);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        assertFalse(replacement.delivered.contains(Long.valueOf(stranded)),
                "the state captured for the previous relay was published to its replacement");
    }

    /**
     * On iOS the external-change observer is installed the first time the platform store is
     * resolved, and enable() does not resolve it. An application that only registers a listener
     * and waits to read values inside the callback was therefore never told about a change made
     * on another device until some unrelated read or write happened to bring the store up.
     */
    @EdtTest
    public void registeringAStoreListenerResolvesThePlatformStore() {
        CountingStoreBridge counting = new CountingStoreBridge();
        Continuity.setBridge(counting);
        SyncedStoreListener l = new SyncedStoreListener() {
            public void storeChanged() {
            }
        };
        registered.add(l);

        SyncedStore.addChangeListener(l);

        assertTrue(counting.storeQueries() > 0,
                "registering a listener never reached the platform store, so on iOS no observer "
                        + "would exist and a remote change could not call the listener");
    }

    /** A LocalContinuityBridge that counts how often the synced store was resolved. */
    static class CountingStoreBridge extends LocalContinuityBridge {
        private final java.util.concurrent.atomic.AtomicInteger queries =
                new java.util.concurrent.atomic.AtomicInteger();

        private final java.util.concurrent.atomic.AtomicInteger callbacks =
                new java.util.concurrent.atomic.AtomicInteger();

        @Override
        public boolean isSyncedStoreSupported() {
            queries.incrementAndGet();
            return super.isSyncedStoreSupported();
        }

        @Override
        public void setCallback(com.codename1.continuity.spi.ContinuityCallback c) {
            callbacks.incrementAndGet();
            super.setCallback(c);
        }

        int storeQueries() {
            return queries.get();
        }

        int callbackInstalls() {
            return callbacks.get();
        }
    }

    /** Holds every fetch until released, and records how many ran at once. */
    static class BlockingFetchRelay implements StateRelay {
        private final java.util.concurrent.CountDownLatch gate =
                new java.util.concurrent.CountDownLatch(1);
        private final java.util.concurrent.atomic.AtomicInteger inFlight =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger peak =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger count =
                new java.util.concurrent.atomic.AtomicInteger();

        public void publish(AppState state) {
        }

        public AppState fetch() {
            count.incrementAndGet();
            int now = inFlight.incrementAndGet();
            for (;;) {
                int seen = peak.get();
                if (now <= seen || peak.compareAndSet(seen, now)) {
                    break;
                }
            }
            try {
                gate.await(2, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            inFlight.decrementAndGet();
            return null;
        }

        void release() {
            gate.countDown();
        }

        int maxConcurrent() {
            return peak.get();
        }

        int fetches() {
            return count.get();
        }

        /** Waits for the first fetch to actually be inside the relay before releasing it. */
        void awaitInFlight() {
            long deadline = System.currentTimeMillis() + 2000L;
            while (inFlight.get() == 0 && System.currentTimeMillis() < deadline) {
                sleep();
            }
        }

        /** Waits for the coalesced follow-up to run and the worker to stand down. */
        void awaitQuiet() {
            long deadline = System.currentTimeMillis() + 3000L;
            while (System.currentTimeMillis() < deadline) {
                if (inFlight.get() == 0 && count.get() >= 2) {
                    return;
                }
                sleep();
            }
        }

        private void sleep() {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Fails every publish until `fail` is cleared, and records what got through. */
    static class FailingThenWorkingRelay implements StateRelay {
        volatile boolean fail = true;
        final List<Long> delivered =
                java.util.Collections.synchronizedList(new ArrayList<Long>());
        private final java.util.concurrent.atomic.AtomicInteger attempts =
                new java.util.concurrent.atomic.AtomicInteger();

        public void publish(AppState state) throws java.io.IOException {
            attempts.incrementAndGet();
            if (fail) {
                throw new java.io.IOException("no network");
            }
            delivered.add(Long.valueOf(state.getSequence()));
        }

        public AppState fetch() {
            return null;
        }

        void awaitAttempts(int n) {
            await(new Condition() {
                public boolean met() {
                    return attempts.get() >= n;
                }
            });
        }

        void awaitDelivered(int n) {
            await(new Condition() {
                public boolean met() {
                    return delivered.size() >= n;
                }
            });
        }

        private void await(Condition c) {
            long deadline = System.currentTimeMillis() + 1500L;
            while (System.currentTimeMillis() < deadline && !c.met()) {
                try {
                    Thread.sleep(25);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        interface Condition {
            boolean met();
        }
    }

    /**
     * disable() documents that arriving states are ignored. A delivery that had already reached
     * the event queue kept its lastSeen marker and dispatched anyway -- running listeners and
     * restoring after the application had turned the framework off.
     */
    @EdtTest
    public void aDeliveryQueuedBeforeDisableDoesNotDispatch() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        RecordingListener listener = new RecordingListener();
        Continuity.addContinuationListener(listener);

        // Queued but not drained: deliver() posts to the event queue and nothing runs it yet.
        Continuity.deliver(fromElsewhere("after disable", 1L));
        Continuity.disable();
        flushSerialCalls();

        assertEquals(0, listener.calls, "a delivery from before disable() still dispatched");
    }

    /**
     * And re-enabling before the queue drains must not resurrect it, which is why this is a
     * generation rather than a flag: an `enabled` test at dispatch time would pass here.
     */
    @EdtTest
    public void disablingAndReEnablingDoesNotResurrectAQueuedDelivery() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        RecordingListener listener = new RecordingListener();
        Continuity.addContinuationListener(listener);

        Continuity.deliver(fromElsewhere("stale run", 1L));
        Continuity.disable();
        Continuity.enable();
        flushSerialCalls();

        assertEquals(0, listener.calls,
                "a delivery from the previous run survived disable/enable");
    }

    /**
     * A state that is still QUEUED when the user signs out is never sent. The worker is held
     * inside its first request, a second checkpoint queues behind it, and clear() then empties
     * the queue -- so when the worker is released it finds nothing of the old session to send.
     *
     * <p>The boundary this does NOT cover is a request already on the wire. Holding the relay
     * inside publish is exactly that case, and clear()'s own documentation says it cannot recall
     * one -- which is why the first state is expected to arrive and only the second must not.</p>
     */
    @EdtTest
    public void aStateStillQueuedAtLogoutIsNeverSent() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        GatedRelay r = new GatedRelay();
        Continuity.setRelay(r);

        Continuity.checkpoint();
        r.awaitEntered();
        long inFlight = Continuity.getRestorableState().getSequence();

        // Queued behind the request the worker is holding.
        provider.saved.put("n", Integer.valueOf(2));
        Continuity.checkpoint();
        long queued = Continuity.getRestorableState().getSequence();
        assertTrue(queued > inFlight, "the second checkpoint did not advance the sequence");

        Continuity.clear();
        r.release();
        r.awaitQuiet();

        assertFalse(r.sent.contains(Long.valueOf(queued)),
                "a state queued before logout was published after it: " + r.sent);
    }

    /**
     * Blocks on entry to publish so a test can act while a state is dequeued but unsent. Records
     * only what it was actually asked to send AFTER being released.
     */
    static class GatedRelay implements StateRelay {
        final List<Long> sent = java.util.Collections.synchronizedList(new ArrayList<Long>());
        private final java.util.concurrent.CountDownLatch entered =
                new java.util.concurrent.CountDownLatch(1);
        private final java.util.concurrent.CountDownLatch gate =
                new java.util.concurrent.CountDownLatch(1);

        public void publish(AppState state) {
            entered.countDown();
            try {
                gate.await(3, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            sent.add(Long.valueOf(state.getSequence()));
        }

        public AppState fetch() {
            return null;
        }

        void awaitEntered() {
            try {
                entered.await(3, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        void release() {
            gate.countDown();
        }

        void awaitQuiet() {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Records the sequence of everything the relay is handed, slowly enough to overlap. */
    static class OrderRecordingRelay implements StateRelay {
        final List<Long> published =
                java.util.Collections.synchronizedList(new ArrayList<Long>());
        private volatile long lastFinished;

        public void publish(AppState state) {
            try {
                Thread.sleep(15);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            published.add(Long.valueOf(state.getSequence()));
            lastFinished = System.currentTimeMillis();
        }

        public AppState fetch() {
            return null;
        }

        /// Waits until the relay has been quiet for a moment, so the assertions read a settled
        /// list rather than a race of their own.
        void awaitQuiet() {
            long deadline = System.currentTimeMillis() + 5000L;
            while (System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (!published.isEmpty() && System.currentTimeMillis() - lastFinished > 300L) {
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // The synced store
    // ------------------------------------------------------------------

    @EdtTest
    public void theSyncedStoreRoundTripsAndEnumerates() {
        assertTrue(SyncedStore.isSupported());

        assertTrue(SyncedStore.put("sortOrder", "byDate"));
        assertTrue(SyncedStore.put("theme", "dark"));

        assertEquals("byDate", SyncedStore.get("sortOrder", "byName"));
        List<String> keys = new ArrayList<String>(Arrays.asList(SyncedStore.keys()));
        assertTrue(keys.contains("sortOrder"));
        assertTrue(keys.contains("theme"));

        SyncedStore.remove("theme");
        assertEquals("light", SyncedStore.get("theme", "light"));
        assertFalse(new ArrayList<String>(Arrays.asList(SyncedStore.keys())).contains("theme"));
    }

    /**
     * put() used to answer true whenever a store merely existed, so the fallback the guide
     * recommends -- write locally when the synced write fails -- could never run and a value the
     * store refused was reported saved.
     */
    @EdtTest
    public void aRefusedSyncedWriteIsReportedAsFailure() {
        JavaSEStyleRefusingBridge refusing = new JavaSEStyleRefusingBridge();
        Continuity.setBridge(refusing);

        assertFalse(SyncedStore.put("sortOrder", "byDate"),
                "a store that did not take the value must not report success");
        assertEquals("byName", SyncedStore.get("sortOrder", "byName"));
    }

    /** And still answers true when the store really did take it. */
    @EdtTest
    public void anAcceptedSyncedWriteIsReportedAsSuccess() {
        assertTrue(SyncedStore.put("sortOrder", "byDate"));
        assertEquals("byDate", SyncedStore.get("sortOrder", "byName"));
    }

    /** A store that reports supported and then silently drops every write. */
    static class JavaSEStyleRefusingBridge extends LocalContinuityBridge {
        @Override
        public boolean syncedStorePut(String key, String value) {
            return false;
        }

        @Override
        public String syncedStoreGet(String key) {
            return null;
        }
    }

    /**
     * A key containing a newline is one this API accepts -- the platform store imposes no such
     * rule, so the simulation must not either. The newline-delimited index used to read it back
     * as two phantom keys, and nothing could then remove the value that was actually stored.
     */
    @EdtTest
    public void aKeyContainingANewlineSurvivesTheSimulatedIndex() {
        assertTrue(SyncedStore.put("multi\nline", "value"));

        List<String> keys = new ArrayList<String>(Arrays.asList(SyncedStore.keys()));
        assertTrue(keys.contains("multi\nline"), "the key came back as " + keys);
        assertFalse(keys.contains("multi"), "a phantom key appeared: " + keys);
        assertEquals("value", SyncedStore.get("multi\nline", "missing"));

        SyncedStore.remove("multi\nline");
        assertFalse(new ArrayList<String>(Arrays.asList(SyncedStore.keys()))
                .contains("multi\nline"), "the key could not be removed");
    }

    /** A backslash in a key is the other half of the escaping, and round-trips too. */
    @EdtTest
    public void aKeyContainingABackslashSurvivesTheSimulatedIndex() {
        assertTrue(SyncedStore.put("back\\slash", "v"));

        List<String> keys = new ArrayList<String>(Arrays.asList(SyncedStore.keys()));
        assertTrue(keys.contains("back\\slash"), "the key came back as " + keys);
    }

    @EdtTest
    public void aChangeMadeElsewhereReachesTheListener() {
        CountingStoreListener listener = new CountingStoreListener();
        registered.add(listener);
        SyncedStore.addChangeListener(listener);

        bridge.simulateStoreChange();
        flushSerialCalls();

        assertEquals(1, listener.calls);
    }

    /**
     * A listener that unregisters itself while being notified is ordinary, and would otherwise
     * mutate the list being walked.
     */
    @EdtTest
    public void aListenerMayUnregisterItselfWhileBeingNotified() {
        SyncedStoreListener selfRemoving = new SyncedStoreListener() {
            public void storeChanged() {
                SyncedStore.removeChangeListener(this);
            }
        };
        registered.add(selfRemoving);
        SyncedStore.addChangeListener(selfRemoving);

        bridge.simulateStoreChange();
        flushSerialCalls();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static AppState fromElsewhere(String note, long sequence) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("note", note);
        return new AppState()
                .setPayload(payload)
                .setDeviceId("some-other-device")
                .setSequence(sequence)
                .setTimestamp(System.currentTimeMillis());
    }

    private void deliverFromElsewhereAged(String note, long sequence, long timestamp) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("note", note);
        AppState state = new AppState()
                .setPayload(payload)
                .setDeviceId("some-other-device")
                .setSequence(sequence)
                .setTimestamp(timestamp);
        bridge.simulateArrival(Continuity.getActivityType(), StateCodec.toMap(state));
        flushSerialCalls();
    }

    private void deliverFromElsewhere(String note, long sequence) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("note", note);
        AppState state = new AppState()
                .setPayload(payload)
                .setDeviceId("some-other-device")
                .setSequence(sequence)
                .setTimestamp(System.currentTimeMillis());
        bridge.simulateArrival(Continuity.getActivityType(), StateCodec.toMap(state));
        flushSerialCalls();
    }

    static class RecordingProvider implements StateProvider {
        final Map<String, Object> saved = new HashMap<String, Object>();
        Map<String, Object> restored;

        public Map<String, Object> saveState() {
            return saved;
        }

        public void restoreState(Map<String, Object> payload) {
            restored = payload;
        }
    }

    static class CountingProvider implements StateProvider {
        int saves;

        public Map<String, Object> saveState() {
            saves++;
            return null;
        }

        public void restoreState(Map<String, Object> payload) {
        }
    }

    static class RecordingListener implements ContinuityListener {
        AppState seen;
        int calls;

        public boolean stateReceived(AppState state) {
            calls++;
            seen = state;
            return true;
        }
    }

    static class CountingStoreListener implements SyncedStoreListener {
        int calls;

        public void storeChanged() {
            calls++;
        }
    }
}
