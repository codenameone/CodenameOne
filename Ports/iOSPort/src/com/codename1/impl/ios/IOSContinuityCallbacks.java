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

import com.codename1.continuity.spi.ContinuityCallback;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.ui.Display;

import java.util.HashMap;
import java.util.Map;

/// Static callback surface the native continuity glue calls into.
///
/// #### Why the static initializer calls everything once
///
/// ParparVM's dead-code eliminator decides a Java method is reachable by scanning the `.m` sources
/// for its mangled symbol and by following Java call graphs. These methods have no Java caller, and
/// the failure mode when they are stripped is not a link error -- they translate to empty stubs and
/// the native dispatch silently does nothing, so the build is green and continuations never arrive.
/// The guarded self-call in the static initializer is what keeps them alive.
///
/// The call must be unconditional. Wrapping it in an `if` the optimizer can prove false folds the
/// whole thing away and reintroduces the bug.
final class IOSContinuityCallbacks {
    /// The framework's inbound seam, owned by the event thread.
    ///
    /// The platform hands a continuation over on a thread of its own, so `nativeContinuation`
    /// marshals with `com.codename1.ui.Display#callSerially` and everything below it is ordinary
    /// EDT code. The one arrival that cannot be marshalled is the one that beats the event thread
    /// into existence -- a cold launch delivers from `willConnectToSession`, before Display is
    /// initialized -- and that one is parked on the platform's thread. It needs no guard either:
    /// the writes happen before the EDT is started, and starting a thread publishes everything
    /// written before it.
    private static ContinuityCallback callback;

    /// Written once by the class initializer, which every thread's first touch of this class
    /// happens after, so it needs no lock of its own.
    private static boolean dceGuard;

    /// A continuation that arrived before the framework was enabled, and the type it arrived
    /// under. Only ever one: a cold launch delivers a single activity, and a second arrival means
    /// the app is running and the callback is installed.
    private static String pendingType;
    private static String pendingJson;

    static {
        // Keep the native callback targets reachable for the iOS VM optimizer.
        dceGuard = true;
        nativeContinuation(null, null);
        nativeSyncedStoreChanged();
        dceGuard = false;
    }

    private IOSContinuityCallbacks() {
    }

    static void setCallback(ContinuityCallback c) {
        callback = c;
        String type = pendingType;
        String json = pendingJson;
        if (c == null || type == null) {
            return;
        }
        // A continuation that cold-launched the app reaches this class before the application's
        // init() has called Continuity.enable(), which is what installs the callback -- the scene
        // delegate hands it over from willConnectToSession, which runs first. Delivered now
        // instead of dropped, which is what the whole feature is for.
        boolean claimed = false;
        try {
            claimed = c.continuationReceived(type, parse(json));
        } catch (Throwable t) {
            Log.e(t);
        }
        if (claimed) {
            // Cleared only once a callback has actually TAKEN it. The callback can legitimately
            // decline: SyncedStore.addChangeListener installs one without enabling continuity --
            // a key/value store is not consent to restore a route stack -- and on a cold launch
            // that can happen before the application's init() calls enable(). Clearing regardless
            // meant the launch activity was erased by the refusal, and the enable() moments later
            // had nothing left to deliver.
            pendingType = null;
            pendingJson = null;
        }
    }

    /// An `NSUserActivity` of this app's continuity type arrived.
    ///
    /// #### Returns
    ///
    /// true when the framework claimed it, so the delegate can answer the system honestly rather
    /// than swallowing an activity this app never published
    public static boolean nativeContinuation(String activityType, String userInfoJson) {
        if (dceGuard) {
            return false;
        }
        // Answered on the platform's thread, from the activity type alone, because the delegate
        // needs the answer now: it decides whether the activity falls through to the intents
        // branch beside it, and one this app is about to act on must not.
        //
        // Declined only on a POSITIVE mismatch. Asking for the expected type can fail this early,
        // before the stub has published package_name, and treating "cannot tell" as "not ours"
        // would decline the framework's own cold launch -- the one case the whole feature exists
        // for. Belt and braces in any case: the delegate already matches the exact type the build
        // resolved, and this guards the app whose generated project predates that key.
        String expected = expectedTypeOrNull();
        if (expected != null && !expected.equals(activityType)) {
            return false;
        }
        if (!Display.isInitialized()) {
            // The event thread does not exist yet, so there is nothing to marshal to. Parked here
            // and drained by setCallback; the EDT is started after these writes, which publishes
            // them to it.
            pendingType = activityType;
            pendingJson = userInfoJson;
            return true;
        }
        // Handed over DIRECTLY, on this thread, and the framework does its own marshalling.
        //
        // This used to queue and answer true. The queue is where the arrival lost its place in
        // time: Continuity binds a continuation to the lifecycle generation it arrived in, and a
        // logout already sitting on the event queue runs first -- so the generation captured
        // after this hop is the one AFTER the logout, and the previous account's state is
        // restored and persisted by a session that promised nothing from before it survives.
        // Calling through means the generation is read at the instant the activity actually
        // arrived.
        //
        // The answer is the framework's own rather than an unconditional true, which is also what
        // the delegate should be told.
        return deliverToFramework(activityType, userInfoJson);
    }

