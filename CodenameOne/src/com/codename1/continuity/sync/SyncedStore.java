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
package com.codename1.continuity.sync;

import com.codename1.continuity.Continuity;
import com.codename1.continuity.spi.ContinuityBridge;
import com.codename1.io.Log;

import java.util.ArrayList;
import java.util.List;

/// A small key/value store the platform carries between the devices one person signed in to,
/// without them ever being in the same room.
///
/// This is the slow, patient half of continuity. `com.codename1.continuity.Continuity` hands the
/// current activity to a device that is *here, now*; this keeps a handful of durable settings --
/// which theme, which sort order, which tutorial they already dismissed, the id of the document
/// they are working through -- in step across everything they own.
///
/// ```java
/// SyncedStore.put("sortOrder", "byDate");
/// String order = SyncedStore.get("sortOrder", "byName");
/// ```
///
/// #### What it is not
///
/// Not storage. Not a database, not a cache, and not a place for anything the app cannot cheerfully
/// do without: the platform decides when to sync, the user can turn the whole mechanism off, and a
/// device that has never been online has an empty store. Treat every read as "the value, or the
/// default" -- which is why there is no read without a default.
///
/// Not secret. The contents leave the device and are held by the platform on the user's behalf.
/// Credentials belong in `com.codename1.security.SecureStorage`.
///
/// Not large. The platform imposes a total size and a key count, both small; `put` reports a
/// failure to write rather than pretending it stored something.
///
/// #### What it costs
///
/// Referencing this package is what makes an iOS build ask for the entitlement that gives the app
/// a synced store, which in turn requires the capability to be enabled on the App ID. That is why
/// it is a package of its own: an app that wants continuation to a nearby device and nothing else
/// should not have to arrange an entitlement to get it. Where the platform has no such store --
/// Android, desktop, the browser -- `isSupported()` is false and every call here is an inert
/// no-op, so the sensible shape is a synced value with a local default behind it.
/// #### Threading
///
/// Called on the event dispatch thread, like the rest of the toolkit. Codename One is single
/// threaded by design -- one thread on each side of a native boundary, marshalled at the boundary
/// rather than locked -- and this class follows that rule rather than making an exception to it.
///
/// It is worth stating because the simulation behind `isSupported() == true` on a desktop keeps
/// its key index as a second stored value: two threads writing different NEW keys at once would
/// each read that index, add their own key, and write it back, so one of them would vanish from
/// `keys()` while its value stayed readable by name. The platform stores have no such structure
/// and no such exposure. The answer is the toolkit's answer everywhere else -- call it from the
/// event thread, and use `com.codename1.ui.Display#callSerially(Runnable)` if you are on another
/// one -- not a lock inside a framework that does not have them.
public final class SyncedStore {
    private static final List<SyncedStoreListener> listeners = new ArrayList<SyncedStoreListener>();

    private SyncedStore() {
    }

