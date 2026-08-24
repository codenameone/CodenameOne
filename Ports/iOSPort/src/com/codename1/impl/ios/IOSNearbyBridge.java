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

import com.codename1.nearby.spi.NearbyBridge;

/// Carries `com.codename1.nearby` onto Nearby Interaction,
/// MultipeerConnectivity and AccessorySetupKit.
///
/// Thin on purpose: everything here is a forward to [IOSNative], and the
/// interesting work -- session lifetimes, delegate queues, the accessory
/// handshake -- lives in `CN1Nearby.m` where the frameworks are. The two
/// things this layer does own are joining string arrays into the single
/// argument the native side takes, and splitting the batches it returns.
///
/// The three halves report independently, so an Apple TV build (no Nearby
/// Interaction, no AccessorySetupKit, but MultipeerConnectivity present) says
/// so honestly rather than reporting the whole feature missing.
class IOSNearbyBridge implements NearbyBridge {

    private final IOSNative nativeInstance;

    IOSNearbyBridge(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
        // Initializes the callback class, and with it the dead-code guard
        // that keeps the native call targets from being optimized away.
        IOSNearbyCallbacks.keepAlive();
    }

    // ------------------------------------------------------------------
    // Shared
    // ------------------------------------------------------------------

    public boolean isRangingSupported() {
        return nativeInstance.nearbyRangingSupported();
    }

    public boolean isCompanionSupported() {
        return nativeInstance.nearbyCompanionSupported();
    }

    public boolean isTransportSupported() {
        return nativeInstance.nearbyTransportSupported();
    }

    public int getRangingAvailability() {
        return nativeInstance.nearbyRangingAvailability();
    }

    public int getCompanionAvailability() {
        return nativeInstance.nearbyCompanionAvailability();
    }

    public int getTransportAvailability() {
        return nativeInstance.nearbyTransportAvailability();
    }

    public void requestPermissions(int requestId, int permissionBits) {
        nativeInstance.nearbyRequestPermissions(requestId, permissionBits);
    }

    // ------------------------------------------------------------------
    // Ranging
    // ------------------------------------------------------------------

    public int getRangingCapabilities() {
        return nativeInstance.nearbyRangingCapabilities();
    }

    public void prepareRangingSession(int requestId, int sessionHandle,
            boolean controller) {
        nativeInstance.nearbyPrepareSession(requestId, sessionHandle,
                controller);
    }

    public void startRanging(int requestId, int sessionHandle,
            byte[] peerToken) {
        nativeInstance.nearbyStartRanging(requestId, sessionHandle, peerToken);
    }

    public void startAccessoryRanging(int requestId, int sessionHandle,
            byte[] accessoryData) {
        nativeInstance.nearbyStartAccessoryRanging(requestId, sessionHandle,
                accessoryData);
    }

    public void stopRangingSession(int sessionHandle) {
        nativeInstance.nearbyStopSession(sessionHandle);
    }

    // ------------------------------------------------------------------
    // Companion
    // ------------------------------------------------------------------

    public void associate(int requestId, int profile, boolean singleDevice,
            String[] filters) {
        nativeInstance.nearbyAssociate(requestId, profile, singleDevice,
                join(filters));
    }

    public String[] getAssociations() {
        return IOSNearbyCallbacks.split(nativeInstance.nearbyAssociations());
    }

    public void disassociate(int requestId, String associationId) {
        nativeInstance.nearbyDisassociate(requestId, associationId);
    }

    public boolean startObservingPresence(String associationId) {
        return nativeInstance.nearbyStartObservingPresence(associationId);
    }

    public void stopObservingPresence(String associationId) {
        nativeInstance.nearbyStopObservingPresence(associationId);
    }

    // ------------------------------------------------------------------
    // Transport
    // ------------------------------------------------------------------

    public int getMaxPayloadSize() {
        return nativeInstance.nearbyMaxPayloadSize();
    }

    public void startAdvertising(int requestId, String serviceId,
            String localName, int strategy) {
        nativeInstance.nearbyStartAdvertising(requestId, serviceId, localName,
                strategy);
    }

    public void stopAdvertising() {
        nativeInstance.nearbyStopAdvertising();
    }

    public void startDiscovery(int requestId, String serviceId, int strategy) {
        nativeInstance.nearbyStartDiscovery(requestId, serviceId, strategy);
    }

    public void stopDiscovery() {
        nativeInstance.nearbyStopDiscovery();
    }

    public void requestConnection(int requestId, String endpointId,
            String localName) {
        nativeInstance.nearbyRequestConnection(requestId, endpointId,
                localName);
    }

    public void acceptConnection(int requestId, String endpointId) {
        nativeInstance.nearbyAcceptConnection(requestId, endpointId);
    }

    public void rejectConnection(String endpointId) {
        nativeInstance.nearbyRejectConnection(endpointId);
    }

    public void sendPayload(int requestId, String[] endpointIds, int payloadId,
            int payloadType, byte[] bytes, String path) {
        nativeInstance.nearbySendPayload(requestId, join(endpointIds),
                payloadId, payloadType, bytes, path);
    }

    public void cancelPayload(int payloadId) {
        nativeInstance.nearbyCancelPayload(payloadId);
    }

    public void disconnect(String endpointId) {
        nativeInstance.nearbyDisconnect(endpointId);
    }

    public void stopAllTransport() {
        nativeInstance.nearbyStopAllTransport();
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /// Joins records with newlines, which is safe because a record field can
    /// never contain one -- `NearbyWire.sanitize` turns a newline into a space
    /// before anything is encoded.
    ///
    /// #### Parameters
    ///
    /// - `values`: the records, may be null
    ///
    /// #### Returns
    ///
    /// the joined batch, never null
    private static String join(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                b.append('\n');
            }
            b.append(values[i] == null ? "" : values[i]);
        }
        return b.toString();
    }
}
