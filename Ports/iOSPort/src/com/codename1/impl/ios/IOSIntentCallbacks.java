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
package com.codename1.impl.ios;

import com.codename1.intents.AppEntity;
import com.codename1.intents.IntentCompletion;
import com.codename1.intents.IntentDeclaration;
import com.codename1.intents.IntentResult;
import com.codename1.intents.IntentSerializer;
import com.codename1.intents.IntentSource;
import com.codename1.intents.Intents;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Static callback surface the native intent glue calls into.
///
/// #### Why the static initializer calls everything once
///
/// ParparVM's dead-code eliminator decides a Java method is reachable by
/// scanning the `.m` sources for its mangled symbol and by following Java call
/// graphs. These methods have no Java caller, and the failure mode when they are
/// stripped is not a link error -- they translate to empty stubs and the native
/// dispatch silently does nothing. The guarded self-call in the static
/// initializer is what keeps them alive.
///
/// The call must be unconditional. Wrapping it in an `if` the optimizer can
/// prove false folds the whole thing away and reintroduces the bug.
final class IOSIntentCallbacks {
    private static IOSIntentBridge bridge;
    private static boolean dceGuard;

    static {
        // Keep the native callback targets reachable for the iOS VM optimizer.
        dceGuard = true;
        nativePerformIntent(null, null, null, false);
        nativeSpotlightItemSelected(null);
        nativeUserActivity(null, null);
        nativeQueryEntities(null, null, null);
        dceGuard = false;
    }

    private IOSIntentCallbacks() {
    }

    /// The bridge, asking the implementation for it when nothing has yet.
    ///
    /// `bridge` is only assigned when something calls through the port -- an application using
    /// `Intents.index`, for instance. An application that only declares intents never does, so
    /// the very first App Intent would reach the completion below with a null bridge and drop
    /// its result, leaving the Swift continuation unresumed: the handler ran, and the assistant
    /// was told nothing. Going through Display also flushes the declarations that could not be
    /// published before Display.init existed.
    private static IOSIntentBridge resolveBridge() {
        IOSIntentBridge b = bridge;
        if (b != null) {
            return b;
        }
        if (!Display.isInitialized()) {
            // Asked before there is anything to ask. getInstance() hands back a Display whose
            // implementation is still null, so reaching through it for the bridge throws --
            // and this is a supported moment, not an error: the whole point of the queue below
            // is that a headless intent can finish this early. Answered by returning null, so
            // the caller holds the result rather than discovering the same thing by exception.
            return bridge;
        }
        try {
            // Assigns the static as a side effect, through getBridge below.
            Display.getInstance().getIntentBridge();
        } catch (Throwable t) {
            // The logging is guarded too, and that is not belt-and-braces. Log.e routes
            // through the same implementation that is missing here, so it can throw on its
            // own -- and an exception escaping this method escapes completeOrQueue with it,
            // which means the answer is never queued and the Swift continuation is never
            // resumed. The assistant then waits forever on an intent that has already
            // finished. A lost log line costs incomparably less than that.
            try {
                Log.e(t);
            } catch (Throwable ignored) {
            }
        }
        return bridge;
    }

    /// Returns the singleton intent bridge, creating it on first use.
    static synchronized IOSIntentBridge getBridge(IOSNative nativeInstance) {
        if (bridge == null) {
            bridge = new IOSIntentBridge(nativeInstance);
            // Anything that finished before there was a bridge is waiting on this moment.
            deliverQueuedCompletions(bridge);
        }
        return bridge;
    }

    /// Results that finished before a bridge existed to carry them.
    private static final List<Object[]> QUEUED = new ArrayList<Object[]>();

    /// Answers the native token, or holds the answer until something can.
    ///
    /// The bridge needs the implementation's IOSNative instance, so in the earliest cold-start
    /// window -- before Display.init, which is a window this feature deliberately supports --
    /// there is none, and this used to drop the result. The Swift side is a withCheckedContinuation
    /// inside perform() with no other resume path, so dropping it left Siri or Shortcuts waiting
    /// on an intent that had already finished, with no timeout and nothing in the log.
    private static void completeOrQueue(String token, String json, Map<String, byte[]> images) {
        IOSIntentBridge b = resolveBridge();
        if (b != null) {
            b.completeInvocation(token, json, images);
            return;
        }
        synchronized (QUEUED) {
            QUEUED.add(new Object[]{token, json, images});
        }
        // Racing getBridge: one may have appeared between the check above and the queueing.
        // Draining here as well is idempotent -- the queue is emptied under its own lock -- and
        // it is what closes that window.
        IOSIntentBridge appeared = resolveBridge();
        if (appeared != null) {
            deliverQueuedCompletions(appeared);
        }
    }

