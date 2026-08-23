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
package com.codename1.impl.android;

import android.app.Activity;

import com.codename1.nearby.NearbyAvailability;
import com.codename1.nearby.spi.NearbyBridge;

/// The always-compiled half of the Android nearby bridge: a shell that finds
/// the real implementation, or reports the feature missing when there is
/// none.
///
/// #### Why the work is not here
///
/// The Android port jar is compiled against an SDK from 2017 and against no
/// optional dependency at all. Everything this feature needs is newer than
/// that -- `CompanionDeviceService` and `AssociationInfo` are API 31 and 33,
/// `androidx.core.uwb` and `play-services-nearby` are gradle dependencies the
/// build only adds for an app that referenced the matching package. So the
/// implementation lives in `com.codename1.impl.android.nearby`, which is
/// excluded from the port jar compile and compiled inside the generated app
/// instead, where a modern `compileSdk` and those dependencies exist.
///
/// That is the same arrangement `com.codename1.impl.android.ar` and
/// `com.codename1.impl.android.cipher` use, and the reason the load below is
/// reflective and its failure is a shrug rather than an error: for most apps
/// the package is not there at all, because the builder deleted it.
public class AndroidNearbyBridge implements NearbyBridge {

    private final NearbyBridge delegate;

    /// Loads the optional backend, or `null` when the build did not include
    /// it.
    ///
    /// #### Parameters
    ///
    /// - `activity`: the host activity, which the backend needs for the
    ///   association chooser
    public AndroidNearbyBridge(Activity activity) {
        Object instance = null;
        try {
            Class<?> clazz = Class.forName(
                    "com.codename1.impl.android.nearby.AndroidNearbyBackend");
            instance = clazz.getConstructor(Activity.class)
                    .newInstance(activity);
        } catch (Throwable t) {
            // Expected for every app that never referenced com.codename1
            // .nearby: the builder deleted the package. Nothing to log.
            instance = null;
        }
        // Tested rather than cast inside the catch. A failed cast does not
        // throw under ParparVM, so a `catch` around one is a handler that
        // never runs -- and scripts/check-cast-semantics.sh rejects the
        // shape repo-wide, on Android sources too, so the rule stays one
        // rule rather than a per-port exception.
        this.delegate = instance instanceof NearbyBridge
                ? (NearbyBridge) instance : null;
    }

    // ------------------------------------------------------------------
    // Shared
    // ------------------------------------------------------------------

    public boolean isRangingSupported() {
        return delegate != null && delegate.isRangingSupported();
    }

    public boolean isCompanionSupported() {
        return delegate != null && delegate.isCompanionSupported();
    }

    public boolean isTransportSupported() {
        return delegate != null && delegate.isTransportSupported();
    }

    public int getRangingAvailability() {
        return delegate == null ? NearbyAvailability.NOT_SUPPORTED.ordinal()
                : delegate.getRangingAvailability();
    }

    public int getCompanionAvailability() {
        return delegate == null ? NearbyAvailability.NOT_SUPPORTED.ordinal()
                : delegate.getCompanionAvailability();
    }

    public int getTransportAvailability() {
        return delegate == null ? NearbyAvailability.NOT_SUPPORTED.ordinal()
                : delegate.getTransportAvailability();
    }

    public void requestPermissions(int requestId, int permissionBits) {
        if (delegate != null) {
            delegate.requestPermissions(requestId, permissionBits);
        } else {
            // Still answered, because a caller is holding a resource.
            com.codename1.nearby.ranging.Ranging.deliverPermissionResult(
                    requestId, false);
        }
    }

    // ------------------------------------------------------------------
    // Ranging
    // ------------------------------------------------------------------

    public int getRangingCapabilities() {
        return delegate == null ? 0 : delegate.getRangingCapabilities();
    }

    public void prepareRangingSession(int requestId, int sessionHandle,
            boolean controller) {
        if (delegate != null) {
            delegate.prepareRangingSession(requestId, sessionHandle,
                    controller);
        } else {
            failRanging(requestId);
        }
    }

    public void startRanging(int requestId, int sessionHandle,
            byte[] peerToken) {
        if (delegate != null) {
            delegate.startRanging(requestId, sessionHandle, peerToken);
        } else {
            failRanging(requestId);
        }
    }

