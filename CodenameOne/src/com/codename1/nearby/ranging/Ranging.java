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
package com.codename1.nearby.ranging;

import com.codename1.impl.async.EdtResult;
import com.codename1.impl.async.PendingMap;
import com.codename1.impl.nearby.NearbyRequests;
import com.codename1.nearby.NearbyAvailability;
import com.codename1.nearby.NearbyError;
import com.codename1.nearby.NearbyException;
import com.codename1.nearby.NearbyPermission;
import com.codename1.nearby.spi.NearbyBridge;
import com.codename1.util.AsyncResource;

/// Precision ranging: how far away another device is, and in which
/// direction.
///
/// This is ultra-wideband ranging -- Apple's Nearby Interaction on iOS and
/// Jetpack UWB on Android -- which measures distance by timing a radio
/// round trip rather than by guessing from signal strength. Where an RSSI
/// estimate off a Bluetooth advertisement is worth a few meters on a good
/// day, UWB is worth about ten centimeters, and on hardware with multiple
/// antennas it also reports which way the peer is.
///
/// #### The shape of a session
///
/// Both platforms need the two devices to exchange a token over some
/// channel they already share before any radio ranging can begin, so the
/// API is in two steps and there is no way to collapse them:
///
/// ```java
/// if (!Ranging.isSupported()) {
///     return;                       // no UWB radio on this device
/// }
/// Ranging.prepareSession(RangingRole.CONTROLLER).onResult((session, err) -> {
///     if (err != null) {
///         return;
///     }
///     // 1. publish our token however the two apps already talk --
///     //    a GATT characteristic from com.codename1.bluetooth is typical
///     characteristic.writeValue(session.getLocalToken().toByteArray());
///
///     // 2. when theirs arrives, start ranging
///     session.addRangingListener(new RangingAdapter() {
///         public void updated(RangingUpdate u) {
///             if (u.hasDistance()) {
///                 label.setText(Math.round(u.getDistance(RangingUnit.CENTIMETERS)) + " cm");
///             }
///         }
///     });
///     session.start(RangingToken.fromByteArray(theirToken));
/// });
/// ```
///
/// A session ranges exactly one peer. That is a hard limit of Apple's
/// `NINearbyPeerConfiguration` rather than a simplification, so an app that
/// tracks several peers prepares several sessions -- which is also what the
/// Android port does under the hood.
///
/// #### Threading
///
/// Every callback here -- `AsyncResource` results and every
/// [RangingListener] method -- is delivered on the EDT.
///
/// #### Platform support
///
/// - **iOS** -- Nearby Interaction on devices with a U1 or newer chip
///   (iPhone 11 and later). Peer and accessory ranging, direction where the
///   hardware provides it. Not available on tvOS, watchOS or Mac Catalyst.
/// - **Android** -- Jetpack UWB on devices that report the UWB hardware
///   feature. Peer ranging natively; an accessory is ranged by building a
///   token with [RangingToken#forUwbAddress].
/// - **Simulator, desktop and JavaScript** -- a simulated implementation
///   with peers that really move, so ranging UI is developable without
///   hardware. Reports [NearbyAvailability#LOCAL_ONLY].
/// - **Every other port** -- [#isSupported()] is `false` and every call
///   fails with [NearbyError#NOT_SUPPORTED].
public final class Ranging {

    private static final PendingMap<RangingSession> PENDING_SESSIONS =
            new PendingMap<RangingSession>();
    private static final PendingMap<byte[]> PENDING_ACCESSORY =
            new PendingMap<byte[]>();
    private static final java.util.Map<Integer, RangingSession> STARTING =
            new java.util.HashMap<Integer, RangingSession>();

    private Ranging() {
    }

    /// `true` when this port and this device can range at all.
    ///
    /// This answers for the hardware, not for whether a peer is nearby. It
    /// is the query to hide a feature on; use [#getAvailability()] to tell
    /// a user why a supported feature is not working right now.
    public static boolean isSupported() {
        NearbyBridge b = NearbyRequests.bridge();
        return b != null && b.isRangingSupported();
    }

