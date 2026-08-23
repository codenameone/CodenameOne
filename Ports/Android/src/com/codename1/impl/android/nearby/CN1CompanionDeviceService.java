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

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.nearby.companion.CompanionDevices;
import com.codename1.ui.Display;

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
/// and therefore no registered listener yet. The event is not dispatched and
/// dropped in that case: `CompanionDevices` parks it and replays it to the
/// first listener that registers, which in a cold start is the one the app
/// adds from `init()`.
///
/// #### What this does NOT do, and why
///
/// It does not run the application's `init()` headlessly, so an app is not
/// executing code the moment a watch walks into range -- it hears about it
/// when it next initializes, replayed in order.
///
/// It was suggested this service should bootstrap the whole lifecycle. That
/// is a bigger promise than Codename One makes anywhere else on Android, and
/// deliberately so: an app's `init()` is allowed to touch a `Form`, and a
/// service has no UI thread to touch one on. The framework already had to
/// decide this once, for App Intents, and decided the same way -- see the
/// note on `AndroidImplementation.deliverPendingIntentRequests`, where a
/// handler that is not headless can only ask for the app to be brought
/// forward. Inventing a headless-lifecycle contract for presence alone would
/// make this one feature run app code in a state nothing else does.
///
/// What it does do is start the Codename One context, so the event is parked
/// in an initialized runtime with a real event thread rather than dispatched
/// inline on a binder thread. The public documentation says plainly that the
/// listener hears about the sighting when the app initializes.
///
/// The builder writes the `<service>` element that binds this, guarded by
/// `android.permission.BIND_COMPANION_DEVICE_SERVICE` and the
/// `CompanionDeviceService` intent filter, only for an app that observes
/// presence.
@SuppressLint("NewApi")
public class CN1CompanionDeviceService extends CompanionDeviceService {

    /// Whether this service is the one that started the Codename One context.
    private boolean startedContext;

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
    public void onCreate() {
        super.onCreate();
        // Started so the parked event sits in an initialized runtime: without
        // it Display.isInitialized() is false and the delivery runs inline on
        // whatever binder thread the platform used.
        if (!Display.isInitialized()) {
            startedContext = true;
            try {
                AndroidImplementation.startContext(this);
            } catch (Throwable notStartable) {
                startedContext = false;
            }
        }
    }

    @Override
    public void onDestroy() {
        if (startedContext) {
            startedContext = false;
            try {
                AndroidImplementation.stopContext(this);
            } catch (Throwable alreadyGone) {
                // Nothing to do: the context is going away either way.
            }
        }
        super.onDestroy();
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
        // The profile the association was actually made under, not a
        // hardcoded GENERIC -- the same record AndroidNearbyBackend builds,
        // and it has to agree with it or one presence event would contradict
        // the association the app already holds.
        String encoded = sanitize(id) + '\t'
                + sanitize(name == null ? "" : name.toString()) + '\t'
                + sanitize(mac == null ? "" : mac) + '\t'
                + AndroidNearbyBackend.profileOrdinalOf(info) + '\t'
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
