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
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.router.Navigation;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.HashMap;
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
public final class Continuity {
    /// The `Storage` entry the checkpoint is written to.
    static final String STORAGE_KEY = "CN1$Continuity";

    /// Where this installation's device id lives, so a state can recognize its own echo across
    /// restarts.
    static final String PREF_DEVICE_ID = "CN1$ContinuityDevice";

    /// Where the sequence counter lives. Persisted because a counter that restarted at zero would
    /// make every state after a relaunch look older than one the receiver had already seen.
    static final String PREF_SEQUENCE = "CN1$ContinuitySeq";

    /// How long to wait for the application to produce its first form before giving up on a
    /// continuation that cold-launched it. A launch that never produces one is a broken
    /// application, and restoring minutes later into whatever the user is doing by then is worse
    /// than not restoring at all.
    private static final long WINDOW_WAIT_MILLIS = 15000L;

    private static final List<ContinuityListener> listeners = new ArrayList<ContinuityListener>();

    /// Highest sequence seen from each device, so a state delivered twice -- which happens
    /// routinely, since a continuation and a relay can carry the same one -- acts once.
    private static final Map<String, Long> lastSeen = new HashMap<String, Long>();

    // Configured by the application while it starts, then read. A lock rather than volatile
    // fields, which the project's PMD gate forbids and which would be the wrong tool anyway for
    // the two below whose invariant spans more than one read.
    private static StateProvider provider;
    private static StateRelay relay;
    private static ContinuityBridge bridge;
    private static boolean bridgeOverridden;
    private static boolean enabled;
    private static boolean autoRestore = true;
    private static boolean flushScheduled;
    private static String title;
    private static long sequence;
    private static long maxAge;

    /// Guards the two fields a port can touch from a thread of its own.
    ///
    /// A continuation arrives on whatever thread the platform hands it over on -- on Apple
    /// platforms the main thread, not the event dispatch thread -- so `parked` and `deviceId` are
    /// written from there and read on the EDT. `HANDOFF_LOCK` is never held while application
    /// code runs, so it cannot be part of a deadlock.
    private static final Object HANDOFF_LOCK = new Object();

    /// The device id, lazily created. Guarded by HANDOFF_LOCK.
    private static String deviceId;

    /// Whether a checkpoint is owed. Guarded by HANDOFF_LOCK, because Android asks this from its
    /// own main thread on the suspend path and a stale "no" there loses the last edit -- which is
    /// the one thing the question exists to protect.
    private static boolean dirty;

    /// True while a thread is waiting for the first form. Guarded by HANDOFF_LOCK: it is cleared
    /// by that thread and read on the EDT, and a stale "true" would leave a parked state with
    /// nobody left to deliver it.
    private static boolean waitingForWindow;

