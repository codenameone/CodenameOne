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
package com.codename1.nearby.companion;

import com.codename1.impl.async.EdtResult;
import com.codename1.impl.async.PendingMap;
import com.codename1.impl.nearby.NearbyRequests;
import com.codename1.impl.nearby.NearbyWire;
import com.codename1.nearby.NearbyAvailability;
import com.codename1.nearby.NearbyError;
import com.codename1.nearby.NearbyException;
import com.codename1.nearby.spi.NearbyBridge;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Companion-device association: the OS-managed relationship between this
/// app and one particular accessory.
///
/// Associating is not pairing. It is the app telling the operating system
/// "this is my device", through a chooser the OS draws and the user picks
/// from, and getting back privileges that an ordinary Bluetooth scan does
/// not carry:
///
/// - **The OS watches for the device instead of the app.**
///   [#startObservingPresence] asks the platform to wake the app when the
///   accessory comes into range, which replaces a scan the app would
///   otherwise run -- and pay for in battery -- forever.
/// - **Scanning stops needing location permission.** On Android, finding
///   your own associated device is not the same question as finding out
///   where the user is, and the platform treats it accordingly.
/// - **The user sees one honest prompt** naming one device, instead of a
///   blanket "this app wants to find nearby devices".
///
/// ```java
/// AssociationRequest req = new AssociationRequest.Builder()
///         .addFilter(DeviceFilter.bleService("180D"))
///         .build();
/// CompanionDevices.associate(req).onResult((device, err) -> {
///     if (err == null) {
///         Preferences.set("sensor", device.getId());
///         CompanionDevices.startObservingPresence(device.getId());
///     }
/// });
/// ```
///
/// #### Platform support
///
/// - **Android** -- `CompanionDeviceManager`, with presence observation.
/// - **iOS** -- AccessorySetupKit, on iOS 18 and later. The picker returns
///   an accessory the app may then talk to over
///   `com.codename1.bluetooth` without holding the blanket Bluetooth
///   authorization. Earlier iOS versions report [#isSupported()] false;
///   there the app scans with `com.codename1.bluetooth` as before.
/// - **Simulator, desktop and JavaScript** -- a simulated association store
///   reporting [NearbyAvailability#LOCAL_ONLY].
/// - **Every other port** -- unsupported, and every call fails fast.
public final class CompanionDevices {

    private static final PendingMap<CompanionDevice> PENDING_ASSOCIATE =
            new PendingMap<CompanionDevice>();
    private static final PendingMap<Boolean> PENDING_DISASSOCIATE =
            new PendingMap<Boolean>();
    private static final List<PresenceListener> LISTENERS =
            new ArrayList<PresenceListener>();

    private CompanionDevices() {
    }

    /// `true` when this port can associate companion devices.
    public static boolean isSupported() {
        NearbyBridge b = NearbyRequests.bridge();
        return b != null && b.isCompanionSupported();
    }

