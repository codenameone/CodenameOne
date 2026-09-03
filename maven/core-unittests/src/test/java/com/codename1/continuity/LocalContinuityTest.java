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

    /// Runs a blocking wait OFF the event thread.
    ///
    /// Every relay round trip now finishes with a callSerially: the worker hands its answer back
    /// to the event thread rather than touching framework state itself. A test that blocks the
    /// EDT waiting for one therefore waits for a runnable queued behind its own wait, and the
    /// harness reports "pendingSerialCalls=1". invokeAndBlock releases the EDT for the duration,
    /// which is what an application doing a long wait does too.
    private static void awaitOffEdt(Runnable r) {
        Display.getInstance().invokeAndBlock(r);
    }

    /// Sleeps without holding the event thread. See awaitOffEdt.
    private static void pause(final long millis) {
        awaitOffEdt(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

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
        final long newest = Continuity.getRestorableState().getSequence();
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitPublished(newest);
            }
        });

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
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitAttempts(1);
            }
        });
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
            // pollRelay() on the EDT, the wait off it: the poll reads event-thread state to
            // decide whether a fetch is already out, and the completion it is waiting for is a
            // queued runnable.
            Continuity.pollRelay();
            pause(40L);
        }

        assertEquals(1, r.delivered.size(), "the retained state never reached the relay");
        assertEquals(Long.valueOf(failed), r.delivered.get(0),
                "a different state was sent, so the failed one was not the one retained");
    }

    /**
     * A fetch that was already on the wire when the user signed out must not be delivered into
     * the session that follows it. The relay is held inside fetch(), clear() runs while it is
     * held, and the answer it finally returns belongs to the account that has gone.
     *
     * <p>This is the one thing a relay round trip needs that a single-threaded framework cannot
     * get for free: the request outlives the event-thread turn that started it. It is answered
     * with a session counter read back on the event thread, not with a lock.</p>
     */
    @EdtTest
    public void aFetchStartedBeforeALogoutIsNotDeliveredAfterIt() {
        Continuity.enable();
        final int[] seen = new int[1];
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                seen[0]++;
                return true;
            }
        });

        final BlockingFetchRelay r = new BlockingFetchRelay();
        r.answer = foreign("device-x", 3);
        Continuity.setRelay(r);
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitInFlight();
            }
        });

        // The user signs out while the fetch is still held.
        Continuity.clear();
        r.release();
        pause(250L);

        assertEquals(0, seen[0],
                "a state fetched before the logout was delivered into the session after it");
    }

    /**
     * An expired parked arrival must not hide a valid local checkpoint. Returning null the moment
     * the parked state aged out reported "nothing to restore" while storage held a perfectly good
     * one -- ordinary with automatic restore off and the user still navigating -- so a single
     * restore() call told the application to show its initial screen instead.
     */
    @EdtTest
    public void anExpiredParkedStateFallsBackToTheStoredCheckpoint() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        Continuity.enable();
        // A local checkpoint that is fresh and valid.
        Continuity.checkpoint();
        long mine = Continuity.getRestorableState().getSequence();

        // An arrival from elsewhere, parked through the real path: no maxAge yet, so it is
        // admitted, and automatic restore is off so dispatch parks it rather than applying it.
        Continuity.setAutoRestore(false);
        AppState stale = foreign("device-stale", 2);
        stale.setTimestamp(System.currentTimeMillis() - 5000L);
        Continuity.deliver(stale);
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        // Now it is too old, which is what an application configuring an expiry would see.
        Continuity.setMaxAge(1000L);

        AppState offered = Continuity.getRestorableState();

        assertNotNull(offered, "the expired arrival hid the valid stored checkpoint");
        assertEquals(mine, offered.getSequence(),
                "the stored checkpoint should be offered once the parked one has expired");
    }

    /**
     * A parked state lives only in a field, so a process killed before the application calls
     * restore() loses it. Persisting the sender's high-water mark at park time therefore left a
     * durable "already handled" for something nothing ever handled, and the relay's repeat was
     * rejected on the next launch.
     */
    @EdtTest
    public void parkingAStateDoesNotDurablyMarkItHandled() {
        Continuity.enable();
        Continuity.setAutoRestore(false);
        AppState fromA = foreign("device-parked", 3);

        bridge.simulateArrival(Continuity.getActivityType(), StateCodec.toMap(fromA));
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        assertNotNull(Continuity.getRestorableState(), "the state should be parked for the app");

        // The relaunch: everything in memory goes, storage and preferences stay.
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
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        assertEquals(1, seen[0],
                "the parked state was marked handled durably, so the relay's repeat was rejected "
                        + "and a state nothing ever restored is now unrecoverable");
    }

    /**
     * The listener contract documents "do the work yourself and return false". That path never
     * reaches restore(), so nothing recorded the acknowledgement durably: after a relaunch the
     * relay's unchanged document was accepted again and the listener repeated its side effects.
     * acknowledge() is the explicit answer, and it is explicit on purpose -- false also means "I
     * am going to prompt", and marking THAT handled would lose the state if the process died
     * before the user answered.
     */
    @EdtTest
    public void acknowledgingAHandledStateSurvivesARestart() {
        Continuity.enable();
        final AppState handled = foreign("device-self-handled", 5);
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                // Did the work here; nothing to restore.
                Continuity.acknowledge(state);
                return false;
            }
        });

        Continuity.deliver(handled);
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // The relaunch.
        Continuity.reset();
        Continuity.setBridge(bridge);
        Continuity.enable();
        final int[] seen = new int[1];
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                seen[0]++;
                return false;
            }
        });

        Continuity.deliver(handled);
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
                "an acknowledged state came back after the restart, so the listener's side "
                        + "effects would run a second time");
    }

    /**
     * A platform continuation carries nothing but the state, and is delivered on its own merits.
     * Kept as the positive case beside the rejections above: a guard that refused everything
     * would pass those and still break the feature.
     */
    @EdtTest
    public void aPlatformArrivalIsDelivered() {
        Continuity.enable();
        final int[] seen = new int[1];
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                seen[0]++;
                return true;
            }
        });

        AppState arrival = foreign("device-platform", 11);
        Continuity.disable();
        Continuity.enable();

        Continuity.deliver(arrival);
        pause(250L);

        assertEquals(1, seen[0], "a platform arrival must be delivered");
    }

    /**
     * An off-EDT capture() must report an unrepresentable payload the same way the on-EDT path
     * does. Marshalled to the EDT, the IllegalArgumentException died in the runnable: the caller
     * waited out the timeout and got null, so the programming error the exception exists to name
     * became a silent nothing -- and only when called off the EDT.
     */
    @EdtTest
    public void anInvalidPayloadFailsTheSameWayFromAnyThread() {
        Continuity.enable();
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                Map<String, Object> bad = new HashMap<String, Object>();
                bad.put("unsupported", new StringBuilder("not a representable type"));
                return bad;
            }

            public void restoreState(Map<String, Object> state) {
            }
        });

        // On the EDT, where this test body runs: the refusal is immediate. This half was never
        // broken, and asserting only it is what made the first version of this test vacuous --
        // it passed with the exception swallowed, because it never reached the marshalled path.
        boolean threwOnEdt = false;
        try {
            Continuity.capture();
        } catch (IllegalArgumentException expected) {
            threwOnEdt = true;
        }
        assertTrue(threwOnEdt, "capture() must refuse an unrepresentable payload");

        // And OFF the EDT, which is the path that swallowed it. invokeAndBlock runs this on a
        // separate thread and releases the EDT to process what capture() marshals to it, so
        // offEdt() is true here and the call really does go through runOnEdt.
        final Throwable[] caught = new Throwable[1];
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                try {
                    Continuity.capture();
                } catch (Throwable t) {
                    caught[0] = t;
                }
            }
        });

        assertTrue(caught[0] instanceof IllegalArgumentException,
                "an off-EDT capture() reported " + caught[0] + " instead of the "
                        + "IllegalArgumentException the on-EDT path raises for the same payload");
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
        // Drained, because the durable mark is written when a state is ACTED ON and not when it
        // is admitted -- a process killed between the two would otherwise leave a mark on disk for
        // a state nothing had handled. Both arrivals dispatch through callSerially and this test
        // body is the EDT, so without this the states were never acted on and "survives a restart"
        // would be asserting about something that never happened.
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
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
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitInPublish();
            }
        });
        // The application reconnects while the first attempt is still on the wire.
        Continuity.pollRelay();
        // Now let that attempt fail; the next one is allowed to succeed.
        r.fail = false;
        r.release();

        long deadline = System.currentTimeMillis() + 3000L;
        while (r.delivered() == 0 && System.currentTimeMillis() < deadline) {
            pause(25L);
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
        final BlockingFetchRelay old = new BlockingFetchRelay();
        Continuity.enable();
        Continuity.setRelay(old);
        awaitOffEdt(new Runnable() {
            public void run() {
                old.awaitInFlight();
            }
        });

        // Queued while the old relay's fetch is still held, which is what makes it coalesce.
        BlockingFetchRelay replacement = new BlockingFetchRelay();
        replacement.release();
        Continuity.setRelay(replacement);
        old.release();

        long deadline = System.currentTimeMillis() + 3000L;
        while (replacement.fetches() == 0 && System.currentTimeMillis() < deadline) {
            pause(20L);
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
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitInFlight();
            }
        });
        r.release();
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitQuiet();
            }
        });

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
        /** What fetch() returns once released, or null for "the endpoint has nothing". */
        volatile AppState answer;

        private final java.util.concurrent.CountDownLatch gate =
                new java.util.concurrent.CountDownLatch(1);
        private final java.util.concurrent.atomic.AtomicInteger inFlight =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger peak =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger count =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger posts =
                new java.util.concurrent.atomic.AtomicInteger();

        public void publish(AppState state) {
            posts.incrementAndGet();
        }

        /** How many states actually reached the endpoint. */
        int published() {
            return posts.get();
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
            return answer;
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
     * disable() documents that checkpoints stop. A publish deferred behind a relay fetch used to
     * go out anyway: the state sat in the pending slot, disable() left it there, and the fetch
     * landing afterwards started a publisher that POSTed it -- after disable() had returned.
     *
     * <p>The EDT model is what makes this the only shape worth testing. Nothing can interleave
     * within a turn, so the sole way work outlives the decision is a relay round trip, and this
     * is that path.</p>
     */
    @EdtTest
    public void aPublishDeferredBehindAFetchIsNotSentAfterDisable() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        final BlockingFetchRelay r = new BlockingFetchRelay();
        Continuity.setRelay(r);
        // setRelay polls, so the fetch is in flight and any checkpoint defers behind it.
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitInFlight();
            }
        });

        Continuity.checkpoint();
        Continuity.disable();
        r.release();
        pause(500L);

        assertEquals(0, r.published(),
                "a state queued before disable() was published after it");
    }

    /**
     * A payload-only continuation -- what an app that does not use @Route gets -- is APPLIED even
     * though no form appears, so it must be marked acted-on. The route-less return skipped the
     * acknowledgement, so the relay's unchanged document was accepted again after a restart and
     * the listener repeated its side effects.
     */
    @EdtTest
    public void aPayloadOnlyRestoreIsStillMarkedActedOn() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);

        AppState payloadOnly = fromElsewhere("payload only", 5L);
        payloadOnly.setRoutes(new ArrayList<String>());

        assertFalse(Continuity.restore(payloadOnly),
                "a route-less state shows no form, so restore must report false");

        // Re-delivered exactly as a relay would after a restart. The mark has to reject it.
        final int[] seen = new int[1];
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                seen[0]++;
                return true;
            }
        });
        Continuity.deliver(payloadOnly);
        flushSerialCalls();

        assertEquals(0, seen[0],
                "a payload-only state that was already applied was delivered a second time");
    }

    /**
     * A relay holds ONE document per user, so a GET started while a POST is on the wire can lose
     * the other device's state outright: the POST replaces it, and the GET then reads back this
     * device's own echo, which admit() correctly drops. startPublisher() already defers behind an
     * active fetch for this reason; the poll had no matching guard, so the rule held in one
     * direction only.
     */
    @EdtTest
    public void aPollDoesNotOverlapAPublishInFlight() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        final GatedRelay r = new GatedRelay();
        Continuity.setRelay(r);

        // A checkpoint puts a POST on the wire and the relay holds it there.
        Continuity.checkpoint();
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitEntered();
            }
        });

        // A DELTA, not an absolute count: setRelay() polls on installation, so a fetch has
        // already been and gone by the time the publish is on the wire. What must not happen is
        // another one starting now.
        final int before = r.fetches();

        // The application reconnects and polls while that POST is still in flight.
        Continuity.pollRelay();
        // Given time to happen before it is declared absent. startPoll() spawns a worker, so
        // reading the count on the very next line races it: the assertion passed whether or not a
        // fetch had been wrongly started, which is no assertion at all. The positive signal below
        // is what makes this absence mean something.
        pause(400L);
        assertEquals(before, r.fetches(),
                "a fetch was started while a publish was on the wire, so the POST can overwrite "
                        + "the other device's state before the GET reads it");

        // Released, the deferred poll runs.
        r.release();
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitFetched(before + 1);
            }
        });
        assertTrue(r.fetches() > before,
                "the deferred poll was dropped rather than run afterwards");
    }

    /**
     * A route-only state whose routes this build no longer registers applies nothing. Writing it
     * over the stored checkpoint destroyed the user's own restorable position, and acknowledging
     * it stopped the relay offering it again -- so the next launch found only the unusable state.
     */
    @EdtTest
    public void anUnrestorableStateDoesNotReplaceTheCheckpoint() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);

        // The user's own checkpoint, which must survive.
        Continuity.routeStackChanged();
        Continuity.checkpoint();
        long mine = Continuity.getRestorableState().getSequence();

        // A foreign state naming a route this build does not have, and carrying no payload.
        AppState foreignState = new AppState()
                .setDeviceId("some-other-device")
                .setSequence(77L)
                .setTimestamp(System.currentTimeMillis());
        List<String> unknown = new ArrayList<String>();
        unknown.add("/a-route-this-build-does-not-register");
        foreignState.setRoutes(unknown);

        assertFalse(Continuity.restore(foreignState), "an unknown route cannot show a form");

        AppState stored = Continuity.getRestorableState();
        assertNotNull(stored, "the local checkpoint was destroyed by a state that applied nothing");
        assertEquals(mine, stored.getSequence(),
                "the unusable foreign state replaced the user's own checkpoint");
    }

    /**
     * Automatic restoration must not write the durable mark when the restore itself declined to.
     * admit() has already put the sequence in the live map, so persisting the map behind
     * commit()'s back marks a state whose checkpoint never stored -- and after a restart enable()
     * reloads that mark and refuses the relay's only recoverable copy.
     */
    @EdtTest
    public void anAutoRestoreWhoseWriteFailedIsNotDurablyMarked() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);

        AppState arrival = fromElsewhere("unstorable auto", 31L);
        Storage original = Storage.getInstance();
        Storage.setStorageInstance(new RefusingStorage());
        try {
            Continuity.deliver(arrival);
            flushSerialCalls();
        } finally {
            Storage.setStorageInstance(original);
        }

        // What the next launch would load. The mark must not be there.
        Map<String, Long> persisted = Continuity.readSeenForTest();
        assertFalse(persisted.containsKey("some-other-device"),
                "a state whose checkpoint never stored was durably marked handled, so the relay's "
                        + "only copy is refused after a restart");
    }

    /**
     * The high-water map is bounded where entries go IN, not only where they are written out.
     * Trimming the serialization copy alone left the live map growing for the life of the
     * process, and made every acknowledgement copy and rescan it -- memory and CPU both climbing
     * with a relay that supplies many device ids.
     */
    @EdtTest
    public void theLiveHighWaterMapIsBounded() {
        Continuity.enable();

        // Comfortably past the cap, all from distinct devices.
        for (int i = 0; i < 200; i++) {
            AppState s = new AppState()
                    .setDeviceId("device-" + i)
                    .setSequence(i + 1)
                    .setTimestamp(System.currentTimeMillis());
            Continuity.deliver(s);
        }
        flushSerialCalls();

        assertTrue(Continuity.seenSizeForTest() <= 64,
                "the live map grew past its cap: " + Continuity.seenSizeForTest());
    }

    /**
     * A provider that throws is an attempt that FAILED, not an absence of work. It happens
     * transiently -- a dependency that is not up yet on a cold launch is the ordinary cause --
     * and marking the state handled with none of its payload applied and nothing stored left the
     * relay's remaining copy refused after the next launch, so the state was gone for good.
     *
     * <p>The distinction matters because "no provider at all" must still acknowledge, or a state
     * an application can never consume re-prompts for ever. Same flag once, two opposite right
     * answers, which is why there are two now.</p>
     */
    @EdtTest
    public void aProviderThatThrowsLeavesTheStateOnTheRelay() {
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                return new HashMap<String, Object>();
            }

            public void restoreState(Map<String, Object> payload) {
                throw new IllegalStateException("a dependency is not up yet");
            }
        });

        AppState arrival = fromElsewhere("provider blew up", 41L);
        assertFalse(Continuity.restore(arrival), "a route-less state shows no form");

        Map<String, Long> persisted = Continuity.readSeenForTest();
        assertFalse(persisted.containsKey("some-other-device"),
                "a state whose provider threw was marked handled, so the relay's remaining copy "
                        + "is refused and none of its payload was ever applied");
    }

    /**
     * A brand new device is not the one to evict. Sequences are each origin's own counter, so a
     * device that has just been set up and sent its first state carries the LOWEST number in the
     * map -- and evicting by sequence therefore threw that entry out the moment it was admitted.
     * The dispatch queued behind admit() then found no mark of its own and dropped a perfectly
     * good continuation with nothing logged.
     */
    @EdtTest
    public void aBrandNewDeviceIsNotEvictedByItsOwnArrival() {
        Continuity.enable();
        final int[] seen = new int[1];
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                seen[0]++;
                return true;
            }
        });

        // Fill the map with established devices, all counting far higher than a new one would.
        for (int i = 0; i < 64; i++) {
            AppState old = new AppState()
                    .setDeviceId("established-" + i)
                    .setSequence(5000 + i)
                    .setTimestamp(System.currentTimeMillis());
            Continuity.deliver(old);
        }
        flushSerialCalls();
        seen[0] = 0;

        // A device unboxed this morning, sending its first ever state.
        AppState firstEver = fromElsewhere("hello from a new phone", 1L);
        Continuity.deliver(firstEver);
        flushSerialCalls();

        assertEquals(1, seen[0],
                "a new device's first state was evicted by its own admission and never dispatched");
    }

    /**
     * And the cap is enforced whatever the sequences are. The eviction this replaced scanned for
     * the lowest value starting from Long.MAX_VALUE, so a map whose values all equalled
     * Long.MAX_VALUE selected nothing and quietly stopped bounding anything at all.
     */
    @EdtTest
    public void theCapHoldsEvenWhenEverySequenceIsMaxValue() {
        Continuity.enable();
        for (int i = 0; i < 80; i++) {
            AppState s = new AppState()
                    .setDeviceId("maxed-" + i)
                    .setSequence(Long.MAX_VALUE)
                    .setTimestamp(System.currentTimeMillis());
            Continuity.deliver(s);
        }
        flushSerialCalls();

        assertTrue(Continuity.seenSizeForTest() <= 64,
                "the cap stopped being enforced: " + Continuity.seenSizeForTest());
    }

    /**
     * A state that was admitted but never completed must not become durable on the back of an
     * unrelated one. lastSeen holds every admitted state so a run does not dispatch the same
     * thing twice; serializing that whole map meant a later, successful state carried the failed
     * one to disk, and after a restart the relay's only usable copy was refused -- undoing, from
     * the writer, exactly the gating commit() performs.
     */
    @EdtTest
    public void aFailedStateIsNotMadeDurableByAnUnrelatedSuccess() {
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                return new HashMap<String, Object>();
            }

            public void restoreState(Map<String, Object> payload) {
                if (payload.containsKey("boom")) {
                    throw new IllegalStateException("cannot apply this one");
                }
            }
        });

        // DELIVERED, not restored directly: admission is what puts the state in the in-memory
        // dedup map, and that is the precondition -- a state sitting in memory, never completed.
        // Restoring it by hand skips admit() entirely, so the map never holds it and the test
        // asserts about a situation that cannot arise.
        Map<String, Object> boom = new HashMap<String, Object>();
        boom.put("boom", Boolean.TRUE);
        AppState failing = new AppState().setPayload(boom).setDeviceId("device-b")
                .setSequence(7L).setTimestamp(System.currentTimeMillis());
        Continuity.deliver(failing);
        flushSerialCalls();

        // A, from another device, then completes normally and writes the marks out.
        Continuity.deliver(fromElsewhere("fine", 3L));
        flushSerialCalls();

        Map<String, Long> persisted = Continuity.readSeenForTest();
        assertTrue(persisted.containsKey("some-other-device"),
                "the state that completed should have been marked");
        assertFalse(persisted.containsKey("device-b"),
                "a state that failed to apply was made durable by an unrelated success, so the "
                        + "relay's only usable copy is refused after a restart");
    }

    /**
     * Device ids are not all ours. setDeviceId is public and a state arrives carrying whatever
     * the relay was given, so an id can contain the characters the persisted format is delimited
     * by. Unescaped, "phone|work" produced a sequence field that would not parse, and a semicolon
     * produced a whole second entry -- a mark against an origin that never sent anything, which
     * then suppresses that origin's real states for good.
     */
    @EdtTest
    public void aDeviceIdContainingTheDelimitersSurvivesARestart() {
        Continuity.setStateProvider(new RecordingProvider());

        String awkward = "phone|work;other";
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("note", "hello");
        Continuity.restore(new AppState().setPayload(payload).setDeviceId(awkward)
                .setSequence(12L).setTimestamp(System.currentTimeMillis()));

        Map<String, Long> persisted = Continuity.readSeenForTest();
        assertEquals(Long.valueOf(12L), persisted.get(awkward),
                "the id did not round-trip through the persisted map: " + persisted.keySet());
        assertFalse(persisted.containsKey("other"),
                "the id's semicolon invented a mark for an origin that never sent anything");
    }

    /**
     * The continuation label is the previous user's, and clear() is the logout path. Withdrawing
     * the advertised activity is not enough: the label is a field that outlives it, so the first
     * checkpoint afterwards -- a login screen, or the next account's opening route -- published
     * it again to every device around them.
     */
    @EdtTest
    public void logoutForgetsTheContinuationLabel() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        Continuity.setTitle("Invoice 2031 for Dana");
        Continuity.routeStackChanged();
        Continuity.checkpoint();

        Continuity.clear();

        assertNull(Continuity.getTitle(), "the previous account's label survived the logout");

        // And the next account's first checkpoint must not carry it either.
        Continuity.routeStackChanged();
        Continuity.checkpoint();
        assertNull(bridge.getPublishedTitle(),
                "the first checkpoint after logout re-advertised the previous user's label");
    }

    /**
     * A checkpoint whose write failed is still owed. `dirty` is cleared on the way in, so leaving
     * it clear told the next suspend there was nothing to save -- a checkpoint lost to a full
     * disk was never retried and the app came back to the last write that had succeeded.
     */
    @EdtTest
    public void aFailedCheckpointWriteLeavesTheStateOwed() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);

        Storage original = Storage.getInstance();
        Storage.setStorageInstance(new RefusingStorage());
        try {
            Continuity.routeStackChanged();
            Continuity.checkpoint();
            assertTrue(Continuity.isCheckpointPending(),
                    "a checkpoint whose write failed was reported as saved");
        } finally {
            Storage.setStorageInstance(original);
        }
    }

    /**
     * And a restore whose write failed must not acknowledge the state. noteActedOn() is durable
     * and stops the relay ever offering it again, so doing it on top of a failed write loses the
     * state in both directions at once -- nothing stored here, nothing left to fetch.
     */
    @EdtTest
    public void aFailedRestoreWriteDoesNotAcknowledgeTheState() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);

        AppState arrival = fromElsewhere("unstorable", 21L);
        Storage original = Storage.getInstance();
        Storage.setStorageInstance(new RefusingStorage());
        try {
            Continuity.restore(arrival);
        } finally {
            Storage.setStorageInstance(original);
        }

        // The relay would offer it again. It has to be accepted, not refused as already handled.
        final int[] seen = new int[1];
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                seen[0]++;
                return true;
            }
        });
        Continuity.deliver(arrival);
        flushSerialCalls();

        assertEquals(1, seen[0],
                "a state whose write failed was marked handled, so it can never be recovered");
    }

    /** Storage whose writes always fail, which is what a full disk looks like. */
    static class RefusingStorage extends Storage {
        @Override
        public boolean writeObject(String name, Object o) {
            return false;
        }
    }

    /**
     * An applied state has to reach local storage, and a payload-only one is the case that proves
     * it. noteActedOn() is durable -- once it runs the relay's copy is refused for good -- so a
     * state that was acknowledged and never written is lost outright if the process dies before
     * anything else checkpoints. An app that does not use @Route has nothing else that
     * checkpoints, which is exactly the app this shape of state belongs to.
     */
    @EdtTest
    public void aPayloadOnlyRestoreIsWrittenToStorage() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);

        AppState payloadOnly = fromElsewhere("stored payload", 12L);
        payloadOnly.setRoutes(new ArrayList<String>());

        assertFalse(Continuity.restore(payloadOnly), "a route-less state shows no form");

        // What the next cold start would find: nothing is parked, so this is storage answering.
        AppState stored = Continuity.getRestorableState();
        assertNotNull(stored,
                "the applied payload-only state was acknowledged but never written to storage");
        assertEquals("stored payload", stored.getPayload().get("note"),
                "storage holds a different state than the one that was applied");
    }

    /**
     * And the parked slot is released on application, not on a form appearing. Gating it on the
     * return value kept a payload-only arrival parked for ever, so every restore() re-applied it.
     */
    @EdtTest
    public void aPayloadOnlyParkedStateIsNotOfferedTwice() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(false);

        AppState payloadOnly = fromElsewhere("parked payload", 9L);
        payloadOnly.setRoutes(new ArrayList<String>());
        Continuity.deliver(payloadOnly);
        flushSerialCalls();

        assertFalse(Continuity.restore(), "a route-less state shows no form");

        // Storage legitimately holds it now -- an applied state IS the local checkpoint, which is
        // what the next cold start should come back to -- so asking getRestorableState() alone
        // cannot tell the parked slot from the stored copy. Remove the stored copy and ask again:
        // whatever answers now can only be the parked slot, and it has to be empty.
        Storage.getInstance().deleteStorageFile(Continuity.STORAGE_KEY);
        AppState left = Continuity.getRestorableState();
        assertNull(left, "the parked state was applied and must not still be waiting");
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
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitEntered();
            }
        });
        final long inFlight = Continuity.getRestorableState().getSequence();

        // Queued behind the request the worker is holding.
        provider.saved.put("n", Integer.valueOf(2));
        Continuity.checkpoint();
        long queued = Continuity.getRestorableState().getSequence();
        assertTrue(queued > inFlight, "the second checkpoint did not advance the sequence");

        Continuity.clear();
        r.release();
        // The positive signal FIRST: the worker got past the gate and finished the request it was
        // holding. Asserting the absence of `queued` before that proved nothing at all.
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitSent(inFlight);
                r.settle();
            }
        });

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

        private final java.util.concurrent.atomic.AtomicInteger fetched =
                new java.util.concurrent.atomic.AtomicInteger();

        public AppState fetch() {
            fetched.incrementAndGet();
            return null;
        }

        /** How many GETs have actually reached the endpoint. */
        int fetches() {
            return fetched.get();
        }

        /** Waits until at least `count` GETs have run, so a deferred poll can be seen to land. */
        void awaitFetched(int count) {
            long deadline = System.currentTimeMillis() + 5000L;
            while (System.currentTimeMillis() < deadline && fetched.get() < count) {
                sleepBriefly();
            }
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

        /// Waits until `sequence` has been sent -- a POSITIVE signal that the worker resumed.
        ///
        /// The test that uses this asserts an ABSENCE (the state queued before logout must not go
        /// out), and an absence asserted too early is not evidence of anything: the publish simply
        /// had not happened yet. A bare sleep gave exactly that, so the test could pass without
        /// the code under it ever running.
        void awaitSent(long sequence) {
            long deadline = System.currentTimeMillis() + 5000L;
            while (System.currentTimeMillis() < deadline) {
                if (sent.contains(Long.valueOf(sequence))) {
                    return;
                }
                sleepBriefly();
            }
        }

        /// A bounded pause after the positive signal, so a worker that WOULD take the next state
        /// has had its chance. A bound, not a proof -- but the proof is the assertion above it.
        void settle() {
            sleepBriefly();
            sleepBriefly();
        }

        private void sleepBriefly() {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Records the sequence of everything the relay is handed, slowly enough to overlap. */
    static class OrderRecordingRelay implements StateRelay {
        final List<Long> published =
                java.util.Collections.synchronizedList(new ArrayList<Long>());

        public void publish(AppState state) {
            try {
                Thread.sleep(15);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            published.add(Long.valueOf(state.getSequence()));
        }

        public AppState fetch() {
            return null;
        }

        /// Waits until `sequence` has actually been published.
        ///
        /// NOT "until the relay goes quiet", which is what this did and why it failed about one
        /// run in five. Quiet is not finished: the publisher coalesces while the EDT is still
        /// checkpointing, so a gap longer than the idle window happens naturally on a loaded
        /// machine and was read as settled -- the assertions then ran against a half-delivered
        /// list and reported the relay's last value as an older checkpoint. Waiting for the
        /// condition the test actually asserts is the only version of this that cannot lie.
        void awaitPublished(long sequence) {
            long deadline = System.currentTimeMillis() + 10000L;
            while (System.currentTimeMillis() < deadline) {
                synchronized (published) {
                    if (!published.isEmpty()
                            && published.get(published.size() - 1).longValue() >= sequence) {
                        return;
                    }
                }
                try {
                    Thread.sleep(25);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
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
