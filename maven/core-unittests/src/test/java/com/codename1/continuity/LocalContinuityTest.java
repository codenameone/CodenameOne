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

import com.codename1.continuity.spi.ContinuityCallback;
import com.codename1.continuity.sync.SyncedStore;
import com.codename1.continuity.sync.SyncedStoreListener;
import com.codename1.impl.continuity.LocalContinuityBridge;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.Storage;
import com.codename1.io.rest.RequestBuilder;
import com.codename1.io.rest.Rest;
import com.codename1.junit.EdtTest;
import com.codename1.router.Navigation;
import com.codename1.router.RouteDispatcher;
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
import static org.junit.jupiter.api.Assertions.fail;

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
        // The delivery high-water marks are DURABLE by design, so they outlive reset() -- which
        // is the whole point of them, and which makes them leak from one test into the next
        // unless each starts from a clean slate. clearStorage() above now covers them: they moved
        // out of Preferences, which cannot report a failed write, and into Storage, which can.
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
     * The marks stay writable when the device ids are long.
     *
     * <p>MAX_SEEN bounds the COUNT, and that is not the bound storage imposes. The whole map goes
     * out as ONE string, written as modified UTF-8 with a length that stops at 65535 bytes, while
     * a device id is only checked against that limit one at a time on its way into an AppState --
     * and ids arrive from other devices. A handful of large ones therefore make a combined string
     * no write can hold.</p>
     *
     * <p>The failure is the quiet kind, which is why it is asserted on the PERSISTED map rather
     * than the live one: this run keeps acknowledging correctly from memory, and nothing reaches
     * the disk, so after every restart the relay offers an already-applied state again and its
     * side effects run a second time.</p>
     */
    @EdtTest
    public void longDeviceIdsDoNotStopTheMarksFromBeingStored() {
        // A provider and a payload, because a mark only becomes durable once the state has been
        // APPLIED -- commit() writes nothing for a state that restored nothing. A first version
        // of this test delivered bare states and asserted on a file nothing had written yet, and
        // it failed identically with SHORT ids, which is what showed the fixture was wrong rather
        // than the code under test.
        Continuity.setStateProvider(new RecordingProvider());

        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < 4000; i++) {
            pad.append('d');
        }
        // Forty of these is roughly 160KB of ids, so the combined string is far past what one
        // stored string can hold while every id on its own is comfortably legal.
        String newest = null;
        for (int i = 0; i < 40; i++) {
            newest = "device-" + i + "-" + pad;
            Map<String, Object> payload = new HashMap<String, Object>();
            payload.put("note", "from " + i);
            Continuity.deliver(new AppState()
                    .setPayload(payload)
                    .setDeviceId(newest)
                    .setSequence(i + 1)
                    .setTimestamp(System.currentTimeMillis()));
            flushSerialCalls();
        }

        Map<String, Long> persisted = Continuity.readSeenForTest();
        assertTrue(persisted.containsKey(newest),
                "the marks could not be written once the ids were long, so the most recent "
                        + "acknowledgement is not durable and that state will be offered again "
                        + "after a restart; persisted=" + persisted.size());
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
     * A payload the provider took is real work even when every route in the same state is stale.
     * Treating the route failure as fatal discarded it twice: never written to the local
     * checkpoint, so a cold start lost it, and never acknowledged, so the relay offered the same
     * half-usable state after every restart -- re-applying the payload and failing the same
     * routes each time. The routes will not start working on the next launch; the payload
     * already worked on this one.
     */
    @EdtTest
    public void anAppliedPayloadSurvivesStaleRoutesInTheSameState() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("note", "the payload applied fine");
        AppState mixed = new AppState().setPayload(payload).setDeviceId("some-other-device")
                .setSequence(55L).setTimestamp(System.currentTimeMillis());
        List<String> stale = new ArrayList<String>();
        stale.add("/a-route-this-build-does-not-register");
        mixed.setRoutes(stale);

        assertFalse(Continuity.restore(mixed), "no form can be shown for a stale route");

        assertTrue(provider.restored.containsKey("note"),
                "the provider should have been given the payload");
        AppState stored = Continuity.getRestorableState();
        assertNotNull(stored, "the applied payload never reached the local checkpoint");
        assertEquals(Long.valueOf(55L), Long.valueOf(stored.getSequence()),
                "the checkpoint holds a different state than the one that was applied");
        Map<String, Long> persisted = Continuity.readSeenForTest();
        assertTrue(persisted.containsKey("some-other-device"),
                "the state was never acknowledged, so the relay re-offers it after every restart");
    }

    /**
     * A state that was fresh when it landed but expired while waiting for the first form must not
     * be restored. The cold-launch waiter comes back through dispatch() up to WINDOW_WAIT_MILLIS
     * later, past the check in admit() and the one in getRestorableState() -- so an expired
     * checkout or booking hold was auto-restored anyway. That check existed before the
     * event-thread rewrite and the rewrite dropped it.
     *
     * <p>Driven through the parked slot and the waiter's own drain, because the wait cannot be
     * reproduced here: it needs a launch with no form and this harness always has one. An earlier
     * version of this test delivered the same state twice and asserted nothing at all -- the
     * second delivery was refused by the in-memory mark long before it could reach dispatch.</p>
     */
    @EdtTest
    public void aStateThatExpiresWhileParkedIsNotRestored() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        final int[] seen = new int[1];
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                seen[0]++;
                return true;
            }
        });

        // Parked while it was fresh, and older than the limit by the time the waiter drains it.
        AppState aged = fromElsewhere("stale by the time a form appeared", 61L);
        aged.setTimestamp(System.currentTimeMillis() - 5000L);
        Continuity.setMaxAge(1000L);
        Continuity.parkForTest(aged);

        Continuity.drainParkedForTest();
        flushSerialCalls();

        assertEquals(0, seen[0],
                "a state that expired while it was parked was restored anyway");
    }

    /**
     * An empty document is a tombstone, not an offer. An enabled app with no routes and no
     * payload still checkpoints, and the relay holds one document per user, so that empty state
     * is published to overwrite the stale one -- which is the point. It carries a device id and a
     * sequence though, so the receiving side ran the listeners over it: a "continue what you were
     * doing?" prompt about nothing. The platform path already withdrew the activity for an empty
     * state; only the relay half was missing it.
     */
    @EdtTest
    public void anEmptyArrivalIsConsumedRatherThanOffered() {
        Continuity.setStateProvider(new RecordingProvider());
        final int[] seen = new int[1];
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                seen[0]++;
                return true;
            }
        });

        AppState empty = new AppState().setDeviceId("some-other-device").setSequence(71L)
                .setTimestamp(System.currentTimeMillis());
        Continuity.deliver(empty);
        flushSerialCalls();

        assertEquals(0, seen[0],
                "an empty state was offered to the listeners, prompting the user over nothing");
    }

    /**
     * A provider that throws must not replace a stored draft with an empty state. It leaves the
     * state with no payload, and an app with no routes has nothing else in it -- so the write
     * destroyed what was safely stored a moment ago, cleared the pending flag so no later suspend
     * retried, and withdrew the platform continuation, all for a read that may succeed next time.
     */
    @EdtTest
    public void aFailingProviderDoesNotWipeAStoredDraft() {
        final boolean[] blowUp = new boolean[1];
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                if (blowUp[0]) {
                    throw new IllegalStateException("cannot read the draft right now");
                }
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("draft", "half a sentence");
                return m;
            }

            public void restoreState(Map<String, Object> payload) {
            }
        });

        Continuity.checkpoint();
        assertEquals("half a sentence",
                Continuity.getRestorableState().getPayload().get("draft"),
                "the draft should have been stored");

        blowUp[0] = true;
        Continuity.checkpoint();

        AppState stored = Continuity.getRestorableState();
        assertNotNull(stored, "the stored draft was destroyed by a provider that threw");
        assertEquals("half a sentence", stored.getPayload().get("draft"),
                "an empty state was written over the stored draft");
        assertTrue(Continuity.isCheckpointPending(),
                "the failed capture left nothing owed, so no later suspend retries it");
    }

    /*
     * There is deliberately NO test here for setBridge(null) handing resolution back to the
     * platform. The fix covers it -- refreshBridge() resolves override-or-platform and installs
     * the callback on whichever it picks -- but core-unittests has no platform bridge for
     * resolution to find, so nothing observable changes in this harness. A test written for it
     * passed against the BROKEN code too, because the fixture's bridge still held the callback
     * installed at enable(): it asserted nothing and was removed rather than left looking like
     * cover. The sibling below exercises the other half of the same condition, and does catch it.
     */


    /**
     * And a sync-only client gets one too. It installs the inbound seam through
     * SyncedStore.addChangeListener and deliberately leaves continuity off -- a key/value store
     * is not consent to broadcast a route stack -- so gating the callback on `enabled` left it
     * with none, and store changes made on another device never reached its listener.
     */
    @EdtTest
    public void aSyncOnlyClientGetsTheCallbackOnABridgeSwap() {
        final int[] changes = new int[1];
        SyncedStoreListener l = new SyncedStoreListener() {
            public void storeChanged() {
                changes[0]++;
            }
        };
        registered.add(l);
        SyncedStore.addChangeListener(l);
        assertFalse(Continuity.isEnabled(),
                "registering a store listener must not enable continuity");

        LocalContinuityBridge replacement = new LocalContinuityBridge();
        Continuity.setBridge(replacement);

        replacement.simulateStoreChange();
        flushSerialCalls();

        assertEquals(1, changes[0],
                "a sync-only client got no callback on the replacement bridge");
    }

    /**
     * A checkpoint must not overwrite the relay's copy of an arrival the user is still deciding
     * about. The relay holds one document per user, and a parked state exists ONLY in memory --
     * so publishing replaces the last copy of it that exists anywhere, and a process death while
     * the prompt is on screen loses it outright.
     */
    @EdtTest
    public void aCheckpointWaitsWhileAnArrivalIsParked() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(false);
        final GatedRelay r = new GatedRelay();
        Continuity.setRelay(r);
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitEntered();
            }
        });
        r.release();
        pause(300L);
        final int before = r.sent.size();

        // An arrival the application has been asked about and has not answered.
        Continuity.deliver(fromElsewhere("waiting on the user", 91L));
        flushSerialCalls();
        assertNotNull(Continuity.getRestorableState(), "the arrival should be parked");

        Continuity.checkpoint();
        pause(300L);
        assertEquals(before, r.sent.size(),
                "a checkpoint overwrote the relay's only copy of a state the user was still "
                        + "being asked about");

        // Answering releases it -- held, not dropped.
        //
        // acknowledge(), not restore(). Both are decisions, and only one of them leaves the held
        // checkpoint still true: restoring REPLACES the screen it describes, so sending it
        // afterwards would put the superseded work over the relay's copy of the state just
        // accepted, and it is dropped on purpose --
        // aCheckpointQueuedBeforeARestoreIsNotPublishedAfterIt covers that. Acknowledging changes
        // nothing on screen, so the work captured while the user was being asked is still what
        // this device is doing, and it has to go out or the hold is a place things vanish into.
        Continuity.acknowledge(Continuity.getRestorableState());
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitAnySince(before);
            }
        });
        assertTrue(r.sent.size() > before,
                "the held publication was dropped rather than sent once the decision was made");
    }

    /**
     * A mark that never reached storage must not be reported as durable. Preferences cannot say
     * whether a write landed -- set() fills a static table and save() discards
     * Storage.writeObject()'s result -- so these values moved to Storage, which can.
     */
    @EdtTest
    public void marksThatCannotBeStoredAreNotSilentlyClaimed() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);

        Storage original = Storage.getInstance();
        Storage.setStorageInstance(new RefusingStorage());
        try {
            Continuity.acknowledge(fromElsewhere("cannot be stored", 95L));
        } finally {
            Storage.setStorageInstance(original);
        }

        Map<String, Long> persisted = Continuity.readSeenForTest();
        assertFalse(persisted.containsKey("some-other-device"),
                "a mark that never reached storage was reported as durable");
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

    /**
     * A sequence that could not be stored must not be handed to another device. The receiver
     * records it durably, this device hands the same number out again after a restart, and every
     * checkpoint it then sends is refused as already seen until the counter climbs past it.
     *
     * <p>The local write still happens: this device does not deduplicate against itself, so the
     * stored checkpoint is worth having. Only the PUBLISHING is harmful, which is why the payload
     * failure beside it -- where publishing is fine -- cannot share the same flag.</p>
     */
    @EdtTest
    public void aSequenceThatCannotBeStoredIsNotPublished() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        final GatedRelay r = new GatedRelay();
        Continuity.setRelay(r);
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitEntered();
            }
        });
        r.release();
        pause(300L);
        final int before = r.sent.size();
        bridge.clearContinuation();

        Storage original = Storage.getInstance();
        Storage.setStorageInstance(new RefusingOneStorage(original, Continuity.PREF_SEQUENCE));
        try {
            // NOT routeStackChanged(): that only schedules a flush, and the queued checkpoint
            // then ran after the finally below had put the real storage back -- so the test was
            // watching a second, perfectly successful checkpoint publish and calling it a
            // failure of the first.
            Continuity.checkpoint();
            flushSerialCalls();
        } finally {
            Storage.setStorageInstance(original);
        }
        pause(300L);

        assertEquals(before, r.sent.size(),
                "a sequence that never reached storage was published to the relay, so the "
                        + "receiver's durable mark will outlive the counter that produced it");
        assertNull(bridge.getPublishedType(),
                "the same sequence was advertised over the platform continuation");
        assertTrue(Continuity.isCheckpointPending(),
                "the checkpoint was reported as done even though the counter is not durable");
    }

    /**
     * The public capture() is documented for feeding the application's own transport, so it is a
     * publisher too -- and a state whose sequence never reached storage is exactly what must not
     * be published. The caller cannot tell: nothing on AppState says whether its number is one
     * this device will issue again after a restart.
     *
     * <p>The control half matters more than the failing half here. Returning null unconditionally
     * would satisfy the assertion below while breaking the method, so the same call is made with
     * working storage first and required to produce a state.</p>
     */
    @EdtTest
    public void aCaptureWhoseSequenceCannotBeStoredIsRefused() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);

        AppState healthy = Continuity.capture();
        assertNotNull(healthy,
                "capture() returned nothing with storage working, so the refusal below proves "
                        + "nothing about the sequence");

        Storage original = Storage.getInstance();
        Storage.setStorageInstance(new RefusingOneStorage(original, Continuity.PREF_SEQUENCE));
        AppState refused;
        try {
            refused = Continuity.capture();
        } finally {
            Storage.setStorageInstance(original);
        }

        assertNull(refused,
                "capture() handed out a state whose sequence never reached storage, and the "
                        + "caller publishes it -- so the receiver's durable mark outlives the "
                        + "counter that produced it and later states are silently ignored");
    }

    /**
     * A poll that brings back a state nobody has dealt with yet must not release a queued publish.
     *
     * <p>The publisher's hold on a parked arrival exists because a parked state's only copy is on
     * the relay, and publishing replaces that single document. admit() deliberately queues the
     * dispatch for a LATER turn -- that second turn is what lets an older state notice it was
     * superseded -- so when the poll finished in the same turn, {@code parked} was still null and
     * the hold had nothing to see. The worker it started never looks again.</p>
     */
    @EdtTest
    public void aFetchedStateNobodyHasHandledYetHoldsTheQueuedPublish() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        // Nothing applies the arrival, so it parks -- the state the hold is for.
        Continuity.setAutoRestore(false);

        final AppState waiting = fromElsewhere("fetched, unhandled", 91L);
        final java.util.concurrent.CountDownLatch inFetch =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release =
                new java.util.concurrent.CountDownLatch(1);
        Continuity.setRelay(new StateRelay() {
            public void publish(AppState state) {
                published.add(state);
            }

            public AppState fetch() {
                if (!served.getAndSet(false)) {
                    return null;
                }
                // Held open so the checkpoint below is queued WHILE the poll is running, which is
                // the situation the finding is about: work owed to the relay, and a state coming
                // back that nobody has looked at yet.
                inFetch.countDown();
                try {
                    release.await(2L, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                return waiting;
            }
        });

        awaitOffEdt(new Runnable() {
            public void run() {
                try {
                    inFetch.await(2L, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        // Owed while the poll is in flight, so pollFinished() is what releases it.
        Continuity.checkpoint();
        release.countDown();

        // Two turns: the one pollFinished() runs in, and the one it queues the dispatch into.
        pause(400L);
        flushSerialCalls();
        pause(200L);
        flushSerialCalls();

        assertNotNull(Continuity.getRestorableState(),
                "the fetched state never parked, so this test is not about the hold at all");
        assertTrue(published.isEmpty(),
                "a checkpoint was published over the relay's only copy of a state the "
                        + "application has not dealt with yet: published=" + published.size());

        // And it was genuinely HELD, not simply never owed. Acknowledging the arrival is what
        // releases the hold, so a publish arriving now is the proof that one was waiting -- and
        // without it the assertion above would pass just as well on a relay nothing ever wanted
        // to write to, which is how the first version of this test proved nothing.
        Continuity.acknowledge(waiting);
        pause(300L);
        flushSerialCalls();
        assertFalse(published.isEmpty(),
                "no publish followed the acknowledgement, so nothing was ever owed to the relay "
                        + "and the empty check above was vacuous");
    }

    /** What the fetch above hands over, once. */
    private final java.util.concurrent.atomic.AtomicBoolean served =
            new java.util.concurrent.atomic.AtomicBoolean(true);

    /** What that relay was asked to publish. */
    private final List<AppState> published =
            java.util.Collections.synchronizedList(new ArrayList<AppState>());

    /**
     * Logout has to leave nothing restorable even when the port refuses to delete the file.
     *
     * <p>{@code deleteStorageFile} returns void and the ports behind it discard the answer they do
     * get -- JavaSE ignores {@code File.delete()}'s boolean, Android ignores
     * {@code Context.deleteFile()}'s -- so a refused deletion is invisible and leaves the
     * signed-out account's routes and payload on disk, ready to be restored into the next
     * login.</p>
     */
    @EdtTest
    public void aLogoutLeavesNothingRestorableEvenWhenTheDeleteIsRefused() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("secret", "the previous account's work");
        Continuity.setStateProvider(provider);
        Continuity.checkpoint();
        assertNotNull(Continuity.getRestorableState(),
                "there is no checkpoint to lose, so the assertion below would pass on nothing");

        Storage original = Storage.getInstance();
        Storage.setStorageInstance(new UndeletableStorage(original));
        try {
            Continuity.clear();
        } finally {
            Storage.setStorageInstance(original);
        }

        assertNull(Continuity.getRestorableState(),
                "logout left the previous account's checkpoint on disk, so a restart restores "
                        + "one account's routes and payload into the next account's session");
    }

    /** Storage whose deleteStorageFile does nothing at all, silently, as a refusing port does. */
    static class UndeletableStorage extends Storage {
        private final Storage delegate;

        UndeletableStorage(Storage delegate) {
            this.delegate = delegate;
        }

        @Override
        public void deleteStorageFile(String name) {
            // Deliberately nothing. This is the port behaviour under test: the entry survives and
            // the caller is told nothing, because the method cannot tell it anything.
        }

        @Override
        public boolean writeObject(String name, Object o) {
            return delegate.writeObject(name, o);
        }

        @Override
        public Object readObject(String name) {
            return delegate.readObject(name);
        }

        @Override
        public boolean exists(String name) {
            return delegate.exists(name);
        }
    }

    /**
     * A relay that is no longer installed refuses on the credential path, before it reads a token.
     *
     * <p>The publish worker confirms its session on the event thread before calling a relay, and
     * that leaves one gap: the worker is a different thread, so between the confirmation and the
     * relay reading its token a logout and a login can both have happened.
     * {@code RestStateRelay.getToken()} is read at each request by design, so an object kept
     * across both would answer with whoever is signed in NOW -- and the previous account's state
     * would go out under the next account's credentials.</p>
     *
     * <p>What closes it is installing the new account's relay, which is the documented way to
     * change accounts: the replaced object is refused for good. An application that instead keeps
     * one relay and swaps the token inside it is beyond what any framework check can see, and
     * getToken() says so.</p>
     *
     * <p>No server is involved: the refusal is the first thing {@code auth} does, so reaching the
     * network at all would be the failure.</p>
     */
    @EdtTest
    public void aRelayThatIsNoLongerInstalledRefusesBeforeReadingItsToken() {
        final boolean[] tokenRead = new boolean[1];
        RestStateRelay relay = new RestStateRelay("https://example.invalid/continuity") {
            @Override
            protected String getToken() {
                tokenRead[0] = true;
                return "the-next-account-token";
            }
        };

        Continuity.setRelay(relay);
        // The control: while it IS installed the guard must not fire, or the assertion below
        // would pass against a relay that simply never works.
        assertTrue(Continuity.isInstalledRelay(relay),
                "the relay was not installed, so the refusal below proves nothing");

        // setRelay() starts a poll, and THAT read is entitled to a token -- the relay is
        // installed. Let it finish and forget it, because the property under test is what happens
        // after the relay is replaced. Without this the test raced its own fixture: the worker
        // sometimes reached getToken() before the assertion and sometimes did not, so it passed
        // for a timing reason rather than a behavioural one.
        pause(300L);
        flushSerialCalls();
        tokenRead[0] = false;

        // REPLACED, not cleared. clear() is a logout and deliberately keeps the relay installed:
        // the same endpoint usually serves the next account. What the framework can recognise is
        // a relay that is no longer the installed one, which is why switching accounts means
        // installing the new account's relay rather than swapping a token inside the old object.
        Continuity.setRelay(new StateRelay() {
            public void publish(AppState state) {
            }

            public AppState fetch() {
                return null;
            }
        });

        try {
            relay.publish(new AppState().setDeviceId("previous-account").setSequence(1L));
            fail("a relay that setRelay() replaced must not send anything");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("may not send"),
                    expected.getMessage());
        }
        assertFalse(tokenRead[0],
                "the token was read before the relay noticed it had been removed, which is the "
                        + "credential the previous account's state would have gone out under");
    }

    /**
     * A restore that FAILED keeps the parked state, and keeps the publication held with it.
     *
     * <p>restore(AppState) deliberately does not acknowledge a failed attempt -- a provider that
     * throws is usually transient, a dependency not up yet on a cold launch -- so the state stays
     * on the relay for a launch that can use it. The no-argument restore() cleared the slot
     * anyway, and both halves of that hurt: admit() has already put the sequence in the live map,
     * so nothing offers the state again this run, and releasing the hold lets a checkpoint
     * overwrite the relay's only copy. The retry it was being kept for has nothing left to
     * retry.</p>
     */
    @EdtTest
    public void aFailedRestoreKeepsTheParkedState() {
        Continuity.setAutoRestore(false);
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                return new HashMap<String, Object>();
            }

            public void restoreState(Map<String, Object> payload) {
                throw new IllegalStateException("the dependency this needs is not up yet");
            }
        });

        AppState arrival = fromElsewhere("work worth keeping", 41L);
        arrival.setRoutes(new ArrayList<String>());
        Continuity.deliver(arrival);
        flushSerialCalls();
        assertNotNull(Continuity.getRestorableState(),
                "the arrival never parked, so there is no slot for the restore to lose");

        assertFalse(Continuity.restore(), "a provider that threw cannot have shown anything");

        assertNotNull(Continuity.getRestorableState(),
                "a restore that failed threw away the only copy it was keeping: nothing offers "
                        + "the state again this run and the relay's copy is now replaceable");
    }

    /**
     * And the control: a restore that WORKED still releases the slot.
     *
     * <p>Without this the fix above is satisfied by never clearing at all, which would keep a
     * handled arrival on offer for ever and hold every later checkpoint behind it.</p>
     */
    @EdtTest
    public void aSuccessfulRestoreStillReleasesTheParkedState() {
        Continuity.setAutoRestore(false);
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        final List<AppState> out = java.util.Collections.synchronizedList(new ArrayList<AppState>());
        Continuity.setRelay(new StateRelay() {
            public void publish(AppState state) {
                out.add(state);
            }

            public AppState fetch() {
                return null;
            }
        });
        pause(200L);
        flushSerialCalls();
        out.clear();

        AppState arrival = fromElsewhere("work that applies", 42L);
        arrival.setRoutes(new ArrayList<String>());
        Continuity.deliver(arrival);
        flushSerialCalls();

        // Asserted on the HOLD, not on getRestorableState(). A successful restore persists the
        // state, so that method legitimately keeps answering afterwards -- with the stored
        // checkpoint rather than the parked arrival -- and a first version of this test read that
        // as the slot never being released.
        Continuity.checkpoint();
        pause(200L);
        flushSerialCalls();
        assertTrue(out.isEmpty(), "the parked arrival did not hold the checkpoint back at all");

        Continuity.restore();
        pause(300L);
        flushSerialCalls();

        // NOT the checkpoint captured before the restore: that one described the screen the
        // restore replaced, and sending it would overwrite the relay's copy of the state just
        // accepted. It is dropped on purpose. What has to work is the NEXT one -- if the hold
        // had never been released, this would be held too and the arrival would keep every
        // future checkpoint off the relay for good.
        assertTrue(out.isEmpty(), "the stale pre-restore checkpoint was published after all");
        provider.saved.put("after", "work done since the restore");
        Continuity.checkpoint();
        pause(300L);
        flushSerialCalls();

        assertFalse(out.isEmpty(),
                "a checkpoint made after the restore was still held, so the arrival keeps every "
                        + "later checkpoint off the relay for good");
    }

    /**
     * A listener that defers an arrival keeps it parked.
     *
     * <p>False has two documented meanings: "I did the work myself" and "keep it, I will prompt
     * and call restore() when the user accepts". The second is a state waiting on a human whose
     * only other copy is the relay's, and returning without the slot left no hold at all -- so a
     * queued checkpoint could replace that copy while the prompt was still on screen, and a
     * process death before the answer lost the work.</p>
     */
    @EdtTest
    public void aListenerThatDefersAnArrivalKeepsItParked() {
        Continuity.setStateProvider(new RecordingProvider());
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                // "Keep it, I am prompting" -- the documented deferral.
                return false;
            }
        });

        Continuity.deliver(fromElsewhere("waiting on the user", 43L));
        flushSerialCalls();

        AppState offered = Continuity.getRestorableState();
        assertNotNull(offered,
                "a deferred arrival was dropped, so nothing holds a checkpoint off the relay's "
                        + "only copy while the user is being asked about it");
        assertEquals(43L, offered.getSequence(), "a different state was left on offer");

        // And the hold really ends when the application says so, rather than never.
        Continuity.acknowledge(offered);
        assertNull(Continuity.getRestorableState(),
                "acknowledge() did not release the deferred state, which would hold every later "
                        + "checkpoint behind an arrival the application has finished with");
    }

    /**
     * A tagged boolean the encoder never wrote keeps its text instead of becoming false.
     *
     * <p>Boolean.valueOf answers false for every string that is not "true", so "b:unknown"
     * arrived as a confident false -- application data changed in transit, restored, and
     * acknowledged, with nothing said. Every other tag in this codec already preserves a body it
     * cannot parse; this one did not.</p>
     */
    @EdtTest
    public void aTaggedBooleanThatWillNotParseIsNotSilentlyFalse() throws Exception {
        AppState real = StateCodec.fromJson(
                "{\"device\":\"other\",\"seq\":\"3\",\"enc\":\"1\","
                        + "\"payload\":{\"on\":\"b:true\",\"off\":\"b:false\"}}");
        assertEquals(Boolean.TRUE, real.getPayload().get("on"),
                "the encoder's own true did not survive the round trip");
        assertEquals(Boolean.FALSE, real.getPayload().get("off"),
                "the encoder's own false did not survive the round trip");

        AppState odd = StateCodec.fromJson(
                "{\"device\":\"other\",\"seq\":\"3\",\"enc\":\"1\","
                        + "\"payload\":{\"enabled\":\"b:unknown\"}}");
        Object kept = odd.getPayload().get("enabled");
        assertEquals("b:unknown", kept,
                "a boolean body this codec cannot read became " + kept + " instead of keeping "
                        + "its text, so the application is handed a value the sender never sent "
                        + "-- and the state is acknowledged, so the sender never learns of it");
    }

    /**
     * A logout refuses a worker that is already inside the relay, even though the relay stays
     * installed.
     *
     * <p>This is the half the identity check could not see. {@code setRelay()} swaps the object,
     * so a replaced relay was caught -- but {@code clear()} deliberately leaves the SAME relay in
     * place, because the same endpoint usually serves the next account. A worker whose preflight
     * passed a moment before the logout therefore found its relay still installed and sent the
     * previous account's state anyway. With cookie or client-certificate authentication there is
     * not even a token for getToken() to have stopped returning.</p>
     *
     * <p>Asked at the point {@code RestStateRelay.auth()} asks it, on the worker, inside the
     * relay call -- which is the only place the window exists. The first answer is asserted too:
     * a guard that refused every worker would pass the second half of this while breaking every
     * ordinary publish.</p>
     */
    @EdtTest
    public void aLogoutRefusesAWorkerAlreadyInsideTheRelay() {
        final java.util.concurrent.atomic.AtomicInteger asked =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicBoolean beforeLogout =
                new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.concurrent.atomic.AtomicBoolean afterLogout =
                new java.util.concurrent.atomic.AtomicBoolean(true);
        final java.util.concurrent.CountDownLatch inPublish =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch loggedOut =
                new java.util.concurrent.CountDownLatch(1);
        final StateRelay[] self = new StateRelay[1];

        StateRelay relay = new StateRelay() {
            public void publish(AppState state) {
                // Exactly what RestStateRelay.auth() does, in the same place: on the worker,
                // inside the relay call, immediately before the credentials would be read.
                beforeLogout.set(Continuity.mayRelaySend(self[0]));
                asked.incrementAndGet();
                inPublish.countDown();
                try {
                    loggedOut.await(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                afterLogout.set(Continuity.mayRelaySend(self[0]));
                asked.incrementAndGet();
            }

            public AppState fetch() {
                return null;
            }
        };
        self[0] = relay;

        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(false);
        Continuity.setRelay(relay);
        Continuity.checkpoint();

        // Pumped rather than blocked: the worker calls back onto the event thread to read the
        // session, and this test IS the event thread. Blocking here would deadlock the very
        // mechanism under test.
        for (int i = 0; i < 40 && inPublish.getCount() > 0; i++) {
            pause(50L);
            flushSerialCalls();
        }
        assertEquals(0L, inPublish.getCount(),
                "the publish worker never reached the relay, so this test asserts nothing");

        Continuity.clear();
        loggedOut.countDown();
        for (int i = 0; i < 40 && asked.get() < 2; i++) {
            pause(50L);
            flushSerialCalls();
        }
        assertEquals(2, asked.get(), "the worker never asked again after the logout");

        assertTrue(beforeLogout.get(),
                "an ordinary worker was refused before any logout, which would stop every "
                        + "publish this framework makes");
        assertFalse(afterLogout.get(),
                "clear() left the relay installed, so the worker was told it could still send -- "
                        + "and the previous account's state goes out after the logout that "
                        + "promised nothing would");
    }

    /**
     * A sequence past the range of a long is a failed read, and an ordinary one still is not.
     *
     * <p>The pair matters: a guard that refused every numeric sequence would pass the first half
     * of this and silently drop every sender that writes seq as a number rather than a string,
     * which this codec has always accepted and still must.</p>
     */
    @EdtTest
    public void anOutOfRangeSequenceIsRefusedAndAnOrdinaryOneIsNot() throws Exception {
        AppState fine = StateCodec.fromJson("{\"device\":\"other\",\"seq\":10,\"ts\":99}");
        assertNotNull(fine, "a sender writing seq as a plain number was refused");
        assertEquals(10L, fine.getSequence(), "the numeric sequence did not survive");
        assertEquals(99L, fine.getTimestamp(), "the numeric timestamp did not survive");

        // The largest sequence a long holds is itself in range and must go through: the guard is
        // about values OUTSIDE the type, not about large ones.
        AppState edge = StateCodec.fromJson(
                "{\"device\":\"other\",\"seq\":\"" + Long.MAX_VALUE + "\"}");
        assertEquals(Long.MAX_VALUE, edge.getSequence(),
                "a sequence written as the largest long there is was not preserved");

        try {
            StateCodec.fromJson("{\"device\":\"other\",\"seq\":9223372036854775808}");
            fail("2^63 was accepted as a sequence. It is the same double as (double) "
                    + "Long.MAX_VALUE, so a range test written against that constant compares "
                    + "equal and clamps it back to Long.MAX_VALUE -- the exact poisoning the "
                    + "1e100 guard was added to stop, one value past where it looks.");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().length() > 0, "the refusal explained nothing");
        }

        try {
            StateCodec.fromJson("{\"device\":\"other\",\"seq\":1e100}");
            fail("1e100 was accepted as a sequence -- clamped to Long.MAX_VALUE, it becomes this "
                    + "origin's durable high-water mark and refuses everything it sends later");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().length() > 0, "the refusal explained nothing");
        }
    }

    /**
     * A route array whose elements are not strings fails the fetch instead of arriving empty.
     *
     * <p>This is the whole chain, not just the codec: {@code {"routes":[1]}} passed the outer
     * List check, the reader dropped the element it could not use, and what came back was an
     * AppState with no routes and no payload. That is precisely a tombstone -- the shape this
     * framework reads as "the origin cleared its work" -- so a document that merely had one bad
     * element was consumed as an instruction to drop work, and it was marked durably so the
     * correction could never be re-read.</p>
     *
     * <p>The observable here is that durable mark. A refused fetch is an IOException the poll
     * reports, and nothing about the sender is remembered; an admitted tombstone always records
     * one, which is what {@code aConsumedTombstoneIsMarkedDurably} pins down.</p>
     */
    @EdtTest
    public void aFetchWithANonStringRouteIsNotConsumedAsATombstone() {
        Continuity.setStateProvider(new RecordingProvider());
        Continuity.setAutoRestore(false);

        // What the OLD code produced from this document, spelled out so the harm is not taken on
        // trust: an empty state, which isEmpty() -- and therefore admission -- reads as a
        // tombstone.
        AppState routeless = new AppState()
                .setDeviceId("bad-element-sender")
                .setSequence(10L)
                .setTimestamp(System.currentTimeMillis());
        assertTrue(routeless.isEmpty(),
                "an AppState with no routes and no payload is not empty here, so the tombstone "
                        + "consequence this test is about does not exist");

        final String[] doc = {
            "{\"device\":\"bad-element-sender\",\"seq\":\"10\",\"routes\":[1]}"
        };
        Continuity.setRelay(new StateRelay() {
            public void publish(AppState state) {
                published.add(state);
            }

            public AppState fetch() throws java.io.IOException {
                // Exactly what RestStateRelay does with the body it received.
                return StateCodec.fromJson(doc[0]);
            }
        });
        // setRelay() polls on a background thread, so a flush alone proves nothing: the first
        // version of this test asserted before the fetch had run and passed against the unfixed
        // code. Wait for the poll, then drain what it queued.
        pause(300L);
        flushSerialCalls();

        assertNull(Continuity.readSeenForTest().get("bad-element-sender"),
                "a document with a non-string route was admitted and marked durably, so a "
                        + "malformed element was consumed as an instruction to drop work -- and "
                        + "the mark means the sender's correction is refused as already seen");
    }

    /**
     * A consumed tombstone is marked durably.
     *
     * <p>It is the one arrival that cannot fail -- no payload to hand over, no route to rebuild --
     * so there is nothing to gate the mark on. Recording it in memory only meant the next launch
     * had never heard of it, and an older state from the same origin that was already in flight
     * passed admission and offered work the tombstone exists to say no longer exists.</p>
     */
    @EdtTest
    public void aConsumedTombstoneIsMarkedDurably() {
        Continuity.setStateProvider(new RecordingProvider());

        AppState tombstone = new AppState()
                .setDeviceId("some-other-device")
                .setSequence(77L)
                .setTimestamp(System.currentTimeMillis());
        assertTrue(tombstone.isEmpty(), "this is not a tombstone, so the test is about nothing");
        Continuity.deliver(tombstone);
        flushSerialCalls();

        Map<String, Long> persisted = Continuity.readSeenForTest();
        Long mark = persisted.get("some-other-device");
        assertNotNull(mark,
                "the tombstone was consumed without a durable mark, so after a restart an older "
                        + "state still in flight from that origin resurrects the work it cleared");
        assertEquals(77L, mark.longValue(), "the durable mark is not the tombstone's sequence");
    }

    /**
     * A listener that acknowledges inside stateReceived and returns false leaves nothing parked.
     *
     * <p>That is the documented handle-it-yourself pattern, and it does both things: the
     * acknowledgement runs first, while there is nothing parked for it to release, so parking
     * afterwards left a finished arrival on offer for the rest of the process with every relay
     * checkpoint held behind it -- the hold applied to work that was already done.</p>
     */
    @EdtTest
    public void anArrivalAcknowledgedInsideTheListenerIsNotParked() {
        Continuity.setStateProvider(new RecordingProvider());
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                Continuity.acknowledge(state);
                return false;
            }
        });

        Continuity.deliver(fromElsewhere("handled in the callback", 51L));
        flushSerialCalls();

        assertNull(Continuity.getRestorableState(),
                "an arrival the listener had already acknowledged was parked anyway, so it stays "
                        + "on offer and holds every later checkpoint off the relay");
    }

    /**
     * A checkpoint queued before a restore is not sent afterwards.
     *
     * <p>A navigation while a relay GET is in flight leaves that checkpoint in the slot. If the
     * GET brings back a state that is restored, the queued one describes a screen that no longer
     * exists -- and sending it replaces the relay's copy of the state just accepted with the work
     * the restore superseded.</p>
     */
    @EdtTest
    public void aCheckpointQueuedBeforeARestoreIsNotPublishedAfterIt() {
        final RecordingProvider provider = new RecordingProvider();
        provider.saved.put("screen", "one");
        Continuity.setStateProvider(provider);

        // A relay whose FIRST publish blocks. That is what leaves a second checkpoint sitting in
        // the slot, which is the state this test is about -- and it is deterministic, unlike
        // racing a relay GET: an earlier version timed the fetch and passed alone while failing
        // in the suite, because the window it needed was never actually open.
        final java.util.concurrent.CountDownLatch inPublish =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release =
                new java.util.concurrent.CountDownLatch(1);
        Continuity.setRelay(new StateRelay() {
            public void publish(AppState state) {
                published.add(state);
                if (inPublish.getCount() > 0) {
                    inPublish.countDown();
                    try {
                        release.await(5L, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            public AppState fetch() {
                return null;
            }
        });

        Continuity.checkpoint();
        final boolean[] blocked = new boolean[1];
        awaitOffEdt(new Runnable() {
            public void run() {
                try {
                    blocked[0] = inPublish.await(5L, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        assertTrue(blocked[0], "no publish is in flight, so nothing would queue behind it");

        // Queued behind the worker: this is the checkpoint a restore is about to make stale.
        provider.saved.put("stale", Boolean.TRUE);
        Continuity.checkpoint();

        // And the restore that supersedes it.
        Continuity.deliver(fromElsewhere("what the other device was doing", 52L));
        flushSerialCalls();
        assertTrue(provider.restored != null && provider.restored.containsKey("note"),
                "the arrival was not applied, so nothing superseded the queued checkpoint and "
                        + "this test is about nothing");

        release.countDown();
        pause(400L);
        flushSerialCalls();

        boolean staleWentOut = false;
        for (AppState sent : published) {
            if (sent.getPayload().containsKey("stale")) {
                staleWentOut = true;
            }
        }
        assertFalse(staleWentOut,
                "the checkpoint queued before the restore was published over the relay's copy of "
                        + "the state that restore had just accepted");
    }

    /**
     * Logout forgets the route history, not only the stored checkpoint.
     *
     * <p>A route stack is the previous account's work as surely as a checkpoint is. Leaving it
     * kept two promises broken: back() reopened the signed-out account's forms, and the next
     * navigation checkpointed and republished a stack that still began with their routes.</p>
     */
    @EdtTest
    public void logoutForgetsTheRouteHistory() {
        Continuity.setStateProvider(new RecordingProvider());
        // Restored in the finally, because the dispatcher is global: a first version left one
        // installed that answered EVERY path with a form, and the tests that run after it -- the
        // ones about routes this build does not register -- then found every stale route
        // perfectly dispatchable and failed on an assertion that had nothing to do with them.
        Navigation.setDispatcher(new RouteDispatcher() {
            public Form dispatch(String path) {
                return new Form(path);
            }
        });
        try {
            Navigation.navigate("/account/statement");
            flushSerialCalls();
            assertFalse(Navigation.getStack().isEmpty(),
                    "nothing was navigated, so there is no history for logout to forget");

            Continuity.clear();

            assertTrue(Navigation.getStack().isEmpty(),
                    "logout left the signed-out account's route history in place, so back() "
                            + "reopens their forms and the next navigation republishes them");
        } finally {
            Navigation.setDispatcher(null);
            Navigation.clearStack();
        }
    }

    /**
     * An automatic restore that FAILED leaves the arrival parked.
     *
     * <p>pollFinished() has already queued a publisher behind this dispatch, so with the slot
     * empty it posts the pending local checkpoint over the relay's only copy of the state that
     * just failed -- and the retry the failure is kept for has nothing left to retry. The
     * deferred-listener branch beside it got this; the automatic one threw the answer away.</p>
     */
    @EdtTest
    public void anAutomaticRestoreThatFailedKeepsTheArrivalParked() {
        Continuity.setAutoRestore(true);
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                return new HashMap<String, Object>();
            }

            public void restoreState(Map<String, Object> payload) {
                throw new IllegalStateException("the dependency this needs is not up yet");
            }
        });

        Continuity.deliver(fromElsewhere("work worth keeping", 61L));
        flushSerialCalls();

        AppState offered = Continuity.getRestorableState();
        assertNotNull(offered,
                "an automatic restore that threw dropped the arrival, so a queued checkpoint can "
                        + "replace the relay's only copy of it");
        assertEquals(61L, offered.getSequence(), "a different state is on offer");
    }

    /**
     * A checkpoint the storage refused keeps the parked state too.
     *
     * <p>commit() used to be void, so a refused write ended there silently: the restore reported
     * no failure, the slot was released, and a pending publish could erase the relay copy of a
     * state with no durable copy anywhere and no acknowledgement.</p>
     */
    @EdtTest
    public void aRestoreWhoseCheckpointCannotBeStoredKeepsTheParkedState() {
        Continuity.setAutoRestore(false);
        Continuity.setStateProvider(new RecordingProvider());

        AppState arrival = fromElsewhere("unstorable", 62L);
        arrival.setRoutes(new ArrayList<String>());
        Continuity.deliver(arrival);
        flushSerialCalls();
        assertNotNull(Continuity.getRestorableState(), "the arrival never parked");

        Storage original = Storage.getInstance();
        Storage.setStorageInstance(new RefusingOneStorage(original, Continuity.STORAGE_KEY));
        try {
            Continuity.restore();
        } finally {
            Storage.setStorageInstance(original);
        }

        AppState still = Continuity.getRestorableState();
        assertNotNull(still,
                "a restore whose checkpoint storage refused released the slot, so nothing "
                        + "durable holds this state and a queued publish can erase the relay's "
                        + "copy of it");
        assertEquals(62L, still.getSequence(), "a different state is on offer");
    }

    /**
     * The synced store reaches the platform without consulting the entitlement probe.
     *
     * <p>The gate was on three layers -- the native store, the iOS bridge and the public facade --
     * and removing it from the first two changed nothing, because the third still made every call
     * unreachable. This drives the facade, which is the layer an application actually touches, and
     * is the check that was missing when the first two "fixes" were called done.</p>
     */
    @EdtTest
    public void theSyncedStoreFacadeReachesTheBridgeWithoutTheProbe() {
        // A bridge that reports the feature UNSUPPORTED while still holding values, which is
        // exactly the iOS shape the fix is about: the entitlement probe has not succeeded, and
        // the store underneath is a local one that works anyway.
        Continuity.setBridge(new LocalContinuityBridge() {
            @Override
            public boolean isSyncedStoreSupported() {
                return false;
            }
        });

        assertFalse(SyncedStore.isSupported(),
                "the fixture says the probe succeeded, so this proves nothing about the gate");
        assertTrue(SyncedStore.put("draft", "half a sentence"),
                "the facade refused a write on a bridge that would have taken it");
        assertEquals("half a sentence", SyncedStore.get("draft", "nothing"),
                "the facade refused to read a value the bridge is holding");

        SyncedStore.remove("draft");
        assertEquals("nothing", SyncedStore.get("draft", "nothing"),
                "the facade did not reach the bridge to remove the key");
    }

    /**
     * A failed relay read holds the publication until a read SUCCEEDS, not until the next
     * checkpoint.
     *
     * <p>Writing over the relay's single document is only safe because a poll established what
     * was there; a timeout establishes nothing. The hold used to end the moment pollFinished()
     * cleared {@code polling}, so the very next checkpoint published over a document this device
     * had never managed to read.</p>
     */
    @EdtTest
    public void aFailedReadHoldsThePublicationUntilAReadSucceeds() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);

        final java.util.concurrent.atomic.AtomicInteger fetches =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicBoolean readWorks =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        Continuity.setRelay(new StateRelay() {
            public void publish(AppState state) {
                published.add(state);
            }

            public AppState fetch() throws java.io.IOException {
                fetches.incrementAndGet();
                if (!readWorks.get()) {
                    throw new java.io.IOException("no network");
                }
                return null;
            }
        });
        pause(300L);
        flushSerialCalls();
        assertTrue(fetches.get() > 0, "the relay was never read, so no read has failed yet");
        published.clear();

        // The checkpoint that used to go out on the strength of a read that never happened.
        provider.saved.put("after", "work done while offline");
        Continuity.checkpoint();
        pause(300L);
        flushSerialCalls();
        assertTrue(published.isEmpty(),
                "a checkpoint was published over a relay document this device has never managed "
                        + "to read: published=" + published.size());

        // And it is a hold, not a refusal: the work is still owed, and a read that succeeds
        // releases it. Without that this test would pass on a relay that had simply stopped.
        readWorks.set(true);
        Continuity.checkpoint();
        pause(500L);
        flushSerialCalls();
        assertFalse(published.isEmpty(),
                "the state stayed owed for ever once a read had failed, so this device never "
                        + "publishes again for the life of the process");
    }

    /**
     * Completing a newer state from an origin releases an older one parked from the same origin.
     *
     * <p>A device can have two states in flight -- a continuation and a relay poll routinely carry
     * different sequences -- so N can be parked while N+1 is admitted and restored. An identity
     * comparison left N in the slot: it was still offered, restoring it would have walked the user
     * and the stored checkpoint backwards, and the publication hold never lifted.</p>
     */
    @EdtTest
    public void completingANewerStateReleasesAnOlderOneFromTheSameOrigin() {
        Continuity.setAutoRestore(false);
        Continuity.setStateProvider(new RecordingProvider());

        Continuity.deliver(fromElsewhere("the older screen", 70L));
        flushSerialCalls();
        AppState older = Continuity.getRestorableState();
        assertNotNull(older, "nothing parked, so there is no predecessor to strand");
        assertEquals(70L, older.getSequence(), "a different state parked");

        // The same origin, further along. Acknowledging it is the origin saying where it is now.
        Continuity.acknowledge(fromElsewhere("the newer screen", 71L));
        flushSerialCalls();

        AppState stranded = Continuity.getRestorableState();
        assertTrue(stranded == null || stranded.getSequence() != 70L,
                "the superseded state is still on offer, so restoring it walks the user backwards "
                        + "and the publication hold never lifts");
    }

    /**
     * A synchronous acknowledgement survives a device id too long to keep a durable mark for.
     *
     * <p>Two fixes meeting. The durable map is bounded by what one stored string can hold, so
     * trimToWritable() can evict an entry the moment it goes in -- an id long enough to blow that
     * budget on its own does exactly that. Asking only that map whether the arrival had been acted
     * on then said no a microsecond after acknowledge() returned, and parked a finished state:
     * still offered, with every relay checkpoint held behind it.</p>
     *
     * <p>Neither fix is wrong alone. The map answers what the next launch will know; this question
     * is what this process has already done.</p>
     */
    @EdtTest
    public void aSynchronousAcknowledgementSurvivesAnIdTooLongToStore() {
        Continuity.setStateProvider(new RecordingProvider());
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                Continuity.acknowledge(state);
                return false;
            }
        });

        // Exactly at the limit an AppState accepts for a device id, which is one byte per
        // character here -- so the id is legal, and the MARK for it is not: the entry costs the
        // id plus its separators and sequence, which is past what a single stored string holds,
        // and the size budget evicts it the instant it goes in. A shorter id would fit and prove
        // nothing; a longer one is refused by setDeviceId before this test begins, which is how
        // the first version of it failed.
        StringBuilder id = new StringBuilder("device-");
        while (id.length() < 65535) {
            id.append('d');
        }
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("note", "handled in the callback");
        Continuity.deliver(new AppState()
                .setPayload(payload)
                .setDeviceId(id.toString())
                .setSequence(81L)
                .setTimestamp(System.currentTimeMillis()));
        flushSerialCalls();

        assertNull(Continuity.getRestorableState(),
                "an arrival the listener had acknowledged was parked because its durable mark did "
                        + "not fit, so it stays on offer and holds every checkpoint off the relay");
    }

    /**
     * The simulated synced store reports a write that did not reach storage.
     *
     * <p>It used to persist through {@code Preferences}, whose {@code set()} fills an in-memory
     * table and whose {@code save()} discards the write's result -- and whose {@code get()} reads
     * that table. The read-back therefore consulted the cache it had just written and agreed with
     * itself, so put() reported success for a value that vanishes at the next launch. The
     * simulator and the desktop app are what applications develop against, so this taught them
     * something false about the device.</p>
     */
    @EdtTest
    public void theSimulatedStoreReportsAWriteThatDidNotReachStorage() {
        // The control first: with storage working the same call must succeed, or an
        // unconditional false would satisfy the assertion below and break the store.
        assertTrue(SyncedStore.put("draft", "half a sentence"),
                "the write failed with storage working, so the refusal below proves nothing");
        assertEquals("half a sentence", SyncedStore.get("draft", "nothing"));

        Storage original = Storage.getInstance();
        Storage.setStorageInstance(new RefusingStorage());
        try {
            assertFalse(SyncedStore.put("draft", "a longer sentence"),
                    "a write storage refused was reported as success, so the value is gone at the "
                            + "next launch and the application was told it was saved");
        } finally {
            Storage.setStorageInstance(original);
        }
    }

    /**
     * Restoring a foreign state withdraws the activity this device was advertising.
     *
     * <p>The platform activity stays current until something replaces or withdraws it, and
     * {@code applyingRestore} suppresses the checkpoint the rebuilt route stack would have
     * triggered -- so the pre-restore screen went on being offered to every Apple device around
     * until the user next navigated, and a third device could continue into a screen this one had
     * already moved off.</p>
     */
    @EdtTest
    public void restoringAForeignStateWithdrawsTheStaleAdvertisement() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("screen", "the one this device was on");
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(true);

        // What this device is advertising before anything arrives.
        Continuity.checkpoint();
        flushSerialCalls();
        assertNotNull(bridge.getPublishedType(),
                "nothing was advertised, so there is no stale activity for the restore to leave");

        Continuity.deliver(fromElsewhere("what the other device was doing", 91L));
        flushSerialCalls();
        assertNotNull(provider.restored,
                "the arrival was not applied, so this test is about nothing");

        assertNull(bridge.getPublishedType(),
                "the pre-restore activity is still advertised after restoring somebody else's "
                        + "state, so a third device continues into a screen this one has left");
    }

    /**
     * A recovery read after a failed fetch never overlaps another read.
     *
     * <p>The recovery branch was placed ABOVE the one-fetch-at-a-time guard, so a second
     * checkpoint launched a second GET while the first was still in flight. Two overlapping reads
     * can return different documents -- the relay holds one per user and the other device may
     * replace it between them -- and nothing downstream re-orders the answers, so whichever
     * finished first cleared {@code polling} and could release the publisher while the other was
     * still outstanding.</p>
     */
    @EdtTest
    public void aRecoveryReadNeverOverlapsAnotherRead() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);

        final java.util.concurrent.atomic.AtomicInteger live =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger mostAtOnce =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger fetches =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.CountDownLatch inRecovery =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release =
                new java.util.concurrent.CountDownLatch(1);

        Continuity.setRelay(new StateRelay() {
            public void publish(AppState state) {
                published.add(state);
            }

            public AppState fetch() throws java.io.IOException {
                int now = live.incrementAndGet();
                synchronized (mostAtOnce) {
                    if (now > mostAtOnce.get()) {
                        mostAtOnce.set(now);
                    }
                }
                try {
                    if (fetches.incrementAndGet() == 1) {
                        // The first read fails, which is what arms the recovery path.
                        throw new java.io.IOException("no network");
                    }
                    // Every later read is held open, so a checkpoint arriving now would start a
                    // second one if anything still let it.
                    inRecovery.countDown();
                    release.await(5L, java.util.concurrent.TimeUnit.SECONDS);
                    return null;
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return null;
                } finally {
                    live.decrementAndGet();
                }
            }
        });
        pause(300L);
        flushSerialCalls();
        assertEquals(1, fetches.get(), "the first read did not happen, so nothing armed recovery");

        // Starts the recovery read, which then blocks.
        Continuity.checkpoint();
        final boolean[] recovering = new boolean[1];
        awaitOffEdt(new Runnable() {
            public void run() {
                try {
                    recovering[0] = inRecovery.await(5L, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        assertTrue(recovering[0], "no recovery read started, so there is nothing to overlap");

        // The second checkpoint, while that read is still outstanding.
        provider.saved.put("more", "work done meanwhile");
        Continuity.checkpoint();
        pause(300L);

        release.countDown();
        pause(400L);
        flushSerialCalls();

        assertEquals(1, mostAtOnce.get(),
                "two relay reads were in flight at once, so whichever answered first could "
                        + "release a publish over a document the other had not seen");
    }

    /**
     * A state with no origin is refused rather than admitted.
     *
     * <p>Every mark is keyed by origin and sequence, so an empty origin is a single key shared by
     * every producer that forgot to set one -- and noteActedOn() has to refuse such a state, which
     * meant nothing was marked durably and the same state was restored again after every restart.
     * Worse, a listener following the documented acknowledge() path left it parked for the life of
     * the process, with relay publication held behind it.</p>
     */
    @EdtTest
    public void aStateWithNoOriginIsRefused() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(false);
        final int[] offered = new int[1];
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                offered[0]++;
                return false;
            }
        });

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("note", "from nowhere in particular");
        // No setDeviceId at all, which is what a hand-built relay state or a document with no
        // "device" member produces.
        Continuity.deliver(new AppState()
                .setPayload(payload)
                .setSequence(5L)
                .setTimestamp(System.currentTimeMillis()));
        flushSerialCalls();

        assertEquals(0, offered[0],
                "a state with no origin reached the application, and nothing can ever mark it "
                        + "handled");
        assertNull(Continuity.getRestorableState(),
                "an origin-less state was parked, so it is offered for ever and every relay "
                        + "checkpoint waits behind it");
    }

    /**
     * A listener that logs out mid-callback stops the dispatch it is inside.
     *
     * <p>Discovering that an arrival belongs to another account is exactly the decision this
     * callback exists for, and calling clear() is the documented response. Dispatch carried on
     * regardless: with automatic restore on it restored and PERSISTED the signed-out account's
     * state, moments after logout had deleted it.</p>
     */
    @EdtTest
    public void aListenerThatLogsOutStopsTheDispatchItIsInside() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(true);
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                // "This is not the account that is signed in" -- the documented reason to log out.
                Continuity.clear();
                return true;
            }
        });

        Continuity.deliver(fromElsewhere("the previous account's work", 95L));
        flushSerialCalls();

        assertNull(provider.restored,
                "the signed-out account's state was restored after clear(), so its work is back "
                        + "on screen and back in storage moments after logout deleted it");
        assertNull(Continuity.getRestorableState(),
                "logout left something restorable behind");
    }

    /**
     * And disable() inside the callback stops it too.
     *
     * <p>disable() documents that arriving states are ignored from the moment it is called, which
     * has to include the one being dispatched when it is called.</p>
     */
    @EdtTest
    public void aListenerThatDisablesStopsTheDispatchItIsInside() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(true);
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                Continuity.disable();
                return true;
            }
        });

        Continuity.deliver(fromElsewhere("arrived as it was switched off", 96L));
        flushSerialCalls();

        assertNull(provider.restored,
                "a state was restored after disable(), which says arrivals are ignored from the "
                        + "moment it is called");
    }

    /**
     * Logout removes the delivery marks even when the write that empties them is refused.
     *
     * <p>rememberSeen() writes the emptied map, and a write storage refuses leaves the whole of
     * the signed-out account's marks on disk for the next launch to reload. Which devices an
     * account synced with, and how far, is that account's data as much as its routes are.</p>
     */
    @EdtTest
    public void logoutRemovesTheDeliveryMarksEvenWhenTheWriteIsRefused() {
        Continuity.setStateProvider(new RecordingProvider());
        // An acknowledged arrival, so there is a durable mark to lose.
        Continuity.acknowledge(fromElsewhere("dealt with", 9L));
        flushSerialCalls();
        assertFalse(Continuity.readSeenForTest().isEmpty(),
                "no mark was written, so logout has nothing to remove and this proves nothing");

        Storage original = Storage.getInstance();
        Storage.setStorageInstance(new RefusingOneStorage(original, Continuity.PREF_SEEN));
        try {
            Continuity.clear();
        } finally {
            Storage.setStorageInstance(original);
        }

        assertTrue(Continuity.readSeenForTest().isEmpty(),
                "the signed-out account's delivery marks are still on disk, so the next launch "
                        + "reloads which devices it synced with and how far: "
                        + Continuity.readSeenForTest());
    }

    /**
     * The relay's requests refuse redirects, because they carry a bearer token.
     *
     * <p>A redirect is followed with the same headers, so a 307 hands the token and the state to
     * whatever host the response names -- including an {@code http://} one, which silently undoes
     * the HTTPS the constructor insists on. A 302 turns the POST into a GET and the 2xx that
     * follows reports a write that never happened.</p>
     *
     * <p>Asked of the builder the relay actually produces, so it fails if the call is dropped
     * from {@code auth()}, and paired with a control: a plain request still follows redirects,
     * which is CodenameOne's default and not something this may change for everyone.</p>
     */
    @EdtTest
    public void theRelayRefusesRedirectsOnItsAuthenticatedRequests() throws Exception {
        RestStateRelay relay = new RestStateRelay("https://example.invalid/continuity");
        Continuity.setRelay(relay);

        java.lang.reflect.Method auth = RestStateRelay.class.getDeclaredMethod(
                "auth", RequestBuilder.class);
        auth.setAccessible(true);
        RequestBuilder built = (RequestBuilder) auth.invoke(
                relay, Rest.post("https://example.invalid/continuity"));

        assertEquals(Boolean.FALSE, redirectSetting(built),
                "the relay's requests follow redirects, so a 307 forwards the bearer token and "
                        + "the state to whatever host the endpoint names");
        // UNSPECIFIED, not "true". The setting is three-state on purpose: a request that never
        // asked must leave ConnectionRequest's global default alone, in either direction, so
        // asserting true here would have pinned the wrong contract -- an application that turned
        // redirects off globally would still get them.
        assertNull(redirectSetting(Rest.post("https://example.invalid/continuity")),
                "an ordinary request now carries a redirect setting of its own, which overrides "
                        + "whatever the application chose globally");
    }

    /**
     * An explicit followRedirects(true) reaches the request even when the global default is false.
     *
     * <p>Asserted on the built {@code ConnectionRequest}, not on the builder's own field: the
     * field is recorded either way, and the defect was in applying it. A first version of this
     * check read the builder and passed against the broken code, which is the same wrong-layer
     * mistake the probe exists to catch.</p>
     */
    @EdtTest
    public void anExplicitRedirectChoiceReachesTheRequest() throws Exception {
        boolean previous = ConnectionRequest.isDefaultFollowRedirects();
        ConnectionRequest.setDefaultFollowRedirects(false);
        try {
            assertTrue(builtFollowsRedirects(
                    Rest.post("https://example.invalid/x").followRedirects(true)),
                    "an explicit followRedirects(true) did not reach the request, so a per-request "
                            + "setting cannot override the global one");
            assertFalse(builtFollowsRedirects(Rest.post("https://example.invalid/x")),
                    "a request that never asked stopped inheriting the global default");
        } finally {
            ConnectionRequest.setDefaultFollowRedirects(previous);
        }
    }

    /// What the builder actually hands to the network layer.
    private static boolean builtFollowsRedirects(RequestBuilder b) throws Exception {
        java.lang.reflect.Method m =
                RequestBuilder.class.getDeclaredMethod("createRequest", boolean.class);
        m.setAccessible(true);
        return ((ConnectionRequest) m.invoke(b, Boolean.FALSE)).isFollowRedirects();
    }

    /// The builder's redirect setting: TRUE, FALSE, or null for "the caller did not say".
    private static Boolean redirectSetting(RequestBuilder b) throws Exception {
        java.lang.reflect.Field f = RequestBuilder.class.getDeclaredField("followRedirects");
        f.setAccessible(true);
        return (Boolean) f.get(b);
    }

    /**
     * A restore cancels the checkpoint a navigation had already scheduled.
     *
     * <p>routeStackChanged() sets the pending flag and queues a flush, and that flush asks only
     * whether a checkpoint is pending. So it ran after the restore, captured the state that had
     * just ARRIVED under this device's identity, and published the echo the restore path exists
     * to suppress -- which the origin then accepts and restores on its next poll.</p>
     */
    @EdtTest
    public void aRestoreCancelsTheCheckpointANavigationHadScheduled() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("screen", "before the arrival");
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(true);
        Continuity.setRelay(new StateRelay() {
            public void publish(AppState state) {
                published.add(state);
            }

            public AppState fetch() {
                return null;
            }
        });
        pause(200L);
        flushSerialCalls();
        published.clear();

        // The ORDER is the whole test, and getting it wrong is why the first version passed
        // against the unfixed code. deliver() queues admission, admission queues the dispatch a
        // turn later, and the navigation has to land BETWEEN them -- so its flush is queued
        // behind the dispatch and runs after the restore has committed. Calling
        // routeStackChanged() directly put the flush FIRST, where checkpoint() cleared the
        // pending flag itself and the fix could not be observed at all.
        Continuity.deliver(fromElsewhere("what the other device was doing", 97L));
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Continuity.routeStackChanged();
                assertTrue(Continuity.isCheckpointPending(),
                        "the navigation scheduled nothing, so there is no capture to cancel");
            }
        });
        flushSerialCalls();
        assertNotNull(provider.restored, "the arrival was not applied, so this test is vacuous");

        // Asserted on what reached the RELAY, not on the pending flag. checkpoint() clears that
        // flag itself, so it reads false whether the capture was cancelled or performed -- a
        // second version of this test asserted on it and passed against the unfixed code.
        pause(300L);
        flushSerialCalls();
        assertTrue(published.isEmpty(),
                "the checkpoint scheduled before the restore still ran, so the arrival went back "
                        + "out under this device's id -- the echo the origin then accepts and "
                        + "restores on its next poll: published=" + published.size());
    }

    /**
     * Keys that Storage would fold together stay distinct in the simulated store.
     *
     * <p>Storage normalizes {@code /}, {@code %}, {@code ?}, {@code *}, {@code :} and {@code =}
     * to {@code _} in a file name, so "a/b" and "a_b" addressed the same value: both writes
     * reported success, the index listed both keys, and either read returned whichever was
     * written last while removing one deleted the other. That arrived with the move off
     * Preferences, which has no such rule -- a defect introduced while fixing a different one.</p>
     */
    @EdtTest
    public void keysThatStorageWouldFoldTogetherStayDistinct() {
        assertTrue(SyncedStore.put("a/b", "slash"), "the first key was refused");
        assertTrue(SyncedStore.put("a_b", "underscore"), "the second key was refused");

        assertEquals("slash", SyncedStore.get("a/b", "missing"),
                "\"a/b\" reads back the value written under \"a_b\", so the two share a "
                        + "storage name");
        assertEquals("underscore", SyncedStore.get("a_b", "missing"),
                "\"a_b\" lost its own value");

        // And removing one must not take the other with it.
        SyncedStore.remove("a/b");
        assertEquals("missing", SyncedStore.get("a/b", "missing"), "the removal did not happen");
        assertEquals("underscore", SyncedStore.get("a_b", "missing"),
                "removing \"a/b\" deleted the value stored under \"a_b\"");
    }

    /**
     * Forgetting the back history is a change worth checkpointing.
     *
     * <p>clearStack() stayed silent so that logout could call it without checkpointing the
     * emptied stack back over the storage it was deleting -- which made it silent for every other
     * caller too. An application that forgot its history and did not then navigate left the
     * previous routes in the stored checkpoint, so a process death restored exactly what it had
     * just cleared.</p>
     */
    @EdtTest
    public void forgettingTheBackHistoryIsCheckpointed() {
        Continuity.setStateProvider(new RecordingProvider());
        Continuity.checkpoint();
        assertFalse(Continuity.isCheckpointPending(), "the fixture starts with nothing owed");

        Navigation.clearStack();

        assertTrue(Continuity.isCheckpointPending(),
                "clearing the back history scheduled no checkpoint, so the previous routes stay "
                        + "in storage and a process death restores what was just cleared");
    }

    /**
     * A continuation arriving while continuity is off is DECLINED, so the port can hold it.
     *
     * <p>SyncedStore.addChangeListener() installs the same callback without enabling continuity --
     * a key/value store is not consent to restore a route stack -- and on a cold launch that
     * happens before the application's init() calls enable(). The iOS port holds a declined
     * activity and offers it again when the next callback is installed; claiming it instead threw
     * it away, because admit() drops an arrival while the framework is disabled. An application
     * that registered a store listener first lost its Handoff for good.</p>
     */
    @EdtTest
    public void aContinuationArrivingBeforeEnableIsDeclinedRatherThanSwallowed() {
        // NOTHING SAID YET, which is the window the port retains for, and which the per-test
        // reset() already gives. This used to call disable() to force the state -- harmless when
        // disable() was a no-op before the first enable(), and wrong once it began recording the
        // application's choice: an explicit "off" is a different answer from silence, and the
        // sibling test below is about that one.
        ContinuityCallback callback = Continuity.callbackForTest();

        Map<String, Object> info = StateCodec.toMap(fromElsewhere("from the other device", 88L));
        boolean claimed = callback.continuationReceived(Continuity.getActivityType(), info);

        assertFalse(claimed,
                "the callback claimed a continuation while continuity was disabled, so the port "
                        + "discards it and the enable() moments later has nothing to deliver");

        // And once enabled it IS claimed, or the decline above would just be a feature that never
        // works.
        Continuity.enable();
        assertTrue(callback.continuationReceived(Continuity.getActivityType(), info),
                "an enabled framework refused its own activity type");
    }

    /**
     * A disable() before any enable() is still an answer, and an arrival during it is dropped.
     *
     * <p>The gap the previous fix left. An application that enables continuity only after a login
     * and calls disable() while logged out was leaving the flag unset, because disable() returned
     * early when there was nothing to turn off -- so an arrival during that interval was read as
     * a pre-enable cold-launch arrival: declined, retained by the port, and delivered by the
     * enable() that came with the login. Saying "no" before saying anything else is still saying
     * it.</p>
     */
    @EdtTest
    public void aDisableBeforeAnyEnableStillDropsAnArrival() {
        // Never enabled. This is the whole point: disable() has nothing to turn off here.
        Continuity.disable();
        ContinuityCallback callback = Continuity.callbackForTest();

        Map<String, Object> info = StateCodec.toMap(fromElsewhere("during the logged-out spell", 92L));
        assertTrue(callback.continuationReceived(Continuity.getActivityType(), info),
                "the callback declined an arrival after an explicit disable() that happened to be "
                        + "the application's FIRST word, so the port holds it and the enable() "
                        + "that comes with the login restores it");

        Continuity.enable();
        assertNull(Continuity.getRestorableState(),
                "the arrival from the disabled interval was delivered after all");
    }

    /**
     * A platform continuation gets the same schema check the relay wire gets.
     *
     * <p>Continuity.Callback calls fromMap() DIRECTLY -- an NSUserActivity, or anything a custom
     * bridge hands over, never touches fromJson -- so every check added for the relay was missing
     * from the other way in. A continuation with a good origin and sequence but "routes" as a
     * string dropped the field, produced an empty state, and admission consumed that as a
     * tombstone and advanced the origin's durable high-water mark. Same harm, other path.</p>
     */
    @EdtTest
    public void aMalformedPlatformContinuationIsNotConsumedAsATombstone() {
        Continuity.enable();
        Continuity.setStateProvider(new RecordingProvider());
        ContinuityCallback callback = Continuity.callbackForTest();

        Map<String, Object> malformed = new HashMap<String, Object>();
        malformed.put("device", "bridge-sender");
        malformed.put("seq", "10");
        // A LIST is what this field is; a string here is the malformed case, and it used to be
        // dropped in silence.
        malformed.put("routes", "/orders,/orders/17");

        assertFalse(callback.continuationReceived(Continuity.getActivityType(), malformed),
                "a malformed continuation was claimed, so the framework took responsibility for "
                        + "a document it could not read");
        flushSerialCalls();

        assertNull(Continuity.readSeenForTest().get("bridge-sender"),
                "the malformed continuation was consumed as a tombstone and marked durably, so "
                        + "the sender's correction is refused after a restart as already seen");
    }

    /**
     * A raw control character inside a JSON string is a failed read, not a tombstone.
     *
     * <p>The grammar check accepted every unescaped character except quote and backslash, and
     * JSON allows neither below U+0020. It is not a formality: the framework parser appends a
     * raw control byte to whatever it is building rather than stopping, so a document carrying a
     * literal newline inside a KEY -- "pay(LF)load" -- passed as valid and came out with a key
     * that is not "payload". The field is unknown and dropped, and a state with no payload and
     * no routes is a tombstone, which the sending device meant as nothing of the sort.</p>
     */
    @EdtTest
    public void aRawControlCharacterInAStringIsRefused() throws Exception {
        String withNewlineInAKey =
                "{\"device\":\"other\",\"seq\":\"10\",\"pay\nload\":{\"a\":1}}";
        try {
            AppState s = StateCodec.fromJson(withNewlineInAKey);
            fail("a raw newline inside a JSON string was accepted"
                    + (s != null && s.isEmpty()
                            ? " -- and as an EMPTY state, which is read as a tombstone: the "
                                    + "sending device is told to have cleared its work"
                            : ""));
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().length() > 0, "the refusal explained nothing");
        }

        // The ESCAPED form is the legitimate one and must still go through, or this guard would
        // refuse every payload that contains a line break.
        AppState fine = StateCodec.fromJson(
                "{\"device\":\"other\",\"seq\":\"10\",\"enc\":\"1\","
                        + "\"payload\":{\"note\":\"s:two\\nlines\"}}");
        assertEquals("two\nlines", fine.getPayload().get("note"),
                "an escaped newline was mangled, so the guard refuses legitimate documents");
    }

    /**
     * An arrival admitted before disable() is not dispatched by an enable() that follows.
     *
     * <p>{@code enabled} alone could not see a disable() and an enable() that BOTH ran before the
     * queued dispatch did -- two queued turns are enough, and the flag is true again by the time
     * it is read -- so the arrival from before the disable was dispatched and restored after
     * all. lastSeen still holds its sequence, so the supersession check waves it through too. The
     * generation is the field that remembers a session ended, which is what disable() actually
     * promises.</p>
     */
    @EdtTest
    public void anArrivalAdmittedBeforeDisableIsNotDispatchedByALaterEnable() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(true);

        // Admitted, which queues the dispatch for the next turn.
        Continuity.deliver(fromElsewhere("the previous session's work", 150L));

        // Both land BEFORE that queued turn runs, which is what the flag could not see.
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Continuity.disable();
                Continuity.enable();
            }
        });
        flushSerialCalls();
        flushSerialCalls();

        assertNull(provider.restored,
                "a state admitted before disable() was restored by the enable() that followed, "
                        + "though disable() documents that arriving states are ignored from the "
                        + "moment it is called, including the ones already admitted");
    }

    /**
     * A key whose value is gone is not listed, even when the index still names it.
     *
     * <p>The sibling of the deletion-failure case, in the other direction: the value IS deleted
     * and the index write then fails, so the stored index goes on naming a key whose get()
     * answers the default. The platform being simulated has no such gap --
     * NSUbiquitousKeyValueStore enumerates its own dictionary, so a phantom key cannot exist
     * there -- and the simulation should not invent one.</p>
     */
    @EdtTest
    public void aKeyWhoseValueIsGoneIsNotListed() {
        Storage real = Storage.getInstance();
        LocalContinuityBridge b = new LocalContinuityBridge();
        try {
            assertTrue(b.syncedStorePut("locale", "en"), "the fixture could not write a value");
            assertTrue(java.util.Arrays.asList(b.syncedStoreKeys()).contains("locale"),
                    "the fixture's own key is not listed, so this test is about nothing");

            // The scenario itself rather than a hand-made facsimile of it: from here every write
            // fails while deletes and reads still work, so the remove below deletes the value
            // successfully and cannot rewrite the index.
            Storage.setStorageInstance(new WriteRefusingStorage(real));
            b.syncedStoreRemove("locale");
            assertNull(b.syncedStoreGet("locale"),
                    "the fixture is wrong: the value survived, so there is no phantom entry to "
                            + "test");

            assertFalse(java.util.Arrays.asList(b.syncedStoreKeys()).contains("locale"),
                    "keys() named a key whose value is not there, so an application walking the "
                            + "store reads the default for a key the store says it has");
        } finally {
            Storage.setStorageInstance(real);
            new LocalContinuityBridge().syncedStoreRemove("locale");
        }
    }

    /**
     * A synced-store value that would not delete keeps its index entry.
     *
     * <p>The two writes are the value and the index, and every caller has to know which of them
     * happened. Dropping the index entry for a value the delete failed to remove left the old
     * value readable through get() while keys() omitted it and clearing the store could not
     * reach it -- a value with no way to see it and no way to remove it.</p>
     *
     * <p>put()'s rollback had the identical unchecked delete: when the index write fails it
     * removes the value it just wrote, and claimed a cleanup it had not performed. That one came
     * out of enumerating the file rather than from the report.</p>
     */
    @EdtTest
    public void aValueThatWillNotDeleteKeepsItsIndexEntry() {
        Storage real = Storage.getInstance();
        try {
            LocalContinuityBridge b = new LocalContinuityBridge();
            assertTrue(b.syncedStorePut("theme", "dark"), "the fixture could not write a value");
            assertTrue(java.util.Arrays.asList(b.syncedStoreKeys()).contains("theme"),
                    "the fixture's own key is not listed, so this test is about nothing");

            // A store whose delete does nothing at all, which is what a file the desktop cannot
            // remove looks like from in here.
            Storage.setStorageInstance(new UndeletableStorage(real));

            b.syncedStoreRemove("theme");

            assertEquals("dark", b.syncedStoreGet("theme"),
                    "the fixture is wrong: the value did go away, so there is no divergence to "
                            + "test");
            assertTrue(java.util.Arrays.asList(b.syncedStoreKeys()).contains("theme"),
                    "the index dropped a key whose value is still readable through get(), so "
                            + "the value cannot be listed, cannot be found by a store-wide "
                            + "cleanup, and cannot be removed");
        } finally {
            Storage.setStorageInstance(real);
            new LocalContinuityBridge().syncedStoreRemove("theme");
        }
    }

    /**
     * A provider that signs out and then throws stops the restore before any route runs.
     *
     * <p>The guard sat on the normal-return path only, so a provider that called clear() and then
     * failed -- cleanup breaking after it noticed an expired account -- was carried past it by
     * the catch, and the route rebuild ran for the session that had just ended. The later
     * lifecycle check does undo the stack, but only after that account's route factories, form
     * constructors and show callbacks have run and put its data in front of the user.</p>
     *
     * <p>The same mistake capture() had, in the method that mirrors it. Found there first, and
     * still here.</p>
     */
    @EdtTest
    public void aProviderThatSignsOutAndThenThrowsStopsBeforeTheRoutes() {
        final java.util.concurrent.atomic.AtomicInteger routesBuilt =
                new java.util.concurrent.atomic.AtomicInteger();
        Navigation.setDispatcher(new RouteDispatcher() {
            public Form dispatch(String url) {
                routesBuilt.incrementAndGet();
                Form f = new Form();
                f.setTitle(url);
                return f;
            }
        });
        try {
            Continuity.setStateProvider(new StateProvider() {
                public Map<String, Object> saveState() {
                    return new HashMap<String, Object>();
                }

                public void restoreState(Map<String, Object> payload) {
                    // The documented answer to "this payload belongs to a signed-out account",
                    // and then a failure on the way out of it.
                    Continuity.clear();
                    throw new IllegalStateException("the cleanup after the logout failed");
                }
            });
            Continuity.setAutoRestore(false);

            Map<String, Object> payload = new HashMap<String, Object>();
            payload.put("account", "the previous one");
            AppState arriving = new AppState()
                    .setPayload(payload)
                    .setRoutes(java.util.Arrays.asList("/orders", "/orders/17"))
                    .setDeviceId("some-other-device")
                    .setSequence(140L)
                    .setTimestamp(System.currentTimeMillis());
            Continuity.restore(arriving);
            flushSerialCalls();

            assertEquals(0, routesBuilt.get(),
                    "the signed-out account's routes were dispatched anyway: its route factories, "
                            + "form constructors and show callbacks all ran, and undoing the "
                            + "stack afterwards cannot unrun them");
        } finally {
            Navigation.setDispatcher(null);
        }
    }

    /**
     * A listener that signs out and then throws is not followed by the next listener.
     *
     * <p>The check was after the {@code continue}, so a throw jumped straight past it and the
     * next listener was handed the signed-out account's state. The check at the end of dispatch
     * stops the restore, but it cannot undo what that listener did with the payload, or unsee
     * it.</p>
     */
    @EdtTest
    public void aListenerThatSignsOutAndThenThrowsStopsTheWalk() {
        final java.util.concurrent.atomic.AtomicInteger secondSaw =
                new java.util.concurrent.atomic.AtomicInteger();
        Continuity.setStateProvider(new RecordingProvider());
        Continuity.setAutoRestore(false);
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                Continuity.clear();
                throw new IllegalStateException("the cleanup after the logout failed");
            }
        });
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                secondSaw.incrementAndGet();
                return true;
            }
        });

        Continuity.deliver(fromElsewhere("the previous account's work", 141L));
        flushSerialCalls();

        assertEquals(0, secondSaw.get(),
                "the second listener was handed a state from a session the first had already "
                        + "ended, and nothing afterwards can unsee it");
    }

    /**
     * A continuation arriving after an explicit disable() is DROPPED, not held for the next
     * enable().
     *
     * <p>The sibling above is the other half, and the two want opposite answers from the same
     * {@code enabled == false}. Declining is right before the application's first enable(),
     * because the port holds a declined activity and offers it again when a callback is next
     * installed -- which is exactly what recovers a cold-launch Handoff. After an explicit
     * disable() that same retention delivered a state from the interval disable() documents as
     * ignored, whenever the application switched continuity back on.</p>
     *
     * <p>Claiming is what discards it: the port lets go of an activity that was handled. Nothing
     * else answers to this application's own activity type, so taking it costs no other handler
     * anything.</p>
     */
    @EdtTest
    public void aContinuationArrivingAfterDisableIsDroppedRatherThanHeld() {
        Continuity.enable();
        Continuity.disable();
        ContinuityCallback callback = Continuity.callbackForTest();

        Map<String, Object> info = StateCodec.toMap(fromElsewhere("during the off period", 91L));
        assertTrue(callback.continuationReceived(Continuity.getActivityType(), info),
                "the callback declined a continuation after an explicit disable(), so the port "
                        + "holds it and the next enable() restores a state that arrived while "
                        + "the application had said it wanted none");

        // Dropped, not delivered: claiming must not become a back door into the disabled
        // framework either.
        Continuity.enable();
        assertNull(Continuity.getRestorableState(),
                "the arrival from the disabled interval was delivered after all");
    }

    /**
     * The cold-launch waiter finishes, hands over, and leaves nothing set behind it.
     *
     * <p>The waiter asks the EVENT THREAD whether a window has appeared rather than reading
     * Display.getCurrent() itself: that method is not a field read -- for a disposed dialog or a
     * menu it walks animationQueue by index, size first and then each element -- and a cold
     * launch is exactly when the event thread is building forms and running transitions through
     * that queue. This test pins the behaviour the marshalling has to keep: the parked arrival is
     * still handed over once a window exists, and {@code waitingForWindow} is left clear so a
     * LATER arrival can start a waiter of its own. That second half is what a throw inside the
     * old worker destroyed -- the flag stayed set for the rest of the process and every
     * subsequent arrival parked behind it with nothing coming to look.</p>
     */
    @EdtTest
    public void theColdLaunchWaiterHandsOverAndLeavesNothingSet() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(false);

        Continuity.parkForTest(fromElsewhere("first cold-launch arrival", 120L));
        Continuity.drainParkedForTest();
        AppState first = Continuity.getRestorableState();
        assertNotNull(first, "the first parked arrival was never handed over");
        assertEquals(120L, first.getSequence(), "a different state was handed over");
        Continuity.restore(first);

        // The SECOND is the point. A waiter that did not report back leaves waitingForWindow set,
        // and this one is then parked for ever behind it.
        Continuity.parkForTest(fromElsewhere("second arrival, later in the run", 121L));
        Continuity.drainParkedForTest();
        AppState second = Continuity.getRestorableState();
        assertNotNull(second,
                "a second arrival was never handed over, which is what a waiter that failed to "
                        + "report back leaves behind: waitingForWindow set for the rest of the "
                        + "process and every later arrival parked behind it");
        assertEquals(121L, second.getSequence(), "the second arrival was not the one offered");
    }

    /**
     * The eviction order of the delivery marks survives a restart.
     *
     * <p>rememberSeen() writes durableSeen in its own order, least-recently-seen first, so the
     * file carries the eviction order. Reading it back into a HashMap threw that away, and
     * enable() replayed an arbitrary order into a map whose whole job is to evict the front -- so
     * the next new origin could evict a device the user is actively using instead of the one
     * quiet longest, and a delayed duplicate from the evicted device ran its side effects
     * again.</p>
     */
    @EdtTest
    public void theEvictionOrderOfTheMarksSurvivesARestart() {
        Continuity.setStateProvider(new RecordingProvider());
        // A full set, acknowledged oldest first, so "device-0" is the one quiet longest.
        for (int i = 0; i < 64; i++) {
            Map<String, Object> payload = new HashMap<String, Object>();
            payload.put("note", "seen " + i);
            Continuity.acknowledge(new AppState()
                    .setPayload(payload)
                    .setDeviceId("device-" + i)
                    .setSequence(i + 1)
                    .setTimestamp(System.currentTimeMillis()));
        }
        flushSerialCalls();
        assertEquals(64, Continuity.readSeenForTest().size(), "the fixture is not a full set");

        // The restart: memory forgotten, the file reloaded.
        Continuity.reset();
        Continuity.setStateProvider(new RecordingProvider());
        Continuity.enable();

        // One more origin, which must evict the eldest and only the eldest.
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("note", "the new one");
        Continuity.acknowledge(new AppState()
                .setPayload(payload)
                .setDeviceId("device-new")
                .setSequence(99L)
                .setTimestamp(System.currentTimeMillis()));
        flushSerialCalls();

        Map<String, Long> after = Continuity.readSeenForTest();
        assertFalse(after.containsKey("device-0"),
                "the eldest mark survived, so something else was evicted in its place");
        assertTrue(after.containsKey("device-63"),
                "the most recently seen device was evicted instead of the eldest, so a delayed "
                        + "duplicate from it reaches the listeners and repeats its side effects");
        assertTrue(after.containsKey("device-new"), "the new origin was not recorded at all");
    }

    /**
     * Logout leaves nothing owed, so a later flush cannot rebuild what it deleted.
     *
     * <p>clear() empties the navigation stack, and that emptying set the pending flag before the
     * guard that was meant to suppress it. A flush queued earlier -- or Android's next suspend --
     * then performed the checkpoint, rebuilding the deleted state from the still-installed
     * provider and publishing the signed-out account's payload after logout had removed it.</p>
     */
    @EdtTest
    public void logoutLeavesNothingOwedForALaterFlushToRebuild() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("secret", "the previous account's work");
        Continuity.setStateProvider(provider);
        Continuity.checkpoint();

        Continuity.clear();

        assertFalse(Continuity.isCheckpointPending(),
                "logout left a checkpoint owed, so the next flush writes the signed-out "
                        + "account's payload back over the state it just deleted");
        // And nothing a later flush could do brings it back.
        flushSerialCalls();
        assertNull(Continuity.getRestorableState(),
                "a flush after logout restored the deleted checkpoint");
    }

    /**
     * A provider that ends the session while restoring stops the restoration.
     *
     * <p>Detecting that a payload belongs to a signed-out account is exactly what this callback is
     * for, and clear() is the documented response. Restoration carried on regardless: it rebuilt
     * the routes and committed, persisting the state clear() had just deleted. The listener
     * callback had this guard; the provider is the other application callback on the path.</p>
     */
    @EdtTest
    public void aProviderThatLogsOutWhileRestoringStopsTheRestore() {
        Continuity.setAutoRestore(true);
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                return new HashMap<String, Object>();
            }

            public void restoreState(Map<String, Object> payload) {
                // "This payload is not the account that is signed in."
                Continuity.clear();
            }
        });

        Continuity.deliver(fromElsewhere("the previous account's work", 77L));
        flushSerialCalls();

        assertNull(Continuity.getRestorableState(),
                "the arrival the provider logged out over is still on offer -- either persisted "
                        + "past the clear() or parked back into the session it just emptied, and "
                        + "both hand the signed-out account's work to whoever signs in next");
    }

    /**
     * A route that logs out while being rebuilt stops the restore.
     *
     * <p>Rebuilding a stack runs application code -- the route factory, the form's constructor,
     * its show callback -- and any of it may discover the session is over. Committing afterwards
     * repopulates both the navigation stack and the stored checkpoint with the signed-out
     * account's state.</p>
     *
     * <p>This is the indirect case. The framework never calls the route factory; Navigation does,
     * on its behalf, which is why an enumeration of the callbacks the framework invokes directly
     * did not find it.</p>
     */
    @EdtTest
    public void aRouteThatLogsOutWhileBeingRebuiltStopsTheRestore() {
        Continuity.setStateProvider(new RecordingProvider());
        Continuity.setAutoRestore(true);
        Navigation.setDispatcher(new RouteDispatcher() {
            public Form dispatch(String path) {
                // "The session behind this route has expired."
                Continuity.clear();
                return new Form(path);
            }
        });
        try {
            AppState arrival = fromElsewhere("the previous account's screen", 66L);
            List<String> routes = new ArrayList<String>();
            routes.add("/account/statement");
            arrival.setRoutes(routes);

            Continuity.deliver(arrival);
            flushSerialCalls();

                assertNull(Continuity.getRestorableState(),
                    "the restore committed after the route logged out, so the signed-out "
                            + "account's stack and checkpoint are back");
            assertTrue(Navigation.getStack().isEmpty(),
                    "the rebuilt stack survived the logout that happened while building it");
            // The SCREEN as well as the history. clearStack() deliberately leaves the current
            // form alone, so undoing only the stack left the signed-out account's work in front
            // of the user with nothing but its breadcrumbs removed.
            Form current = Display.getInstance().getCurrent();
            assertNotNull(current, "no form is showing at all");
            assertFalse("/account/statement".equals(current.getTitle()),
                    "the signed-out account's restored screen is still displayed after the "
                            + "logout that cancelled the restore");
        } finally {
            Navigation.setDispatcher(null);
            Navigation.clearStack();
        }
    }

    /**
     * A logout stops a fetch that has not reached the network yet.
     *
     * <p>The publish worker has confirmed its session on the event thread since the first round of
     * this review; the poll worker never did, so only its COMPLETION was rejected -- after the
     * read had gone out. A custom relay that resolves authentication inside fetch() would issue a
     * request after logout, and could present the next account's credentials to the previous
     * endpoint, while clear() promises that only a request already on the wire survives it.</p>
     */
    @EdtTest
    public void aLogoutStopsAFetchThatHasNotReachedTheNetwork() {
        Continuity.setStateProvider(new RecordingProvider());
        final java.util.concurrent.atomic.AtomicInteger reads =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.CountDownLatch letGo =
                new java.util.concurrent.CountDownLatch(1);

        Continuity.setRelay(new StateRelay() {
            public void publish(AppState state) {
            }

            public AppState fetch() {
                // The worker reaches here only if the session check let it through. Waiting first
                // so the test can log out while it is still queued.
                try {
                    letGo.await(5L, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                reads.incrementAndGet();
                return null;
            }
        });

        // The worker is created and queued; the logout lands before it runs.
        Continuity.clear();
        letGo.countDown();
        pause(500L);
        flushSerialCalls();

        assertEquals(0, reads.get(),
                "a relay read went out after logout, so a relay that resolves its credentials "
                        + "inside fetch() presents the next account's to the previous endpoint");
    }

    /**
     * A truncated relay document is a failed read, not an empty relay.
     *
     * <p>JSONParser does not throw on malformed input: it logs and returns the partial map it had
     * built. A document cut off after "device" and "seq" therefore parsed into a state with no
     * routes and no payload -- which this framework reads as a TOMBSTONE -- so the origin was
     * recorded as having cleared its work, durably, while fetch() reported a successful read and
     * released a queued POST over the relay's real document.</p>
     */
    @EdtTest
    public void aTruncatedRelayDocumentIsAFailedReadNotAnEmptyRelay() {
        String whole = StateCodec.toJson(fromElsewhere("what the other device was doing", 42L));
        assertTrue(whole.length() > 20, "the fixture document is too short to truncate usefully");
        String cut = whole.substring(0, whole.length() / 2);

        // The control: the whole document still reads.
        try {
            assertNotNull(StateCodec.fromJson(whole), "a complete document failed to parse");
        } catch (java.io.IOException e) {
            fail("a complete document was refused: " + e.getMessage());
        }

        try {
            AppState partial = StateCodec.fromJson(cut);
            fail("a truncated document was accepted"
                    + (partial != null && partial.isEmpty()
                            ? " as an EMPTY state, which is read as a tombstone: the origin is "
                                    + "recorded as having cleared its work and the relay's real "
                                    + "document is then overwritten"
                            : ""));
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("valid JSON object"),
                    expected.getMessage());
        }
    }

    /**
     * A simulated-store write that could not be indexed leaves nothing behind.
     *
     * <p>Returning false while the value stayed durable made the answer a lie in the other
     * direction: the caller takes its documented fallback path while get() returns the value it
     * was told had failed, keys() omits it, and clearing the store cannot reach it.</p>
     */
    @EdtTest
    public void aStoreWriteThatCouldNotBeIndexedLeavesNothingBehind() {
        Storage original = Storage.getInstance();
        Storage.setStorageInstance(new RefusingOneStorage(original, "CN1$SyncedStoreKeys"));
        boolean reported;
        try {
            reported = SyncedStore.put("draft", "half a sentence");
        } finally {
            Storage.setStorageInstance(original);
        }

        assertFalse(reported, "the write was reported as successful although the index refused");
        assertEquals("missing", SyncedStore.get("draft", "missing"),
                "the value the caller was told had FAILED is readable, so the application took "
                        + "its fallback path over a value that is really there -- and keys() and "
                        + "clearing cannot see it");
    }

    /**
     * A syntactically invalid document is refused even when its delimiters balance.
     *
     * <p>The first version of this guard counted braces and closed strings, which catches a
     * document cut in half and lets a bad TOKEN through -- and the parser answers a bad token the
     * same way it answers truncation: it logs, and returns the map it had built so far. A partial
     * state that looks like a tombstone has the same consequences either way.</p>
     */
    @EdtTest
    public void aSyntacticallyInvalidDocumentIsRefusedEvenWhenBalanced() throws Exception {
        // Balanced braces, closed strings, invalid: "tru" is not a token.
        String balancedButInvalid = "{\"device\":\"d\",\"seq\":\"2\",\"payload\":tru}";
        assertFalse(StateCodec.isValidJsonObject(balancedButInvalid),
                "a bad token passed the check, so the parser's partial map becomes a state");

        // The shapes that must still be accepted, or the check is just breaking the feature.
        assertTrue(StateCodec.isValidJsonObject(
                StateCodec.toJson(fromElsewhere("a real one", 3L))),
                "a document this codec itself wrote was refused");
        assertTrue(StateCodec.isValidJsonObject("{}"), "an empty object was refused");
        assertTrue(StateCodec.isValidJsonObject(
                "{\"a\":[1,-2.5e3,true,null,{\"b\":\"\\u00e9\"}]}"),
                "a valid nested document was refused");

        // And the shapes that must not be.
        assertFalse(StateCodec.isValidJsonObject("{\"a\":1,}"), "a trailing comma was accepted");
        assertFalse(StateCodec.isValidJsonObject("{\"a\":1} junk"),
                "trailing content after the object was accepted");
        assertFalse(StateCodec.isValidJsonObject("{\"a\":\"unterminated}"),
                "an unterminated string was accepted");
        assertFalse(StateCodec.isValidJsonObject("{\"a\":01}"),
                "a malformed number was accepted");
        assertFalse(StateCodec.isValidJsonObject("{\"a\":\"\\uZZZZ\"}"),
                "a bad unicode escape was accepted");
    }

    /**
     * A tombstone does not release the publisher while a coalesced read is still owed.
     *
     * <p>pollFinished() clears {@code polling} before the tombstone is handled, so releasing here
     * started the POST BEFORE the follow-up GET and then ran the two together -- against a relay
     * that holds one document, which is exactly what the one-fetch-at-a-time rule exists to
     * prevent. The remote update the second read was going to see is overwritten, and that read
     * comes back with this device's own echo.</p>
     */
    @EdtTest
    public void aTombstoneDoesNotReleaseThePublisherWhileAReadIsOwed() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(false);

        // The tombstone arrives FROM the read, which is what makes this reachable: pollFinished()
        // clears `polling` before handing the state to admit(), so the tombstone branch runs in a
        // window where the publisher looks free while a coalesced read is still owed. A first
        // version delivered the tombstone by hand while a fetch was blocked -- `polling` was
        // still true, startPublisher() stopped at its own guard, and the test passed against the
        // unfixed code.
        final AppState tombstone = new AppState()
                .setDeviceId("some-other-device")
                .setSequence(41L)
                .setTimestamp(System.currentTimeMillis());
        final java.util.concurrent.atomic.AtomicInteger reads =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.CountDownLatch inTombstoneRead =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch releaseTombstone =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch releaseSecond =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicInteger publishedWhileReading =
                new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicBoolean reading =
                new java.util.concurrent.atomic.AtomicBoolean();

        Continuity.setRelay(new StateRelay() {
            public void publish(AppState state) {
                if (reading.get()) {
                    publishedWhileReading.incrementAndGet();
                }
                published.add(state);
            }

            public AppState fetch() {
                int n = reads.incrementAndGet();
                if (n == 1) {
                    // setRelay() polls immediately. Answering nothing here leaves the arrival
                    // below free to park -- returning the tombstone on this read admitted it
                    // FIRST, so the seq-40 arrival was refused as already seen and there was
                    // nothing to supersede.
                    return null;
                }
                if (n == 2) {
                    // Held so the test can ask for another read WHILE this one is in flight,
                    // which is what sets pollAgain -- and then answers with the tombstone, so it
                    // reaches admit() from pollFinished() with `polling` already cleared.
                    inTombstoneRead.countDown();
                    try {
                        releaseTombstone.await(5L, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    return tombstone;
                }
                // The coalesced follow-up, held open so an overlapping POST is observable.
                reading.set(true);
                try {
                    releaseSecond.await(5L, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                reading.set(false);
                return null;
            }
        });

        // An arrival parked, so the tombstone has something to supersede, and work owed to the
        // relay so there is a POST to release.
        Continuity.deliver(fromElsewhere("waiting on the user", 40L));
        flushSerialCalls();
        assertNotNull(Continuity.getRestorableState(), "nothing parked to supersede");
        // Owed to the relay, and HELD by the parked arrival rather than sent.
        Continuity.checkpoint();

        // The read that will answer with the tombstone.
        Continuity.pollRelay();
        final boolean[] inRead = new boolean[1];
        awaitOffEdt(new Runnable() {
            public void run() {
                try {
                    inRead[0] = inTombstoneRead.await(5L, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        assertTrue(inRead[0], "the tombstone read never started");
        // Asked for WHILE that read is in flight, which is what sets pollAgain.
        Continuity.pollRelay();
        releaseTombstone.countDown();

        pause(600L);
        flushSerialCalls();
        pause(400L);
        flushSerialCalls();

        assertTrue(reads.get() >= 2,
                "the coalesced follow-up read never happened, so nothing could overlap: reads="
                        + reads.get());
        assertEquals(0, publishedWhileReading.get(),
                "the tombstone released a POST while a coalesced read was still outstanding, so "
                        + "the two ran together against a relay that holds one document");

        releaseSecond.countDown();
        pause(400L);
        flushSerialCalls();
    }

    /**
     * A restored stack that cannot be shown leaves the previous one in place.
     *
     * <p>show() runs application code. If it throws, the stack had already been replaced, so the
     * old form stayed on screen while getCurrent(), back() and the next checkpoint all described
     * a stack the user never saw -- and a later navigation persisted a restoration that failed.</p>
     */
    @EdtTest
    public void aRestoredStackThatCannotBeShownLeavesThePreviousOne() {
        Navigation.setDispatcher(new RouteDispatcher() {
            public Form dispatch(String path) {
                if (path.startsWith("/explodes")) {
                    return new Form(path) {
                        @Override
                        public void show() {
                            throw new IllegalStateException("this screen cannot be shown");
                        }
                    };
                }
                return new Form(path);
            }
        });
        try {
            Navigation.navigate("/account/statement");
            flushSerialCalls();
            assertEquals(1, Navigation.getStack().size(), "the fixture stack was not established");

            List<String> restored = new ArrayList<String>();
            restored.add("/explodes");
            try {
                Navigation.restoreStack(restored);
                fail("showing threw, so restoreStack must not report success");
            } catch (IllegalStateException expected) {
                assertEquals("this screen cannot be shown", expected.getMessage());
            }

            assertEquals(1, Navigation.getStack().size(),
                    "the stack was replaced by a restoration that could not be shown, so back() "
                            + "and the next checkpoint describe screens the user never saw");
            assertEquals("/account/statement", Navigation.getStack().get(0).getPath(),
                    "the previous stack was not the one that survived");
        } finally {
            Navigation.setDispatcher(null);
            Navigation.clearStack();
        }
    }

    /**
     * A navigation whose own show() ended the session does not checkpoint afterwards.
     *
     * <p>Navigation notifies continuity AFTER the route's form has been shown, and a show callback
     * calling clear() on an expired login is the ordinary way to end a session. The notification
     * then described a session that no longer existed, and checkpointing it captured whatever the
     * provider still held for the account that had just signed out -- while clear() promises that
     * nothing follows it.</p>
     */
    @EdtTest
    public void aNavigationWhoseShowEndedTheSessionDoesNotCheckpoint() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("secret", "the previous account's work");
        Continuity.setStateProvider(provider);
        Navigation.setDispatcher(new RouteDispatcher() {
            public Form dispatch(String path) {
                // Only the account screen discovers the expiry. An unconditional logout made the
                // login navigation below end the session too, so the control could never pass --
                // and a control that cannot pass is not a control.
                if (!"/account/statement".equals(path)) {
                    return new Form(path);
                }
                return new Form(path) {
                    @Override
                    public void show() {
                        super.show();
                        // "This login has expired."
                        Continuity.clear();
                    }
                };
            }
        });
        try {
            Navigation.navigate("/account/statement");
            flushSerialCalls();

            assertFalse(Continuity.isCheckpointPending(),
                    "the navigation that ended the session left a checkpoint owed, so the "
                            + "signed-out account's payload is written back after logout");
            assertNull(Continuity.getRestorableState(),
                    "a checkpoint was written after the logout that cancelled it");

            // And the NEXT navigation is ordinary: one notification is skipped, not a mode.
            //
            // Asserted on what was WRITTEN, not on the pending flag: the flush performs the
            // checkpoint and checkpoint() clears that flag, so it reads false either way. That
            // observable has now been the wrong one three times on this branch.
            Navigation.navigate("/login");
            flushSerialCalls();
            assertNotNull(Continuity.getRestorableState(),
                    "navigation stopped checkpointing altogether after a logout, so nothing is "
                            + "ever stored again for the account that signs in next");
        } finally {
            Navigation.setDispatcher(null);
            Navigation.clearStack();
        }
    }

    /**
     * A remote route this device cannot store never enters the navigation stack.
     *
     * <p>Decoding accepts a remote document's routes unchecked, deliberately -- another device's
     * mistake must not throw here. But an accepted route reaches the live stack, and the next
     * checkpoint reads that stack back through the validating setter: one route past this
     * device's limit threw out of capture(), left the pending flag set, and every later
     * navigation retried the same throw while nothing was persisted or published again.</p>
     */
    @EdtTest
    public void aRemoteRouteThisDeviceCannotStoreNeverEntersTheStack() {
        RecordingProvider provider = new RecordingProvider();
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(true);
        Navigation.setDispatcher(new RouteDispatcher() {
            public Form dispatch(String path) {
                return new Form(path);
            }
        });
        try {
            StringBuilder huge = new StringBuilder("/");
            while (huge.length() <= 65535) {
                huge.append('r');
            }
            AppState arrival = fromElsewhere("from a device with a longer limit", 55L);
            List<String> routes = new ArrayList<String>();
            routes.add(huge.toString());
            routes.add("/account/statement");
            // Unchecked, exactly as a relay document would arrive.
            arrival.setRoutesUnchecked(routes);

            Continuity.deliver(arrival);
            flushSerialCalls();

            // The usable route still restored; the impossible one did not come with it.
            for (int i = 0; i < Navigation.getStack().size(); i++) {
                assertTrue(Navigation.getStack().get(i).getPath().length() < 65535,
                        "a route this device cannot store entered the navigation stack, so the "
                                + "next checkpoint throws out of capture() for ever");
            }

            // And a checkpoint still works, which is what the stack poisoning prevented.
            Continuity.checkpoint();
            flushSerialCalls();
            assertFalse(Continuity.isCheckpointPending(),
                    "capture() could not complete, so nothing is persisted or published again");
        } finally {
            Navigation.setDispatcher(null);
            Navigation.clearStack();
        }
    }

    /**
     * A relay field of the wrong type is a failed read, not an empty state.
     *
     * <p>Valid syntax is not a valid state. {@code {"device":"other","seq":"10","payload":[]}}
     * parses cleanly and fromMap ignores the array where a payload belongs, leaving routes and
     * payload both empty -- which is an EMPTY state, which the framework reads as a tombstone. One
     * wrong type therefore records the origin as having cleared its work, marks it durably, and
     * releases a queued publish over the server's document.</p>
     */
    @EdtTest
    public void aRelayFieldOfTheWrongTypeIsAFailedRead() throws Exception {
        // The control first: a document this codec writes must still read.
        assertNotNull(StateCodec.fromJson(StateCodec.toJson(fromElsewhere("real", 4L))),
                "a document this codec itself wrote was refused");

        String[] wrong = {
            "{\"device\":\"other\",\"seq\":\"10\",\"payload\":[]}",
            "{\"device\":\"other\",\"seq\":\"10\",\"routes\":{}}",
            "{\"device\":42,\"seq\":\"10\"}",
            "{\"device\":\"other\",\"seq\":\"not a number\"}",
            "{\"device\":\"other\",\"seq\":\"10\",\"title\":[1,2]}",
            // The array is an array; its CONTENTS are the door. Checking the container alone let
            // these through, and the reader that drops what it cannot use turned each of them
            // into a state with fewer routes than the sender meant.
            "{\"device\":\"other\",\"seq\":\"10\",\"routes\":[1]}",
            "{\"device\":\"other\",\"seq\":\"10\",\"routes\":[\"/a\",null]}",
            "{\"device\":\"other\",\"seq\":\"10\",\"routes\":[\"/a\",{\"b\":1}]}",
            // A sequence that is a NUMBER but not one this device can hold. asLong() clamps
            // 1e100 to Long.MAX_VALUE, and once that is the durable high-water mark for this
            // origin every ordinary sequence it sends afterwards is refused as already seen --
            // for the life of the installation.
            "{\"device\":\"other\",\"seq\":1e100}",
            "{\"device\":\"other\",\"seq\":-1e100}",
            "{\"device\":\"other\",\"seq\":10,\"ts\":1e300}",
            // Fractional, which is the same harm in miniature: 5.7 becomes 5, so the sender's
            // own 5 is then indistinguishable from it.
            "{\"device\":\"other\",\"seq\":5.7}",
            // 2^63, one past the range. It is the SAME double as (double) Long.MAX_VALUE --
            // that constant is not representable and rounds up to this -- so a "greater than
            // Long.MAX_VALUE" test compares equal and lets it through, to be clamped straight
            // back to Long.MAX_VALUE by the conversion.
            "{\"device\":\"other\",\"seq\":9223372036854775808}",
            // A raw control character INSIDE a string. JSON forbids it unescaped, and the
            // framework parser appends it rather than stopping -- so the key here is not
            // "payload", the field is dropped as unknown, and what is left is a tombstone.
            "{\"device\":\"other\",\"seq\":\"10\",\"pay\nload\":{\"a\":1}}",
            "{\"device\":\"other\",\"seq\":\"10\",\"title\":\"two\u0000parts\"}",
        };
        for (int i = 0; i < wrong.length; i++) {
            try {
                AppState s = StateCodec.fromJson(wrong[i]);
                fail("a document with a wrong field type was accepted: " + wrong[i]
                        + (s != null && s.isEmpty()
                                ? " -- and as an EMPTY state, which is read as a tombstone"
                                : ""));
            } catch (java.io.IOException expected) {
                assertTrue(expected.getMessage().length() > 0, "the refusal explained nothing");
            }
        }

        // A field this codec does not know is NOT a failure: that is how the format stays
        // extensible, and refusing it would break every sender that knows more than this build.
        assertNotNull(StateCodec.fromJson(
                "{\"device\":\"other\",\"seq\":\"10\",\"somethingNewer\":{\"a\":1}}"),
                "an unknown field was refused, so a newer sender cannot talk to this build");
    }

    /**
     * A provider that ends the session and then throws stops the capture too.
     *
     * <p>The lifecycle check sat on the normal-return path only, so a provider that called
     * clear() and then failed -- cleanup breaking after it noticed an expired account -- carried
     * on and had its state persisted and advertised for the account that had just signed out.</p>
     */
    @EdtTest
    public void aProviderThatLogsOutAndThenThrowsStopsTheCapture() {
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                Continuity.clear();
                throw new IllegalStateException("cleanup failed after the logout");
            }

            public void restoreState(Map<String, Object> payload) {
            }
        });

        Continuity.checkpoint();
        flushSerialCalls();

        assertNull(Continuity.getRestorableState(),
                "a checkpoint was written for the account the provider had just signed out of");
    }

    /**
     * A relay field that is present and null is a failed read.
     *
     * <p>The convenience parser drops null-valued fields before anything can look at them, so
     * {@code {"payload":null}} arrived as an ABSENT payload -- and absent routes with an absent
     * payload is an empty state, which the framework reads as a tombstone. The type checks added
     * for {@code payload:[]} could not see it.</p>
     */
    @EdtTest
    public void aRelayFieldThatIsPresentAndNullIsAFailedRead() throws Exception {
        String[] nulls = {
            "{\"device\":\"other\",\"seq\":\"10\",\"payload\":null}",
            "{\"device\":\"other\",\"seq\":\"10\",\"routes\":null}",
            "{\"device\":\"other\",\"seq\":null}",
        };
        for (int i = 0; i < nulls.length; i++) {
            try {
                AppState got = StateCodec.fromJson(nulls[i]);
                fail("a null field was accepted: " + nulls[i]
                        + (got != null && got.isEmpty() ? " -- as an empty state, a tombstone" : ""));
            } catch (java.io.IOException expected) {
                assertTrue(expected.getMessage().length() > 0, "the refusal explained nothing");
            }
        }

        // Leaving the key OUT is how a sender says absent, and must still work.
        assertNotNull(StateCodec.fromJson("{\"device\":\"other\",\"seq\":\"10\"}"),
                "a document that simply omits a field was refused");
    }

    /** Storage that refuses ONE name and passes everything else through. */
    /** Storage whose writes all fail while reads and deletes still work -- a full disk, from
     * the point of view of code that has to keep two writes in step. */
    static class WriteRefusingStorage extends Storage {
        private final Storage delegate;

        WriteRefusingStorage(Storage delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean writeObject(String name, Object o) {
            return false;
        }

        @Override
        public Object readObject(String name) {
            return delegate.readObject(name);
        }

        @Override
        public boolean exists(String name) {
            return delegate.exists(name);
        }

        @Override
        public void deleteStorageFile(String name) {
            delegate.deleteStorageFile(name);
        }
    }

    static class RefusingOneStorage extends Storage {
        private final Storage delegate;
        private final String refused;

        RefusingOneStorage(Storage delegate, String refused) {
            this.delegate = delegate;
            this.refused = refused;
        }

        @Override
        public boolean writeObject(String name, Object o) {
            if (refused.equals(name)) {
                return false;
            }
            return delegate.writeObject(name, o);
        }

        @Override
        public Object readObject(String name) {
            return delegate.readObject(name);
        }

        @Override
        public boolean exists(String name) {
            return delegate.exists(name);
        }

        @Override
        public void deleteStorageFile(String name) {
            delegate.deleteStorageFile(name);
        }
    }

    /**
     * Declining an arrival with acknowledge() must release it. The state stays in the parked slot
     * otherwise, so getRestorableState() goes on offering something the application has already
     * dealt with -- and the checkpoint hold, which exists to protect a live arrival's only copy,
     * goes on withholding publications for a state nobody will ever restore.
     */
    @EdtTest
    public void acknowledgingAParkedArrivalReleasesItAndTheHeldPublication() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(false);
        final GatedRelay r = new GatedRelay();
        Continuity.setRelay(r);
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitEntered();
            }
        });
        r.release();
        pause(300L);
        final int before = r.sent.size();

        AppState arrival = fromElsewhere("the user says no", 101L);
        Continuity.deliver(arrival);
        flushSerialCalls();
        Continuity.checkpoint();
        pause(250L);
        assertEquals(before, r.sent.size(), "the checkpoint should be held while it is parked");

        Continuity.acknowledge(arrival);

        // Identity, not null. getRestorableState() falls back to the LOCAL checkpoint when
        // nothing is parked, and this test just wrote one -- so asserting null here fails on
        // correct behaviour. What must not come back is the arrival that was acknowledged.
        AppState left = Continuity.getRestorableState();
        assertFalse(left != null && "some-other-device".equals(left.getDeviceId()),
                "an acknowledged arrival is still being offered for restoration");
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitAnySince(before);
            }
        });
        assertTrue(r.sent.size() > before,
                "the checkpoint stayed held behind a state the application had acknowledged");
    }

    /**
     * And an arrival that expires while parked releases the hold too. The hold protects the
     * relay's only copy of a LIVE arrival; an expired one will never be restored by anything, so
     * holding a checkpoint behind it just means it never reaches the user's other devices.
     */
    @EdtTest
    public void anExpiredParkedArrivalReleasesTheHeldPublication() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(false);
        final GatedRelay r = new GatedRelay();
        Continuity.setRelay(r);
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitEntered();
            }
        });
        r.release();
        pause(300L);
        final int before = r.sent.size();

        Continuity.parkForTest(fromElsewhere("goes stale while waiting", 103L));
        Continuity.setMaxAge(1L);
        Continuity.checkpoint();
        pause(250L);

        // Asking is what discards the expired arrival, and the hold has to go with it. Checked
        // by identity for the reason the sibling above gives: a local checkpoint is a legitimate
        // answer here, the expired arrival is not.
        AppState left = Continuity.getRestorableState();
        assertFalse(left != null && "some-other-device".equals(left.getDeviceId()),
                "an expired parked state was still offered");
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitAnySince(before);
            }
        });
        assertTrue(r.sent.size() > before,
                "the checkpoint stayed held behind an arrival that had already expired");
        Continuity.setMaxAge(0L);
    }

    /**
     * An acknowledged origin stays refused even after the in-memory map has evicted it. lastSeen
     * and durableSeen are bounded independently and hold different sets -- every arrival versus
     * only the completed ones -- so a busy relay can push an acknowledged origin out of lastSeen
     * while its durable mark remains. Consulting only lastSeen let the duplicate through and ran
     * the application's listeners a second time, which is exactly what the durable mark is for.
     */
    @EdtTest
    public void anAcknowledgedOriginIsRefusedAfterItsDedupEntryIsEvicted() {
        AppState handled = fromElsewhere("dealt with", 5L);
        Continuity.acknowledge(handled);

        // The crowd has to be arrivals that were ADMITTED and never COMPLETED, which is the
        // difference between the two maps this test is about. A provider that throws is the
        // cheapest way to say that: every one of these enters lastSeen on admission and none of
        // them earns a durable mark.
        //
        // They used to be empty states, which is a tombstone -- and a consumed tombstone is a
        // completed arrival, so once tombstones started being marked durably the crowd competed
        // for the durable map too and evicted the acknowledgement this test exists to protect.
        // The failure was real and the test was the thing that was wrong: its own premise is
        // "every arrival versus only the completed ones", and an empty state is both.
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                return new HashMap<String, Object>();
            }

            public void restoreState(Map<String, Object> payload) {
                throw new IllegalStateException("nothing here completes");
            }
        });
        for (int i = 0; i < 90; i++) {
            Map<String, Object> payload = new HashMap<String, Object>();
            payload.put("crowd", Integer.valueOf(i));
            Continuity.deliver(new AppState()
                    .setPayload(payload)
                    .setDeviceId("crowd-" + i)
                    .setSequence(i + 1)
                    .setTimestamp(System.currentTimeMillis()));
        }
        flushSerialCalls();

        final int[] seen = new int[1];
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                seen[0]++;
                return true;
            }
        });
        Continuity.deliver(handled);
        flushSerialCalls();

        assertEquals(0, seen[0],
                "a state from an acknowledged origin was delivered again once the in-memory "
                        + "dedup entry had been evicted, so its side effects run twice");
    }

    /**
     * A tombstone supersedes work still parked from the same origin. An empty state is that
     * device saying it has nothing any more, so an older state of its own that is waiting on the
     * user no longer exists -- offering it keeps proposing work the origin cleared, and the
     * publication hold keeps this device's checkpoints off the relay behind it.
     */
    @EdtTest
    public void aTombstoneClearsWorkStillParkedFromTheSameOrigin() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        Continuity.setAutoRestore(false);
        final GatedRelay r = new GatedRelay();
        Continuity.setRelay(r);
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitEntered();
            }
        });
        r.release();
        pause(300L);
        final int before = r.sent.size();

        Continuity.deliver(fromElsewhere("waiting on the user", 111L));
        flushSerialCalls();
        Continuity.checkpoint();
        pause(250L);
        assertEquals(before, r.sent.size(), "the checkpoint should be held while it is parked");

        // The same origin says it has nothing now.
        Continuity.deliver(new AppState()
                .setDeviceId("some-other-device")
                .setSequence(112L)
                .setTimestamp(System.currentTimeMillis()));
        flushSerialCalls();

        AppState left = Continuity.getRestorableState();
        assertFalse(left != null && "some-other-device".equals(left.getDeviceId()),
                "work the origin cleared with a tombstone is still being offered");
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitAnySince(before);
            }
        });
        assertTrue(r.sent.size() > before,
                "the checkpoint stayed held behind work the origin had already cleared");
    }

    /**
     * A logout between queueing a publish and the worker reaching the network must stop the
     * request. RestStateRelay resolves getToken() INSIDE publish(), so a quick logout and login
     * would otherwise send the previous account's state under the next account's credentials --
     * and clear() documents that nothing follows it.
     */
    @EdtTest
    public void aLogoutStopsAPublishThatHasNotReachedTheNetwork() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        final GatedRelay r = new GatedRelay();
        Continuity.setRelay(r);
        awaitOffEdt(new Runnable() {
            public void run() {
                r.awaitEntered();
            }
        });
        r.release();
        pause(300L);

        // Queue a publish, then sign out before the worker can get to the wire. The worker
        // confirms its session on the event thread, and this test body IS the event thread, so
        // the confirmation cannot run until after clear() below.
        Continuity.checkpoint();
        final int before = r.sent.size();
        Continuity.clear();
        pause(500L);

        assertEquals(before, r.sent.size(),
                "a state queued before the logout was sent afterwards, which with a token "
                        + "resolved inside publish() means the previous account's work went out "
                        + "under the next account's credentials");
    }

    /**
     * A fetch that failed is not an empty relay. Collapsing a timeout into the same null said the
     * read had succeeded and found nothing -- which is what makes overwriting the single document
     * safe -- so a queued checkpoint replaced another device's state this one had never read.
     */
    @EdtTest
    public void aFailedFetchDoesNotAuthoriseAPublish() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("n", Integer.valueOf(1));
        Continuity.setStateProvider(provider);
        final ThrowingFetchRelay r = new ThrowingFetchRelay();
        Continuity.setRelay(r);

        Continuity.checkpoint();
        Continuity.pollRelay();
        pause(600L);

        assertTrue(r.fetches() > 0, "the relay should have been asked");
        assertEquals(0, r.published(),
                "a checkpoint was published on the strength of a fetch that failed, so another "
                        + "device's state can be overwritten without ever having been read");
    }

    /** A relay whose fetch always fails, which is what a timeout looks like. */
    static class ThrowingFetchRelay implements StateRelay {
        private final java.util.concurrent.atomic.AtomicInteger fetched =
                new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger posts =
                new java.util.concurrent.atomic.AtomicInteger();

        public void publish(AppState state) {
            posts.incrementAndGet();
        }

        public AppState fetch() throws java.io.IOException {
            fetched.incrementAndGet();
            throw new java.io.IOException("the endpoint timed out");
        }

        int fetches() {
            return fetched.get();
        }

        int published() {
            return posts.get();
        }
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
        /**
         * Waits until more than `count` states have been sent.
         *
         * Deliberately shorter than the harness's own 5s limit. At 5000 a regression raced it and
         * the test reported "FormTest timed out" instead of the assertion that explains what
         * broke -- a failure message that sends the next reader hunting a hung event thread.
         */
        void awaitAnySince(int count) {
            long deadline = System.currentTimeMillis() + 2500L;
            while (System.currentTimeMillis() < deadline && sent.size() <= count) {
                sleepBriefly();
            }
        }

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
