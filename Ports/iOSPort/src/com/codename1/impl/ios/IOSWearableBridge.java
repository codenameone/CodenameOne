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

    IOSWearableBridge(final IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
        // What to do when the pending-delivery cap discards one of our callbacks: forget that the
        // path's value was received. WatchConnectivity replaces the whole context on every publish,
        // so the entry is otherwise treated as unchanged forever and the app never sees it -- there
        // is no per-path redelivery to fall back on. Forgetting makes the next context update look
        // new. Runs after the drain, so the re-offer meets a listener.
        WearableConnection.setDroppedDeliveryHandler(
                new WearableConnection.DroppedDeliveryHandler() {
                    public void deliveryDropped(String path) {
                        // A null path is the rescan request; the native side reads it as "forget
                        // every received marker" and re-runs the delivery over the held context.
                        nativeInstance.wearableForgetReceived(path);
                    }
                });
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
        // Newline-separated with the newlines escaped, because "a CN1 path never contains one" was
        // a convention rather than a rule -- WearableMessage rejects only null and empty paths, and
        // Android and JavaSE carry a path with a newline in it without complaint. The native side
        // percent-escapes '%' and then '\n'; unescaping in the reverse order is what keeps a
        // literal "%0a" in a path from becoming a delimiter on the way back.
        java.util.List<String> parts = com.codename1.util.StringUtil.tokenize(joined, '\n');
        String[] out = new String[parts.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = com.codename1.util.StringUtil.replaceAll(
                    com.codename1.util.StringUtil.replaceAll(parts.get(i), "%0a", "\n"),
                    "%25", "%");
        }
        return out;
    }

    public void transferFile(String path, String name, byte[] contents) {
        // WCSession moves the file itself, so the bytes go across untouched; the native receive
        // side re-encodes them as a WearableMessage carrying name and contents, which is what the
        // delivery path decodes. Sending is therefore raw by design, not by omission.
        nativeInstance.wearableTransferFile(path, name, contents);
    }
}
