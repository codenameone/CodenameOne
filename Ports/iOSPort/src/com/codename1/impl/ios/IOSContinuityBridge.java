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

import com.codename1.continuity.spi.ContinuityBridge;
import com.codename1.continuity.spi.ContinuityCallback;
import com.codename1.io.JSONParser;
import com.codename1.io.JSONWriter;
import com.codename1.io.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Apple's half of the continuity framework: `NSUserActivity` for handing work to a device the
/// user is holding, and `NSUbiquitousKeyValueStore` for the handful of values that should follow
/// them everywhere.
///
/// #### The two halves are independent
///
/// Advertising an activity costs nothing but a declared activity type in the app's `Info.plist`.
/// The synced store costs an entitlement, which has to be granted on the App ID before the app
/// will sign at all. `isSyncedStoreSupported()` therefore asks the native side rather than
/// returning a constant: a build that did not earn the entitlement has no store, and answering
/// "yes" would have the app writing values that silently go nowhere.
///
/// #### Everything crosses as JSON
///
/// Matching the intent natives beside these. The payload has to become an `NSDictionary` the
/// system will accept in an activity's `userInfo`, and doing that conversion once, in C, against
/// a parsed JSON document is simpler than a per-type native call and is the same shape the rest of
/// this port already uses.
class IOSContinuityBridge implements ContinuityBridge {
    private final IOSNative nativeInterface;
    private final boolean supported;

    IOSContinuityBridge(IOSNative n) {
        nativeInterface = n;
        boolean s;
        try {
            s = n.continuitySupported();
        } catch (Throwable t) {
            Log.e(t);
            s = false;
        }
        supported = s;
    }

    @Override
    public void setCallback(ContinuityCallback callback) {
        IOSContinuityCallbacks.setCallback(callback);
    }

    @Override
    public boolean isContinuationSupported() {
        return supported;
    }

    @Override
    public void publishContinuation(String activityType, String title,
            Map<String, Object> userInfo) {
        if (!supported) {
            return;
        }
        try {
            nativeInterface.continuityPublish(activityType, title,
                    userInfo == null ? null : JSONWriter.toJson(userInfo));
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    @Override
    public void clearContinuation() {
        if (!supported) {
            return;
        }
        try {
            nativeInterface.continuityClear();
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    @Override
    public boolean isSyncedStoreSupported() {
        if (!supported) {
            return false;
        }
        try {
            return nativeInterface.continuitySyncedStoreSupported();
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    @Override
    public void syncedStorePut(String key, String value) {
        if (!isSyncedStoreSupported()) {
            return;
        }
        try {
            nativeInterface.continuitySyncedStorePut(key, value);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    @Override
    public String syncedStoreGet(String key) {
        if (!isSyncedStoreSupported()) {
            return null;
        }
        try {
            return nativeInterface.continuitySyncedStoreGet(key);
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    @Override
    public void syncedStoreRemove(String key) {
        if (!isSyncedStoreSupported()) {
            return;
        }
        try {
            nativeInterface.continuitySyncedStoreRemove(key);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    @Override
    public String[] syncedStoreKeys() {
        if (!isSyncedStoreSupported()) {
            return new String[0];
        }
        // The native call and the parse are what can fail, so they are what the handler covers.
        // Everything below it is deliberately outside: the compiler inserts checked casts for the
        // generic element type and for toArray's component type, and a failed cast does not throw
        // on this virtual machine -- so a handler wrapped around one is a handler that cannot run
        // here. See the ClassCastException note in CLAUDE.md.
        Map<String, Object> parsed;
        try {
            String json = nativeInterface.continuitySyncedStoreKeys();
            if (json == null || json.length() == 0) {
                return new String[0];
            }
            parsed = JSONParser.parseJSON(json);
        } catch (Throwable t) {
            Log.e(t);
            return new String[0];
        }
        Object keys = parsed == null ? null : parsed.get("keys");
        if (!(keys instanceof List)) {
            return new String[0];
        }
        List<?> read = (List<?>) keys;
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < read.size(); i++) {
            Object key = read.get(i);
            if (key instanceof String) {
                out.add((String) key);
            }
        }
        return out.toArray(new String[out.size()]);
    }
}
