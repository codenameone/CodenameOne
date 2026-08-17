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
import com.codename1.intents.IntentResult;
import com.codename1.intents.IntentSerializer;
import com.codename1.intents.IntentSource;
import com.codename1.intents.Intents;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.ui.Display;

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

    /// Returns the singleton intent bridge, creating it on first use.
    static synchronized IOSIntentBridge getBridge(IOSNative nativeInstance) {
        if (bridge == null) {
            bridge = new IOSIntentBridge(nativeInstance);
        }
        return bridge;
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
        Intents.dispatchInvocation(intentId, params, IntentSource.VOICE, headless,
                new IntentCompletion() {
                    public void onIntentResult(IntentResult result) {
                        Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
                        String json = IntentSerializer.serializeResult(result, images);
                        IOSIntentBridge b = bridge;
                        if (b != null) {
                            b.completeInvocation(token, json, images);
                        }
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
        return Intents.dispatchUserActivity(activityType, parse(userInfoJson));
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
            return JSONParser.parseJSON(json);
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }
}