    /// Hands every held result to the bridge, in the order they finished.
    @SuppressWarnings("unchecked")
    private static void deliverQueuedCompletions(IOSIntentBridge b) {
        List<Object[]> drained;
        synchronized (QUEUED) {
            if (QUEUED.isEmpty()) {
                return;
            }
            drained = new ArrayList<Object[]>(QUEUED);
            QUEUED.clear();
        }
        // The loop is outside any handler on purpose: iterating List<Object[]> compiles to a
        // CHECKCAST, and ParparVM does not throw for a failed cast, so a catch around one reads
        // as relying on an exception that never arrives.
        for (int i = 0; i < drained.size(); i++) {
            Object[] held = drained.get(i);
            String token = (String) held[0];
            String json = (String) held[1];
            Map<String, byte[]> images = (Map<String, byte[]>) held[2];
            try {
                b.completeInvocation(token, json, images);
            } catch (Throwable t) {
                // One token failing must not strand the rest: every one of them is a Swift
                // continuation that nothing else will ever resume.
                Log.e(t);
            }
        }
    }

    // ---- Callbacks invoked from native code (do not rename) ----------------

    /// Runs an intent the system asked for and reports the result back through
    /// the token the native side is holding.
    ///
    /// This returns immediately. The caller is a Swift `perform()` awaiting a
    /// continuation, and the framework answers it asynchronously through
    /// `IntentBridge.completeInvocation` once the handler finishes or the
    /// deadline passes -- exactly once, because resuming a continuation twice is
    /// a hard crash.
    public static void nativePerformIntent(final String token, String intentId,
                                            String paramsJson, boolean headless) {
        if (dceGuard) {
            return;
        }
        Map<String, Object> params = parse(paramsJson);
        if (!headless && !hasWindow()) {
            // The same rule nativeUserActivity applies, for the same reason and on the other
            // door. openAppWhenRun asks the system to foreground the app; it does not wait for
            // Codename One to have started, so a cold launch runs perform() while init/start
            // are still only enqueued. The handler of a non-headless intent is documented to
            // update the screen through callSerially -- against no Form at all, that either
            // does nothing or builds into a Display that is not ready.
            //
            // Held on its own thread rather than parked in a list: unlike a continued activity,
            // an invocation owns a token that has to be answered exactly once, so it cannot be
            // dropped when the wait expires.
            Thread t = new Thread(new InvocationWaiter(token, intentId, params, headless),
                    "CN1 Intent window " + intentId);
            t.start();
            return;
        }
        runInvocation(token, intentId, params, headless);
    }

    /// Waits for the application to have a window, then runs the invocation.
    ///
    /// Named and static rather than anonymous: it outlives the callback that started it.
    private static final class InvocationWaiter implements Runnable {
        private final String token;
        private final String intentId;
        private final Map<String, Object> params;
        private final boolean headless;

        InvocationWaiter(String token, String intentId, Map<String, Object> params,
                          boolean headless) {
            this.token = token;
            this.intentId = intentId;
            this.params = params;
            this.headless = headless;
        }

