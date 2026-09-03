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

import com.codename1.continuity.spi.ContinuityBridge;
import com.codename1.continuity.spi.ContinuityCallback;
import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.router.Navigation;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Saves what the user was doing, brings it back when the app starts again, and -- where the
/// platform or your own endpoint can carry it -- lets them pick it up on another device.
///
/// ```java
/// // in init()
/// Continuity.setStateProvider(new StateProvider() {
///     public Map<String, Object> saveState() {
///         Map<String, Object> m = new HashMap<String, Object>();
///         m.put("draft", draftField.getText());
///         return m;
///     }
///     public void restoreState(Map<String, Object> payload) {
///         pendingDraft = (String) payload.get("draft");
///     }
/// });
///
/// // in start()
/// if (!Continuity.restore()) {
///     Navigation.navigate("/home");
/// }
/// ```
///
/// #### What is saved
///
/// Two halves. The framework contributes the `com.codename1.router.Navigation` stack, so an app
/// whose screens are declared with `@Route` gets them back with no code at all. Your
/// `StateProvider` contributes everything else. An app that navigates with `new MyForm().show()`
/// has no route stack to save -- those navigations are not addressable -- and restores from the
/// payload alone.
///
/// #### When it is saved
///
/// Continuously, not at shutdown. Every navigation marks the state dirty and a checkpoint is
/// written once per event loop pass, so by the time the operating system suspends the app the work
/// is already done. This is deliberate: on Android the platform blocks its own main thread until
/// the app's `stop()` returns, and an app that did its saving there would be paying for it on
/// every single suspend. Call `checkpoint()` directly after changing something the provider
/// reports but no navigation touched.
///
/// #### Getting it back
///
/// `restore()` returns true when it showed something, so `start()` reads as "restore, or else
/// begin". It is never called for you: an app that adopts this API decides where restoration fits
/// in its own launch, and an app that does not is completely unaffected.
///
/// #### Other devices
///
/// `isContinuationSupported()` reports whether this platform can advertise the current state to
/// the user's nearby devices; on Apple platforms it can, elsewhere it cannot and the call is a
/// no-op rather than an error. For everything the platform will not carry -- iOS to Android, two
/// devices that are never together -- set a `StateRelay`, which is your own endpoint. Codename One
/// runs no server for this, because deciding which states belong to the same person is your
/// account system's job.
///
/// Arriving states are offered to every `ContinuityListener` before anything happens, and this
/// device's own echo is never offered at all.
///
/// #### Zero cost when unused
///
/// Referencing this package is what makes the build declare the activity type on Apple platforms
/// and compile the native continuation handling in. An app that never touches
/// `com.codename1.continuity` gets none of it.
///
/// #### Threading
///
/// Call this class from the event dispatch thread, like the rest of the toolkit. Codename One is
/// single threaded: every method here runs on the EDT and every field it keeps is owned by the
/// EDT, so there is nothing to synchronize and nothing that can interleave.
///
/// Two kinds of foreign thread exist, and both hand over at the boundary rather than reaching in.
/// A port delivering a continuation arrives on the platform's own thread and is marshalled with
/// `com.codename1.ui.Display#callSerially`. The relay's `publish` and `fetch` are blocking calls
/// that must not sit on the EDT, so they run on a worker -- one that is handed the state it needs
/// as a parameter, touches no field of this class, and returns its answer through `callSerially`
/// as well. That is the whole concurrency design, and it is deliberately the toolkit's: one
/// thread on each side of a boundary, never two on the same state.
public final class Continuity {
    /// The `Storage` entry the checkpoint is written to.
    static final String STORAGE_KEY = "CN1$Continuity";

    /// Where this installation's device id lives, so a state can recognize its own echo across
    /// restarts.
    /// Every value below lives in Storage, not Preferences, and the names are kept only because
    /// they are what the data was already called.
    ///
    /// Preferences cannot say whether a write reached the disk. set() puts the value in a static
    /// Hashtable and then calls save(), which discards Storage.writeObject()'s result -- and
    /// get() reads that same Hashtable, so reading a value back after writing it confirms the
    /// cache and nothing else. A verification built that way was added here once and could not
    /// detect the failure it was written for.
    ///
    /// These three all have durable meaning: an id that changes across a restart makes every
    /// state this device sent look foreign, a counter that reloads lower has receivers refusing
    /// this device until it catches up, and a mark that never lands lets an acknowledged state be
    /// acted on twice. Storage.writeObject returns a boolean, so the failure is at least visible.
    static final String PREF_DEVICE_ID = "CN1$ContinuityDevice";

    /// Where the sequence counter lives. Persisted because a counter that restarted at zero would
    /// make every state after a relaunch look older than one the receiver had already seen.
    static final String PREF_SEQUENCE = "CN1$ContinuitySeq";

    /// Where the per-device delivery high-water marks live between runs.
    static final String PREF_SEEN = "CN1$ContinuitySeen";

    /// How many devices' marks are kept.
    ///
    /// A user has a handful of devices, but the ids come off a relay and nothing stops one from
    /// feeding many, so this is bounded. When it overflows the LOWEST sequences go: those are the
    /// devices that have been quiet longest, and losing a mark costs one duplicate delivery rather
    /// than anything durable.
    private static final int MAX_SEEN = 64;

    /// How long to wait for the application to produce its first form before giving up on a
    /// continuation that cold-launched it. A launch that never produces one is a broken
    /// application, and restoring minutes later into whatever the user is doing by then is worse
    /// than not restoring at all.
    private static final long WINDOW_WAIT_MILLIS = 15000L;

    private static final List<ContinuityListener> listeners = new ArrayList<ContinuityListener>();

    /// Highest sequence seen from each device, so a state delivered twice -- which happens
    /// routinely, since a continuation and a relay can carry the same one -- acts once.
    /// Insertion-ordered, so the cap can evict the device that has been quiet longest.
    ///
    /// A HashMap forced the eviction to pick a victim by comparing SEQUENCES, and sequences are
    /// each origin's own counter -- a device at 5000 is not busier than one at 3, it has simply
    /// been counting longer. Worse, the lowest sequence in a full map is usually a device that
    /// has just been set up and sent its first state, so admitting it evicted it immediately and
    /// the dispatch queued behind admit() found its mark gone and dropped a perfectly good
    /// continuation without a word.
    private static final Map<String, Long> lastSeen = new LinkedHashMap<String, Long>();

    /// The marks that are allowed to reach storage: states this device actually COMPLETED.
    ///
    /// Separate from `lastSeen`, which holds every state that was admitted, because those two
    /// sets are not the same and writing the wrong one throws states away. A state admitted and
    /// then failed -- a provider that threw, routes that could not be rebuilt -- stays in
    /// lastSeen so it is not re-dispatched twice in this run, and must NOT become durable:
    /// serializing the whole map meant an unrelated state completing later carried the failed
    /// one to disk with it, and after a restart the relay's only usable copy was refused. That
    /// is exactly the gating commit() performs, undone by the writer.
    private static final Map<String, Long> durableSeen = new LinkedHashMap<String, Long>();

    // EDT-owned, like every field in this class. See the threading note on the class.
    private static StateProvider provider;
    private static StateRelay relay;
    private static ContinuityBridge bridge;
    private static boolean bridgeOverridden;
    private static boolean enabled;
    private static boolean autoRestore = true;
    private static boolean flushScheduled;

    /// True while an inbound state is being applied, so the navigation it causes is not mistaken
    /// for the user moving and republished.
    private static boolean applyingRestore;

    /// True once a synced-store listener has asked for the inbound seam, independently of
    /// `enabled`.
    private static boolean storeCallbackInstalled;
    private static String title;
    private static long sequence;
    private static long maxAge;

    /// The device id, lazily created.
    private static String deviceId;

    /// Whether a checkpoint is owed.
    private static boolean dirty;

    /// True while the cold-launch waiter is running, so a second arrival does not start another.
    private static boolean waitingForWindow;

    /// A state that arrived and could not be shown yet.
    private static AppState parked;

    /// Which relay session the in-flight worker belongs to, bumped by `clear()`, `setRelay()` and
    /// `reset()`.
    ///
    /// Ordinary bookkeeping rather than a memory-model device: a relay round trip is the one
    /// thing here that outlives the EDT turn that started it, so a fetch begun before a logout
    /// can return after one. The worker carries the session it was started in and the completion
    /// -- which runs back on the EDT -- ignores an answer whose session has moved on, rather than
    /// delivering the previous account's state into the next account's screen.
    private static int relaySession;

    private Continuity() {
    }

    // ------------------------------------------------------------------
    // Enabling
    // ------------------------------------------------------------------

