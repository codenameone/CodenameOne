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

import com.codename1.home.SmartHome;
import com.codename1.util.StringUtil;

import java.util.List;

/// Static callback surface invoked from `CN1SmartHome` when HomeKit answers.
///
/// Mirrors `IOSWearableCallbacks`: the static initializer calls each entry
/// point once, guarded so it has no effect, purely to keep the ParparVM
/// dead-code eliminator from stripping targets that no Java code calls.
/// Without that guard the optimizer replaces them with empty stubs and every
/// operation hangs waiting for an answer that was compiled away -- a failure
/// with nothing in the log to explain it.
///
/// Everything here forwards straight to [SmartHome], which owns EDT dispatch.
/// That matters here specifically: `HMHomeManagerDelegate` and
/// `HMAccessoryDelegate` callbacks arrive on the Objective-C main queue, and
/// under ParparVM that is not the Codename One EDT.
final class IOSHomeCallbacks {

    private static IOSHomeBridge bridge;
    private static boolean dceGuard;

    static {
        // Keep the native callback targets reachable for the iOS VM
        // optimizer.
        dceGuard = true;
        started(0, 0, null);
        refreshed(0, null);
        authorization(0, 0, null);
        readings(0, null, null);
        writeResults(0, null, null);
        sceneResult(0, null, null, null);
        commissioningResult(0, null, null, null, 0, null);
        identifyResult(0, null);
        drained(0, 0, null);
        changes(null, null);
        resyncRequired(null);
        structureChanged(0, null, null);
        dceGuard = false;
    }

    private IOSHomeCallbacks() {
    }

    /// Returns the singleton smart-home bridge, creating it on first use.
    ///
    /// #### Parameters
    ///
    /// - `nativeInstance`: the port's native surface
    ///
    /// #### Returns
    ///
    /// the bridge, never `null`
    static synchronized IOSHomeBridge getBridge(IOSNative nativeInstance) {
        if (bridge == null) {
            bridge = new IOSHomeBridge(nativeInstance);
        }
        return bridge;
    }

    /// Reached from the bridge's constructor so this class is initialized --
    /// and its dead-code guard therefore runs -- before any native code can
    /// call back into it.
    static void keepAlive() {
        // The static initializer is the work; this exists to trigger it from
        // a caller the optimizer can see.
    }

    // ---- Callbacks invoked from native code (do not rename) ---------------

    /// Called from native when the initial connection and graph load
    /// finishes.
    static void started(int requestId, int availabilityOrdinal, String error) {
        if (dceGuard) {
            return;
        }
        SmartHome.deliverStarted(requestId, availabilityOrdinal, error);
    }

    /// Called from native when a graph reload finishes.
    static void refreshed(int requestId, String error) {
        if (dceGuard) {
            return;
        }
        SmartHome.deliverRefreshed(requestId, error);
    }

    /// Called from native when the authorization prompt closes.
    static void authorization(int requestId, int statusOrdinal, String error) {
        if (dceGuard) {
            return;
        }
        SmartHome.deliverAuthorization(requestId, statusOrdinal, error);
    }

    /// Called from native with the answer to a read, as newline-joined
    /// records.
    static void readings(int requestId, String joinedLines, String error) {
        if (dceGuard) {
            return;
        }
        SmartHome.deliverReadings(requestId, split(joinedLines), error);
    }

    /// Called from native with the outcome of each write, as newline-joined
    /// records.
    static void writeResults(int requestId, String joinedLines, String error) {
        if (dceGuard) {
            return;
        }
        SmartHome.deliverWriteResults(requestId, split(joinedLines), error);
    }

    /// Called from native when a scene is run, created or deleted.
    static void sceneResult(int requestId, String sceneLine,
            String structureId, String error) {
        if (dceGuard) {
            return;
        }
        SmartHome.deliverSceneResult(requestId, sceneLine, structureId, error);
    }

    /// Called from native when the MatterSupport add-device flow finishes.
    static void commissioningResult(int requestId, String accessoryId,
            String accessoryName, String structureId, int commissionedToThisApp,
            String error) {
        if (dceGuard) {
            return;
        }
        SmartHome.deliverCommissioningResult(requestId, accessoryId,
                accessoryName, structureId, commissionedToThisApp, error);
    }

    /// Called from native when an identify request finishes.
    static void identifyResult(int requestId, String error) {
        if (dceGuard) {
            return;
        }
        SmartHome.deliverIdentifyResult(requestId, error);
    }

    /// Called from native when a drain finishes, after its changes.
    static void drained(int requestId, int deliveredCount, String error) {
        if (dceGuard) {
            return;
        }
        SmartHome.deliverDrained(requestId, deliveredCount, error);
    }

    /// Called from native when watched characteristics change, as
    /// newline-joined records.
    static void changes(String subscriptionId, String joinedLines) {
        if (dceGuard) {
            return;
        }
        SmartHome.deliverChanges(subscriptionId, split(joinedLines));
    }

    /// Called from native when HomeKit's notification stream was interrupted
    /// and the values a subscription is tracking can no longer be trusted.
    static void resyncRequired(String subscriptionId) {
        if (dceGuard) {
            return;
        }
        SmartHome.deliverResyncRequired(subscriptionId);
    }

    /// Called from native when the home graph moves.
    static void structureChanged(int changeKindOrdinal, String structureId,
            String accessoryId) {
        if (dceGuard) {
            return;
        }
        SmartHome.notifyStructureChanged(changeKindOrdinal, structureId,
                accessoryId);
    }

    private static String[] split(String joined) {
        if (joined == null || joined.length() == 0) {
            return new String[0];
        }
        List<String> parts = StringUtil.tokenize(joined, '\n');
        String[] out = new String[parts.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = parts.get(i);
        }
        return out;
    }
}
