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
package com.codename1.impl.continuity;

import com.codename1.continuity.spi.ContinuityBridge;
import com.codename1.continuity.spi.ContinuityCallback;
import com.codename1.io.Log;
import com.codename1.io.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A simulated continuity platform, used by the simulator, the desktop builds and the unit tests.
///
/// A simulation rather than nothing, for the reason the call and nearby bridges carry one: almost
/// everything an app does with continuity -- deciding what belongs in the payload, prompting before
/// a jump, rebuilding a screen from a route -- has nothing to do with the operating system that
/// carries the state, and a port that reported nothing would make all of it testable only on a
/// pair of phones.
///
/// It keeps the last published activity in memory so the Simulate menu can show what the app is
/// offering, and it can hand that activity straight back through `simulateArrival()` -- which is
/// what "continue this on another device" is, minus the second device. The synced store is real
/// within one machine: it is backed by `com.codename1.io.Preferences`, so it survives a simulator
/// restart the way the platform store survives a device one.
public class LocalContinuityBridge implements ContinuityBridge {
    /// Prefix for the simulated synced store's keys inside `Preferences`.
    private static final String PREFIX = "CN1$SyncedStore$";

    /// The list of keys, kept beside them because `Preferences` cannot be enumerated.
    private static final String INDEX = "CN1$SyncedStoreKeys";

    private ContinuityCallback callback;
    private String publishedType;
    private String publishedTitle;
    private Map<String, Object> publishedInfo;

    @Override
    public void setCallback(ContinuityCallback c) {
        callback = c;
    }

    @Override
    public boolean isContinuationSupported() {
        return true;
    }

    @Override
    public void publishContinuation(String activityType, String title,
            Map<String, Object> userInfo) {
        publishedType = activityType;
        publishedTitle = title;
        publishedInfo = userInfo == null ? null : new HashMap<String, Object>(userInfo);
    }

    @Override
    public void clearContinuation() {
        publishedType = null;
        publishedTitle = null;
        publishedInfo = null;
    }

    /// The activity type currently advertised, or null when nothing is.
    ///
    /// #### Returns
    ///
    /// the type
    public String getPublishedType() {
        return publishedType;
    }

    /// The label currently advertised, or null.
    ///
    /// #### Returns
    ///
    /// the label
    public String getPublishedTitle() {
        return publishedTitle;
    }

    /// The payload currently advertised, or null when nothing is.
    ///
    /// #### Returns
    ///
    /// a copy of the payload
    public Map<String, Object> getPublishedInfo() {
        return publishedInfo == null ? null : new HashMap<String, Object>(publishedInfo);
    }

    /// Delivers the currently advertised activity back to the app as though it had arrived from
    /// another device, which is what the Simulate menu's "continue on this device" does.
    ///
    /// The device id inside the payload is rewritten first. Without that the framework would
    /// recognize the state as this device's own echo and correctly ignore it, and the menu item
    /// would appear to do nothing.
    ///
    /// #### Returns
    ///
    /// true when there was an activity to deliver and the app claimed it
    public boolean simulateArrival() {
        if (publishedType == null || publishedInfo == null) {
            return false;
        }
        Map<String, Object> copy = new HashMap<String, Object>(publishedInfo);
        copy.put("device", "simulated-device");
        return simulateArrival(publishedType, copy);
    }

    /// Delivers an arbitrary activity, for tests that build their own.
    ///
    /// #### Parameters
    ///
    /// - `activityType`: the type it arrives under
    /// - `userInfo`: the payload
    ///
    /// #### Returns
    ///
    /// true when the app claimed it
    public boolean simulateArrival(String activityType, Map<String, Object> userInfo) {
        ContinuityCallback c = callback;
        if (c == null) {
            return false;
        }
        try {
            return c.continuationReceived(activityType, userInfo);
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Synced store
    // ------------------------------------------------------------------

    @Override
    public boolean isSyncedStoreSupported() {
        return true;
    }

    @Override
    public boolean syncedStorePut(String key, String value) {
        Preferences.set(PREFIX + key, value);
        List<String> keys = indexKeys();
        if (!keys.contains(key)) {
            keys.add(key);
            writeIndex(keys);
        }
        // Read back rather than assume, so the simulation answers the same question the device
        // does: is the value there now?
        return value.equals(Preferences.get(PREFIX + key, null));
    }

    @Override
    public String syncedStoreGet(String key) {
        return Preferences.get(PREFIX + key, null);
    }

    @Override
    public void syncedStoreRemove(String key) {
        Preferences.delete(PREFIX + key);
        List<String> keys = indexKeys();
        if (keys.remove(key)) {
            writeIndex(keys);
        }
    }

    @Override
    public String[] syncedStoreKeys() {
        List<String> keys = indexKeys();
        return keys.toArray(new String[keys.size()]);
    }

    /// Reports a change made "on another device", which the Simulate menu uses to exercise an
    /// app's `SyncedStoreListener` without a second machine.
    public void simulateStoreChange() {
        ContinuityCallback c = callback;
        if (c == null) {
            return;
        }
        try {
            c.syncedStoreChanged();
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private List<String> indexKeys() {
        List<String> keys = new ArrayList<String>();
        String raw = Preferences.get(INDEX, "");
        if (raw == null || raw.length() == 0) {
            return keys;
        }
        // Newline separated because a synced store key is an application-chosen string and the
        // separators one might reach for first -- comma, semicolon, space -- are all plausible
        // inside one. A newline is not, and put() is the only writer.
        int start = 0;
        while (start <= raw.length()) {
            int end = raw.indexOf('\n', start);
            if (end < 0) {
                end = raw.length();
            }
            String key = raw.substring(start, end);
            if (key.length() > 0 && !keys.contains(key)) {
                keys.add(key);
            }
            start = end + 1;
        }
        return keys;
    }

    private void writeIndex(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(key);
        }
        Preferences.set(INDEX, sb.toString());
    }
}