    /// Turns the framework on. Called for you by `setStateProvider(StateProvider)`; call it
    /// directly when the route stack alone is all you need saved.
    ///
    /// Nothing before this call has any effect, which is what keeps an app that does not use this
    /// API behaving exactly as it always did.
    public static void enable() {
        if (enabled) {
            return;
        }
        // Registered once, and only from here, so that a build which merely links this class --
        // because something else in the framework mentions it -- never installs a callback or
        // touches storage.
        Util.register(AppState.OBJECT_ID, AppState.class);
        // getDeviceId() rather than loadDeviceId(): it is the one that mints and persists a UUID,
        // so the id this device sends is the id it will still be using after a restart.
        deviceId = getDeviceId();
        sequence = loadSequence();
        // Merged rather than replaced. A mark already in memory is from this run and is at least
        // as new as the stored one.
        Map<String, Long> restored = readSeen();
        for (Map.Entry<String, Long> e : restored.entrySet()) {
            // The two maps are advanced INDEPENDENTLY, because they can disagree and gating one
            // on the other loses a mark. A loaded value describes a state a previous run
            // COMPLETED, so it belongs in the durable set even when the in-memory set already
            // holds something newer from this run -- deciding both on the lastSeen comparison
            // alone dropped it, and the durable set is the one that survives the next restart.
            //
            // Found by auditing the read sites after a review found the same divergence at
            // admission, rather than waiting for it to be reported.
            long loaded = e.getValue().longValue();
            Long inMemory = lastSeen.get(e.getKey());
            if (inMemory == null || inMemory.longValue() < loaded) {
                recordSeen(e.getKey(), loaded, false);
            }
            Long durable = durableSeen.get(e.getKey());
            if (durable == null || durable.longValue() < loaded) {
                recordDurable(e.getKey(), loaded);
            }
        }
        enabled = true;
        ContinuityBridge b = bridgeInternal();
        if (b != null) {
            try {
                b.setCallback(new Callback());
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    /// Turns the framework off. Checkpoints stop, the advertised activity is withdrawn, and
    /// arriving states are ignored. What is already in storage is left alone -- use `clear()` to
    /// remove it.
    public static void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        dirty = false;
        parked = null;
        // The relay session ends too. A checkpoint whose publish was deferred behind a fetch is
        // still sitting in the slot, and pollFinished() starts a publisher when that fetch lands
        // -- so without this a state queued before disable() went out on the wire after it had
        // returned, which is exactly what "checkpoints stop" is supposed to mean. clear() has
        // always done this; disable() is a weaker promise about the same machinery, not a
        // different one.
        endRelaySession();
        clearContinuation();
    }

    /// Whether the framework is on.
    ///
    /// #### Returns
    ///
    /// true when enabled
    public static boolean isEnabled() {
        return enabled;
    }

    /// Whether this platform can save and restore state at all. False only where there is no
    /// storage to write to, which in practice means before `Display` has been initialized.
    ///
    /// #### Returns
    ///
    /// true when state can be saved on this device
    public static boolean isSupported() {
        return Display.isInitialized();
    }

    /// Whether this platform can advertise the current state to the user's other devices while
    /// they are together.
    ///
    /// Branch on this rather than on the platform name: it is true on Apple platforms today and
    /// the set is expected to grow, and a `com.codename1.ui.Display#getPlatformName` test would
    /// have to be found and changed when it does.
    ///
    /// #### Returns
    ///
    /// true when continuation to a nearby device is supported
    public static boolean isContinuationSupported() {
        ContinuityBridge b = bridgeInternal();
        try {
            return b != null && b.isContinuationSupported();
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------

    /// Installs the object that supplies and consumes the application half of the state, and
    /// enables the framework.
    ///
    /// #### Parameters
    ///
    /// - `p`: the provider, or null to contribute nothing beyond the route stack
    public static void setStateProvider(StateProvider p) {
        provider = p;
        enable();
    }

    /// The installed state provider, or null.
    ///
    /// #### Returns
    ///
    /// the provider
    public static StateProvider getStateProvider() {
        return provider;
    }

    /// Registers a listener for states arriving from elsewhere.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public static void addContinuationListener(ContinuityListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public static void removeContinuationListener(ContinuityListener l) {
        listeners.remove(l);
    }

    /// Installs the endpoint that carries state to devices the platform will not reach, and asks
    /// it immediately for anything newer than what is here.
    ///
    /// #### Parameters
    ///
    /// - `r`: the relay, or null to stop using one
    public static void setRelay(StateRelay r) {
        // A different endpoint is a different destination for anything queued for the old one and
        // a different source for a fetch already in flight. Without this a state retained after a
        // failed send was published to the REPLACEMENT endpoint -- an application's data sent
        // somewhere it was never handed to -- and a poll started against the relay the app has
        // just removed could still deliver its answer afterwards.
        endRelaySession();
        relay = r;
        if (r != null) {
            enable();
            pollRelay();
        }
    }

    /// The installed relay, or null.
    ///
    /// #### Returns
    ///
    /// the relay
    public static StateRelay getRelay() {
        return relay;
    }

    /// Whether a restorable state found at startup, or arriving from another device, is applied
    /// automatically. On by default.
    ///
    /// Turning it off leaves `restore()` and every listener working exactly as before; what stops
    /// is the framework acting on its own. Use it when the decision to move the user is always
    /// the app's.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to restore automatically
    public static void setAutoRestore(boolean b) {
        autoRestore = b;
    }

    /// Whether automatic restoration is on.
    ///
    /// #### Returns
    ///
    /// true when on
    public static boolean isAutoRestore() {
        return autoRestore;
    }

    /// Sets the label a receiving device may show before the user accepts a continuation -- "Draft
    /// to Dana", "Invoice 2031". Update it as the user moves around; it is read at every
    /// checkpoint.
    ///
    /// #### Parameters
    ///
    /// - `t`: the label, or null for none
    public static void setTitle(String t) {
        title = t;
    }

    /// The current continuation label, or null.
    ///
    /// #### Returns
    ///
    /// the label
    public static String getTitle() {
        return title;
    }

    /// How old a stored state may be and still be restored, in milliseconds. Zero, the default,
    /// means no limit: an app the user opens after a month comes back where they left it, which is
    /// what they expect of it.
    ///
    /// Set it when coming back is only meaningful for a while -- a checkout, a queue position, a
    /// booking hold.
    ///
    /// #### Parameters
    ///
    /// - `millis`: the limit, or 0 for none
    public static void setMaxAge(long millis) {
        maxAge = millis < 0 ? 0 : millis;
    }

    /// The staleness limit in milliseconds, or 0 for none.
    ///
    /// #### Returns
    ///
    /// the limit
    public static long getMaxAge() {
        return maxAge;
    }

    /// This installation's device id, the value that lets a state be recognized as this device's
    /// own echo when it comes back through a relay. Stable across restarts.
    ///
    /// #### Returns
    ///
    /// the device id, never null
    public static String getDeviceId() {
        String id = deviceId;
        if (id != null) {
            return id;
        }
        // Read rather than cached here. enable() is what installs the field, and this is
        // reachable before that -- a continuation can arrive on a cold launch -- so the
        // pre-enable path answers from storage instead. loadDeviceId() persists the UUID it
        // mints, so asking twice gives the same answer; there is no second identity to create.
        return loadDeviceId();
    }

    // ------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------

    /// Internal. Called by `com.codename1.router.Navigation` after every change to the navigation
    /// stack; schedules a checkpoint rather than taking one, so a burst of navigations costs a
    /// single write.
    public static void routeStackChanged() {
        if (!enabled) {
            return;
        }
        if (applyingRestore) {
            // See restore(). The stack is being rebuilt from a state we already hold, so there is
            // nothing new to record, and publishing it would start a restore loop between this
            // device and the one that sent it. Not marked dirty either -- restore() persists the
            // state it applied.
            return;
        }
        dirty = true;
        if (!Display.isInitialized() || flushScheduled) {
            return;
        }
        // Coalesced: a burst of navigations in one cycle costs a single write.
        flushScheduled = true;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                flushScheduled = false;
                if (isCheckpointPending()) {
                    checkpoint();
                }
            }
        });
    }

    /// Writes the current state now, and offers it to every enabled channel: storage always, the
    /// platform's continuation where there is one, and the relay if one is set.
    ///
    /// Cheap enough to call freely -- the state is a list of paths and a small map -- but it does
    /// touch storage, so it belongs at the end of a change rather than inside a loop.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the provider returned a payload that cannot cross to
    ///   another device
    public static void checkpoint() {
        if (!enabled) {
            return;
        }
        // TWO answers, because the two failures need opposite handling and one flag gave them the
        // same. A payload that could not be read still leaves routes worth saving and worth
        // advertising; a sequence that did not reach the disk makes PUBLISHING the harmful part,
        // because the receiving devices record that number durably and this one will hand it out
        // again after a restart -- so every checkpoint it then sends is refused as already seen
        // until the counter climbs past it. Folding them together published exactly that.
        boolean[] payloadFailed = new boolean[1];
        boolean[] sequenceFailed = new boolean[1];
        AppState state = capture(payloadFailed, sequenceFailed);
        if (state == null) {
            return;
        }
        if (payloadFailed[0]) {
            // The last payload is CARRIED FORWARD rather than replaced by nothing. Writing an
            // empty state over a stored draft loses it, for a read that may well succeed next
            // time; skipping the write instead loses the routes, which are current and real.
            // Carrying forward gives up neither -- the checkpoint keeps the newest routes and the
            // newest payload that ever read cleanly.
            AppState previous = readStored();
            if (previous != null && !previous.getPayload().isEmpty()) {
                state.setPayloadUnchecked(previous.getPayload());
            }
        }
        // Owed while anything about this capture was not durable, so a later suspend retries it.
        dirty = payloadFailed[0] || sequenceFailed[0];
        if (!persist(state)) {
            dirty = true;
        }
        if (sequenceFailed[0]) {
            // Stored locally and told to nobody. The local copy is still worth having -- this
            // device does not deduplicate against itself -- but a sequence that cannot be proved
            // durable must not reach another device, because a receiver's mark outlives the
            // counter that produced it.
            return;
        }
        publishContinuation(state);
        publishToRelay(state);
    }

