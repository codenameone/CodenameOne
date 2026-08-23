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
package com.codename1.nearby.transport;

import com.codename1.impl.async.EdtResult;
import com.codename1.impl.async.PendingMap;
import com.codename1.impl.nearby.NearbyRequests;
import com.codename1.impl.nearby.NearbyWire;
import com.codename1.nearby.NearbyAvailability;
import com.codename1.nearby.NearbyError;
import com.codename1.nearby.NearbyException;
import com.codename1.nearby.NearbyPermission;
import com.codename1.nearby.spi.NearbyBridge;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.List;

/// Moving bytes and files to a device that is physically nearby, with no
/// access point, no pairing and no internet.
///
/// The platform picks and combines the radios itself -- Bluetooth to find
/// each other, then Wi-Fi to move the data -- so an app advertises a service
/// id, discovers peers using the same id, connects, and sends payloads.
///
/// #### This transport does not cross ecosystems
///
/// **Android talks to Android and Apple talks to Apple, and the two do not
/// meet.** Underneath are Google's Nearby Connections and Apple's
/// MultipeerConnectivity, which share no wire protocol; nothing in this API
/// papers over that, because a portable-looking API that silently never
/// finds the peer is worse than an honest limitation.
///
/// For an iPhone that must talk to an Android phone, the framework already
/// has two things that do work across the divide:
///
/// - `com.codename1.bluetooth.le.L2capChannel` -- a raw bidirectional byte
///   stream over BLE, on every platform that has BLE.
/// - `com.codename1.io.bonjour` plus ordinary sockets, when both devices are
///   on the same Wi-Fi network.
///
/// #### Quick start
///
/// ```java
/// NearbyTransport.addTransportListener(new TransportAdapter() {
///     public void endpointFound(Endpoint e) {
///         NearbyTransport.requestConnection(e, "Shai's phone");
///     }
///     public void connectionRequested(ConnectionRequest r) {
///         // show r.getAuthenticationToken() on both screens before this
///         r.accept();
///     }
///     public void connected(Endpoint e) {
///         NearbyTransport.send(e, Payload.fromBytes(data));
///     }
///     public void payloadReceived(Endpoint e, Payload p) {
///         process(p.getBytes());
///     }
/// });
/// NearbyTransport.startAdvertising("com.example.chat", "Shai's phone",
///         TransportStrategy.CLUSTER);
/// NearbyTransport.startDiscovery("com.example.chat", TransportStrategy.CLUSTER);
/// ```
///
/// #### Threading
///
/// Every callback here is delivered on the EDT.
public final class NearbyTransport {

    private static final PendingMap<Boolean> PENDING =
            new PendingMap<Boolean>();
    private static final List<TransportListener> LISTENERS =
            new ArrayList<TransportListener>();

    private NearbyTransport() {
    }

    /// `true` when this port implements the nearby transport.
    public static boolean isSupported() {
        NearbyBridge b = NearbyRequests.bridge();
        return b != null && b.isTransportSupported();
    }