    public void startAccessoryRanging(int requestId, int sessionHandle,
            byte[] accessoryData) {
        if (delegate != null) {
            delegate.startAccessoryRanging(requestId, sessionHandle,
                    accessoryData);
        } else {
            failRanging(requestId);
        }
    }

    public void stopRangingSession(int sessionHandle) {
        if (delegate != null) {
            delegate.stopRangingSession(sessionHandle);
        }
    }

    // ------------------------------------------------------------------
    // Companion
    // ------------------------------------------------------------------

    public void associate(int requestId, int profile, boolean singleDevice,
            String[] filters) {
        if (delegate != null) {
            delegate.associate(requestId, profile, singleDevice, filters);
        } else {
            com.codename1.nearby.companion.CompanionDevices
                    .deliverRequestFailed(requestId,
                            com.codename1.nearby.NearbyError.NOT_SUPPORTED
                                    .ordinal(), null);
        }
    }

    public String[] getAssociations() {
        return delegate == null ? new String[0] : delegate.getAssociations();
    }

    public void disassociate(int requestId, String associationId) {
        if (delegate != null) {
            delegate.disassociate(requestId, associationId);
        } else {
            com.codename1.nearby.companion.CompanionDevices
                    .deliverRequestFailed(requestId,
                            com.codename1.nearby.NearbyError.NOT_SUPPORTED
                                    .ordinal(), null);
        }
    }

    public boolean startObservingPresence(String associationId) {
        return delegate != null
                && delegate.startObservingPresence(associationId);
    }

    public void stopObservingPresence(String associationId) {
        if (delegate != null) {
            delegate.stopObservingPresence(associationId);
        }
    }

    // ------------------------------------------------------------------
    // Transport
    // ------------------------------------------------------------------

    public int getMaxPayloadSize() {
        return delegate == null ? 0 : delegate.getMaxPayloadSize();
    }

    public void startAdvertising(int requestId, String serviceId,
            String localName, int strategy) {
        if (delegate != null) {
            delegate.startAdvertising(requestId, serviceId, localName,
                    strategy);
        } else {
            failTransport(requestId);
        }
    }

    public void stopAdvertising() {
        if (delegate != null) {
            delegate.stopAdvertising();
        }
    }

    public void startDiscovery(int requestId, String serviceId, int strategy) {
        if (delegate != null) {
            delegate.startDiscovery(requestId, serviceId, strategy);
        } else {
            failTransport(requestId);
        }
    }

    public void stopDiscovery() {
        if (delegate != null) {
            delegate.stopDiscovery();
        }
    }

    public void requestConnection(int requestId, String endpointId,
            String localName) {
        if (delegate != null) {
            delegate.requestConnection(requestId, endpointId, localName);
        } else {
            failTransport(requestId);
        }
    }

    public void acceptConnection(int requestId, String endpointId) {
        if (delegate != null) {
            delegate.acceptConnection(requestId, endpointId);
        } else {
            failTransport(requestId);
        }
    }

    public void rejectConnection(String endpointId) {
        if (delegate != null) {
            delegate.rejectConnection(endpointId);
        }
    }

    public void sendPayload(int requestId, String[] endpointIds, int payloadId,
            int payloadType, byte[] bytes, String path) {
        if (delegate != null) {
            delegate.sendPayload(requestId, endpointIds, payloadId,
                    payloadType, bytes, path);
        } else {
            failTransport(requestId);
        }
    }

    public void cancelPayload(int payloadId) {
        if (delegate != null) {
            delegate.cancelPayload(payloadId);
        }
    }

    public void disconnect(String endpointId) {
        if (delegate != null) {
            delegate.disconnect(endpointId);
        }
    }

    public void stopAllTransport() {
        if (delegate != null) {
            delegate.stopAllTransport();
        }
    }

    private static void failRanging(int requestId) {
        com.codename1.nearby.ranging.Ranging.deliverRequestFailed(requestId,
                com.codename1.nearby.NearbyError.NOT_SUPPORTED.ordinal(),
                null);
    }

    private static void failTransport(int requestId) {
        com.codename1.nearby.transport.NearbyTransport.deliverRequestFailed(
                requestId,
                com.codename1.nearby.NearbyError.NOT_SUPPORTED.ordinal(),
                null);
    }
}