    /// Internal. Whether a checkpoint is owed -- something changed since the last one was
    /// written.
    ///
    /// Exists so a port with a suspend callback can skip the event-thread round trip entirely in
    /// the common case, where the write-through already happened as the user navigated.
    ///
    /// #### Returns
    ///
    /// true when `checkpoint()` would write something new
    public static boolean isCheckpointPending() {
        return enabled && dirty;
    }

    /// Builds a state from the route stack and the provider. Useful for sending one somewhere of
    /// your own.
    ///
    /// The state itself is not stored -- only `checkpoint()` does that -- but the sequence counter
    /// it allocates is remembered, so states keep a rising order across a relaunch even for an
    /// application that never checkpoints.
    ///
    /// #### Returns
    ///
    /// the current state, or null when the framework is not enabled
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the provider returned an unrepresentable payload
    public static AppState capture() {
        // Best effort, which is what this method has always been: an application calling it to
        // feed its own transport wants whatever can be gathered. checkpoint() asks the private
        // form instead, because for the DURABLE path a provider failure is not "no payload".
        return capture(new boolean[1], new boolean[1]);
    }

    /// As above, reporting the two ways a capture can come up short -- SEPARATELY, because the
    /// caller has to treat them differently.
    ///
    /// `payloadFailed` means the provider threw and this state has no payload of its own. The
    /// routes are still real and the state is still worth storing and advertising.
    ///
    /// `sequenceFailed` means the counter did not reach the disk. That one makes publishing the
    /// harmful act: a receiver records the sequence durably, this device hands the same number
    /// out again after a restart, and every later checkpoint is refused as already seen.
    ///
    /// They were one flag, twice: first called providerFailed and then given a second meaning,
    /// then renamed to captureFailed to match -- which papered over the fact that the two answers
    /// call for opposite handling rather than a better name.
    private static AppState capture(boolean[] payloadFailed, boolean[] sequenceFailed) {
        if (!enabled) {
            return null;
        }
        StateProvider p = provider;
        AppState state = new AppState();
        state.setRoutes(currentRoutes());
        if (p != null) {
            Map<String, Object> payload = null;
            try {
                payload = p.saveState();
            } catch (Throwable t) {
                // The provider is application code running on a housekeeping path. Its failure
                // must not take down the navigation that triggered the checkpoint, so the routes
                // are still saved and the payload is simply absent from THIS state.
                //
                // Reported, though, because "absent" and "could not be gathered" are different
                // answers and the caller decides what to do with them. A payload-only app whose
                // provider threw would otherwise checkpoint an EMPTY state over the last good
                // one, publish it, and clear the pending flag -- so a process death lost the
                // draft that was safely stored a moment earlier, because of a failure that may
                // well be transient.
                Log.e(t);
                payloadFailed[0] = true;
            }
            if (payload != null) {
                // NOT caught. An unrepresentable value is a programming error with exactly one
                // correct moment to surface -- here, naming the key -- rather than as a payload
                // that silently stops arriving on the other device.
                state.setPayload(payload);
            }
        }
        sequence = nextSequence();
        long seq = sequence;
        String label = title;
        // Persisted HERE rather than in persist(), which only checkpoint() reaches. capture() is
        // public and documented for sending a state through the application's own transport, and
        // a counter that only advanced durably on the checkpoint path restarted lower after a
        // relaunch -- so a receiver still holding the old high-water mark in lastSeen silently
        // ignored every state until the counter caught up.
        if (!rememberSequence(seq)) {
            sequenceFailed[0] = true;
        }
        state.setDeviceId(getDeviceId())
                .setSequence(seq)
                .setTimestamp(System.currentTimeMillis())
                .setTitle(label);
        return state;
    }

    // ------------------------------------------------------------------
    // Restoring
    // ------------------------------------------------------------------

    /// The state waiting to be restored: one that arrived from another device if there is one,
    /// otherwise the last checkpoint written on this device.
    ///
    /// #### Returns
    ///
    /// the state, or null when there is nothing to restore or it is older than `getMaxAge()`
    public static AppState getRestorableState() {
        AppState waiting = parked;
        if (waiting != null) {
            // Aged like a stored one. A parked state is one that arrived from elsewhere and could
            // not be shown yet -- during a cold launch, say -- and time passes while it waits, so
            // exempting it would have let exactly the expiry the application configured slip
            // through on the one path where the delay is longest.
            if (isTooOld(waiting)) {
                // Cleared, and then we keep looking. Returning null here reported "nothing to
                // restore" while a perfectly valid local checkpoint sat in storage -- which is
                // ordinary with automatic restore off and the user still navigating -- so a
                // single restore() call told the application to show its initial screen instead.
                parked = null;
                // And the checkpoint that was waiting behind it goes out. The hold is there to
                // protect the relay's only copy of a live arrival; this one has expired and will
                // not be restored by anything, so holding a publication for it forever is just a
                // checkpoint that never reaches the user's other devices.
                startPublisher();
            } else {
                return waiting;
            }
        }
        AppState stored = readStored();
        if (stored == null || isTooOld(stored)) {
            return null;
        }
        return stored;
    }

    /// Whether `getMaxAge()` has passed since a state was produced.
    ///
    /// A state with no timestamp is never too old: it came from a build that did not set one, and
    /// discarding it would be reading "unknown" as "expired".
    private static boolean isTooOld(AppState state) {
        return maxAge > 0 && state.getTimestamp() > 0
                && System.currentTimeMillis() - state.getTimestamp() > maxAge;
    }

    /// Restores whatever `getRestorableState()` offers.
    ///
    /// Written to read as "restore, or else begin":
    ///
    /// ```java
    /// public void start() {
    ///     if (!Continuity.restore()) {
    ///         Navigation.navigate("/home");
    ///     }
    /// }
    /// ```
    ///
    /// #### Returns
    ///
    /// true when a form was shown, so the caller should not show its own
    public static boolean restore() {
        AppState state = getRestorableState();
        if (state == null) {
            return false;
        }
        // Cleared only AFTER the restore has actually happened. Clearing first threw away the
        // only copy, and because dispatch had already written the sender's durable mark, a relay
        // retry was rejected after the next launch too -- a state that was never restored then
        // could not be restored at all, which is the one outcome this feature exists to prevent.
        boolean shown = restore(state);
        // Released because the state was APPLIED, not because a form appeared. Gating this on
        // `shown` kept a payload-only arrival parked for ever: restore(state) hands the payload
        // to the provider and returns false, so every later call re-applied the same state.
        if (isSameState(parked, state)) {
            parked = null;
            // The slot is what holds a publication back; the decision has been made, so anything
            // waiting on it can go out now.
            startPublisher();
        }
        return shown;
    }

    /// Records that the application has handled `state` itself, so it is not offered again.
    ///
    /// For the pattern `ContinuityListener` documents: do the work yourself and return false. That
    /// path never reaches restore(), so nothing recorded the acknowledgement durably -- the
    /// sequence stayed in this process only, and after a relaunch the relay's unchanged document
    /// was accepted again and the listener repeated its side effects, against the act-once
    /// guarantee.
    ///
    /// Deliberately NOT inferred from a false return. False also means "keep it, I will prompt and
    /// call restore() when the user accepts", and marking that handled immediately would lose the
    /// state if the process died before they answered -- which is the same data loss as marking a
    /// parked state. The two intentions are different, so the application says which it means.
    ///
    /// #### Parameters
    ///
    /// - `state`: the state that has been dealt with
    public static void acknowledge(AppState state) {
        if (state == null) {
            return;
        }
        noteActedOn(state);
    }