    /// Hands an arrival to the framework, or holds it. Called on whatever thread the activity
    /// arrived on; the framework marshals what it needs to.
    private static boolean deliverToFramework(String activityType, String userInfoJson) {
        ContinuityCallback c = callback;
        boolean claimed = false;
        if (c != null) {
            try {
                claimed = c.continuationReceived(activityType, parse(userInfoJson));
            } catch (Throwable t) {
                Log.e(t);
            }
        }
        if (claimed) {
            return true;
        }
        // Held, because DECLINED is not the same as "not ours". A callback is installed by
        // SyncedStore.addChangeListener() as well as by Continuity.enable(), and the store
        // listener deliberately leaves continuity disabled -- so an app that registers one before
        // enabling has a live callback that answers false to everything. A continuation arriving
        // in that window used to be handed over, refused, and dropped, and the enable() moments
        // later had nothing to recover: registering an unrelated store listener turned a parked
        // cold-launch continuation into a lost one.
        pendingType = activityType;
        pendingJson = userInfoJson;
        return false;
    }

    /// The synced store changed on another of the user's devices.
    public static void nativeSyncedStoreChanged() {
        if (dceGuard) {
            return;
        }
        if (!Display.isInitialized()) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                ContinuityCallback c = callback;
                if (c == null) {
                    return;
                }
                try {
                    c.syncedStoreChanged();
                } catch (Throwable t) {
                    Log.e(t);
                }
            }
        });
    }

    /// This app's continuity activity type, or null when it cannot be determined yet.
    ///
    /// Null rather than a guess: `Continuity.getActivityType()` substitutes a placeholder package
    /// when the property is missing, and a placeholder compared against a real activity type is a
    /// mismatch that reads as certainty.
    private static String expectedTypeOrNull() {
        try {
            String pkg = Display.getInstance().getProperty("package_name", null);
            if (pkg == null || pkg.length() == 0) {
                return null;
            }
            return pkg + ".continuity";
        } catch (Throwable t) {
            return null;
        }
    }

    private static Map<String, Object> parse(String json) {
        if (json == null || json.length() == 0) {
            return new HashMap<String, Object>();
        }
        try {
            // CONFIGURED like StateCodec.fromJson, which is the reference: this is the same
            // document arriving through the other door, and a parser set up differently changes
            // what the application receives.
            //
            // useBoolean, because the default answers a raw JSON true or false with the strings
            // "true" and "false". Harmless for the tagged form this framework writes -- "b:true"
            // is a string either way -- and wrong for an untagged compatibility document from a
            // hand-written sender: the payload reaches the listeners and the provider with
            // Strings where booleans were sent, passes validation because a String is a
            // representable type, and is acknowledged.
            //
            // includeNulls, because dropping a null here is worse than refusing it. fromMap()
            // refuses a null nested in a list -- a property list cannot carry one, and the iOS
            // sanitiser shifts every index after it -- but only if it can see it. Dropped by the
            // parser, the list simply arrives one element shorter, which is the corruption that
            // check exists to prevent.
            JSONParser parser = new JSONParser();
            parser.setUseBooleanInstance(true);
            parser.setIncludeNullsInstance(true);
            Map<String, Object> parsed = parser.parseJSON(new java.io.StringReader(json));
            return parsed == null ? new HashMap<String, Object>() : parsed;
        } catch (Throwable t) {
            Log.e(t);
            return new HashMap<String, Object>();
        }
    }
}
