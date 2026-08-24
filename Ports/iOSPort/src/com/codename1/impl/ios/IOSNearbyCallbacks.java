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

import com.codename1.nearby.companion.CompanionDevices;
import com.codename1.nearby.ranging.Ranging;
import com.codename1.nearby.ranging.RangingSession;
import com.codename1.nearby.ranging.RangingToken;
import com.codename1.nearby.transport.NearbyTransport;
import com.codename1.util.StringUtil;

import java.util.List;

/// Static callback surface invoked from `CN1Nearby` when Nearby Interaction,
/// MultipeerConnectivity or AccessorySetupKit answer.
///
/// Mirrors [IOSHomeCallbacks]: the static initializer calls each entry point
/// once, guarded so it has no effect, purely to keep the ParparVM dead-code
/// eliminator from stripping targets that no Java code calls. Without that
/// guard the optimizer replaces them with empty stubs and every operation
/// hangs waiting for an answer that was compiled away -- a failure with
/// nothing in the log to explain it.
///
/// Everything here forwards straight to the public facades, which own EDT
/// dispatch. That matters here specifically: `NISessionDelegate`,
/// `MCSessionDelegate` and the AccessorySetupKit event stream all call back on
/// their own queues, and under ParparVM none of those is the Codename One EDT.
final class IOSNearbyCallbacks {

    private static IOSNearbyBridge bridge;
    private static boolean dceGuard;

    static {
        // Keep the native callback targets reachable for the iOS VM
        // optimizer.
        dceGuard = true;
        permissionResult(0, false);
        sessionPrepared(0, 0, false, null);
        sessionStarted(0, 0);
        accessoryConfiguration(0, 0, null);
        rangingFailed(0, 0, null);
        rangingUpdate(0, false, 0, false, 0, false, 0, false, 0, 0, 0);
        peerRemoved(0, 0);
        sessionSuspended(0);
        sessionResumed(0);
        sessionInvalidated(0, 0, null);
        associated(0, null);
        disassociated(0);
        companionFailed(0, 0, null);
        transportOk(0);
        transportFailed(0, 0, null);
        endpointFound(null, false);
        connectionRequested(null, null);
        connectionResult(null, false, 0, null);
        disconnected(null);
        payloadReceived(null, 0, 0, null, null);
        payloadProgress(null, 0, 0, 0, 0);
        dceGuard = false;
    }

    private IOSNearbyCallbacks() {
    }

    /// Returns the singleton nearby bridge, creating it on first use.
    ///
    /// #### Parameters
    ///
    /// - `nativeInstance`: the port's native surface
    ///
    /// #### Returns
    ///
    /// the bridge, never `null`
    static synchronized IOSNearbyBridge getBridge(IOSNative nativeInstance) {
        if (bridge == null) {
            bridge = new IOSNearbyBridge(nativeInstance);
        }
        return bridge;
    }

    /// Reached from the bridge's constructor so this class is initialized --
    /// and its dead-code guard therefore runs -- before any native code can
    /// call back into it.
    static void keepAlive() {
        // The static initializer is the work; this exists to trigger it from
        // a caller the optimizer can see.
    }

    // ---- Callbacks invoked from native code (do not rename) ---------------

    /// Called from native when a permission request closes.
    static void permissionResult(int requestId, boolean granted) {
        if (dceGuard) {
            return;
        }
        Ranging.deliverPermissionResult(requestId, granted);
    }

    /// Called from native once an NISession exists and has a discovery token.
    static void sessionPrepared(int requestId, int sessionHandle,
            boolean controller, byte[] tokenPayload) {
        if (dceGuard) {
            return;
        }
        Ranging.deliverSessionPrepared(requestId, sessionHandle, controller,
                RangingToken.PLATFORM_APPLE_NI, tokenPayload);
    }

    /// Called from native once a session is running against a peer.
    static void sessionStarted(int requestId, int sessionHandle) {
        if (dceGuard) {
            return;
        }
        Ranging.deliverSessionStarted(requestId, sessionHandle);
    }

    /// Called from native with the bytes to hand back to an accessory.
    static void accessoryConfiguration(int requestId, int sessionHandle,
            byte[] shareable) {
        if (dceGuard) {
            return;
        }
        Ranging.deliverAccessoryConfiguration(requestId, sessionHandle,
                shareable);
    }

    /// Called from native when a ranging request fails.
    static void rangingFailed(int requestId, int errorOrdinal, String message) {
        if (dceGuard) {
            return;
        }
        Ranging.deliverRequestFailed(requestId, errorOrdinal, message);
    }

