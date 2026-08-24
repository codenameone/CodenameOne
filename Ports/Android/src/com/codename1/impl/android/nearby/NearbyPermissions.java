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
package com.codename1.impl.android.nearby;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.codename1.nearby.spi.NearbyBridge;

import java.util.ArrayList;
import java.util.List;

/// The permissions the nearby transport needs, in one place.
///
/// Two callers need the same answer and used to work it out separately:
/// `AndroidNearbyBackend` when it asks the user, and `AndroidNearbyTransport`
/// when it reports availability. Answering differently is worse than either
/// answer -- an app is told the transport is ready and then refused.
final class NearbyPermissions {

    private NearbyPermissions() {
    }

    /// The API level whose rules actually apply to this app.
    ///
    /// NOT `Build.VERSION.SDK_INT` on its own. Android's Bluetooth permission
    /// model switches on the app's TARGET, not on the device: an app
    /// targeting 30 running on Android 12 still uses `BLUETOOTH` and
    /// `ACCESS_FINE_LOCATION`, and the split permissions do not apply to it.
    /// Asking such an app for `BLUETOOTH_SCAN` requested something the
    /// platform was never going to route to it, and asking it for the wrong
    /// one meant the grant it did need was never requested at all.
    ///
    /// #### Parameters
    ///
    /// - `context`: any context
    ///
    /// #### Returns
    ///
    /// the lower of the device level and the app's target
    static int effectiveSdk(Context context) {
        int device = Build.VERSION.SDK_INT;
        int target = device;
        try {
            target = context.getApplicationInfo().targetSdkVersion;
        } catch (Throwable unreadable) {
            // A context with no application info is not a case worth failing
            // for; the device level alone is the pre-existing behaviour.
        }
        return target < device ? target : device;
    }

    /// The runtime permissions the requested transport operations need.
    ///
    /// #### Parameters
    ///
    /// - `context`: any context
    /// - `permissionBits`: the `NearbyBridge.PERMISSION_*` bits asked for
    ///
    /// #### Returns
    ///
    /// the permission strings, never null
    static List<String> transportPermissions(Context context,
            int permissionBits) {
        List<String> out = new ArrayList<String>();
        int sdk = effectiveSdk(context);
        if (sdk >= 31) {
            if ((permissionBits & NearbyBridge.PERMISSION_DISCOVERY) != 0) {
                out.add("android.permission.BLUETOOTH_SCAN");
            }
            if ((permissionBits & NearbyBridge.PERMISSION_ADVERTISE) != 0) {
                out.add("android.permission.BLUETOOTH_ADVERTISE");
            }
            if ((permissionBits & NearbyBridge.PERMISSION_CONNECT) != 0) {
                out.add("android.permission.BLUETOOTH_CONNECT");
            }
        }
        // The nearby-Wi-Fi and location grants belong to the operations that
        // SCAN or BROADCAST, so an app asking only to CONNECT to an endpoint
        // it has already discovered is not made to answer for them -- and no
        // longer fails because it declined something it never needed.
        //
        // It was suggested these belong to DISCOVERY alone. Advertising needs
        // them too: Nearby Connections advertises over BLE and brings up
        // Wi-Fi to carry the payload, which is the same radio use discovery
        // asks about, and an advertise that cannot use them does not start.
        // So the gate is discovery OR advertise, not discovery alone.
        boolean scansOrBroadcasts = (permissionBits
                & (NearbyBridge.PERMISSION_DISCOVERY
                        | NearbyBridge.PERMISSION_ADVERTISE)) != 0;
        if (!scansOrBroadcasts) {
            return out;
        }
        if (sdk >= 33) {
            out.add("android.permission.NEARBY_WIFI_DEVICES");
        } else {
            // Below 33 Nearby Connections genuinely refuses to start without
            // a location grant; it is not a scan-results technicality there.
            //
            // Both, and in this order. From Android 12 the two are granted
            // together -- the system shows one dialog offering precise or
            // approximate -- and asking for fine without coarse is refused
            // outright, so the grant the transport needs never arrived.
            out.add("android.permission.ACCESS_FINE_LOCATION");
            out.add("android.permission.ACCESS_COARSE_LOCATION");
        }
        return out;
    }

    /// True when every permission in the list is granted right now. Never
    /// prompts: this is the question an availability query asks.
    ///
    /// #### Parameters
    ///
    /// - `context`: any context
    /// - `permissions`: the permissions to test
    ///
    /// #### Returns
    ///
    /// true when all of them are granted
    static boolean allGranted(Context context, List<String> permissions) {
        if (Build.VERSION.SDK_INT < 23) {
            // Install-time grants; anything in the manifest is held.
            return true;
        }
        for (int i = 0; i < permissions.size(); i++) {
            if (context.checkSelfPermission(permissions.get(i))
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