    /// How usable association is right now.
    ///
    /// #### Returns
    ///
    /// the current availability, never null
    public static NearbyAvailability getAvailability() {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isCompanionSupported()) {
            return NearbyAvailability.NOT_SUPPORTED;
        }
        NearbyAvailability[] all = NearbyAvailability.values();
        int o = b.getCompanionAvailability();
        return o >= 0 && o < all.length ? all[o]
                : NearbyAvailability.NOT_SUPPORTED;
    }

    /// Shows the system device chooser and associates whatever the user
    /// picks.
    ///
    /// This always involves the user -- there is no way to associate
    /// silently on either platform, by design.
    ///
    /// #### Parameters
    ///
    /// - `request`: what to offer the user
    ///
    /// #### Returns
    ///
    /// resolves with the associated device, or fails with
    /// [NearbyError#USER_CANCELED] when the user dismissed the chooser
    public static AsyncResource<CompanionDevice> associate(
            AssociationRequest request) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isCompanionSupported()) {
            EdtResult<CompanionDevice> out = new EdtResult<CompanionDevice>();
            out.error(new NearbyException(NearbyError.NOT_SUPPORTED,
                    "this platform does not support companion devices"));
            return out;
        }
        if (request == null) {
            request = new AssociationRequest.Builder().build();
        }
        List<DeviceFilter> filters = request.getFilters();
        String[] encoded = new String[filters.size()];
        for (int i = 0; i < encoded.length; i++) {
            encoded[i] = NearbyWire.encodeFilter(filters.get(i));
        }
        int id = NearbyRequests.nextId();
        EdtResult<CompanionDevice> out = PENDING_ASSOCIATE.open(id);
        b.associate(id, request.getProfile().ordinal(),
                request.isSingleDevice(), encoded);
        return out;
    }

    /// Every association this app currently holds.
    ///
    /// Associations survive restarts, so this is what an app calls on
    /// startup to find the accessory it was using last time rather than
    /// asking the user again.
    ///
    /// #### Returns
    ///
    /// the associations, never null and possibly empty
    public static List<CompanionDevice> getAssociations() {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isCompanionSupported()) {
            return Collections.emptyList();
        }
        String[] rows = b.getAssociations();
        if (rows == null || rows.length == 0) {
            return Collections.emptyList();
        }
        List<CompanionDevice> out =
                new ArrayList<CompanionDevice>(rows.length);
        for (int i = 0; i < rows.length; i++) {
            CompanionDevice d = NearbyWire.decodeCompanionDevice(rows[i]);
            if (d != null) {
                out.add(d);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /// Drops an association and the privileges that came with it.
    ///
    /// #### Parameters
    ///
    /// - `associationId`: the id from [CompanionDevice#getId()]
    ///
    /// #### Returns
    ///
    /// resolves `true` once the association is gone
    public static AsyncResource<Boolean> disassociate(String associationId) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isCompanionSupported()) {
            EdtResult<Boolean> out = new EdtResult<Boolean>();
            out.error(new NearbyException(NearbyError.NOT_SUPPORTED,
                    "this platform does not support companion devices"));
            return out;
        }
        int id = NearbyRequests.nextId();
        EdtResult<Boolean> out = PENDING_DISASSOCIATE.open(id);
        b.disassociate(id, associationId);
        return out;
    }

    /// Asks the platform to watch for the device and tell this app when it
    /// comes and goes, delivering to every registered [PresenceListener].
    ///
    /// #### Parameters
    ///
    /// - `associationId`: the id from [CompanionDevice#getId()]
    ///
    /// #### Returns
    ///
    /// `true` when the platform accepted the request. `false` where
    /// presence observation is unsupported -- the association itself is
    /// unaffected, so an app can carry on scanning for the device itself.
    public static boolean startObservingPresence(String associationId) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isCompanionSupported() || associationId == null) {
            return false;
        }
        return b.startObservingPresence(associationId);
    }

    /// Stops watching an association. Idempotent.
    ///
    /// #### Parameters
    ///
    /// - `associationId`: the id from [CompanionDevice#getId()]
    public static void stopObservingPresence(String associationId) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b != null && associationId != null) {
            b.stopObservingPresence(associationId);
        }
    }

    /// Registers a presence listener. Callbacks arrive on the EDT.
    ///
    /// Register from the app's `init()`: presence is exactly the event that
    /// can arrive during a cold start, because the platform launched the app
    /// to deliver it.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public static void addPresenceListener(PresenceListener l) {
        if (l == null) {
            return;
        }
        synchronized (LISTENERS) {
            LISTENERS.add(l);
        }
    }

    /// Removes a listener added by [#addPresenceListener].
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public static void removePresenceListener(PresenceListener l) {
        synchronized (LISTENERS) {
            LISTENERS.remove(l);
        }
    }


    /// Clears every in-flight request, so one test cannot see the requests of
    /// the test that ran before it. Reached through
    /// `com.codename1.impl.nearby.NearbyRequests#resetForTest`.
    ///
    /// In-flight requests are failed rather than dropped: a resource that
    /// never settles is worse than one that fails, and a test holding one
    /// would hang rather than report.
    ///
    /// @hidden not part of the public API; test-only.
    public static void resetForTest() {
        NearbyException reset = new NearbyException(NearbyError.UNKNOWN,
                "the nearby framework was reset");
        PENDING_ASSOCIATE.failAll(reset);
        PENDING_DISASSOCIATE.failAll(reset);
        synchronized (LISTENERS) {
            LISTENERS.clear();
        }
    }

    // ------------------------------------------------------------------
    // Port entry points
    // ------------------------------------------------------------------

    /// Answers [#associate].
    ///
    /// @hidden not part of the public API; called by ports.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id the request was made with
    /// - `encodedDevice`: the device, encoded by
    ///   `com.codename1.impl.nearby.NearbyWire`
    public static void deliverAssociated(int requestId, String encodedDevice) {
        EdtResult<CompanionDevice> r = PENDING_ASSOCIATE.take(requestId);
        if (r == null) {
            return;
        }
        CompanionDevice d = NearbyWire.decodeCompanionDevice(encodedDevice);
        if (d == null) {
            r.error(new NearbyException(NearbyError.UNKNOWN,
                    "the port reported an association with no id"));
        } else {
            r.complete(d);
        }
    }

    /// Answers [#disassociate].
    ///
    /// @hidden not part of the public API; called by ports.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id the request was made with
    public static void deliverDisassociated(int requestId) {
        EdtResult<Boolean> r = PENDING_DISASSOCIATE.take(requestId);
        if (r != null) {
            r.complete(Boolean.TRUE);
        }
    }

    /// Fails whichever companion request carries this id.
    ///
    /// @hidden not part of the public API; called by ports.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id the request was made with
    /// - `errorOrdinal`: the ordinal of a `com.codename1.nearby.NearbyError`
    ///   constant
    /// - `message`: a human-readable detail, may be null
    public static void deliverRequestFailed(int requestId, int errorOrdinal,
            String message) {
        NearbyException ex = NearbyWire.decodeError(errorOrdinal, message);
        EdtResult<CompanionDevice> a = PENDING_ASSOCIATE.take(requestId);
        if (a != null) {
            a.error(ex);
            return;
        }
        EdtResult<Boolean> d = PENDING_DISASSOCIATE.take(requestId);
        if (d != null) {
            d.error(ex);
        }
    }

    /// Reports that an associated device came into or went out of range.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `encodedDevice`: the device, encoded by
    ///   `com.codename1.impl.nearby.NearbyWire`
    /// - `present`: true when it appeared, false when it disappeared
    public static void deliverPresenceChanged(String encodedDevice,
            final boolean present) {
        final CompanionDevice d =
                NearbyWire.decodeCompanionDevice(encodedDevice);
        if (d == null) {
            return;
        }
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                PresenceListener[] ls;
                synchronized (LISTENERS) {
                    ls = LISTENERS.toArray(
                            new PresenceListener[LISTENERS.size()]);
                }
                for (int i = 0; i < ls.length; i++) {
                    if (present) {
                        ls[i].deviceAppeared(d);
                    } else {
                        ls[i].deviceDisappeared(d);
                    }
                }
            }
        });
    }
}
