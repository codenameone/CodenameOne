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
    private static ContinuityCallback callback;
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
        pendingType = null;
        pendingJson = null;
        if (c != null && type != null) {
            // A continuation that cold-launched the app can reach this class before the
            // application's init() has called Continuity.enable(), which is what installs the
            // callback -- the scene delegate hands it over from willConnectToSession, which runs
            // first. Delivered now instead of dropped, which is what the whole feature is for.
            try {
                c.continuationReceived(type, parse(json));
            } catch (Throwable t) {
                Log.e(t);
            }
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
        ContinuityCallback c = callback;
        if (c == null) {
            // The framework has not been enabled yet. That is the ordinary cold-launch ordering
            // rather than a mistake, so the activity is held for setCallback to deliver instead
            // of being dropped.
            //
            // Claimed all the same. The delegate's answer decides whether the activity falls
            // through to the intents branch beside it, and one this app is about to act on must
            // not: an app using both frameworks would otherwise have its own continuation offered
            // to the wrong one, which would correctly decline it, and the launch would land on the
            // home screen.
            pendingType = activityType;
            pendingJson = userInfoJson;
            return true;
        }
        try {
            return c.continuationReceived(activityType, parse(userInfoJson));
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    /// The synced store changed on another of the user's devices.
    public static void nativeSyncedStoreChanged() {
        if (dceGuard) {
            return;
        }
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

    private static Map<String, Object> parse(String json) {
        if (json == null || json.length() == 0) {
            return new HashMap<String, Object>();
        }
        try {
            Map<String, Object> parsed = JSONParser.parseJSON(json);
            return parsed == null ? new HashMap<String, Object>() : parsed;
        } catch (Throwable t) {
            Log.e(t);
            return new HashMap<String, Object>();
        }
    }
}
