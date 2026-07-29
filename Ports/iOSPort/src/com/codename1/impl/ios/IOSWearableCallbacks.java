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

import com.codename1.wearable.WearableConnection;

/// Static callback surface invoked from `CN1WatchConnectivity` when the peer app sends something.
///
/// Mirrors the `IOSSurfaceCallbacks` pattern: the static initializer calls each callback once
/// (guarded so it has no effect) purely to keep the ParparVM dead-code eliminator from stripping
/// targets that have no Java caller. Everything here forwards straight to
/// `WearableConnection`, which owns EDT dispatch and the cold-start queue.
final class IOSWearableCallbacks {
    private static IOSWearableBridge bridge;
    private static boolean dceGuard;

    static {
        // Keep the native callback targets reachable for the iOS VM optimizer.
        dceGuard = true;
        nativeMessageReceived(null, null, 0);
        nativeReplyReceived(0, null, null);
        nativeDataChanged(null, null);
        nativeDataRemoved(null);
        nativeStateChanged();
        dceGuard = false;
    }

    private IOSWearableCallbacks() {
    }

    /// Returns the singleton wearable bridge, creating it on first use.
    static synchronized IOSWearableBridge getBridge(IOSNative nativeInstance) {
        if (bridge == null) {
            bridge = new IOSWearableBridge(nativeInstance);
        }
        return bridge;
    }

    // ---- Callbacks invoked from native code (do not rename) ----------------

    /// Called from native when the peer app sends a live message.
    static void nativeMessageReceived(String path, byte[] payload, int replyToken) {
        if (dceGuard) {
            return;
        }
        WearableConnection.deliverMessage(path, payload, replyToken);
    }

    /// Called from native with the peer's answer to a message that asked for one.
    static void nativeReplyReceived(int replyToken, byte[] payload, String error) {
        if (dceGuard) {
            return;
        }
        WearableConnection.deliverReply(replyToken, payload, error);
    }

    /// Called from native when the peer publishes or updates a replicated value.
    static void nativeDataChanged(String path, byte[] payload) {
        if (dceGuard) {
            return;
        }
        WearableConnection.deliverDataChanged(path, payload);
    }

    /// Called from native when the peer removes a replicated value.
    static void nativeDataRemoved(String path) {
        if (dceGuard) {
            return;
        }
        WearableConnection.deliverDataRemoved(path);
    }

    /// Called from native when reachability, pairing or peer-app installation changes.
    static void nativeStateChanged() {
        if (dceGuard) {
            return;
        }
        WearableConnection.notifyStateChanged();
    }
}
