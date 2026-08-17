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

import com.codename1.intents.spi.IntentBridge;

import java.util.Map;

/// iOS `IntentBridge`, backing `com.codename1.intents` with Core Spotlight and
/// App Intents.
///
/// Two native frameworks sit behind this, and they are not equally available:
///
/// - **Core Spotlight** carries indexing and the search-result tap. It is plain
///   Objective-C and has been available far below this port's deployment floor,
///   so indexing works on every device the app runs on.
/// - **App Intents** carries Siri, App Shortcuts and the Shortcuts app. It is
///   Swift-only and needs a newer iOS, so it is reached through the generated
///   Swift declarations and reports unsupported on older devices.
///
/// That asymmetry is why the capability queries are answered separately rather
/// than from one flag: an app can meaningfully index content on a device that
/// can never run an App Intent.
///
/// The whole class is inert unless the build linked the intent natives -- the
/// `CN1_USE_INTENTS` define the builder flips when the app references
/// `com.codename1.intents`. Without it every native answers unsupported and the
/// public API no-ops.
final class IOSIntentBridge implements IntentBridge {
    private final IOSNative nativeInstance;
    /// Images are staged into one native dictionary and consumed by the call that follows, so
    /// staging and consuming have to be one transaction. Two threads indexing at once would
    /// otherwise interleave: the second clears the staging area while the first is still between
    /// its own stage and index, and the first publishes without its thumbnails.
    private final Object stagingLock = new Object();

    IOSIntentBridge(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
    }

    public boolean areIntentsSupported() {
        return nativeInstance.intentsSupported();
    }

    public boolean isHeadlessExecutionSupported() {
        // An App Intent declared in the app target causes the system to launch
        // the app in the background and run perform() in-process, so headless
        // execution stands or falls with App Intents availability.
        return nativeInstance.intentsAppIntentsSupported();
    }

    public boolean isVoiceInvocationSupported() {
        return nativeInstance.intentsAppIntentsSupported();
    }

    public boolean isIndexingSupported() {
        return nativeInstance.intentsIndexingSupported();
    }

    public void registerIntents(String declarationsJson) {
        nativeInstance.intentsRegister(declarationsJson);
    }

    public void donate(String intentId, String paramsJson) {
        // A suggestion outlives the process while a parameterization does not, so the donation
        // has to record the base intent and the bound values. Donating the runtime id would
        // produce a suggestion that works until the app is killed and then fails as unknown.
        com.codename1.intents.DynamicIntent dyn =
                com.codename1.intents.Intents.getDynamicIntent(intentId);
        if (dyn == null) {
            nativeInstance.intentsDonate(intentId, paramsJson);
            return;
        }
        nativeInstance.intentsDonate(dyn.getBaseIntentId(),
                com.codename1.intents.IntentSerializer.mergeParams(
                        dyn.getBoundParameters(), paramsJson));
    }

    public void index(String entitiesJson, Map<String, byte[]> images) {
        // Thumbnails travel as their own call so the JSON stays a plain string
        // across the boundary; the native side matches them by the name the
        // serializer embedded in the document.
        synchronized (stagingLock) {
            stage(images);
            nativeInstance.intentsIndex(entitiesJson);
        }
    }

    public void removeFromIndex(String idsJson) {
        nativeInstance.intentsRemoveFromIndex(idsJson);
    }

    public void clearIndex(String entityType) {
        nativeInstance.intentsClearIndex(entityType);
    }

    public void completeInvocation(String token, String resultJson, Map<String, byte[]> images) {
        synchronized (stagingLock) {
            stage(images);
            nativeInstance.intentsCompleteInvocation(token, resultJson);
        }
    }

    /// Stages this request's blobs. Always called while holding the staging lock.
    private void stage(Map<String, byte[]> images) {
        if (images == null) {
            return;
        }
        for (Map.Entry<String, byte[]> e : images.entrySet()) {
            byte[] data = e.getValue();
            if (data != null && data.length > 0) {
                nativeInstance.intentsStageImage(e.getKey(), data, data.length);
            }
        }
    }
}
