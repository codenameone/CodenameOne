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
package com.codename1.wearable.spi;

/// Internal service-provider interface implemented by each platform port to carry the
/// `com.codename1.wearable` API onto the native phone-to-watch transport (Apple's `WCSession` or
/// Google's Wearable Data Layer).
///
/// Application code never touches this interface -- it is obtained by the `com.codename1.wearable`
/// framework from `com.codename1.ui.Display#getWearableBridge()` and driven through the public
/// `com.codename1.wearable.WearableConnection` API. The base implementation returns `null`, which is
/// why the public API degrades to a harmless no-op on the simulator and on ports with no paired
/// device (so application code needs no platform `if` statements).
///
/// Payloads cross this interface as the opaque bytes produced by
/// `com.codename1.wearable.WearableMessage#toByteArray()`, so a port only has to move bytes and
/// never has to understand the value model. Incoming traffic is pushed back the other way by calling
/// the static entry points on `com.codename1.wearable.WearableConnection`
/// (`deliverMessage`, `deliverReply`, `deliverDataChanged`, `deliverDataRemoved`,
/// `notifyStateChanged`), which take care of EDT dispatch and of queueing across a cold start.
public interface WearableBridge {

    /// Returns true when this device can talk to a counterpart at all -- the transport exists and
    /// the app is allowed to use it. False on a platform with no wearable link, which makes the
    /// whole public API inert.
    ///
    /// #### Returns
    ///
    /// true if the wearable transport is available
    boolean isSupported();

    /// Returns true when a counterpart device is paired with this one, whether or not it is
    /// currently switched on or in range.
    ///
    /// #### Returns
    ///
    /// true if a counterpart device is paired
    boolean isPaired();

    /// Returns true when the peer app can receive a live message right now. This is the condition
    /// `sendMessage` needs; replicated data does not.
    ///
    /// #### Returns
    ///
    /// true if the peer app is reachable
    boolean isReachable();

    /// Returns true when the counterpart app is actually installed on the paired device. A paired
    /// watch with no watch app installed is the common case worth telling the user about.
    ///
    /// #### Returns
    ///
    /// true if the peer app is installed
    boolean isCompanionAppInstalled();

    /// Returns the currently connected counterpart devices, one entry per device, each formatted as
    /// `id \t displayName \t 1|0` where the trailing flag is whether the device is nearby. The flat
    /// string form keeps the interface to primitives so native ports do not have to construct Java
    /// objects.
    ///
    /// #### Returns
    ///
    /// the connected nodes, never null; an empty array when nothing is connected
    String[] getConnectedNodes();

    /// Sends a live message to the peer app, delivered only if it is reachable.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path the peer matches on
    /// - `payload`: the encoded payload
    /// - `replyToken`: a positive token to answer with `WearableConnection.deliverReply` when the
    ///   sender wants a reply, or 0 when it does not
    void sendMessage(String path, byte[] payload, int replyToken);

    /// Answers a message the peer sent with a reply token.
    ///
    /// #### Parameters
    ///
    /// - `replyToken`: the token that arrived with the request
    /// - `payload`: the encoded reply payload
    void sendReply(int replyToken, byte[] payload);

    /// Publishes or replaces the replicated value at a path. The value must survive this app being
    /// killed and must reach the peer whenever it next runs.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to publish under
    /// - `payload`: the encoded payload
    void putData(String path, byte[] payload);

    /// Returns the replicated value at a path, as published by either side.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to read
    ///
    /// #### Returns
    ///
    /// the encoded payload, or null when nothing is published at that path
    byte[] getData(String path);

    /// Removes the replicated value at a path.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to clear
    void removeData(String path);

    /// Returns every path that currently holds a replicated value.
    ///
    /// #### Returns
    ///
    /// the published paths, never null
    String[] getDataPaths();

    /// Transfers a file to the peer in the background. Delivery may happen long after this returns,
    /// including after this app has exited.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path the peer matches on
    /// - `name`: the file name to present to the peer
    /// - `contents`: the file bytes
    void transferFile(String path, String name, byte[] contents);
}