    /// How usable ranging is at this moment, which is a different question
    /// from [#isSupported()]: a phone with a U1 chip whose owner denied the
    /// permission is supported and unavailable.
    ///
    /// #### Returns
    ///
    /// the current availability, never null
    public static NearbyAvailability getAvailability() {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isRangingSupported()) {
            return NearbyAvailability.NOT_SUPPORTED;
        }
        return fromOrdinal(b.getRangingAvailability());
    }

    /// What this device can actually measure. Never null: where ranging is
    /// absent this is [RangingCapabilities#UNSUPPORTED], whose every query
    /// is `false`.
    ///
    /// #### Returns
    ///
    /// the capabilities of the local device
    public static RangingCapabilities getCapabilities() {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isRangingSupported()) {
            return RangingCapabilities.UNSUPPORTED;
        }
        int bits = b.getRangingCapabilities();
        return new RangingCapabilities(
                (bits & NearbyBridge.CAPABILITY_DISTANCE) != 0,
                (bits & NearbyBridge.CAPABILITY_DIRECTION) != 0,
                (bits & NearbyBridge.CAPABILITY_ELEVATION) != 0,
                (bits & NearbyBridge.CAPABILITY_CAMERA_ASSISTANCE) != 0,
                (bits & NearbyBridge.CAPABILITY_ACCESSORY) != 0,
                (bits & NearbyBridge.CAPABILITY_BACKGROUND) != 0);
    }

    /// Asks for the runtime permissions ranging needs.
    ///
    /// Safe to call on every platform: a port with nothing to ask for
    /// resolves `true` without showing anything.
    ///
    /// #### Parameters
    ///
    /// - `permissions`: what the app intends to do
    ///
    /// #### Returns
    ///
    /// resolves `true` when every requested permission is granted
    public static AsyncResource<Boolean> requestPermissions(
            NearbyPermission... permissions) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null) {
            return failedBoolean();
        }
        int bits = 0;
        if (permissions != null) {
            for (NearbyPermission permission : permissions) {
                bits |= permissionBit(permission);
            }
        }
        int id = NearbyRequests.nextId();
        EdtResult<Boolean> out = NearbyRequests.openPermissionRequest(id);
        b.requestPermissions(id, bits);
        return out;
    }

    /// Allocates a ranging session and, with it, the local token to publish
    /// to the peer. The session is not ranging yet -- call
    /// [RangingSession#start] once the peer's token arrives.
    ///
    /// #### Parameters
    ///
    /// - `role`: which end of the session this device is. Ignored on
    ///   platforms that negotiate roles themselves, but pick one anyway:
    ///   Android needs exactly one controller.
    ///
    /// #### Returns
    ///
    /// resolves with the prepared session, or fails with a
    /// [NearbyException]
    public static AsyncResource<RangingSession> prepareSession(
            RangingRole role) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isRangingSupported()) {
            EdtResult<RangingSession> out = new EdtResult<RangingSession>();
            out.error(new NearbyException(NearbyError.NOT_SUPPORTED,
                    "this platform does not support precision ranging"));
            return out;
        }
        int id = NearbyRequests.nextId();
        int handle = RangingSession.nextHandle();
        EdtResult<RangingSession> out = PENDING_SESSIONS.open(id);
        b.prepareRangingSession(id, handle, role != RangingRole.CONTROLEE);
        return out;
    }

    // ------------------------------------------------------------------
    // Port entry points
    // ------------------------------------------------------------------

    /// Answers a permission request from ANY entry point in this family --
    /// ranging or transport. Ports report every permission outcome here, and
    /// the pending map it reads is shared for that reason.
    ///
    /// @hidden not part of the public API; called by ports.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id the request was made with
    /// - `granted`: whether every requested permission was granted
    public static void deliverPermissionResult(int requestId,
            boolean granted) {
        EdtResult<Boolean> r = NearbyRequests.takePermissionRequest(requestId);
        if (r != null) {
            r.complete(Boolean.valueOf(granted));
        }
    }

    /// Answers [#prepareSession].
    ///
    /// @hidden not part of the public API; called by ports.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id the request was made with
    /// - `sessionHandle`: the handle passed to
    ///   `NearbyBridge#prepareRangingSession`
    /// - `role`: true when this session is the controller
    /// - `tokenPlatform`: one of the `RangingToken.PLATFORM_` constants
    /// - `tokenPayload`: the native token bytes
    public static void deliverSessionPrepared(int requestId,
            int sessionHandle, boolean role, int tokenPlatform,
            byte[] tokenPayload) {
        EdtResult<RangingSession> r = PENDING_SESSIONS.take(requestId);
        // Cancelled counts as nobody waiting. take() hands back a cancelled
        // resource just the same, and completing one is a no-op -- so building
        // the session here registered it in two places and handed the caller
        // nothing, leaving a radio session alive that no one could stop.
        if (r == null || r.isCancelled()) {
            NearbyBridge b = NearbyRequests.bridge();
            if (b != null) {
                b.stopRangingSession(sessionHandle);
            }
            return;
        }
        RangingSession session = RangingSession.create(sessionHandle,
                role ? RangingRole.CONTROLLER : RangingRole.CONTROLEE,
                RangingToken.forPayload(tokenPlatform, tokenPayload));
        r.complete(session);
    }

    /// Answers [RangingSession#start].
    ///
    /// @hidden not part of the public API; called by ports.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id the request was made with
    /// - `sessionHandle`: the session that started
    public static void deliverSessionStarted(int requestId,
            int sessionHandle) {
        untrackStarting(requestId);
        EdtResult<RangingSession> r = PENDING_SESSIONS.take(requestId);
        if (r != null) {
            RangingSession s = RangingSession.lookup(sessionHandle);
            if (s == null) {
                r.error(new NearbyException(NearbyError.SESSION_INVALIDATED,
                        "the session was closed before it started"));
            } else {
                s.markRunning();
                r.complete(s);
            }
        }
    }

    /// Answers [RangingSession#startAccessory].
    ///
    /// @hidden not part of the public API; called by ports.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id the request was made with
    /// - `sessionHandle`: the session that started
    /// - `shareableConfiguration`: the bytes to send back to the accessory,
    ///   empty where the platform needs no handshake
    public static void deliverAccessoryConfiguration(int requestId,
            int sessionHandle, byte[] shareableConfiguration) {
        untrackStarting(requestId);
        EdtResult<byte[]> r = PENDING_ACCESSORY.take(requestId);
        if (r != null) {
            RangingSession s = RangingSession.lookup(sessionHandle);
            if (s != null) {
                s.markRunning();
            }
            r.complete(shareableConfiguration == null
                    ? new byte[0] : shareableConfiguration);
        }
    }

    /// Fails whichever ranging request carries this id.
    ///
    /// The id is looked up in each of the pending maps in turn; because ids
    /// come from one counter it can be in at most one of them.
    ///
    /// @hidden not part of the public API; called by ports.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id the request was made with
    /// - `errorOrdinal`: the ordinal of a
    ///   `com.codename1.nearby.NearbyError` constant
    /// - `message`: a human-readable detail, may be null
    public static void deliverRequestFailed(int requestId, int errorOrdinal,
            String message) {
        NearbyException ex = toException(errorOrdinal, message);
        EdtResult<RangingSession> s = PENDING_SESSIONS.take(requestId);
        if (s != null) {
            // A start that failed leaves the session prepared but idle, and
            // it has to be told so: the flag that makes a concurrent start
            // answer BUSY is set before the bridge call and cleared only on
            // success, so without this the session answers BUSY to every
            // retry forever.
            releaseStarting(requestId);
            s.error(ex);
            return;
        }
        EdtResult<byte[]> a = PENDING_ACCESSORY.take(requestId);
        if (a != null) {
            releaseStarting(requestId);
            a.error(ex);
            return;
        }
        EdtResult<Boolean> p = NearbyRequests.takePermissionRequest(requestId);
        if (p != null) {
            p.error(ex);
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
        NearbyRequests.failPermissionRequests(reset);
        PENDING_SESSIONS.failAll(reset);
        PENDING_ACCESSORY.failAll(reset);
        synchronized (STARTING) {
            STARTING.clear();
        }
    }

    // ------------------------------------------------------------------
    // Internals shared with RangingSession
    // ------------------------------------------------------------------

    /// Clears the in-progress flag of whichever session issued this request.
    ///
    /// Tracked by request id rather than by handle because the failure path
    /// only ever learns the id: `NearbyBridge` answers a failed start through
    /// `deliverRequestFailed(requestId, ...)`, which names no session.
    private static void releaseStarting(int requestId) {
        RangingSession s;
        synchronized (STARTING) {
            s = STARTING.remove(Integer.valueOf(requestId));
        }
        if (s != null) {
            s.markStartFailed();
        }
    }

    /// The session behind each in-flight start, so a failure can find it.
    ///
    /// @hidden not part of the public API.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id of the start being issued
    /// - `session`: the session issuing it
    static void trackStarting(int requestId, RangingSession session) {
        synchronized (STARTING) {
            STARTING.put(Integer.valueOf(requestId), session);
        }
    }

    /// Forgets a start that has settled.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id of the start that finished
    static void untrackStarting(int requestId) {
        synchronized (STARTING) {
            STARTING.remove(Integer.valueOf(requestId));
        }
    }

    static PendingMap<RangingSession> pendingSessions() {
        return PENDING_SESSIONS;
    }

    static PendingMap<byte[]> pendingAccessory() {
        return PENDING_ACCESSORY;
    }

    static NearbyException toException(int errorOrdinal, String message) {
        NearbyError[] all = NearbyError.values();
        NearbyError e = errorOrdinal >= 0 && errorOrdinal < all.length
                ? all[errorOrdinal] : NearbyError.UNKNOWN;
        return new NearbyException(e,
                message == null ? e.name() : message);
    }

    private static NearbyAvailability fromOrdinal(int ordinal) {
        NearbyAvailability[] all = NearbyAvailability.values();
        if (ordinal < 0 || ordinal >= all.length) {
            return NearbyAvailability.NOT_SUPPORTED;
        }
        return all[ordinal];
    }

    private static int permissionBit(NearbyPermission p) {
        if (p == NearbyPermission.RANGING) {
            return NearbyBridge.PERMISSION_RANGING;
        }
        if (p == NearbyPermission.DISCOVERY) {
            return NearbyBridge.PERMISSION_DISCOVERY;
        }
        if (p == NearbyPermission.ADVERTISE) {
            return NearbyBridge.PERMISSION_ADVERTISE;
        }
        if (p == NearbyPermission.CONNECT) {
            return NearbyBridge.PERMISSION_CONNECT;
        }
        return 0;
    }

    private static AsyncResource<Boolean> failedBoolean() {
        EdtResult<Boolean> out = new EdtResult<Boolean>();
        out.error(new NearbyException(NearbyError.NOT_SUPPORTED,
                "this platform does not support precision ranging"));
        return out;
    }
}
