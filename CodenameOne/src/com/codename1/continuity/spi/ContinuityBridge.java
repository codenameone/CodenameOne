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
package com.codename1.continuity.spi;

import java.util.Map;

/// The platform seam of the continuity framework, implemented by ports and returned from
/// `CodenameOneImplementation.getContinuityBridge()`. A null bridge -- the base implementation --
/// leaves saving and restoring state on this device working, because that half is pure
/// `com.codename1.io.Storage`, and makes every cross-device capability report itself unsupported.
///
/// Two independent capabilities live behind one bridge because a port that has either almost
/// always has both, and an app asks about them separately anyway:
///
/// - *Continuation* advertises what the user is doing so a second device they own can pick it up
///   while the two are together. On Apple platforms this is an `NSUserActivity`; nothing else
///   implements it, and nothing else is expected to.
/// - *The synced store* is a small key/value store the platform carries between the user's
///   devices without them being near each other.
///
/// Everything crosses this boundary as data -- strings and plist-representable maps -- never as
/// live model objects, because on Apple platforms the payload is handed to the operating system
/// and may be delivered to a different device, and a different build of the app, than the one that
/// produced it.
public interface ContinuityBridge {
    /// Returns true when this port can advertise the user's current activity to their other
    /// devices.
    boolean isContinuationSupported();

    /// Advertises the user's current activity, replacing whatever was advertised before.
    ///
    /// The payload has already been validated as representable and within the platform's size
    /// budget by the time it arrives here.
    ///
    /// #### Parameters
    ///
    /// - `activityType`: the reverse-DNS type the build declared
    /// - `title`: a human readable label the receiving device may show, or null
    /// - `userInfo`: the state, as strings, numbers, booleans, lists and maps of those
    void publishContinuation(String activityType, String title, Map<String, Object> userInfo);

    /// Withdraws the advertised activity. Nothing is being continued after this returns.
    void clearContinuation();

    /// Returns true when this port has a key/value store the platform syncs between the user's
    /// devices.
    boolean isSyncedStoreSupported();

    /// Writes a value to the synced store, replacing any previous value for the key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `value`: the value
    void syncedStorePut(String key, String value);

    /// Reads a value from the synced store.
    ///
    /// #### Returns
    ///
    /// the value, or null when the key is absent
    String syncedStoreGet(String key);

    /// Removes a key from the synced store. Removing an absent key does nothing.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    void syncedStoreRemove(String key);

    /// Every key currently in the synced store, in no particular order. Never null.
    String[] syncedStoreKeys();

    /// Installs the framework's inbound seam. Called once during initialization, before any other
    /// method on this bridge; ports must retain it and may call it from any thread.
    ///
    /// #### Parameters
    ///
    /// - `callback`: the seam, never null
    void setCallback(ContinuityCallback callback);
}
