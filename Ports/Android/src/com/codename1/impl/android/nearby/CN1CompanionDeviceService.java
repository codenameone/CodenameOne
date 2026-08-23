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

import android.annotation.SuppressLint;
import android.companion.AssociationInfo;
import android.companion.CompanionDeviceService;
import android.os.Build;

import com.codename1.nearby.companion.CompanionDevices;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/// The service the platform wakes when an associated device comes into or
/// goes out of range.
///
/// This is what makes companion association worth using: the OS runs the
/// watching, and it may start this process to deliver the event, so an app
/// that registered a `PresenceListener` in `init()` hears about a device that
/// appeared while the app was not running.
///
/// The platform may start the process for THIS service alone, with no activity
/// and therefore no initialized Codename One and no registered listener yet.
/// The event is not dispatched and dropped in that case: `CompanionDevices`
/// parks it and replays it to the first listener that registers, which in a
/// cold start is the one the app adds from `init()`.
///
/// The builder writes the `<service>` element that binds this, guarded by
/// `android.permission.BIND_COMPANION_DEVICE_SERVICE` and the
/// `CompanionDeviceService` intent filter, only for an app that observes
/// presence.
@SuppressLint("NewApi")
public class CN1CompanionDeviceService extends CompanionDeviceService {

    private static final Set<String> OBSERVED =
            Collections.synchronizedSet(new HashSet<String>());

    /// Records that the app asked to watch an association, so an event for
    /// one it stopped watching is dropped rather than delivered.
    ///
    /// #### Parameters
    ///
    /// - `associationId`: the association being watched
    public static void register(String associationId) {
        if (associationId != null) {
            OBSERVED.add(associationId);
        }
    }

    /// Forgets an association.
    ///
    /// #### Parameters
    ///
    /// - `associationId`: the association no longer watched
    public static void unregister(String associationId) {
        if (associationId != null) {
            OBSERVED.remove(associationId);
        }
    }

    @Override
    public void onDeviceAppeared(AssociationInfo associationInfo) {
        deliver(associationInfo, true);
    }

    @Override
    public void onDeviceDisappeared(AssociationInfo associationInfo) {
        deliver(associationInfo, false);
    }

    /// The API 31 and 32 form of the same event.
    ///
    /// The AssociationInfo overloads above arrived in API 33, and
    /// startObservingPresence accepts 31 and later -- so on Android 12 and 12L
    /// the platform called these and the two above were never invoked, losing
    /// every appearance and disappearance while still reporting the watch as
    /// accepted. Deprecated upstream, and overridden anyway, because those two
    /// releases have no other delivery path.
    @Override
    public void onDeviceAppeared(String address) {
        deliverByAddress(address, true);
    }

    @Override
    public void onDeviceDisappeared(String address) {
        deliverByAddress(address, false);
    }

    /// Delivers an event that names only a MAC address.
    ///
    /// The address IS the association id below API 33 -- that is what
    /// AndroidNearbyBackend encodes there, having no AssociationInfo to take
    /// an id from -- so no lookup is needed to match the two up.
    private void deliverByAddress(String address, boolean present) {
        if (address == null) {
            return;
        }
        if (!OBSERVED.isEmpty() && !OBSERVED.contains(address)) {
            return;
        }
        String encoded = sanitize(address) + '\t' + sanitize(address) + '\t'
                + sanitize(address) + "\t0\t" + (present ? '1' : '0');
        CompanionDevices.deliverPresenceChanged(encoded, present);
    }

    private void deliver(AssociationInfo info, boolean present) {
        if (info == null || Build.VERSION.SDK_INT < 31) {
            return;
        }
        android.net.MacAddress address = info.getDeviceMacAddress();
        String mac = address == null ? null : address.toString();
        String id = mac != null ? mac : Integer.toString(info.getId());
        if (!OBSERVED.isEmpty() && !OBSERVED.contains(id)) {
            // The platform keeps watching until told otherwise, and it
            // outlives the process. An event for an association the app has
            // since stopped watching is not the app's business.
            return;
        }
        CharSequence name = info.getDisplayName();
        String encoded = sanitize(id) + '\t'
                + sanitize(name == null ? "" : name.toString()) + '\t'
                + sanitize(mac == null ? "" : mac) + "\t0\t"
                + (present ? '1' : '0');
        CompanionDevices.deliverPresenceChanged(encoded, present);
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