    /// A state that arrived and could not be shown yet. Guarded by HANDOFF_LOCK.
    private static AppState parked;

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
        enabled = true;
        // Registered once, and only from here, so that a build which merely links this class --
        // because something else in the framework mentions it -- never installs a callback or
        // touches storage.
        Util.register(AppState.OBJECT_ID, AppState.class);
        deviceId = loadDeviceId();
        sequence = loadSequence();
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
        synchronized (HANDOFF_LOCK) {
            dirty = false;
        }
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
        synchronized (HANDOFF_LOCK) {
            if (deviceId == null) {
                deviceId = loadDeviceId();
            }
            return deviceId;
        }
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
        synchronized (HANDOFF_LOCK) {
            dirty = true;
        }
        if (flushScheduled || !Display.isInitialized()) {
            return;
        }
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
        synchronized (HANDOFF_LOCK) {
            dirty = false;
        }
        AppState state = capture();
        if (state == null) {
            return;
        }
        persist(state);
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
        synchronized (HANDOFF_LOCK) {
            return enabled && dirty;
        }
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
        if (!enabled) {
            return null;
        }
        AppState state = new AppState();
        state.setRoutes(currentRoutes());
        StateProvider p = provider;
        if (p != null) {
            Map<String, Object> payload = null;
            try {
                payload = p.saveState();
            } catch (Throwable t) {
                // The provider is application code running on a housekeeping path. Its failure
                // must not take down the navigation that triggered the checkpoint, so the routes
                // are still saved and the payload is simply absent from this one.
                Log.e(t);
            }
            if (payload != null) {
                // NOT caught. An unrepresentable value is a programming error with exactly one
                // correct moment to surface -- here, naming the key -- rather than as a payload
                // that silently stops arriving on the other device.
                state.setPayload(payload);
            }
        }
        sequence = nextSequence();
        // Persisted HERE rather than in persist(), which only checkpoint() reaches. capture() is
        // public and documented for sending a state through the application's own transport, and
        // a counter that only advanced durably on the checkpoint path restarted lower after a
        // relaunch -- so a receiver still holding the old high-water mark in lastSeen silently
        // ignored every state until the counter caught up.
        rememberSequence();
        state.setDeviceId(getDeviceId())
                .setSequence(sequence)
                .setTimestamp(System.currentTimeMillis())
                .setTitle(title);
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
        AppState waiting;
        synchronized (HANDOFF_LOCK) {
            waiting = parked;
        }
        if (waiting != null) {
            // Aged like a stored one. A parked state is one that arrived from elsewhere and could
            // not be shown yet -- during a cold launch, say -- and time passes while it waits, so
            // exempting it would have let exactly the expiry the application configured slip
            // through on the one path where the delay is longest.
            if (isTooOld(waiting)) {
                setParked(null);
                return null;
            }
            return waiting;
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
        setParked(null);
        return restore(state);
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
    public static boolean restore(AppState state) {
        if (state == null) {
            return false;
        }
        StateProvider p = provider;
        if (p != null) {
            try {
                // Before the routes, so a form the route table is about to build can read what
                // the provider stashed while it is being constructed.
                p.restoreState(state.getPayload());
            } catch (Throwable t) {
                Log.e(t);
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
            return false;
        }
        try {
            return Navigation.restoreStack(routes);
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    /// Asks the relay for anything newer than what is here, on a background thread. Returns
    /// immediately.
    ///
    /// Worth calling when the app comes back to the foreground: a continuation reaches a nearby
    /// device on its own, but a relay is only read when something asks it to be.
    public static void pollRelay() {
        final StateRelay r = relay;
        if (r == null || !enabled || !Display.isInitialized()) {
            return;
        }
        Display.getInstance().startThread(new Runnable() {
            @Override
            public void run() {
                AppState fetched = null;
                try {
                    fetched = r.fetch();
                } catch (Throwable t) {
                    Log.e(t);
                    return;
                }
                if (fetched != null) {
                    deliver(fetched);
                }
            }
        }, "Continuity relay poll").start();
    }

    /// Forgets everything: the stored checkpoint, any parked arrival, and the activity advertised
    /// to the user's other devices.
    ///
    /// Belongs on your logout path. The advertised activity outlives the app's own screen, so an
    /// account's work would otherwise stay offered to the devices around it after the user signed
    /// out.
    public static void clear() {
        setParked(null);
        synchronized (HANDOFF_LOCK) {
            dirty = false;
        }
        lastSeen.clear();
        clearContinuation();
        try {
            if (Display.isInitialized() && Storage.getInstance().exists(STORAGE_KEY)) {
                Storage.getInstance().deleteStorageFile(STORAGE_KEY);
            }
        } catch (Throwable t) {
            Log.e(t);
        }
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

    private static void persist(AppState state) {
        try {
            Storage.getInstance().writeObject(STORAGE_KEY, state);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// Writes the sequence counter so it keeps rising across a relaunch.
    private static void rememberSequence() {
        try {
            Preferences.set(PREF_SEQUENCE, sequence);
        } catch (Throwable t) {
            Log.e(t);
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

    /// True while the single publisher thread is alive. Guarded by PUBLISH_LOCK.
    private static boolean publishing;

    private static final Object PUBLISH_LOCK = new Object();

    /// Hands a state to the relay, in order, one at a time.
    ///
    /// A thread per checkpoint was a race with a silent and durable result: two checkpoints in
    /// quick succession raced to the same endpoint, and because a publish replaces the stored
    /// document, the slower OLDER request could land last and leave the user's other device
    /// fetching work they had already moved past. Nothing failed and nothing was logged.
    private static void publishToRelay(AppState state) {
        if (relay == null || !Display.isInitialized()) {
            return;
        }
        synchronized (PUBLISH_LOCK) {
            pendingPublish = state;
            if (publishing) {
                // The live publisher will pick this up when it finishes its current request,
                // which is what makes the ordering total.
                return;
            }
            publishing = true;
        }
        Display.getInstance().startThread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (;;) {
                        StateRelay r = relay;
                        AppState next;
                        synchronized (PUBLISH_LOCK) {
                            next = pendingPublish;
                            pendingPublish = null;
                        }
                        if (r == null || next == null) {
                            return;
                        }
                        try {
                            r.publish(next);
                        } catch (Throwable t) {
                            // Logged and dropped. The state is already in storage, and the next
                            // checkpoint carries a superset of it, so retrying this one would put
                            // an older state on the wire after a newer one.
                            Log.e(t);
                        }
                    }
                } finally {
                    AppState late;
                    synchronized (PUBLISH_LOCK) {
                        publishing = false;
                        late = pendingPublish;
                    }
                    if (late != null) {
                        // A checkpoint landed between the last read and clearing the flag, so
                        // nothing is publishing it. Handed back to the same entry point, which
                        // starts one publisher and keeps the ordering total.
                        publishToRelay(late);
                    }
                }
            }
        }, "Continuity relay publish").start();
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
    static void deliver(final AppState state) {
        if (!enabled || state == null) {
            return;
        }
        if (getDeviceId().equals(state.getDeviceId())) {
            // This device's own echo, which a relay returns as a matter of course.
            return;
        }
        if (isTooOld(state)) {
            // Checked here rather than only on the stored path. A relay hands back whatever it
            // still holds, which can be days old, and an expired checkout or booking hold that
            // auto-restored was the exact harm setMaxAge exists to prevent. Dropped before
            // lastSeen records it, so the sequence stays free for a fresher state from the same
            // device.
            return;
        }
        synchronized (lastSeen) {
            Long seen = lastSeen.get(state.getDeviceId());
            if (seen != null && seen.longValue() >= state.getSequence()) {
                return;
            }
            lastSeen.put(state.getDeviceId(), Long.valueOf(state.getSequence()));
        }
        if (!Display.isInitialized()) {
            setParked(state);
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                dispatch(state);
            }
        });
    }

    private static void dispatch(AppState state) {
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
            restore(state);
        } else {
            setParked(state);
        }
    }

    private static void park(final AppState state) {
        setParked(state);
        synchronized (HANDOFF_LOCK) {
            if (waitingForWindow) {
                return;
            }
            waitingForWindow = true;
        }
        Display.getInstance().startThread(new Runnable() {
            @Override
            public void run() {
                long deadline = System.currentTimeMillis() + WINDOW_WAIT_MILLIS;
                while (System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException err) {
                        break;
                    }
                    if (Display.getInstance().getCurrent() != null) {
                        break;
                    }
                }
                synchronized (HANDOFF_LOCK) {
                    waitingForWindow = false;
                }
                if (Display.getInstance().getCurrent() == null) {
                    return;
                }
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        // Taken and cleared rather than compared against the state this
                        // waiter was started for. A newer arrival while it waited is the one
                        // worth showing, and identity comparison would have discarded it.
                        AppState waiting;
                        synchronized (HANDOFF_LOCK) {
                            waiting = parked;
                            parked = null;
                        }
                        if (waiting != null) {
                            dispatch(waiting);
                        }
                    }
                });
            }
        }, "Continuity window wait").start();
    }

    private static String loadDeviceId() {
        try {
            String id = Preferences.get(PREF_DEVICE_ID, null);
            if (id == null || id.length() == 0) {
                id = Util.getUUID();
                Preferences.set(PREF_DEVICE_ID, id);
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

    private static long loadSequence() {
        try {
            return Preferences.get(PREF_SEQUENCE, (long) 0);
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
        if (b != null && enabled) {
            try {
                b.setCallback(new Callback());
            } catch (Throwable t) {
                Log.e(t);
            }
        }
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

    /// Internal. Re-installs the framework's inbound seam on whatever bridge the port now
    /// returns. Called by a port that swaps its bridge while the app is running, which only the
    /// simulator does -- a device's bridge is created once and lives as long as the process.
    public static void refreshBridge() {
        if (!enabled) {
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
    static void reset() {
        listeners.clear();
        synchronized (lastSeen) {
            lastSeen.clear();
        }
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
        synchronized (HANDOFF_LOCK) {
            deviceId = null;
            parked = null;
            dirty = false;
            waitingForWindow = false;
        }
        synchronized (PUBLISH_LOCK) {
            pendingPublish = null;
        }
    }

    private static void setParked(AppState state) {
        synchronized (HANDOFF_LOCK) {
            parked = state;
        }
    }

    /// The inbound seam handed to the port's bridge.
    static final class Callback implements ContinuityCallback {
        @Override
        public boolean continuationReceived(String activityType, Map<String, Object> userInfo) {
            if (!enabled || activityType == null || !activityType.equals(getActivityType())) {
                // Not ours. Answering honestly is what keeps a Handoff or third-party activity
                // this app never published from being swallowed by a handler that would do
                // nothing with it.
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
            com.codename1.continuity.sync.SyncedStore.notifyChanged();
        }
    }
}
