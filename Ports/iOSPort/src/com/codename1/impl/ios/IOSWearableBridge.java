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

import com.codename1.wearable.spi.WearableBridge;

/// Apple `WearableBridge`, backing `com.codename1.wearable` with `WCSession`.
///
/// The same class runs on both halves of a pair: WatchConnectivity is symmetric, so the phone app
/// and the watch app use identical code and the Java API behaves identically at both ends. The three
/// transports map onto WCSession as follows:
///
/// - a live message is `sendMessage:replyHandler:`, delivered only while the peer is reachable;
/// - replicated data is the session's application context, which survives both apps being killed and
///   is handed to the peer whenever it next runs;
/// - a file transfer is `transferFile:metadata:`, which the system schedules in the background.
///
/// Payloads cross as opaque bytes, so the native layer never has to understand the value model.
///
/// This whole class is dead code unless the build linked the WatchConnectivity natives (the
/// `CN1_USE_WATCHCONNECTIVITY` define the builder flips when the app references
/// `com.codename1.wearable`); without it every native answers unsupported and the public API no-ops.
final class IOSWearableBridge implements WearableBridge {
    private final IOSNative nativeInstance;

    IOSWearableBridge(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
    }

    public boolean isSupported() {
        return nativeInstance.wearableSupported();
    }

    public boolean isPaired() {
        return nativeInstance.wearablePaired();
    }

    public boolean isReachable() {
        return nativeInstance.wearableReachable();
    }

    public boolean isCompanionAppInstalled() {
        return nativeInstance.wearableCompanionInstalled();
    }

    public String[] getConnectedNodes() {
        if (!isReachable()) {
            // WCSession has no node list -- Apple pairs exactly one watch -- so the peer is either
            // there or it is not, and "there" is what reachable means.
            return new String[0];
        }
        String name = nativeInstance.wearablePeerName();
        String id = nativeInstance.wearablePeerId();
        return new String[] {(id == null ? "peer" : id) + "\t"
                + (name == null ? "Paired device" : name) + "\t1"};
    }

    public void sendMessage(String path, byte[] payload, int replyToken) {
        nativeInstance.wearableSendMessage(path, payload, replyToken);
    }

    public void sendReply(int replyToken, byte[] payload) {
        nativeInstance.wearableSendReply(replyToken, payload);
    }

    public void putData(String path, byte[] payload) {
        nativeInstance.wearablePutData(path, payload);
    }

    public byte[] getData(String path) {
        return nativeInstance.wearableGetData(path);
    }

    public void removeData(String path) {
        nativeInstance.wearableRemoveData(path);
    }

    public String[] getDataPaths() {
        String joined = nativeInstance.wearableDataPaths();
        if (joined == null || joined.length() == 0) {
            return new String[0];
        }
        // Newline-separated: a CN1 path is URL-shaped and never contains one, and a single string
        // keeps the native signature to primitives.
        java.util.List<String> parts = com.codename1.util.StringUtil.tokenize(joined, '\n');
        return parts.toArray(new String[parts.size()]);
    }

    public void transferFile(String path, String name, byte[] contents) {
        nativeInstance.wearableTransferFile(path, name, contents);
    }
}
