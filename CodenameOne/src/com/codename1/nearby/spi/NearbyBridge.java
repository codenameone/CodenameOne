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
package com.codename1.nearby.spi;

/// Internal service-provider interface implemented by each platform port to
/// carry the `com.codename1.nearby` API onto the native short-range stacks:
/// Apple's Nearby Interaction, MultipeerConnectivity and AccessorySetupKit,
/// Android's Jetpack UWB, Nearby Connections and `CompanionDeviceManager`.
///
/// Application code never touches this interface. It is obtained by the
/// `com.codename1.nearby` packages from
/// `com.codename1.ui.Display#getNearbyBridge()`, and the base
/// implementation returns `null` -- which is why the public API degrades to
/// a well-behaved `NOT_SUPPORTED` on ports that implement nothing, and why
/// application code needs no platform `if` statements.
///
/// #### Everything here is primitives, strings and byte arrays
///
/// A port may be Objective-C reached through ParparVM, where constructing a
/// Java object is expensive and easy to get wrong. So no method on this
/// interface takes or returns a framework type: enums cross as their
/// ordinals, capability sets cross as bit masks, and structured records
/// cross as tab-delimited strings built by
/// `com.codename1.impl.nearby.NearbyWire`.
///
/// #### Asynchrony is by request id, and every operation must answer
///
/// Operations that can fail take a `requestId` allocated by the caller and
/// answer exactly once by calling the matching `deliver...` entry point on
/// the public class. **An operation that never answers is worse than one
/// that fails**: the caller holds an `AsyncResource` that will never settle
/// and has no way to find out. A port that cannot start something must
/// still report the failure.
///
/// Unsolicited events -- ranging updates, endpoint discoveries, presence
/// changes -- carry the handle or endpoint id they belong to instead of a
/// request id. Every entry point may be called from any thread; they
/// marshal to the EDT themselves.
public interface NearbyBridge {

    /// [#getRangingCapabilities()] bit: precise distance is measurable.
    int CAPABILITY_DISTANCE = 1;

    /// [#getRangingCapabilities()] bit: horizontal direction is measurable.
    int CAPABILITY_DIRECTION = 2;

    /// [#getRangingCapabilities()] bit: vertical direction is measurable.
    int CAPABILITY_ELEVATION = 4;

    /// [#getRangingCapabilities()] bit: camera assistance is available.
    int CAPABILITY_CAMERA_ASSISTANCE = 8;

    /// [#getRangingCapabilities()] bit: third-party UWB accessories can be
    /// ranged.
    int CAPABILITY_ACCESSORY = 16;

    /// [#getRangingCapabilities()] bit: ranging continues in the background.
    int CAPABILITY_BACKGROUND = 32;

    /// [#requestPermissions] bit for `NearbyPermission.RANGING`.
    int PERMISSION_RANGING = 1;

    /// [#requestPermissions] bit for `NearbyPermission.DISCOVERY`.
    int PERMISSION_DISCOVERY = 2;

    /// [#requestPermissions] bit for `NearbyPermission.ADVERTISE`.
    int PERMISSION_ADVERTISE = 4;

    /// [#requestPermissions] bit for `NearbyPermission.CONNECT`.
    int PERMISSION_CONNECT = 8;

    /// [#sendPayload] type: the payload is the `bytes` argument.
    int PAYLOAD_BYTES = 0;

    /// [#sendPayload] type: the payload is the file at `path`.
    int PAYLOAD_FILE = 1;

    // ------------------------------------------------------------------
    // Shared
    // ------------------------------------------------------------------

    /// Whether this port implements precision ranging at all. Answer for the
    /// port and the hardware, not for whether a peer is around.
    ///
    /// #### Returns
    ///
    /// true when `com.codename1.nearby.ranging` has a real implementation
    boolean isRangingSupported();

    /// Whether this port implements companion-device association.
    ///
    /// #### Returns
    ///
    /// true when `com.codename1.nearby.companion` has a real implementation
    boolean isCompanionSupported();

    /// Whether this port implements the nearby transport.
    ///
    /// #### Returns
    ///
    /// true when `com.codename1.nearby.transport` has a real implementation
    boolean isTransportSupported();