    /// Restores a specific state: hands its payload to the provider, then replays its route stack.
    ///
    /// This is the second half of the "ask first" pattern -- a `ContinuityListener` that returned
    /// false to hold a state calls this once the user accepts it.
    ///
    /// #### Parameters
    ///
    /// - `state`: the state, or null
    ///
    /// #### Returns
    ///
    /// true when a form was shown
    public static boolean restore(final AppState state) {
        if (state == null) {
            return false;
        }
        // Whether any part of this state actually reached the application. Nothing is written
        // or acknowledged until something has: a route-only state naming routes this build no
        // longer registers applies nothing at all, and replacing the stored checkpoint with it
        // destroyed the user's own restorable position -- while acknowledging it stopped the
        // relay offering it again, so the next launch found only the unusable state where a good
        // checkpoint had been. Left alone, the old checkpoint still restores and the relay may
        // offer this one to a build that understands it.
        boolean applied = false;
        // Separate from `applied`, because "there was nothing to do" and "I tried and could not"
        // need opposite answers and one flag cannot say both. A provider that throws is the
        // second: it can happen transiently on a cold launch, when a dependency it needs is not
        // up yet, and treating it as "nothing to do" marked the state handled with none of its
        // payload applied and nothing stored -- so the relay's remaining copy was refused after
        // the next launch and the state was gone.
        boolean failed = false;
        StateProvider p = provider;
        if (p != null) {
            try {
                p.restoreState(state.getPayload());
                // An empty payload is not an application. It is what a route-only state carries,
                // and counting it would make the question above answer yes for every state.
                applied = !state.getPayload().isEmpty();
            } catch (Throwable t) {
                Log.e(t);
                failed = true;
            }
        }
        List<String> routes = state.getRoutes();
        if (routes.isEmpty()) {
            // Payload-only restoration, which is what an app that does not use @Route gets. The
            // provider has been given everything there is, and false is deliberate: it is what
            // makes "restore, or else begin" still show a screen.
            //
            // A review asked for true here, on the reading that the provider shows the form and
            // the caller then shows a second one over it. That is only true of a provider written
            // that way, and StateProvider.restoreState tells providers not to be. True would be
            // the worse failure of the two: a provider that only populates fields -- the
            // documented shape -- would leave the application on no screen at all.
            commit(state, applied, failed);
            return false;
        }
        // Applying a state is not the user navigating, and the difference is not cosmetic. The
        // rebuilt stack reaches routeStackChanged(), which checkpoints, which republishes what we
        // just received under THIS device's id and a fresh sequence. The originating device then
        // cannot recognize its own work -- it arrives as a foreign device's state -- so it
        // restores it and republishes in turn, and the two bounce the same stack back and forth,
        // re-navigating the user on every poll.
        boolean shown;
        applyingRestore = true;
        try {
            shown = Navigation.restoreStack(routes);
        } catch (Throwable t) {
            Log.e(t);
            shown = false;
        } finally {
            applyingRestore = false;
        }
        if (!shown && !applied) {
            // Routes were named, none could be rebuilt, and nothing else in the state applied
            // either -- an attempt that failed outright, so it stays on the relay for a launch
            // that can use it.
            //
            // Only when nothing else applied. A payload the provider took is real work, already
            // in the application, and discarding it because the ROUTES are stale threw it away
            // twice over: never written to the local checkpoint, so a cold start lost it, and
            // never acknowledged, so the relay offered the same half-usable state after every
            // restart -- re-applying the payload and failing the same routes each time. A route
            // this build no longer registers will not start working on the next launch; the
            // payload already worked on this one.
            failed = true;
        }
        commit(state, applied || shown, failed);
        return shown;
    }

    /// Asks the relay for anything newer than what is here, on a background thread. Returns
    /// immediately.
    ///
    /// Worth calling when the app comes back to the foreground: a continuation reaches a nearby
    /// device on its own, but a relay is only read when something asks it to be.
    public static void pollRelay() {
        if (relay == null || !enabled || !Display.isInitialized()) {
            return;
        }
        if (publishing) {
            // Deferred behind the POST, which is the other half of the rule startPublisher()
            // already follows. The relay holds ONE document per user, so a publish in flight can
            // replace the other device's state before a GET started now has read it -- and that
            // GET then returns this device's own echo, which admit() drops as an echo should be
            // dropped. The remote update is gone from the relay and was never seen. Read before
            // write, in BOTH directions, is what makes that impossible.
            pollAgain = true;
            return;
        }
        if (polling) {
            // One fetch at a time. Two overlapping GETs can return DIFFERENT documents -- a relay
            // holds one per user and the other device may replace it between them -- and nothing
            // downstream re-orders the answers: lastSeen is keyed by ORIGINATING device, so a
            // response that left first and arrived second passes deduplication on its own key and
            // puts the older screen over the newer one.
            //
            // Remembered rather than dropped: an application that polls on reconnect while a
            // resume poll is still in flight is asking a real question.
            pollAgain = true;
            return;
        }
        startPoll();
    }