    /// How usable the transport is right now.
    ///
    /// #### Returns
    ///
    /// the current availability, never null
    public static NearbyAvailability getAvailability() {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isTransportSupported()) {
            return NearbyAvailability.NOT_SUPPORTED;
        }
        NearbyAvailability[] all = NearbyAvailability.values();
        int o = b.getTransportAvailability();
        return o >= 0 && o < all.length ? all[o]
                : NearbyAvailability.NOT_SUPPORTED;
    }

    /// The largest byte payload [#send] accepts in one call. Anything bigger
    /// has to go as a file payload.
    ///
    /// #### Returns
    ///
    /// the limit in bytes, or zero when the transport is unsupported
    public static int getMaxPayloadSize() {
        NearbyBridge b = NearbyRequests.bridge();
        return b == null || !b.isTransportSupported()
                ? 0 : b.getMaxPayloadSize();
    }

    /// Asks for the runtime permissions the transport needs -- on Android
    /// that is the Bluetooth trio plus nearby Wi-Fi, which is a lot to ask
    /// for at once, so ask when the user reaches the feature rather than at
    /// startup.
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
        if (b == null || !b.isTransportSupported()) {
            return unsupported();
        }
        int bits = 0;
        if (permissions != null) {
            for (int i = 0; i < permissions.length; i++) {
                bits |= bitFor(permissions[i]);
            }
        }
        int id = NearbyRequests.nextId();
        EdtResult<Boolean> out = PENDING.open(id);
        b.requestPermissions(id, bits);
        return out;
    }

    /// Starts advertising this device so peers running the same service id
    /// can find it.
    ///
    /// The service id must match exactly on both sides. On iOS it also
    /// becomes the Bonjour service type, which the platform restricts to
    /// fifteen characters of lowercase letters, digits and hyphens -- so a
    /// reverse-DNS string works on Android and is rejected on iOS. Pick a
    /// short one.
    ///
    /// #### Parameters
    ///
    /// - `serviceId`: the service both ends agreed on
    /// - `localName`: the name to show peers
    /// - `strategy`: the topology to use; must match on both sides
    ///
    /// #### Returns
    ///
    /// resolves `true` once the platform is advertising
    public static AsyncResource<Boolean> startAdvertising(String serviceId,
            String localName, TransportStrategy strategy) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isTransportSupported()) {
            return unsupported();
        }
        int id = NearbyRequests.nextId();
        EdtResult<Boolean> out = PENDING.open(id);
        b.startAdvertising(id, serviceId, localName, ordinalOf(strategy));
        return out;
    }

    /// Stops advertising. Idempotent; existing connections stay open.
    public static void stopAdvertising() {
        NearbyBridge b = NearbyRequests.bridge();
        if (b != null) {
            b.stopAdvertising();
        }
    }

    /// Starts looking for peers advertising the same service id. Sightings
    /// arrive as [TransportListener#endpointFound].
    ///
    /// #### Parameters
    ///
    /// - `serviceId`: the service both ends agreed on
    /// - `strategy`: the topology to use; must match on both sides
    ///
    /// #### Returns
    ///
    /// resolves `true` once the platform is discovering
    public static AsyncResource<Boolean> startDiscovery(String serviceId,
            TransportStrategy strategy) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isTransportSupported()) {
            return unsupported();
        }
        int id = NearbyRequests.nextId();
        EdtResult<Boolean> out = PENDING.open(id);
        b.startDiscovery(id, serviceId, ordinalOf(strategy));
        return out;
    }

    /// Stops discovery. Idempotent; existing connections stay open.
    public static void stopDiscovery() {
        NearbyBridge b = NearbyRequests.bridge();
        if (b != null) {
            b.stopDiscovery();
        }
    }

    /// Asks a discovered endpoint to connect.
    ///
    /// The resource here resolves once the request has been sent, which is
    /// not the same as being connected: the far side still has to accept,
    /// and that answer arrives as [TransportListener#connected] or
    /// [TransportListener#connectionFailed].
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the peer to ask
    /// - `localName`: the name to show them
    ///
    /// #### Returns
    ///
    /// resolves `true` once the request has been sent
    public static AsyncResource<Boolean> requestConnection(Endpoint endpoint,
            String localName) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isTransportSupported()) {
            return unsupported();
        }
        if (endpoint == null) {
            return failed(NearbyError.PEER_UNAVAILABLE,
                    "an endpoint is required");
        }
        int id = NearbyRequests.nextId();
        EdtResult<Boolean> out = PENDING.open(id);
        b.requestConnection(id, endpoint.getId(), localName);
        return out;
    }

    /// Sends a payload to one connected endpoint.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the recipient
    /// - `payload`: what to send
    ///
    /// #### Returns
    ///
    /// resolves `true` once the payload is handed to the platform. Delivery
    /// is reported by [TransportListener#payloadProgress].
    public static AsyncResource<Boolean> send(Endpoint endpoint,
            Payload payload) {
        return send(new Endpoint[] {endpoint}, payload);
    }

    /// Sends a payload to several connected endpoints at once, which both
    /// platforms do more efficiently than one call each.
    ///
    /// #### Parameters
    ///
    /// - `endpoints`: the recipients
    /// - `payload`: what to send
    ///
    /// #### Returns
    ///
    /// resolves `true` once the payload is handed to the platform
    public static AsyncResource<Boolean> send(Endpoint[] endpoints,
            Payload payload) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b == null || !b.isTransportSupported()) {
            return unsupported();
        }
        if (endpoints == null || endpoints.length == 0) {
            return failed(NearbyError.PEER_UNAVAILABLE,
                    "at least one endpoint is required");
        }
        if (payload == null) {
            return failed(NearbyError.IO_ERROR, "a payload is required");
        }
        if (payload.getType() == Payload.TYPE_BYTES) {
            int max = b.getMaxPayloadSize();
            if (max > 0 && payload.getBytes().length > max) {
                return failed(NearbyError.IO_ERROR,
                        "a byte payload is limited to " + max
                                + " bytes on this platform; send a file"
                                + " payload instead");
            }
        }
        String[] ids = new String[endpoints.length];
        for (int i = 0; i < endpoints.length; i++) {
            if (endpoints[i] == null) {
                return failed(NearbyError.PEER_UNAVAILABLE,
                        "a null endpoint was passed to send");
            }
            ids[i] = endpoints[i].getId();
        }
        int id = NearbyRequests.nextId();
        EdtResult<Boolean> out = PENDING.open(id);
        b.sendPayload(id, ids, payload.getId(),
                payload.getType() == Payload.TYPE_FILE
                        ? NearbyBridge.PAYLOAD_FILE
                        : NearbyBridge.PAYLOAD_BYTES,
                payload.getBytes(), payload.getPath());
        return out;
    }

    /// Cancels an in-flight payload on both sides. Idempotent.
    ///
    /// #### Parameters
    ///
    /// - `payloadId`: the id from [Payload#getId()]
    public static void cancel(int payloadId) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b != null) {
            b.cancelPayload(payloadId);
        }
    }

    /// Disconnects one endpoint. Idempotent.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the peer to drop
    public static void disconnect(Endpoint endpoint) {
        NearbyBridge b = NearbyRequests.bridge();
        if (b != null && endpoint != null) {
            b.disconnect(endpoint.getId());
        }
    }

    /// Stops advertising and discovery and drops every connection. Call it
    /// when the feature's UI closes: both platforms keep the radios busy
    /// until something says stop.
    public static void stop() {
        NearbyBridge b = NearbyRequests.bridge();
        if (b != null) {
            b.stopAllTransport();
        }
    }

    /// Registers a listener. Callbacks arrive on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public static void addTransportListener(TransportListener l) {
        if (l == null) {
            return;
        }
        synchronized (LISTENERS) {
            LISTENERS.add(l);
        }
    }

    /// Removes a listener added by [#addTransportListener].
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public static void removeTransportListener(TransportListener l) {
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
        PENDING.failAll(reset);
        synchronized (LISTENERS) {
            LISTENERS.clear();
        }
    }

    // ------------------------------------------------------------------
    // Port entry points
    // ------------------------------------------------------------------

    /// Answers any request that resolves with a simple acknowledgement --
    /// advertising, discovery, a connection request, a payload handoff.
    ///
    /// @hidden not part of the public API; called by ports.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id the request was made with
    public static void deliverRequestOk(int requestId) {
        EdtResult<Boolean> r = PENDING.take(requestId);
        if (r != null) {
            r.complete(Boolean.TRUE);
        }
    }

    /// Fails whichever transport request carries this id.
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
        EdtResult<Boolean> r = PENDING.take(requestId);
        if (r != null) {
            r.error(NearbyWire.decodeError(errorOrdinal, message));
        }
    }

    /// Reports a discovered or lost endpoint.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `encodedEndpoint`: the endpoint, encoded by
    ///   `com.codename1.impl.nearby.NearbyWire`
    /// - `found`: true for a sighting, false when it went away
    public static void deliverEndpointFound(String encodedEndpoint,
            final boolean found) {
        final Endpoint e = NearbyWire.decodeEndpoint(encodedEndpoint);
        if (e == null) {
            return;
        }
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                TransportListener[] ls = snapshot();
                for (int i = 0; i < ls.length; i++) {
                    if (found) {
                        ls[i].endpointFound(e);
                    } else {
                        ls[i].endpointLost(e);
                    }
                }
            }
        });
    }

    /// Reports an incoming connection request.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `encodedEndpoint`: the endpoint, encoded by
    ///   `com.codename1.impl.nearby.NearbyWire`
    /// - `authenticationToken`: the short comparison string
    public static void deliverConnectionRequested(String encodedEndpoint,
            final String authenticationToken) {
        final Endpoint e = NearbyWire.decodeEndpoint(encodedEndpoint);
        if (e == null) {
            return;
        }
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                ConnectionRequest r =
                        new ConnectionRequest(e, authenticationToken);
                TransportListener[] ls = snapshot();
                for (int i = 0; i < ls.length; i++) {
                    ls[i].connectionRequested(r);
                }
                if (!r.isAnswered()) {
                    // Nobody was listening, so nobody will ever answer. The
                    // far side would sit in its connecting state until it
                    // timed out; reject instead so it learns immediately.
                    r.reject();
                }
            }
        });
    }

    /// Reports the outcome of a connection attempt.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `encodedEndpoint`: the endpoint, encoded by
    ///   `com.codename1.impl.nearby.NearbyWire`
    /// - `connected`: whether the connection is now open
    /// - `errorOrdinal`: when not connected, the ordinal of a
    ///   `com.codename1.nearby.NearbyError` constant
    /// - `message`: when not connected, a human-readable detail
    public static void deliverConnectionResult(String encodedEndpoint,
            final boolean connected, final int errorOrdinal,
            final String message) {
        final Endpoint e = NearbyWire.decodeEndpoint(encodedEndpoint);
        if (e == null) {
            return;
        }
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                TransportListener[] ls = snapshot();
                for (int i = 0; i < ls.length; i++) {
                    if (connected) {
                        ls[i].connected(e);
                    } else {
                        ls[i].connectionFailed(e,
                                NearbyWire.decodeError(errorOrdinal, message));
                    }
                }
            }
        });
    }

    /// Reports that an open connection closed.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `encodedEndpoint`: the endpoint, encoded by
    ///   `com.codename1.impl.nearby.NearbyWire`
    public static void deliverDisconnected(String encodedEndpoint) {
        final Endpoint e = NearbyWire.decodeEndpoint(encodedEndpoint);
        if (e == null) {
            return;
        }
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                TransportListener[] ls = snapshot();
                for (int i = 0; i < ls.length; i++) {
                    ls[i].disconnected(e);
                }
            }
        });
    }

    /// Reports a complete incoming payload.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `encodedEndpoint`: who sent it, encoded by
    ///   `com.codename1.impl.nearby.NearbyWire`
    /// - `payloadId`: the sender's payload id
    /// - `payloadType`: `NearbyBridge.PAYLOAD_BYTES` or
    ///   `NearbyBridge.PAYLOAD_FILE`
    /// - `bytes`: the payload for a byte payload, otherwise null
    /// - `path`: the file the port wrote, for a file payload
    public static void deliverPayloadReceived(String encodedEndpoint,
            final int payloadId, final int payloadType, final byte[] bytes,
            final String path) {
        final Endpoint e = NearbyWire.decodeEndpoint(encodedEndpoint);
        if (e == null) {
            return;
        }
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                Payload p = Payload.received(payloadId,
                        payloadType == NearbyBridge.PAYLOAD_FILE
                                ? Payload.TYPE_FILE : Payload.TYPE_BYTES,
                        bytes, path);
                TransportListener[] ls = snapshot();
                for (int i = 0; i < ls.length; i++) {
                    ls[i].payloadReceived(e, p);
                }
            }
        });
    }

    /// Reports progress on a payload.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `encodedEndpoint`: the other end, encoded by
    ///   `com.codename1.impl.nearby.NearbyWire`
    /// - `payloadId`: the payload
    /// - `bytesTransferred`: bytes moved so far
    /// - `totalBytes`: the payload size, or -1 when unknown
    /// - `statusOrdinal`: the ordinal of a [PayloadStatus] constant
    public static void deliverPayloadProgress(String encodedEndpoint,
            final int payloadId, final long bytesTransferred,
            final long totalBytes, final int statusOrdinal) {
        final Endpoint e = NearbyWire.decodeEndpoint(encodedEndpoint);
        if (e == null) {
            return;
        }
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                PayloadStatus[] all = PayloadStatus.values();
                PayloadStatus s = statusOrdinal >= 0
                        && statusOrdinal < all.length
                        ? all[statusOrdinal] : PayloadStatus.IN_PROGRESS;
                PayloadTransferUpdate u = new PayloadTransferUpdate(payloadId,
                        bytesTransferred, totalBytes, s);
                TransportListener[] ls = snapshot();
                for (int i = 0; i < ls.length; i++) {
                    ls[i].payloadProgress(e, u);
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static TransportListener[] snapshot() {
        synchronized (LISTENERS) {
            return LISTENERS.toArray(
                    new TransportListener[LISTENERS.size()]);
        }
    }

    private static int ordinalOf(TransportStrategy s) {
        return s == null ? TransportStrategy.CLUSTER.ordinal() : s.ordinal();
    }

    private static int bitFor(NearbyPermission p) {
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

    private static AsyncResource<Boolean> unsupported() {
        return failed(NearbyError.NOT_SUPPORTED,
                "this platform does not support the nearby transport");
    }

    private static AsyncResource<Boolean> failed(NearbyError error,
            String message) {
        EdtResult<Boolean> out = new EdtResult<Boolean>();
        out.error(new NearbyException(error, message));
        return out;
    }
}