    /// How usable ranging is right now, as a `NearbyAvailability` ordinal.
    /// A port backed by a simulation must answer `LOCAL_ONLY` rather than
    /// `AVAILABLE`, so an app can tell the developer their peers are not
    /// real.
    ///
    /// #### Returns
    ///
    /// the ordinal of a `com.codename1.nearby.NearbyAvailability` constant
    int getRangingAvailability();

    /// How usable companion association is right now, as a
    /// `NearbyAvailability` ordinal.
    ///
    /// #### Returns
    ///
    /// the ordinal of a `com.codename1.nearby.NearbyAvailability` constant
    int getCompanionAvailability();

    /// How usable the transport is right now, as a `NearbyAvailability`
    /// ordinal.
    ///
    /// #### Returns
    ///
    /// the ordinal of a `com.codename1.nearby.NearbyAvailability` constant
    int getTransportAvailability();

    /// Requests the platform permissions behind the given
    /// `NearbyPermission` bits, answering with
    /// `com.codename1.nearby.ranging.Ranging#deliverPermissionResult`.
    ///
    /// A port that needs no permission for the bits it was given must still
    /// answer, reporting them granted.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id to answer with
    /// - `permissionBits`: an OR of the `PERMISSION_` constants
    void requestPermissions(int requestId, int permissionBits);

    // ------------------------------------------------------------------
    // Ranging
    // ------------------------------------------------------------------

    /// What the device can measure, as an OR of the `CAPABILITY_` constants.
    ///
    /// #### Returns
    ///
    /// the capability bits, or zero when ranging is unsupported
    int getRangingCapabilities();

    /// Allocates a platform ranging session and publishes its local token.
    ///
    /// The port answers with
    /// `com.codename1.nearby.ranging.Ranging#deliverSessionPrepared` on
    /// success, passing back the same `sessionHandle` it was given, or with
    /// `Ranging#deliverRequestFailed` on failure. The session is not ranging
    /// yet -- [#startRanging] or [#startAccessoryRanging] does that.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id to answer with
    /// - `sessionHandle`: the handle every later call and event uses to name
    ///   this session
    /// - `controller`: true for `RangingRole.CONTROLLER`. Ports whose
    ///   platform negotiates roles by itself ignore this.
    void prepareRangingSession(int requestId, int sessionHandle,
            boolean controller);

    /// Starts ranging a peer whose token arrived out of band.
    ///
    /// The token is the full encoded form from
    /// `com.codename1.nearby.ranging.RangingToken#toByteArray()`; the port
    /// validates that it was minted by this platform and fails the request
    /// with `NearbyError.INVALID_TOKEN` when it was not.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id to answer with, via
    ///   `Ranging#deliverSessionStarted`
    /// - `sessionHandle`: the prepared session
    /// - `peerToken`: the peer's encoded token
    void startRanging(int requestId, int sessionHandle, byte[] peerToken);

    /// Starts ranging a third-party UWB accessory.
    ///
    /// The port answers with
    /// `com.codename1.nearby.ranging.Ranging#deliverAccessoryConfiguration`,
    /// passing the bytes the app must send back to the accessory to make it
    /// start ranging (Apple's Nearby Interaction Accessory Protocol
    /// requires this handshake). A platform with no such handshake answers
    /// with an empty array rather than failing.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id to answer with
    /// - `sessionHandle`: the prepared session
    /// - `accessoryData`: the configuration data the accessory published
    void startAccessoryRanging(int requestId, int sessionHandle,
            byte[] accessoryData);

    /// Tears a ranging session down and releases the radio. Idempotent, and
    /// must not deliver any further event for this handle.
    ///
    /// #### Parameters
    ///
    /// - `sessionHandle`: the session to stop
    void stopRangingSession(int sessionHandle);

    // ------------------------------------------------------------------
    // Companion device association
    // ------------------------------------------------------------------

    /// Runs the platform's device chooser and associates whatever the user
    /// picks, answering with
    /// `com.codename1.nearby.companion.CompanionDevices#deliverAssociated`.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id to answer with
    /// - `profile`: the ordinal of a
    ///   `com.codename1.nearby.companion.CompanionProfile` constant
    /// - `singleDevice`: whether to associate without showing a list when
    ///   exactly one device matches
    /// - `filters`: the encoded filters from
    ///   `com.codename1.impl.nearby.NearbyWire`, never null and possibly
    ///   empty
    void associate(int requestId, int profile, boolean singleDevice,
            String[] filters);