    /// Starts the one fetch worker. Called on the EDT; the worker touches nothing.
    ///
    /// NOT preceded by a publish. A relay holds one document per user, so a POST that reaches the
    /// endpoint before this GET erases the other device's state -- and the GET then returns this
    /// device's own echo, which deliver() drops, so the remote update is never seen at all.
    /// Anything owed is sent when the fetch finishes, which is the only ordering that both sends
    /// what is owed and reads what is there.
    private static void startPoll() {
        final StateRelay r = relay;
        if (r == null || !Display.isInitialized()) {
            return;
        }
        final int session = relaySession;
        polling = true;
        Display.getInstance().startThread(new Runnable() {
            @Override
            public void run() {
                // Off the EDT because fetch() blocks, and touching NOTHING: the relay came in as
                // a local and the answer goes back through the event queue.
                AppState fetched = null;
                try {
                    fetched = r.fetch();
                } catch (Throwable t) {
                    Log.e(t);
                }
                final AppState result = fetched;
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        pollFinished(result, session);
                    }
                });
            }
        }, "Continuity relay poll").start();
    }

    /// A fetch has come back. On the EDT, where every field below is owned.
    private static void pollFinished(AppState fetched, int session) {
        if (session != relaySession) {
            // A clear(), a setRelay() or a reset() happened while this was in flight. The answer
            // belongs to the endpoint or the account that has since gone away: delivering it would
            // restore the previous account's work into the next account's session, and the flags
            // were already reset by whoever ended the session.
            return;
        }
        polling = false;
        if (fetched != null) {
            admit(fetched);
        }
        if (pollAgain) {
            pollAgain = false;
            startPoll();
            return;
        }
        // Owed work goes out AFTER the fetch, never before it.
        startPublisher();
    }

    /// Forgets everything: the stored checkpoint, any parked arrival, the activity advertised to
    /// the user's other devices, and anything queued for the relay.
    ///
    /// Belongs on your logout path. The advertised activity outlives the app's own screen, so an
    /// account's work would otherwise stay offered to the devices around it after the user signed
    /// out -- and a queued relay publish would have gone out later under whatever credentials the
    /// relay returned by then, which after a logout is the NEXT account's.
    ///
    /// One thing it cannot undo: a relay request already on the wire when this is called. Nothing
    /// in this process can recall that. What this guarantees is that nothing follows it.
    public static void clear() {
        parked = null;
        dirty = false;
        // The label goes with the work it describes. It is CONTENT, not configuration -- "Draft
        // to Dana", "Invoice 2031", read at every checkpoint -- so leaving it behind meant the
        // first checkpoint after a logout, a login screen or the next account's opening route,
        // re-advertised the previous user's label to every device around them. Withdrawing the
        // current activity below is not enough on its own: the field outlives it and the next
        // publish puts it straight back.
        //
        // The configuration is deliberately left alone: the provider, the relay, autoRestore,
        // maxAge and this device's id are how the application is wired, not what the last user
        // was doing, and an app would have to install them all again after every logout.
        title = null;
        // Anything queued for the relay belonged to the account that just signed out, and a relay
        // reads its credentials when the request runs rather than when it was queued -- so a state
        // left here would have gone out under the NEXT account's token. The session is ended too,
        // so a fetch already in flight is not delivered into the account that just signed in.
        //
        // The one thing this cannot recall is a request already on the wire. Nothing in this
        // process can; what it can do is make sure nothing follows it.
        endRelaySession();
        lastSeen.clear();
        durableSeen.clear();
        // The durable copy as well. Leaving it behind meant the marks of the account that just
        // signed out kept suppressing the NEXT account's deliveries -- a state silently never
        // arriving, which is harder to notice than one arriving twice.
        rememberSeen();
        clearContinuation();
        try {
            if (Display.isInitialized() && Storage.getInstance().exists(STORAGE_KEY)) {
                Storage.getInstance().deleteStorageFile(STORAGE_KEY);
            }
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// Ends the current relay session: nothing queued goes out, and nothing in flight comes back.
    ///
    /// The counter is what makes an in-flight round trip harmless without any locking. A worker
    /// carries the session it started in, and its completion -- which runs on the EDT -- returns
    /// early when the session has moved on, so the flags reset here stay reset.
    private static void endRelaySession() {
        relaySession++;
        pendingPublish = null;
        publishing = false;
        polling = false;
        pollAgain = false;
        publishRequested = false;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static List<String> currentRoutes() {
        List<String> paths = new ArrayList<String>();
        List<com.codename1.router.NavigationEntry> stack;
        try {
            stack = Navigation.getStack();
        } catch (Throwable t) {
            // Only the call is guarded. Walking the list has to sit outside, because the compiler
            // inserts a checked cast per element for the generic type -- and a failed cast does
            // not throw on the iOS virtual machine, so a handler wrapped around one is a handler
            // that cannot run there. See the ClassCastException note in CLAUDE.md.
            Log.e(t);
            return paths;
        }
        for (com.codename1.router.NavigationEntry entry : stack) {
            paths.add(entry.getPath());
        }
        return paths;
    }

    /// Records that a state has been dealt with, and makes it the local checkpoint when there
    /// was something to store.
    ///
    /// Two separate questions, which an earlier version answered with one flag and got wrong.
    ///
    /// WHETHER TO STORE is `applied`: a state that changed nothing here -- a route this build no
    /// longer registers, with no payload the application could take -- must not replace the
    /// user's own checkpoint with something unusable.
    ///
    /// WHETHER TO ACKNOWLEDGE turns on a different question: did anything FAIL. The mark is
    /// durable and stops the relay ever offering this state again, so it must never follow an
    /// attempt that did not work -- a provider that threw, routes that could not be rebuilt, or
    /// a write that was refused. In each of those the relay's copy is the only one left.
    ///
    /// "Nothing to do" is not a failure, and does mean acknowledge: an application with no
    /// provider can never consume a payload, so there is nothing to recover and withholding the
    /// mark only re-prompts the user on every launch for ever. That distinction is why there are
    /// two flags and not one -- a single "did it apply" answers both questions and gets one of
    /// them wrong whichever way it is set.
    private static void commit(AppState state, boolean applied, boolean failed) {
        if (failed) {
            // An attempt was made and it did not work: a provider that threw, or routes that
            // could not be rebuilt. The relay's copy has to stay on offer for a launch that can
            // use it, so nothing is marked.
            return;
        }
        if (applied && !persist(state)) {
            // Tried to store it and could not. The relay's copy is now the only one that exists,
            // so it must go on being offered: acknowledging here loses the state in both
            // directions at once.
            return;
        }
        noteActedOn(state);
    }

    /// Writes the checkpoint, and says whether it got there.
    ///
    /// The answer is used, not logged. Storage.writeObject returns false on a failed write -- a
    /// full disk is the ordinary cause -- and every piece of bookkeeping around this call assumes
    /// the state is now durable: checkpoint() clears `dirty`, and restore() marks the sender's
    /// sequence so the relay stops offering its copy. Doing either after a failed write is how a
    /// state is lost in both directions at once, with nothing anywhere saying so.
    ///
    /// #### Returns
    ///
    /// true when the state is in storage
    private static boolean persist(AppState state) {
        try {
            return Storage.getInstance().writeObject(STORAGE_KEY, state);
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    /// Writes the sequence counter so it keeps rising across a relaunch.
    /// Persists the sequence counter, and says whether it is actually on disk.
    ///
    /// Written through Storage, whose writeObject() reports failure, rather than through
    /// Preferences, which cannot.
    ///
    /// An earlier attempt at this wrote through Preferences and then read the value back to
    /// check. That verifies nothing: Preferences.set() puts the value in a static Hashtable
    /// before calling save(), save() discards Storage.writeObject()'s result, and
    /// Preferences.get() reads the same Hashtable -- so the read-back returns what was just put
    /// there whether or not any of it reached the disk.
    ///
    /// The silence matters on this value: the counter reloads lower after a restart, and every
    /// receiving device whose high-water mark already includes the higher number refuses this
    /// device's states until the counter climbs past it again. States stop arriving on the other
    /// device, with nothing logged on either.
    ///
    /// #### Returns
    ///
    /// true when the counter reached storage
    private static boolean rememberSequence(long seq) {
        try {
            return Storage.getInstance().writeObject(PREF_SEQUENCE, Long.valueOf(seq));
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    private static AppState readStored() {
        try {
            if (!Display.isInitialized() || !Storage.getInstance().exists(STORAGE_KEY)) {
                return null;
            }
            Object o = Storage.getInstance().readObject(STORAGE_KEY);
            // instanceof rather than a cast: a failed cast does not throw on the iOS virtual
            // machine, so the wrong object would be handed to the next instruction instead of
            // reaching a catch. A stored entry of another shape is possible after a downgrade.
            if (o instanceof AppState) {
                return (AppState) o;
            }
            return null;
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    private static void publishContinuation(AppState state) {
        ContinuityBridge b = bridgeInternal();
        if (b == null) {
            return;
        }
        try {
            if (!b.isContinuationSupported()) {
                return;
            }
            if (state.isEmpty()) {
                b.clearContinuation();
                return;
            }
            b.publishContinuation(getActivityType(), state.getTitle(), StateCodec.toMap(state));
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private static void clearContinuation() {
        ContinuityBridge b = bridgeInternal();
        if (b == null) {
            return;
        }
        try {
            if (b.isContinuationSupported()) {
                b.clearContinuation();
            }
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// The newest state waiting to reach the relay, or null when none is.
    ///
    /// A slot rather than a queue: the relay's contract is that a publish REPLACES what it holds,
    /// so an older state waiting behind a newer one has nothing to add. Coalescing here is also
    /// what keeps a burst of checkpoints from becoming a burst of requests.
    private static AppState pendingPublish;

    /// True while the publish worker is out. One at a time, so the relay's single document is
    /// written in the order the checkpoints happened: two workers racing the same endpoint could
    /// leave the older state stored last, and the user's other device then fetches work they had
    /// already moved past.
    private static boolean publishing;

    /// True while a relay fetch is out; `pollAgain` records a poll asked for during one.
    private static boolean polling;

    private static boolean pollAgain;

    /// True when a publisher was wanted while one was already out.
    ///
    /// The publisher does not retry in a loop -- one attempt per change, rather than a spin
    /// against an endpoint that is down -- but a request that arrived DURING an attempt is a new
    /// signal rather than a spin, and pollRelay() on reconnect is exactly that. Without it, an
    /// application that reconnects while the failing attempt is still on the wire had its
    /// reconnect forgotten, and the retained state waited for some later checkpoint.
    private static boolean publishRequested;

    /// Hands a state to the relay, one at a time.
    private static void publishToRelay(AppState state) {
        if (!Display.isInitialized() || relay == null) {
            return;
        }
        // A slot rather than a queue: a publish REPLACES what the relay holds, so an older state
        // waiting behind a newer one has nothing to add.
        pendingPublish = state;
        startPublisher();
    }

    /// Starts the one publish worker, if there is work and nobody is doing it. Called on the EDT.
    ///
    /// Separate from `publishToRelay` because a checkpoint is not the only thing that should start
    /// one: a state retained after a failed send is sent by whatever finishes next -- a poll, or
    /// the following checkpoint -- rather than sitting in the slot forever.
    private static void startPublisher() {
        if (!Display.isInitialized() || relay == null || !enabled) {
            // `enabled` as the general invariant, beside the specific drop in disable(). Nothing
            // may reach the relay while the framework is off, and this is the one funnel every
            // publication passes through -- including the ones started by a worker completing
            // after the application turned it off.
            return;
        }
        // Read into a local before the test. PMD's NonThreadSafeSingleton matches the SHAPE of
        // "null-check a static, then assign a static inside the branch" and reports it as a lazy
        // initializer, which this is not -- and the project's gate has no per-finding allow list,
        // so the shape is what has to change.
        AppState awaitingDecision = parked;
        if (awaitingDecision != null) {
            // A fetched state is waiting on the user and has NOT been acknowledged. The relay
            // holds one document per user, so publishing now replaces the only copy of it that
            // exists anywhere -- it is in memory here and nowhere else -- and a process death
            // while the prompt is on screen loses it for good. autoRestore off, or a listener
            // that returns false to ask first, is the ordinary way to get here.
            //
            // Held rather than dropped. Whatever clears the slot -- the user accepting, the
            // application acknowledging, a logout, the state expiring -- calls back in here, and
            // an acknowledged state is safe to overwrite because the mark is already durable.
            publishRequested = true;
            return;
        }
        if (publishing) {
            // Asked BEFORE the empty-slot check, which is the whole point of the flag. A worker
            // that is out has already taken the state out of the slot, so the slot is empty
            // exactly when this signal matters -- and testing it first threw the reconnect away
            // and left the state that worker is about to fail on waiting for a later checkpoint.
            publishRequested = true;
            return;
        }
        if (pendingPublish == null) {
            return;
        }
        if (polling) {
            // A GET is outstanding. The relay holds ONE document per user, so a POST that lands
            // before the answer overwrites the other device's state -- and the GET then reads back
            // our own write, so the remote update is never seen. pollFinished() starts a publisher
            // when the fetch is done.
            return;
        }
        final StateRelay r = relay;
        final AppState next = pendingPublish;
        final int session = relaySession;
        pendingPublish = null;
        publishing = true;
        Display.getInstance().startThread(new Runnable() {
            @Override
            public void run() {
                // Off the EDT because publish() blocks, and touching NOTHING: the relay and the
                // state came in as locals and the outcome goes back through the event queue.
                boolean sent = true;
                try {
                    r.publish(next);
                } catch (Throwable t) {
                    Log.e(t);
                    sent = false;
                }
                final boolean ok = sent;
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        publishFinished(next, session, ok);
                    }
                });
            }
        }, "Continuity relay publish").start();
    }

    /// A publish has come back. On the EDT, where every field below is owned.
    private static void publishFinished(AppState sent, int session, boolean ok) {
        if (session != relaySession) {
            // The session ended while this was in flight; endRelaySession() has already reset the
            // flags and the state this carried belongs to an account that has signed out.
            return;
        }
        publishing = false;
        if (!ok && pendingPublish == null) {
            // Kept, not dropped -- StateRelay.publish documents that the framework holds a failed
            // state for the next attempt, and dropping it meant the last checkpoint before the
            // network went away never reached the other device at all. Put back only when nothing
            // newer is queued, since a newer state supersedes it entirely.
            pendingPublish = sent;
        }
        if (pollAgain) {
            // A poll asked for while this was on the wire, and it goes FIRST: read before write
            // is the ordering a single-document relay needs, and pollFinished() starts a
            // publisher for whatever is still queued when it lands.
            pollAgain = false;
            publishRequested = false;
            startPoll();
            return;
        }
        if (!ok && !publishRequested) {
            // Stood down rather than retried: one attempt per change, not a spin against an
            // endpoint that is down. The next checkpoint or poll starts the next one.
            return;
        }
        // Somebody asked for a publisher while this attempt was in flight -- an application
        // calling pollRelay() on reconnect is the ordinary case. Consumed rather than looped on,
        // so it is one extra attempt per request.
        publishRequested = false;
        // Drains anything queued while this was in flight, and is a no-op when nothing is.
        startPublisher();
    }

    /// The activity type this app publishes and answers to, which is the app's package name
    /// followed by `.continuity`.
    ///
    /// Fixed by the build, which declares the same string to the platform in `NSUserActivityTypes`;
    /// the two have to agree or the operating system refuses to deliver anything. Exposed because
    /// an app that also publishes activities of its own needs to know which one is this
    /// framework's, and because it is the first thing to check when a continuation never arrives.
    ///
    /// #### Returns
    ///
    /// the activity type, never null
    public static String getActivityType() {
        String pkg = null;
        try {
            pkg = Display.getInstance().getProperty("package_name", null);
        } catch (Throwable t) {
            Log.e(t);
        }
        if (pkg == null || pkg.length() == 0) {
            pkg = "com.codename1.app";
        }
        return pkg + ".continuity";
    }

    /// Routes an arriving state to the application, from whatever channel produced it.
    ///
    /// The one method here that is called from a foreign thread -- a port hands a continuation
    /// over on the platform's own thread -- so it does the marshalling and everything downstream
    /// is ordinary EDT code.
    static void deliver(final AppState state) {
        if (state == null) {
            return;
        }
        if (!Display.isInitialized()) {
            // No event thread yet, so there is nothing to marshal to and nothing running that
            // could be racing this. Held for the EDT that is about to start.
            parked = state;
            return;
        }
        // ALWAYS queued, even when the caller is already on the EDT. Admission and dispatch are
        // deliberately separate turns -- see admit() -- and running one caller's arrival inline
        // while another's is queued would put them in different orders depending on who called.
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                admit(state);
            }
        });
    }

    /// Decides whether an arrival is worth acting on, and records it. On the EDT.
    ///
    /// The dispatch is a SECOND turn rather than the rest of this one. Two states from the same
    /// device can be in flight together -- a continuation and a relay poll routinely carry
    /// different sequences -- and admitting both before either is applied is what lets the older
    /// one notice it has been superseded and stand down. Applying inline instead walked the user
    /// through the stale screen on the way to the fresh one.
    private static void admit(final AppState state) {
        if (!enabled) {
            return;
        }
        if (getDeviceId().equals(state.getDeviceId())) {
            // This device's own echo, which a relay returns as a matter of course.
            return;
        }
        if (isTooOld(state)) {
            // A relay hands back whatever it still holds, which can be days old, and an expired
            // checkout or booking hold that auto-restored is the exact harm setMaxAge exists to
            // prevent. Dropped before lastSeen records it, so the sequence stays free for a
            // fresher state from the same device.
            return;
        }
        Long seen = seenSequence(state.getDeviceId());
        if (seen != null && seen.longValue() >= state.getSequence()) {
            // Delivered twice, which happens routinely: a continuation and a relay poll can carry
            // the same state.
            return;
        }
        // Admission only: not durable until the state has actually been completed.
        recordSeen(state.getDeviceId(), state.getSequence(), false);
        if (state.isEmpty()) {
            // A TOMBSTONE, not an offer. An enabled app with no routes and no payload still
            // checkpoints, and the relay holds one document per user, so that empty state is
            // published to overwrite whatever was there -- which is the point, it clears the
            // other devices' stale copy. It carries a device id and a sequence, though, so the
            // receiving side recognized it as a real arrival and ran the listeners: a
            // "continue what you were doing?" prompt over nothing at all.
            //
            // The platform path has always got this right -- publishContinuation() withdraws the
            // activity for an empty state rather than advertising one -- and only the relay path
            // was missing the other half of it. Marked as seen above, so it is consumed rather
            // than reconsidered, and simply not dispatched.
            //
            // It also SUPERSEDES anything still parked from the same origin. A tombstone is that
            // origin saying it has nothing any more, so an older state of its own that is waiting
            // on the user is work that no longer exists: getRestorableState() would go on offering
            // it, and the publication hold would go on withholding this device's checkpoints
            // behind it. Same shape as acknowledge() and expiry -- another way an arrival ends,
            // and every one of them has to release the slot.
            AppState waiting = parked;
            if (waiting != null && state.getDeviceId().equals(waiting.getDeviceId())
                    && waiting.getSequence() <= state.getSequence()) {
                parked = null;
                startPublisher();
            }
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!enabled) {
                    // disable() between the two turns. Arriving states are ignored from the
                    // moment it is called, including the ones already admitted.
                    return;
                }
                Long newest = lastSeen.get(state.getDeviceId());
                if (newest == null || newest.longValue() != state.getSequence()) {
                    // Superseded while this was queued: a newer state from the same device was
                    // admitted behind it. Applying this one now would move the user backwards.
                    return;
                }
                dispatch(state);
            }
        });
    }

    /// Applies an arrival: offers it to the listeners, then restores or parks it.
    private static void dispatch(AppState state) {
        if (isTooOld(state)) {
            // Rechecked HERE, not only at admission, because admission is not the only way in.
            // A continuation that cold-launches the app is parked and waits up to
            // WINDOW_WAIT_MILLIS for the first form, and the waiter then comes back through this
            // method -- so a state that was fresh when it landed and expired during the wait was
            // restored anyway, past the check in admit() and the one in getRestorableState().
            // An expired checkout or booking hold is exactly what maxAge exists to refuse.
            //
            // This check existed before the event-thread rewrite and was dropped by it. Its
            // comment named this path.
            return;
        }
        if (Display.getInstance().getCurrent() == null) {
            // A continuation can cold-launch the app, and both Apple delegates hand it over while
            // init/start are still queued. Restoring against no form at all would run the route
            // table into a display that is not ready, so it waits -- bounded, because a launch
            // that never produces a form is broken and jumping the user minutes later is worse
            // than doing nothing.
            park(state);
            return;
        }
        // A copy, because a listener that reacts by unregistering itself is ordinary and would
        // otherwise mutate the list being walked.
        List<ContinuityListener> snapshot = new ArrayList<ContinuityListener>(listeners);
        for (ContinuityListener l : snapshot) {
            boolean accepted;
            try {
                accepted = l.stateReceived(state);
            } catch (Throwable t) {
                Log.e(t);
                continue;
            }
            if (!accepted) {
                // Consumed by the listener: it either handled the state itself or decided the user
                // must not be moved. Asking the next listener would undo that decision.
                return;
            }
        }
        if (autoRestore) {
            // The mark is written by restore(), through commit(), and ONLY when it committed
            // something. There was an unconditional rememberSeen() here, which quietly undid that:
            // admit() has already put this sequence in the live map, so persisting the map wrote
            // the mark for a state whose checkpoint had failed to store, or that applied nothing
            // at all. After a restart enable() reloads it and the relay's only recoverable copy is
            // refused -- the very loss commit() gates against, reached down a second path that
            // never went through it.
            //
            // The in-memory mark still goes in at admission, which is what dedups within a run.
            // Durability is a separate question and has one owner.
            restore(state);
        } else {
            parked = state;
        }
    }

    /// Holds a cold-launch arrival until the application has a form to restore into.
    ///
    /// The waiter is a thread only because there is nothing on the EDT to wait on -- no form
    /// exists yet, so there is no timer to bind to. It touches no field of this class: it sleeps,
    /// and hands the decision back to the event thread.
    private static void park(AppState state) {
        parked = state;
        if (waitingForWindow) {
            return;
        }
        waitingForWindow = true;
        Display.getInstance().startThread(new Runnable() {
            @Override
            public void run() {
                long deadline = System.currentTimeMillis() + WINDOW_WAIT_MILLIS;
                while (System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException err) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (Display.getInstance().getCurrent() != null) {
                        break;
                    }
                }
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        windowWaitFinished();
                    }
                });
            }
        }, "Continuity window wait").start();
    }

    /// The cold-launch wait is over. On the EDT.
    private static void windowWaitFinished() {
        waitingForWindow = false;
        if (Display.getInstance().getCurrent() == null) {
            // Still no form after the whole wait. The state stays parked, so an application that
            // gets going later can still ask for it through getRestorableState().
            return;
        }
        // Taken and cleared rather than compared against the state the waiter was started for. A
        // newer arrival while it waited is the one worth showing.
        AppState waiting = parked;
        parked = null;
        if (waiting != null) {
            dispatch(waiting);
        }
        // Whether it dispatched or was refused, the slot is no longer holding anything back.
        startPublisher();
    }

    private static String loadDeviceId() {
        try {
            String id = null;
            if (Display.isInitialized() && Storage.getInstance().exists(PREF_DEVICE_ID)) {
                Object o = Storage.getInstance().readObject(PREF_DEVICE_ID);
                if (o instanceof String) {
                    id = (String) o;
                }
            }
            if (id == null || id.length() == 0) {
                id = Util.getUUID();
                if (!Storage.getInstance().writeObject(PREF_DEVICE_ID, id)) {
                    // Minted but not stored, so the next launch mints another one and every state
                    // this device has sent starts looking like a stranger's. Nothing here can
                    // prevent that; saying so beats a silent identity change.
                    Log.p("Continuity: the device id could not be stored; it will change on the "
                            + "next launch and states already sent will not be recognized as "
                            + "this device's own.");
                }
            }
            return id;
        } catch (Throwable t) {
            Log.e(t);
            // A device with no readable preferences still has to have an id, or every state it
            // produces would look like every other device's. Unstable across restarts, which
            // costs only some duplicate deliveries.
            return "cn1-" + System.currentTimeMillis();
        }
    }

    /// Whether two states are the same one: same origin device, same sequence.
    ///
    /// The pair that identifies a state throughout this class. Neither half alone will do --
    /// sequences restart at zero on a device whose preferences were cleared, and one device
    /// publishes many.
    private static boolean isSameState(AppState a, AppState b) {
        if (a == null || b == null) {
            return false;
        }
        String left = a.getDeviceId();
        String right = b.getDeviceId();
        return left != null && left.equals(right) && a.getSequence() == b.getSequence();
    }

    /// Records that `state` has been acted on, durably.
    private static void noteActedOn(AppState state) {
        String from = state.getDeviceId();
        if (from == null || from.length() == 0 || from.equals(getDeviceId())) {
            // Our own work needs no mark: deliver() drops an echo on the device id alone.
            return;
        }
        long seq = state.getSequence();
        Long inMemory = lastSeen.get(from);
        if (inMemory == null || inMemory.longValue() < seq) {
            recordSeen(from, seq, true);
        } else {
            // The in-memory mark already covers this state: admit() put it there on the way in,
            // which is the ordinary case, so "only if higher" never fires and the DURABLE half
            // would never be written. That is the same bug the note below describes, returned in
            // a new shape once the durable set stopped being the in-memory one -- writing the
            // whole of memory used to hide it.
            //
            // Marked only up to THIS state. If something newer from the same device has been
            // admitted since, it has not been completed and must not be marked on this one's
            // behalf.
            Long durable = durableSeen.get(from);
            if (durable == null || durable.longValue() < seq) {
                recordDurable(from, seq);
            }
        }
        if (isSameState(parked, state)) {
            // The application has dealt with this arrival -- acknowledge() is the documented way
            // to decline one -- so the slot must not go on offering it through
            // getRestorableState(), and must not go on holding a checkpoint back either. The
            // hold exists because a parked state's only copy is on the relay; an acknowledged
            // state has a durable mark, so overwriting the relay's copy is now safe.
            parked = null;
            startPublisher();
        }
        // ALWAYS, not only when a map moved. The condition this replaced was written when the
        // durable copy tracked memory exactly; it does not, and by the time anything calls this
        // memory already holds the entry, so "unchanged" meant "write nothing" and both
        // acknowledge() and the restore path silently persisted nothing at all.
        rememberSeen();
    }

    /// Test seam: parks a state, as a cold-launch arrival with no form yet does.
    static void parkForTest(AppState state) {
        parked = state;
    }

    /// Test seam: the cold-launch drain, entered exactly where the waiter enters it.
    ///
    /// The wait itself cannot be reproduced in a unit harness -- it needs a launch with no form,
    /// and this one always has one -- but the drain is the half that matters: it is where a state
    /// that was fresh when it arrived and expired while waiting reaches dispatch().
    static void drainParkedForTest() {
        windowWaitFinished();
    }

    /// Test seam: the marks as they would be reloaded on the next launch.
    static Map<String, Long> readSeenForTest() {
        return readSeen();
    }

    /// Test seam: how many devices the live map is holding.
    static int seenSizeForTest() {
        return lastSeen.size();
    }

    /// Records a device's high-water mark, most recently used LAST.
    ///
    /// Removed before it is put back, because the map keeps INSERTION order and a plain put()
    /// over an existing key leaves it where it first appeared -- so a device that has been active
    /// all along would still be evicted ahead of one that has said nothing since.
    private static void recordSeen(String device, long sequence, boolean durable) {
        lastSeen.remove(device);
        lastSeen.put(device, Long.valueOf(sequence));
        if (durable) {
            durableSeen.remove(device);
            durableSeen.put(device, Long.valueOf(sequence));
        }
        trimSeen();
    }

    /// The highest sequence known for a device, from EITHER map.
    ///
    /// The two are bounded independently and hold different sets -- lastSeen takes every arrival,
    /// durableSeen only the ones that completed -- so they evict at different rates and an origin
    /// can survive in one after being dropped from the other. Asking only lastSeen therefore let
    /// a duplicate through: acknowledge a state, admit more than MAX_SEEN other origins, and the
    /// acknowledged one is evicted from lastSeen while its durable mark remains. The duplicate
    /// then passed the check and ran the application's listeners a second time, against the
    /// act-once guarantee the durable mark exists to give.
    private static Long seenSequence(String device) {
        Long inMemory = lastSeen.get(device);
        Long durable = durableSeen.get(device);
        if (inMemory == null) {
            return durable;
        }
        if (durable == null) {
            return inMemory;
        }
        return durable.longValue() > inMemory.longValue() ? durable : inMemory;
    }

    /// Marks a device durably without disturbing the in-memory dedup mark, which may be newer.
    private static void recordDurable(String device, long sequence) {
        durableSeen.remove(device);
        durableSeen.put(device, Long.valueOf(sequence));
        trimSeen();
    }

    /// Evicts the least recently seen devices until the map is back inside MAX_SEEN.
    ///
    /// The LIVE map, not a copy taken on the way to storage. A user has a handful of devices, but
    /// the ids arrive from a relay and nothing stops one from feeding many, which is the reason
    /// there is a cap at all -- so it has to apply where entries are added rather than where they
    /// happen to be written out.
    ///
    /// The lowest sequences go: those are the devices that have been quiet longest, and losing a
    /// mark costs one duplicate delivery rather than anything durable.
    private static void trimSeen() {
        trimTo(durableSeen);
        trimTo(lastSeen);
    }

    /// Evicts the least recently seen entries from one map until it is inside MAX_SEEN.
    private static void trimTo(Map<String, Long> map) {
        while (map.size() > MAX_SEEN) {
            Iterator<String> i = map.keySet().iterator();
            if (!i.hasNext()) {
                break;
            }
            // The eldest, and ALWAYS one: the sequence comparison this replaced could select
            // nothing at all -- every value equal to Long.MAX_VALUE left its "lowest" null -- and
            // then simply stopped enforcing the cap. Taking the front of the iteration order
            // cannot fail to find a victim while the map is over size.
            i.next();
            i.remove();
        }
    }

    /// Reads the persisted high-water marks. Never null.
    private static Map<String, Long> readSeen() {
        Map<String, Long> out = new HashMap<String, Long>();
        try {
            if (!Display.isInitialized() || !Storage.getInstance().exists(PREF_SEEN)) {
                return out;
            }
            Object stored = Storage.getInstance().readObject(PREF_SEEN);
            String raw = stored instanceof String ? (String) stored : null;
            if (raw == null || raw.length() == 0) {
                return out;
            }
            // Split on UNESCAPED separators. The ids are not all ours: setDeviceId is public and
            // a state arrives from whatever the relay was given, so an id may contain the very
            // characters this format is delimited by. Unescaped, "phone|work" produced a sequence
            // field that would not parse and a semicolon produced a whole second entry -- a mark
            // written against an origin that never sent anything, which then suppresses that
            // origin's real states for good.
            int from = 0;
            while (from <= raw.length()) {
                int end = indexOfUnescaped(raw, ';', from);
                String entry = end < 0 ? raw.substring(from) : raw.substring(from, end);
                int bar = indexOfUnescaped(entry, '|', 0);
                if (bar > 0 && bar < entry.length() - 1) {
                    try {
                        out.put(unescapeSeenKey(entry.substring(0, bar)),
                                Long.valueOf(Long.parseLong(entry.substring(bar + 1))));
                    } catch (NumberFormatException ignored) {
                        // A corrupt entry costs one duplicate delivery, never a launch.
                    }
                }
                if (end < 0) {
                    break;
                }
                from = end + 1;
            }
        } catch (Throwable t) {
            Log.e(t);
        }
        return out;
    }

    /// The index of the first `c` that is not preceded by an escape, or -1.
    private static int indexOfUnescaped(String s, char c, int from) {
        boolean escaped = false;
        for (int i = from; i < s.length(); i++) {
            char at = s.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (at == '\\') {
                escaped = true;
            } else if (at == c) {
                return i;
            }
        }
        return -1;
    }

    /// Escapes the two delimiters, and the escape itself.
    private static String escapeSeenKey(String key) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '\\' || c == '|' || c == ';') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /// Reverses escapeSeenKey.
    private static String unescapeSeenKey(String key) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /// Writes the high-water marks, trimmed to MAX_SEEN.
    ///
    /// Called after a delivery is accepted, which is rare -- it takes another device publishing --
    /// so this is not on any hot path.
    private static void rememberSeen() {
        // Serialized straight from the live map, which trimSeen() has already bounded. This used
        // to copy and trim HERE, which bounded the preference and left the map itself growing for
        // the life of the process -- and made every acknowledgement copy the whole thing and scan
        // it back down to the cap, so a relay feeding many device ids cost memory and rising CPU
        // at once. The cap belongs where entries go IN.
        Map<String, Long> copy = durableSeen;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> e : copy.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(escapeSeenKey(e.getKey())).append('|').append(e.getValue().longValue());
        }
        // ONLY the write is wrapped. Iterating a generic map compiles to checkcasts, and a
        // catch(Throwable) around them is a handler ParparVM never runs -- its CHECKCAST expands
        // to nothing, so a failed cast hands the wrong object to the next instruction and crashes
        // natively instead. check-cast-semantics.sh refuses the shape, correctly: the only thing
        // here that can actually fail is the preference write.
        try {
            if (!Storage.getInstance().writeObject(PREF_SEEN, sb.toString())) {
                // The marks stay in memory, so this run still acts once. What is lost is the
                // guarantee across a restart: an acknowledged state can be offered again and its
                // side effects run a second time. Recoverable, unlike the alternative of dropping
                // the state, and every later acknowledgement retries the write.
                Log.p("Continuity: the delivery marks could not be stored; an acknowledged state "
                        + "may be offered again after a restart.");
            }
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private static long loadSequence() {
        try {
            if (!Display.isInitialized() || !Storage.getInstance().exists(PREF_SEQUENCE)) {
                return 0;
            }
            Object o = Storage.getInstance().readObject(PREF_SEQUENCE);
            // instanceof rather than a cast: a failed cast does not throw on the iOS virtual
            // machine, it hands the wrong object to the next instruction.
            return o instanceof Number ? ((Number) o).longValue() : 0;
        } catch (Throwable t) {
            Log.e(t);
            return 0;
        }
    }

    private static long nextSequence() {
        return sequence + 1;
    }

    /// Test seam: installs a bridge, bypassing platform resolution.
    ///
    /// #### Parameters
    ///
    /// - `b`: the bridge, or null to resolve from the platform again
    public static void setBridge(ContinuityBridge b) {
        bridge = b;
        bridgeOverridden = b != null;
        // Delegated, not repeated. This used to install the callback itself under
        // `b != null && enabled`, and both halves of that were wrong while refreshBridge() --
        // ten lines below, doing the same job -- had them right.
        //
        // `b != null` skipped the case that needs it most: setBridge(null) hands resolution back
        // to the PLATFORM, and the bridge it then resolves is a different object that has never
        // been given a callback. Outbound calls kept working, so the seam looked healthy while
        // every inbound continuation and synced-store notification went nowhere.
        //
        // `enabled` is not the right question either. A sync-only application installs the
        // inbound seam through SyncedStore.addChangeListener and deliberately leaves continuity
        // off, so storeCallbackInstalled is true while enabled is false -- and it got no callback
        // at all.
        refreshBridge();
    }

    /// Internal. The resolved platform bridge, for `com.codename1.continuity.sync`, which is a
    /// package of its own so that its entitlement is earned separately. Application code uses
    /// `com.codename1.continuity.sync.SyncedStore`.
    ///
    /// #### Returns
    ///
    /// the bridge, or null when this port has none
    public static ContinuityBridge bridgeForSyncedStore() {
        return bridgeInternal();
    }

    /// Internal. Installs the inbound seam WITHOUT turning continuity on. Application code uses
    /// `com.codename1.continuity.sync.SyncedStore.addChangeListener`.
    ///
    /// `com.codename1.continuity.sync` is a package of its own precisely so that its cost is
    /// earned separately, and `enable()` is not a cost the synced store asks for: it makes every
    /// route change checkpoint, and a checkpoint advertises the app's navigation to the devices
    /// around it over Handoff. Registering a store listener used to call it, so an application
    /// that wanted a key/value store the user's devices share -- and nothing else -- was opted
    /// into broadcasting its route stack.
    ///
    /// The store's own notification does not go through `enabled` (see Callback.syncedStoreChanged),
    /// which is what lets the listener work with continuity still off.
    public static void installSyncedStoreCallback() {
        storeCallbackInstalled = true;
        ContinuityBridge b = bridgeInternal();
        if (b == null) {
            return;
        }
        try {
            b.setCallback(new Callback());
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// Internal. Re-installs the framework's inbound seam on whatever bridge the port now
    /// returns. Called by a port that swaps its bridge while the app is running, which only the
    /// simulator does -- a device's bridge is created once and lives as long as the process.
    public static void refreshBridge() {
        // OR the store's own flag, not `enabled` alone. An application that only registers a
        // SyncedStore listener deliberately leaves continuity off -- a key/value store is not
        // consent to broadcast a route stack -- so testing `enabled` here meant the simulator's
        // capability menu, which swaps the bridge and calls this, left the replacement with no
        // callback at all and every later "Change the Synced Store" item silently did nothing.
        // That is the documented sync-only workflow breaking on the first use of an unrelated
        // menu item.
        if (!enabled && !storeCallbackInstalled) {
            return;
        }
        ContinuityBridge b = bridgeInternal();
        if (b == null) {
            return;
        }
        try {
            b.setCallback(new Callback());
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    static ContinuityBridge bridgeInternal() {
        if (bridgeOverridden) {
            return bridge;
        }
        if (!Display.isInitialized()) {
            return null;
        }
        try {
            return Display.getInstance().getContinuityBridge();
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    /// Test seam: returns the framework to its untouched state.
    ///
    /// A live relay worker is not waited for. Ending the session is enough: the worker carries the
    /// session it started in, so its completion returns early and touches none of the flags reset
    /// here.
    static void reset() {
        listeners.clear();
        lastSeen.clear();
        durableSeen.clear();
        endRelaySession();
        provider = null;
        relay = null;
        bridge = null;
        bridgeOverridden = false;
        enabled = false;
        autoRestore = true;
        flushScheduled = false;
        title = null;
        sequence = 0;
        maxAge = 0;
        deviceId = null;
        parked = null;
        dirty = false;
        waitingForWindow = false;
        applyingRestore = false;
        storeCallbackInstalled = false;
    }

    /// The store notification, as a constant rather than an anonymous class per callback.
    ///
    /// It captures nothing -- notifyChanged() is static -- so an inner class would hold its
    /// enclosing Callback alive for no reason, which SpotBugs reports as
    /// SIC_INNER_SHOULD_BE_STATIC_ANON.
    private static final Runnable NOTIFY_STORE = new Runnable() {
        @Override
        public void run() {
            com.codename1.continuity.sync.SyncedStore.notifyChanged();
        }
    };

    /// The inbound seam handed to the port's bridge.
    static final class Callback implements ContinuityCallback {
        @Override
        public boolean continuationReceived(String activityType, Map<String, Object> userInfo) {
            // Called on the platform's thread, and answered from the activity type ALONE. The
            // port needs a synchronous yes or no -- its answer decides whether the activity falls
            // through to another handler -- and the type is a pure function of the package name,
            // so nothing here has to read framework state from a foreign thread. `enabled` is
            // asked on the event thread, in admit(): an activity of this app's own type is
            // ours to claim whether or not the framework happens to be on, and claiming it is what
            // keeps it from being offered to a handler that would do nothing with it.
            if (activityType == null || !activityType.equals(getActivityType())) {
                return false;
            }
            AppState state = StateCodec.fromMap(userInfo);
            if (state == null) {
                return false;
            }
            deliver(state);
            return true;
        }

        @Override
        public void syncedStoreChanged() {
            // Arrives on the platform's thread, like continuationReceived. The listeners are
            // application code and run on the event thread, as every other callback in the
            // toolkit does.
            if (!Display.isInitialized() || Display.getInstance().isEdt()) {
                com.codename1.continuity.sync.SyncedStore.notifyChanged();
                return;
            }
            Display.getInstance().callSerially(NOTIFY_STORE);
        }
    }
}