        @Override
        public void run() {
            long giveUpAt = System.currentTimeMillis() + WINDOW_WAIT_MILLIS;
            while (System.currentTimeMillis() < giveUpAt) {
                if (hasWindow()) {
                    runInvocation(token, intentId, params, headless);
                    return;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            // A launch that never produced a window is a broken application, but the token is
            // still owed an answer: a continuation that is never resumed leaves the assistant
            // waiting forever, which is worse than a reported failure. A parked *activity* can
            // simply be dropped here because nothing is waiting on it.
            Log.p("[intents] the application never produced a window; not running \"" + intentId
                    + "\"", Log.WARNING);
            Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
            completeOrQueue(token, IntentSerializer.serializeResult(
                    IntentResult.failed("The application did not finish starting"), images),
                    images);
        }
    }

    /// Hands one invocation to the framework and answers its token with whatever comes back.
    private static void runInvocation(final String token, String intentId,
                                       Map<String, Object> params, boolean headless) {
        // Not VOICE. Everything the system runs through an App Intent arrives here -- Siri, the
        // Shortcuts app, an App Shortcut on the home screen -- and perform() is not told which.
        // Claiming Siri for all of them was a statement the platform never made, and an
        // application reading getSource() for analytics or for how much detail to speak was
        // reading a guess. UNKNOWN is the case for "the platform did not say", which is exactly
        // what happened.
        Intents.dispatchInvocation(intentId, params, IntentSource.UNKNOWN, headless,
                new IntentCompletion() {
                    @Override
                    public void onIntentResult(IntentResult result) {
                        Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
                        String json;
                        try {
                            json = IntentSerializer.serializeResult(result, images);
                        } catch (Throwable t) {
                            // The token has to be answered whatever happens here. The guard has
                            // already recorded this invocation as complete, so the timeout
                            // thread will never fire for it -- if this throws and nothing
                            // reaches the native side, the Swift continuation is never resumed
                            // and the assistant waits forever. A result too deep to serialize,
                            // for instance, would do that.
                            Log.e(t);
                            images.clear();
                            json = IntentSerializer.serializeResult(
                                    IntentResult.failed("The result could not be delivered"),
                                    images);
                        }
                        completeOrQueue(token, json, images);
                    }
                });
    }

    /// The user tapped an item this app published to device search. Delivered as
    /// the entity's own id, which is what the framework indexed it under.
    public static void nativeSpotlightItemSelected(String identifier) {
        if (dceGuard) {
            return;
        }
        Intents.dispatchSpotlightSelection(identifier);
    }

    /// A non-browsing `NSUserActivity` arrived. Returns true when the app claimed
    /// it, so the delegate can answer the system honestly rather than swallowing
    /// activities it never declared.
    public static boolean nativeUserActivity(String activityType, String userInfoJson) {
        if (dceGuard) {
            return false;
        }
        Map<String, Object> params = parse(userInfoJson);
        if (needsAWindowFirst(activityType)) {
            // A donated activity can cold-launch the app, and both delegates deliver it before
            // the application has finished starting: the legacy one calls through right after
            // IOSImplementation.callback, which only *enqueues* init/start, and the scene
            // delegate calls through from willConnectToSession. The generated bootstrap
            // installed the dispatcher from main, so the framework would happily run the
            // handler -- against no Form at all, for an intent whose whole contract is that it
            // has one. Held until there is a window, which is what Android's foreground queue
            // does for the same reason.
            synchronized (PARKED) {
                PARKED.add(new String[]{activityType, userInfoJson});
                if (!waiting) {
                    waiting = true;
                    startWindowWaiter();
                }
            }
            // Claimed: it is ours, and the policy that decides whether it may actually run is
            // applied on delivery, exactly as it would have been now.
            return true;
        }
        return Intents.dispatchUserActivity(activityType, params);
    }

    /// Activities waiting for the application to finish starting.
    private static final List<String[]> PARKED = new ArrayList<String[]>();

    /// True while a waiter thread is alive. Guarded by PARKED.
    private static boolean waiting;

    /// How long to wait for the first window before giving up on a parked activity.
    ///
    /// A launch that never produces one is a broken application, not something to keep a
    /// thread alive for; and running the handler minutes later, into whatever the user is
    /// doing by then, is worse than not running it.
    private static final long WINDOW_WAIT_MILLIS = 15000L;

    /// Whether this activity has to wait for the application to have a window.
    ///
    /// Only a declared, non-headless intent does. A headless one is expressly allowed to run
    /// with nothing on screen -- that is what the flag buys -- and an activity type this
    /// application does not declare is not ours to hold.
    private static boolean needsAWindowFirst(String activityType) {
        if (hasWindow()) {
            return false;
        }
        IntentDeclaration decl = Intents.getDeclaration(activityType);
        return decl != null && !decl.runsHeadless();
    }

    /// True when the application has started far enough to have something on screen.
    private static boolean hasWindow() {
        return Display.isInitialized() && Display.getInstance().getCurrent() != null;
    }

    /// Polls for the first window and then delivers everything held.
    private static void startWindowWaiter() {
        Thread t = new Thread(new WindowWaiter(), "CN1 Intent window");
        t.start();
    }

    /// Named and static rather than anonymous: it outlives the call that started it, and an
    /// anonymous class here would hold whatever created it.
    private static final class WindowWaiter implements Runnable {
        @Override
        public void run() {
            long giveUpAt = System.currentTimeMillis() + WINDOW_WAIT_MILLIS;
            while (System.currentTimeMillis() < giveUpAt) {
                if (hasWindow()) {
                    deliverParked();
                    return;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            synchronized (PARKED) {
                if (!PARKED.isEmpty()) {
                    Log.p("[intents] the application never produced a window; dropping "
                            + PARKED.size() + " continued activity(ies)", Log.WARNING);
                    PARKED.clear();
                }
                waiting = false;
            }
        }
    }

    /// Hands every held activity to the framework, in the order they arrived.
    private static void deliverParked() {
        List<String[]> drained;
        synchronized (PARKED) {
            drained = new ArrayList<String[]>(PARKED);
            PARKED.clear();
            waiting = false;
        }
        // The loop is outside any handler: iterating List<String[]> compiles to a CHECKCAST,
        // and ParparVM does not throw for a failed cast.
        for (int i = 0; i < drained.size(); i++) {
            String[] held = drained.get(i);
            try {
                Intents.dispatchUserActivity(held[0], parse(held[1]));
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    /// Answers an entity query the platform runs while building its own picker.
    /// Returns the serialized entities, since the native side needs data rather
    /// than objects.
    public static String nativeQueryEntities(String entityType, String kind, String argument) {
        if (dceGuard) {
            return null;
        }
        try {
            List<AppEntity> found = Intents.queryEntities(entityType, kind, argument);
            // Thumbnails travel inside the document here rather than through the staging area
            // the index and result paths use. This reply is synchronous -- the platform is
            // building a picker and blocking on it -- so there is no second call to hand the
            // blobs to, and staging them would leave them for whatever native call happened
            // next to consume. An entity thumbnail is a picker-row image, so inlining a few of
            // them is the whole transaction.
            return IntentSerializer.serializeEntities(found, null, true);
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    private static Map<String, Object> parse(String json) {
        if (json == null || json.length() == 0) {
            return null;
        }
        try {
            return IntentSerializer.parsePayload(json);
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }
}