    /// Every association this app currently holds, each encoded by
    /// `com.codename1.impl.nearby.NearbyWire`.
    ///
    /// #### Returns
    ///
    /// the associations, never null and possibly empty
    String[] getAssociations();

    /// Drops an association, answering with
    /// `com.codename1.nearby.companion.CompanionDevices#deliverRequestFailed`
    /// only on failure and
    /// `CompanionDevices#deliverDisassociated` on success.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id to answer with
    /// - `associationId`: the association to drop
    void disassociate(int requestId, String associationId);

    /// Asks the platform to wake this app when the associated device comes
    /// and goes.
    ///
    /// #### Parameters
    ///
    /// - `associationId`: the association to watch
    ///
    /// #### Returns
    ///
    /// true when the platform accepted the request
    boolean startObservingPresence(String associationId);

    /// Stops watching an association. Idempotent.
    ///
    /// #### Parameters
    ///
    /// - `associationId`: the association to stop watching
    void stopObservingPresence(String associationId);

    // ------------------------------------------------------------------
    // Transport
    // ------------------------------------------------------------------

    /// The largest byte payload [#sendPayload] accepts in one call.
    ///
    /// #### Returns
    ///
    /// the limit in bytes, or zero when the transport is unsupported
    int getMaxPayloadSize();

    /// Starts advertising this device under a service id, answering with
    /// `com.codename1.nearby.transport.NearbyTransport#deliverRequestOk`.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id to answer with
    /// - `serviceId`: the service both ends agreed on
    /// - `localName`: the name to show peers
    /// - `strategy`: the ordinal of a
    ///   `com.codename1.nearby.transport.TransportStrategy` constant
    void startAdvertising(int requestId, String serviceId, String localName,
            int strategy);

    /// Stops advertising. Idempotent.
    void stopAdvertising();

    /// Starts looking for peers advertising the same service id, answering
    /// with `NearbyTransport#deliverRequestOk`. Sightings arrive
    /// unsolicited through `NearbyTransport#deliverEndpointFound`.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id to answer with
    /// - `serviceId`: the service both ends agreed on
    /// - `strategy`: the ordinal of a
    ///   `com.codename1.nearby.transport.TransportStrategy` constant
    void startDiscovery(int requestId, String serviceId, int strategy);

    /// Stops discovery. Idempotent.
    void stopDiscovery();

    /// Asks a discovered endpoint to connect. The endpoint answers by
    /// accepting or rejecting, which arrives through
    /// `NearbyTransport#deliverConnectionResult`.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id to answer with
    /// - `endpointId`: the endpoint to ask
    /// - `localName`: the name to show them
    void requestConnection(int requestId, String endpointId, String localName);

    /// Accepts an incoming connection request.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id to answer with
    /// - `endpointId`: the endpoint that asked
    void acceptConnection(int requestId, String endpointId);

    /// Rejects an incoming connection request.
    ///
    /// #### Parameters
    ///
    /// - `endpointId`: the endpoint that asked
    void rejectConnection(String endpointId);

    /// Sends a payload to one or more connected endpoints, reporting
    /// progress through `NearbyTransport#deliverPayloadProgress`.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id to answer with once the payload is handed to
    ///   the platform
    /// - `endpointIds`: the recipients
    /// - `payloadId`: the id progress and cancellation use
    /// - `payloadType`: [#PAYLOAD_BYTES] or [#PAYLOAD_FILE]
    /// - `bytes`: the payload for [#PAYLOAD_BYTES], otherwise null
    /// - `path`: the file for [#PAYLOAD_FILE], otherwise null
    void sendPayload(int requestId, String[] endpointIds, int payloadId,
            int payloadType, byte[] bytes, String path);

    /// Cancels an in-flight payload. Idempotent.
    ///
    /// #### Parameters
    ///
    /// - `payloadId`: the payload to cancel
    void cancelPayload(int payloadId);

    /// Disconnects one endpoint. Idempotent.
    ///
    /// #### Parameters
    ///
    /// - `endpointId`: the endpoint to drop
    void disconnect(String endpointId);

    /// Stops advertising and discovery and drops every connection. Called
    /// when the app is shutting the transport down.
    void stopAllTransport();
}