    /// Called from native for every measurement.
    ///
    /// The direction arrives as three separate floats rather than an array so
    /// the Objective-C side never has to allocate a Java array on a delegate
    /// callback that fires several times a second.
    static void rangingUpdate(int sessionHandle, boolean hasDistance,
            double distanceMeters, boolean hasDirection, double azimuth,
            boolean hasElevation, double elevation, boolean hasVector,
            float x, float y, float z) {
        if (dceGuard) {
            return;
        }
        RangingSession.deliverUpdate(sessionHandle, hasDistance,
                distanceMeters, hasDirection, azimuth, hasElevation, elevation,
                hasVector ? new float[] {x, y, z} : null);
    }

    /// Called from native when a peer stops being ranged.
    static void peerRemoved(int sessionHandle, int reasonOrdinal) {
        if (dceGuard) {
            return;
        }
        RangingSession.deliverPeerRemoved(sessionHandle, reasonOrdinal);
    }

    /// Called from native when the platform suspends a session.
    static void sessionSuspended(int sessionHandle) {
        if (dceGuard) {
            return;
        }
        RangingSession.deliverSuspended(sessionHandle);
    }

    /// Called from native when a suspended session resumes.
    static void sessionResumed(int sessionHandle) {
        if (dceGuard) {
            return;
        }
        RangingSession.deliverResumed(sessionHandle);
    }

    /// Called from native when a session dies for good.
    static void sessionInvalidated(int sessionHandle, int errorOrdinal,
            String message) {
        if (dceGuard) {
            return;
        }
        RangingSession.deliverInvalidated(sessionHandle, errorOrdinal, message);
    }

    /// Called from native when the accessory picker returns a device.
    static void associated(int requestId, String encodedDevice) {
        if (dceGuard) {
            return;
        }
        CompanionDevices.deliverAssociated(requestId, encodedDevice);
    }

    /// Called from native when an association is dropped.
    static void disassociated(int requestId) {
        if (dceGuard) {
            return;
        }
        CompanionDevices.deliverDisassociated(requestId);
    }

    /// Called from native when an association request fails.
    static void companionFailed(int requestId, int errorOrdinal,
            String message) {
        if (dceGuard) {
            return;
        }
        CompanionDevices.deliverRequestFailed(requestId, errorOrdinal, message);
    }

    /// Called from native when a transport request succeeds.
    static void transportOk(int requestId) {
        if (dceGuard) {
            return;
        }
        NearbyTransport.deliverRequestOk(requestId);
    }

    /// Called from native when a transport request fails.
    static void transportFailed(int requestId, int errorOrdinal,
            String message) {
        if (dceGuard) {
            return;
        }
        NearbyTransport.deliverRequestFailed(requestId, errorOrdinal, message);
    }

    /// Called from native when a peer appears or disappears.
    static void endpointFound(String encodedEndpoint, boolean found) {
        if (dceGuard) {
            return;
        }
        NearbyTransport.deliverEndpointFound(encodedEndpoint, found);
    }

    /// Called from native when a peer invites this device.
    static void connectionRequested(String encodedEndpoint,
            String authenticationToken) {
        if (dceGuard) {
            return;
        }
        NearbyTransport.deliverConnectionRequested(encodedEndpoint,
                authenticationToken);
    }

    /// Called from native when a connection attempt settles.
    static void connectionResult(String encodedEndpoint, boolean connected,
            int errorOrdinal, String message) {
        if (dceGuard) {
            return;
        }
        NearbyTransport.deliverConnectionResult(encodedEndpoint, connected,
                errorOrdinal, message);
    }

    /// Called from native when an open connection closes.
    static void disconnected(String encodedEndpoint) {
        if (dceGuard) {
            return;
        }
        NearbyTransport.deliverDisconnected(encodedEndpoint);
    }

    /// Called from native with a complete incoming payload.
    static void payloadReceived(String encodedEndpoint, int payloadId,
            int payloadType, byte[] bytes, String path) {
        if (dceGuard) {
            return;
        }
        NearbyTransport.deliverPayloadReceived(encodedEndpoint, payloadId,
                payloadType, bytes, path);
    }

    /// Called from native with progress on a payload.
    static void payloadProgress(String encodedEndpoint, int payloadId,
            long bytesTransferred, long totalBytes, int statusOrdinal) {
        if (dceGuard) {
            return;
        }
        NearbyTransport.deliverPayloadProgress(encodedEndpoint, payloadId,
                bytesTransferred, totalBytes, statusOrdinal);
    }

    /// Splits a newline-joined batch, the inverse of what the native side
    /// does to keep the interface to one string.
    ///
    /// #### Parameters
    ///
    /// - `joined`: the batch, may be null or empty
    ///
    /// #### Returns
    ///
    /// the records, never null
    static String[] split(String joined) {
        if (joined == null || joined.length() == 0) {
            return new String[0];
        }
        List<String> parts = StringUtil.tokenize(joined, '\n');
        String[] out = new String[parts.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = parts.get(i);
        }
        return out;
    }
}
