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