    /// Whether this platform has a store that follows the user between devices.
    ///
    /// #### Returns
    ///
    /// true when the store is available
    public static boolean isSupported() {
        ContinuityBridge b = bridge();
        try {
            return b != null && b.isSyncedStoreSupported();
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    /// Writes a value, replacing any previous value for the key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key, must not be null or empty
    /// - `value`: the value, must not be null; use `remove(String)` to delete
    ///
    /// #### Returns
    ///
    /// true when the store holds the value afterwards; false when there is no store, or the
    /// platform would not take it -- a key count or a size past what it allows
    /// Not gated on isSupported(), and that is the THIRD layer this was wrong in.
    ///
    /// isSupported() asks whether this build has a store that follows the user between devices,
    /// which is the right question for an application deciding whether to offer the feature and
    /// the wrong gate for the calls themselves. On iOS the store is a LOCAL persistent one whose
    /// cloud propagation is asynchronous, so reads and writes work and reach other devices later.
    ///
    /// The gate was on all three of IOSNative.m, IOSContinuityBridge and here. Removing it from
    /// the first two changed nothing, because this one still made every call unreachable -- a fix
    /// verified at one layer and dead at the next. Each bridge answers for itself when there is
    /// no store: the Android one returns null and no-ops, the iOS one checks its own port flag,
    /// and the simulation reads local preferences.
    public static boolean put(String key, String value) {
        requireKey(key);
        if (value == null) {
            throw new IllegalArgumentException("A synced store value cannot be null. Use "
                    + "SyncedStore.remove(\"" + key + "\") to delete the key.");
        }
        ContinuityBridge b = bridge();
        if (b == null) {
            return false;
        }
        try {
            return b.syncedStorePut(key, value);
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    /// Reads a value.
    ///
    /// There is no overload without a default on purpose: the store is genuinely empty on a device
    /// that has not synced yet, so every read has to have an answer for that.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key, must not be null or empty
    /// - `def`: what to return when the key is absent or the store is unavailable
    ///
    /// #### Returns
    ///
    /// the value, or `def`
    public static String get(String key, String def) {
        requireKey(key);
        ContinuityBridge b = bridge();
        if (b == null) {
            return def;
        }
        try {
            String value = b.syncedStoreGet(key);
            return value == null ? def : value;
        } catch (Throwable t) {
            Log.e(t);
            return def;
        }
    }

    /// Deletes a key. Deleting an absent key does nothing.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key, must not be null or empty
    public static void remove(String key) {
        requireKey(key);
        ContinuityBridge b = bridge();
        if (b == null) {
            return;
        }
        try {
            b.syncedStoreRemove(key);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// Every key currently in the store, in no particular order.
    ///
    /// #### Returns
    ///
    /// the keys, never null and empty when the store is unavailable
    public static String[] keys() {
        ContinuityBridge b = bridge();
        if (b == null) {
            return new String[0];
        }
        try {
            String[] k = b.syncedStoreKeys();
            return k == null ? new String[0] : k;
        } catch (Throwable t) {
            Log.e(t);
            return new String[0];
        }
    }

    /// Registers a listener for changes made on the user's other devices.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public static void addChangeListener(SyncedStoreListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
        // The callback the port delivers change notifications through, and NOT Continuity.enable():
        // an app that only ever uses the synced store never touches Continuity itself, and would
        // otherwise register a listener nothing could ever reach -- but enabling would also make
        // every route change checkpoint, which on iOS advertises the app's navigation to the
        // devices around it. A key/value store is not consent to broadcast a route stack.
        Continuity.installSyncedStoreCallback();
        // And this resolves the platform store, which is the half that actually creates it. On
        // iOS the external-change observer is installed the first time the store is resolved, and
        // enable() does not resolve it -- so an application that only registers a listener and
        // waits to read values inside the callback was never told about a change made on another
        // device, until some unrelated read or write happened to bring the store up. Idempotent:
        // the port resolves it once and answers from that.
        isSupported();
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public static void removeChangeListener(SyncedStoreListener l) {
        listeners.remove(l);
    }

    /// Internal. Invoked by the continuity framework when a port reports that the store changed
    /// underneath the app. Application code registers a `SyncedStoreListener` instead.
    public static void notifyChanged() {
        // On the EDT: Continuity.Callback marshals the port's notification before it gets here.
        // Copied before iterating, because a listener that reacts to a change by unregistering
        // itself is ordinary and would otherwise mutate the list being walked.
        List<SyncedStoreListener> snapshot = new ArrayList<SyncedStoreListener>(listeners);
        // The element cast the compiler inserts sits in the loop header, outside the handler --
        // a failed cast does not throw on the iOS virtual machine, so a handler wrapped around
        // one could not run there anyway.
        for (SyncedStoreListener l : snapshot) {
            try {
                l.storeChanged();
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    private static void requireKey(String key) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("A synced store key cannot be null or empty.");
        }
    }

    private static ContinuityBridge bridge() {
        return Continuity.bridgeForSyncedStore();
    }

    /// Test seam: forgets every registered listener.
    static void reset() {
        listeners.clear();
    }
}
