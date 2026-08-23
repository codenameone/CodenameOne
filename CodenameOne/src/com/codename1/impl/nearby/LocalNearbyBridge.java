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
package com.codename1.impl.nearby;

import com.codename1.nearby.NearbyAvailability;
import com.codename1.nearby.NearbyError;
import com.codename1.nearby.companion.CompanionDevice;
import com.codename1.nearby.companion.CompanionDevices;
import com.codename1.nearby.companion.DeviceFilter;
import com.codename1.nearby.ranging.Ranging;
import com.codename1.nearby.ranging.RangingRemovalReason;
import com.codename1.nearby.ranging.RangingSession;
import com.codename1.nearby.ranging.RangingToken;
import com.codename1.nearby.spi.NearbyBridge;
import com.codename1.nearby.transport.NearbyTransport;
import com.codename1.nearby.transport.PayloadStatus;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A working `com.codename1.nearby` implementation with no radio behind it,
/// used by the simulator, the desktop ports and the JavaScript port.
///
/// #### Why this is a simulation and not a stub
///
/// Almost none of the code in a ranging feature is about radios. Laying out
/// the screen, animating an arrow toward a peer, deciding what to show while
/// the direction drops out, handling the peer walking away and coming back,
/// getting the association flow right -- all of that is ordinary application
/// code, and a desktop port answering `NOT_SUPPORTED` would make every line
/// of it testable only on a phone with a second phone next to it. In
/// practice that means testable rarely.
///
/// So this reports [NearbyAvailability#LOCAL_ONLY] rather than
/// `NOT_SUPPORTED`: everything works, and nothing outside this process can
/// see it.
///
/// #### Two rules a mock would not follow
///
/// **It never completes inline.** Every answer goes through [#answer], which
/// posts it a few milliseconds later. It could answer synchronously and
/// deliberately does not: code written against a transport that answers
/// instantly races the moment it meets one that does not, and that asymmetry
/// has already shipped in this codebase once -- it is why
/// `com.codename1.impl.async.EdtResult` exists.
///
/// **Peers move, and they move smoothly.** A mock that returned a constant
/// 1.5 m would let an app ship with a distance label that flickers
/// unreadably against real hardware, or with an arrow that snaps. The drift
/// here is a bounded random walk seeded from the session handle, so it is
/// lifelike and still reproducible run to run -- a test that asserts on the
/// tenth update gets the same tenth update every time.
///
/// **What it will not do behind your back** is drop a peer or suspend a
/// session at random. Those are real events an app must handle, but a
/// simulation that fired them unpredictably would make every test using it
/// flaky. They are controls instead: see [#dropPeer], [#suspendSession] and
/// [#resumeSession], which the simulator's Nearby panel drives.
public class LocalNearbyBridge implements NearbyBridge {

    /// How long an operation takes to answer, in milliseconds. Small, and
    /// deliberately not zero. See the class note.
    private static final int LATENCY_MILLIS = 4;

    /// How often a running ranging session produces a measurement. Roughly
    /// what both real platforms deliver at their default update rate.
    private static final int TICK_MILLIS = 120;

    private static final double MIN_DISTANCE = 0.08;
    private static final double MAX_DISTANCE = 14.0;

    private final Map<Integer, SimSession> sessions =
            new LinkedHashMap<Integer, SimSession>();
    private final Map<String, CompanionDevice> associations =
            new LinkedHashMap<String, CompanionDevice>();
    private final Map<String, Boolean> observed =
            new LinkedHashMap<String, Boolean>();
    private final List<Candidate> candidates = new ArrayList<Candidate>();
    private final List<SimEndpoint> endpoints = new ArrayList<SimEndpoint>();
    private final List<String> connected = new ArrayList<String>();

    private int sessionSequence;
    private boolean advertising;
    private boolean discovering;
    private boolean echoPayloads = true;
    private int nextAssociationId = 1;

    // ------------------------------------------------------------------
    // Simulation controls
    // ------------------------------------------------------------------

    /// Adds a device the association chooser may offer.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name to show
    /// - `address`: the address to report
    /// - `serviceUuid`: the BLE service it advertises, matched against
    ///   `DeviceFilter.KIND_BLE_SERVICE`, may be null
    public void addCandidate(String name, String address, String serviceUuid) {
        candidates.add(new Candidate(name, address, serviceUuid));
    }

    /// Adds an endpoint that discovery will find.
    ///
    /// #### Parameters
    ///
    /// - `id`: the endpoint id
    /// - `name`: the name it advertises
    public void addEndpoint(String id, String name) {
        endpoints.add(new SimEndpoint(id, name));
    }

    /// Whether a sent payload is echoed back from the endpoint it went to.
    ///
    /// On by default, because a single process has no real peer and an app
    /// developing its receive path otherwise has nothing to receive. Turn it
    /// off in a test that counts deliveries.
    ///
    /// #### Parameters
    ///
    /// - `echo`: whether to echo
    public void setEchoPayloads(boolean echo) {
        this.echoPayloads = echo;
    }

    /// Makes a running session report that its peer walked away. The session
    /// stays alive; the peer starts being reported again on the next tick,
    /// which is what real hardware does when someone steps back into range.
    ///
    /// #### Parameters
    ///
    /// - `sessionHandle`: the session to disturb
    public void dropPeer(int sessionHandle) {
        SimSession s = sessions.get(Integer.valueOf(sessionHandle));
        if (s != null && s.running) {
            RangingSession.deliverPeerRemoved(sessionHandle,
                    RangingRemovalReason.TIMEOUT.ordinal());
        }
    }

    /// Suspends a running session, as the platform does when an app without
    /// the background entitlement leaves the foreground.
    ///
    /// #### Parameters
    ///
    /// - `sessionHandle`: the session to suspend
    public void suspendSession(int sessionHandle) {
        SimSession s = sessions.get(Integer.valueOf(sessionHandle));
        if (s != null && s.running && !s.suspended) {
            s.suspended = true;
            RangingSession.deliverSuspended(sessionHandle);
        }
    }

    /// Resumes a suspended session.
    ///
    /// #### Parameters
    ///
    /// - `sessionHandle`: the session to resume
    public void resumeSession(int sessionHandle) {
        SimSession s = sessions.get(Integer.valueOf(sessionHandle));
        if (s != null && s.suspended) {
            s.suspended = false;
            RangingSession.deliverResumed(sessionHandle);
            tick(s);
        }
    }

    /// The handles of every session this bridge currently holds, so the
    /// simulator panel can list them.
    ///
    /// #### Returns
    ///
    /// the handles, never null
    public int[] getSessionHandles() {
        int[] out = new int[sessions.size()];
        int i = 0;
        for (Integer k : sessions.keySet()) {
            out[i++] = k.intValue();
        }
        return out;
    }

    /// The last distance a session reported, in meters, or -1 when it has
    /// not started.
    ///
    /// #### Parameters
    ///
    /// - `sessionHandle`: the session to inspect
    ///
    /// #### Returns
    ///
    /// the distance in meters, or -1
    public double getSimulatedDistance(int sessionHandle) {
        SimSession s = sessions.get(Integer.valueOf(sessionHandle));
        return s == null || !s.running ? -1 : s.distance;
    }

    /// Moves a session's peer to an exact distance, so a test or the
    /// simulator panel can drive the value rather than watch it wander.
    ///
    /// #### Parameters
    ///
    /// - `sessionHandle`: the session to move
    /// - `meters`: where to put the peer
    public void setSimulatedDistance(int sessionHandle, double meters) {
        SimSession s = sessions.get(Integer.valueOf(sessionHandle));
        if (s != null) {
            s.distance = clamp(meters, MIN_DISTANCE, MAX_DISTANCE);
        }
    }

    // ------------------------------------------------------------------
    // Shared
    // ------------------------------------------------------------------

    @Override
    public boolean isRangingSupported() {
        return true;
    }

    @Override
    public boolean isCompanionSupported() {
        return true;
    }

    @Override
    public boolean isTransportSupported() {
        return true;
    }

    @Override
    public int getRangingAvailability() {
        return NearbyAvailability.LOCAL_ONLY.ordinal();
    }

    @Override
    public int getCompanionAvailability() {
        return NearbyAvailability.LOCAL_ONLY.ordinal();
    }

    @Override
    public int getTransportAvailability() {
        return NearbyAvailability.LOCAL_ONLY.ordinal();
    }

    @Override
    public void requestPermissions(final int requestId, int permissionBits) {
        // Nothing to ask a desktop for, but the answer still has to arrive
        // asynchronously: an app whose permission callback runs inline here
        // and out-of-line on a device is an app with a startup race.
        answer(new PermissionAnswer(requestId));
    }

    // ------------------------------------------------------------------
    // Ranging
    // ------------------------------------------------------------------

    @Override
    public int getRangingCapabilities() {
        return CAPABILITY_DISTANCE | CAPABILITY_DIRECTION
                | CAPABILITY_ELEVATION | CAPABILITY_ACCESSORY;
    }

    @Override
    public void prepareRangingSession(final int requestId,
            final int sessionHandle, final boolean controller) {
        final SimSession s = new SimSession(sessionHandle, controller,
                ++sessionSequence);
        sessions.put(Integer.valueOf(sessionHandle), s);
        answer(new SessionPrepared(requestId, sessionHandle, controller, s));
    }

    @Override
    public void startRanging(final int requestId, final int sessionHandle,
            final byte[] peerToken) {
        final SimSession s = sessions.get(Integer.valueOf(sessionHandle));
        if (s == null) {
            failRanging(requestId, NearbyError.SESSION_INVALIDATED,
                    "no such session");
            return;
        }
        int platform;
        try {
            platform = RangingToken.fromByteArray(peerToken).getPlatform();
        } catch (IllegalArgumentException e) {
            failRanging(requestId, NearbyError.INVALID_TOKEN, e.getMessage());
            return;
        }
        if (platform != RangingToken.PLATFORM_SIMULATED) {
            // Worth rejecting rather than pretending: an app that got its
            // token exchange backwards should find out here, on the desktop,
            // rather than on a device where the failure looks like hardware.
            failRanging(requestId, NearbyError.INVALID_TOKEN,
                    "this token was minted by another platform");
            return;
        }
        answer(new Runnable() {
            @Override
            public void run() {
                s.running = true;
                Ranging.deliverSessionStarted(requestId, sessionHandle);
                tick(s);
            }
        });
    }

    @Override
    public void startAccessoryRanging(final int requestId,
            final int sessionHandle, byte[] accessoryData) {
        final SimSession s = sessions.get(Integer.valueOf(sessionHandle));
        if (s == null) {
            failRanging(requestId, NearbyError.SESSION_INVALIDATED,
                    "no such session");
            return;
        }
        answer(new Runnable() {
            @Override
            public void run() {
                s.running = true;
                // A real accessory handshake sends configuration back; the
                // shape of the exchange is what an app has to get right, so
                // the simulation produces a non-empty blob rather than an
                // empty one an app could forget to forward.
                Ranging.deliverAccessoryConfiguration(requestId, sessionHandle,
                        new byte[] {'C', 'N', '1', 'A', 'C', 'C'});
                tick(s);
            }
        });
    }

    @Override
    public void stopRangingSession(int sessionHandle) {
        SimSession s = sessions.remove(Integer.valueOf(sessionHandle));
        if (s != null) {
            s.running = false;
        }
    }

    // ------------------------------------------------------------------
    // Companion
    // ------------------------------------------------------------------

    @Override
    public void associate(final int requestId, final int profile,
            boolean singleDevice, final String[] filters) {
        answer(new Runnable() {
            @Override
            public void run() {
                Candidate c = firstMatch(filters);
                if (c == null) {
                    // No candidate is the simulated equivalent of the user
                    // finding nothing they recognise and closing the sheet.
                    CompanionDevices.deliverRequestFailed(requestId,
                            NearbyError.USER_CANCELED.ordinal(),
                            "no simulated device matched the filters");
                    return;
                }
                String id = "sim-assoc-" + (nextAssociationId++);
                CompanionDevice d = new CompanionDevice(id, c.name, c.address,
                        NearbyWire.profileFor(profile), true);
                associations.put(id, d);
                CompanionDevices.deliverAssociated(requestId,
                        NearbyWire.encodeCompanionDevice(d));
            }
        });
    }

    @Override
    public String[] getAssociations() {
        String[] out = new String[associations.size()];
        int i = 0;
        for (CompanionDevice d : associations.values()) {
            out[i++] = NearbyWire.encodeCompanionDevice(d);
        }
        return out;
    }

    @Override
    public void disassociate(final int requestId, final String associationId) {
        answer(new Runnable() {
            @Override
            public void run() {
                observed.remove(associationId);
                if (associations.remove(associationId) == null) {
                    CompanionDevices.deliverRequestFailed(requestId,
                            NearbyError.PEER_UNAVAILABLE.ordinal(),
                            "no such association");
                } else {
                    CompanionDevices.deliverDisassociated(requestId);
                }
            }
        });
    }

    @Override
    public boolean startObservingPresence(String associationId) {
        if (!associations.containsKey(associationId)) {
            return false;
        }
        observed.put(associationId, Boolean.TRUE);
        return true;
    }

    @Override
    public void stopObservingPresence(String associationId) {
        observed.remove(associationId);
    }

    /// Reports an observed association as appearing or disappearing, which
    /// on a device is the platform waking the app.
    ///
    /// #### Parameters
    ///
    /// - `associationId`: the association to move
    /// - `present`: whether it is now in range
    public void setPresent(String associationId, boolean present) {
        CompanionDevice d = associations.get(associationId);
        if (d == null || !Boolean.TRUE.equals(observed.get(associationId))) {
            return;
        }
        CompanionDevice moved = new CompanionDevice(d.getId(),
                d.getDisplayName(), d.getAddress(), d.getProfile(), present);
        associations.put(associationId, moved);
        CompanionDevices.deliverPresenceChanged(
                NearbyWire.encodeCompanionDevice(moved), present);
    }

    // ------------------------------------------------------------------
    // Transport
    // ------------------------------------------------------------------

    @Override
    public int getMaxPayloadSize() {
        // What Nearby Connections allows for a BYTES payload. Matching the
        // tighter of the two real limits means an app that fits here fits
        // everywhere.
        return 32 * 1024;
    }

    @Override
    public void startAdvertising(final int requestId, String serviceId,
            String localName, int strategy) {
        advertising = true;
        answerOk(requestId);
    }

    @Override
    public void stopAdvertising() {
        advertising = false;
    }

    @Override
    public void startDiscovery(final int requestId, final String serviceId,
            int strategy) {
        discovering = true;
        answer(new Runnable() {
            @Override
            public void run() {
                NearbyTransport.deliverRequestOk(requestId);
                for (SimEndpoint e : endpoints) {
                    e.serviceId = serviceId;
                    NearbyTransport.deliverEndpointFound(e.encode(), true);
                }
            }
        });
    }

    @Override
    public void stopDiscovery() {
        discovering = false;
    }

    @Override
    public void requestConnection(final int requestId, final String endpointId,
            String localName) {
        final SimEndpoint e = findEndpoint(endpointId);
        if (e == null) {
            answer(new TransportFailure(requestId,
                    NearbyError.PEER_UNAVAILABLE, "no such endpoint"));
            return;
        }
        answer(new Runnable() {
            @Override
            public void run() {
                NearbyTransport.deliverRequestOk(requestId);
                // The simulated peer always accepts, one hop later, so the
                // app sees the two-step shape the real platforms have.
                answer(new Runnable() {
                    @Override
                    public void run() {
                        connected.add(endpointId);
                        NearbyTransport.deliverConnectionResult(e.encode(),
                                true, 0, null);
                    }
                });
            }
        });
    }

    @Override
    public void acceptConnection(final int requestId, String endpointId) {
        if (!connected.contains(endpointId)) {
            connected.add(endpointId);
        }
        answerOk(requestId);
    }

    @Override
    public void rejectConnection(String endpointId) {
        connected.remove(endpointId);
    }

    @Override
    public void sendPayload(final int requestId, final String[] endpointIds,
            final int payloadId, final int payloadType, final byte[] bytes,
            final String path) {
        answer(new Runnable() {
            @Override
            public void run() {
                NearbyTransport.deliverRequestOk(requestId);
                for (String endpointId : endpointIds) {
                    final SimEndpoint e = findEndpoint(endpointId);
                    if (e == null || !connected.contains(endpointId)) {
                        continue;
                    }
                    long total = payloadType == PAYLOAD_BYTES && bytes != null
                            ? bytes.length : -1;
                    NearbyTransport.deliverPayloadProgress(e.encode(),
                            payloadId, total < 0 ? 0 : total, total,
                            PayloadStatus.SUCCESS.ordinal());
                    if (echoPayloads) {
                        NearbyTransport.deliverPayloadReceived(e.encode(),
                                payloadId, payloadType, bytes, path);
                    }
                }
            }
        });
    }

    @Override
    public void cancelPayload(int payloadId) {
    }

    @Override
    public void disconnect(String endpointId) {
        if (connected.remove(endpointId)) {
            SimEndpoint e = findEndpoint(endpointId);
            if (e != null) {
                NearbyTransport.deliverDisconnected(e.encode());
            }
        }
    }

    @Override
    public void stopAllTransport() {
        advertising = false;
        discovering = false;
        List<String> doomed = new ArrayList<String>(connected);
        connected.clear();
        for (String id : doomed) {
            SimEndpoint e = findEndpoint(id);
            if (e != null) {
                NearbyTransport.deliverDisconnected(e.encode());
            }
        }
    }

    /// Whether [#startAdvertising] is in effect, for the simulator panel.
    public boolean isAdvertising() {
        return advertising;
    }

    /// Whether [#startDiscovery] is in effect, for the simulator panel.
    public boolean isDiscovering() {
        return discovering;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private void tick(final SimSession s) {
        if (!s.running || s.suspended
                || !sessions.containsKey(Integer.valueOf(s.handle))) {
            return;
        }
        s.advance();
        boolean hasDirection = s.distance < 9.0;
        RangingSession.deliverUpdate(s.handle, true, s.distance,
                hasDirection, s.azimuth, hasDirection, s.elevation,
                hasDirection ? s.vector() : null);
        if (!Display.isInitialized()) {
            // No event loop, so [#later] would run the next tick inline and
            // this method would recurse until the stack ran out. One
            // measurement per trigger is the honest behaviour for a unit
            // test; drive more with [#resumeSession].
            return;
        }
        later(TICK_MILLIS, new Runnable() {
            @Override
            public void run() {
                tick(s);
            }
        });
    }

    private SimEndpoint findEndpoint(String id) {
        for (SimEndpoint e : endpoints) {
            if (e.id.equals(id)) {
                return e;
            }
        }
        return null;
    }

    private Candidate firstMatch(String[] filters) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (filters == null || filters.length == 0) {
            return candidates.get(0);
        }
        for (String filter : filters) {
            String[] f = NearbyWire.split(filter);
            int kind = NearbyWire.integer(f, 0, -1);
            String value = NearbyWire.field(f, 1);
            for (Candidate c : candidates) {
                if (c.matches(kind, value)) {
                    return c;
                }
            }
        }
        return null;
    }

    private void failRanging(int requestId, NearbyError error,
            String message) {
        answer(new RangingFailure(requestId, error, message));
    }

    private void answerOk(int requestId) {
        answer(new TransportOk(requestId));
    }

    private void answer(Runnable delivery) {
        later(LATENCY_MILLIS, delivery);
    }

    private void later(int millis, Runnable delivery) {
        if (Display.isInitialized()) {
            Display.getInstance().setTimeout(millis, delivery);
            return;
        }
        // No Display, so this is a unit test driving the bridge directly.
        // Inline is the only option and is safe there: the EDT contract the
        // delay protects is about a running application.
        delivery.run();
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // ------------------------------------------------------------------
    // model records
    // ------------------------------------------------------------------

    /// The deliveries that carry nothing but their arguments.
    ///
    /// Named static classes rather than anonymous ones: an anonymous class
    /// holds a reference to the bridge whether or not it uses one, and these
    /// sit on a timer queue where that reference keeps the whole simulated
    /// world alive for as long as the delivery is pending.
    private static final class PermissionAnswer implements Runnable {
        private final int requestId;

        private PermissionAnswer(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void run() {
            Ranging.deliverPermissionResult(requestId, true);
        }
    }

    private static final class SessionPrepared implements Runnable {
        private final int requestId;
        private final int sessionHandle;
        private final boolean controller;
        private final SimSession session;

        private SessionPrepared(int requestId, int sessionHandle,
                boolean controller, SimSession session) {
            this.requestId = requestId;
            this.sessionHandle = sessionHandle;
            this.controller = controller;
            this.session = session;
        }

        @Override
        public void run() {
            Ranging.deliverSessionPrepared(requestId, sessionHandle,
                    controller, RangingToken.PLATFORM_SIMULATED,
                    session.localTokenPayload());
        }
    }

    private static final class RangingFailure implements Runnable {
        private final int requestId;
        private final NearbyError error;
        private final String message;

        private RangingFailure(int requestId, NearbyError error,
                String message) {
            this.requestId = requestId;
            this.error = error;
            this.message = message;
        }

        @Override
        public void run() {
            Ranging.deliverRequestFailed(requestId, error.ordinal(), message);
        }
    }

    private static final class TransportOk implements Runnable {
        private final int requestId;

        private TransportOk(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void run() {
            NearbyTransport.deliverRequestOk(requestId);
        }
    }

    private static final class TransportFailure implements Runnable {
        private final int requestId;
        private final NearbyError error;
        private final String message;

        private TransportFailure(int requestId, NearbyError error,
                String message) {
            this.requestId = requestId;
            this.error = error;
            this.message = message;
        }

        @Override
        public void run() {
            NearbyTransport.deliverRequestFailed(requestId, error.ordinal(),
                    message);
        }
    }

    private static final class SimSession {
        private final int handle;
        private final boolean controller;
        private boolean running;
        private boolean suspended;
        private double distance = 2.5;
        private double azimuth;
        private double elevation;
        private long seed;

        private SimSession(int handle, boolean controller, int sequence) {
            this.handle = handle;
            this.controller = controller;
            // Seeded from this bridge's own session counter, NOT from the
            // handle. Handles come from a process-wide counter that keeps
            // climbing, so seeding on one would make the first session of a
            // fresh bridge walk differently depending on what ran before it
            // -- which is exactly the order-dependence a reproducible
            // simulation exists to avoid. Counting per bridge means the Nth
            // session of a new LocalNearbyBridge always walks the same path.
            this.seed = 0x5DEECE66DL ^ (sequence * 2654435761L);
        }

        private byte[] localTokenPayload() {
            String s = "sim-peer-" + handle + (controller ? "-c" : "-e");
            byte[] out = new byte[s.length()];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) s.charAt(i);
            }
            return out;
        }

        /// One step of a bounded random walk, reflecting off the ends so the
        /// peer never sticks to a boundary the way a clamp would make it.
        private void advance() {
            distance = reflect(distance + next() * 0.22,
                    MIN_DISTANCE, MAX_DISTANCE);
            azimuth = wrap(azimuth + next() * 7.0);
            elevation = reflect(elevation + next() * 3.0, -40.0, 40.0);
        }

        private float[] vector() {
            double az = azimuth * Math.PI / 180.0;
            double el = elevation * Math.PI / 180.0;
            double cosEl = Math.cos(el);
            // x right, y up, z toward the viewer: the same frame iOS uses,
            // so an app reading the vector sees the same thing on both.
            return new float[] {
                (float) (cosEl * Math.sin(az)),
                (float) Math.sin(el),
                (float) (-cosEl * Math.cos(az))
            };
        }

        /// A value in -1..1 from a linear congruential generator. Not a good
        /// source of randomness and not trying to be: it is deterministic,
        /// dependency-free and identical on every platform, which is what a
        /// reproducible simulation needs.
        private double next() {
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
            return ((int) (seed >>> 20) % 2001 - 1000) / 1000.0;
        }

        private static double reflect(double v, double lo, double hi) {
            if (v < lo) {
                return lo + (lo - v);
            }
            if (v > hi) {
                return hi - (v - hi);
            }
            return v;
        }

        /// Folds an angle into -180..180.
        ///
        /// By remainder rather than by subtracting in a loop: a loop counted
        /// on a double is both slower for a large input and a correctness
        /// smell, because the step never lands exactly on the bound.
        private static double wrap(double deg) {
            double d = deg % 360.0;
            if (d > 180.0) {
                return d - 360.0;
            }
            if (d < -180.0) {
                return d + 360.0;
            }
            return d;
        }
    }

    /// A device the chooser may offer.
    ///
    /// Deliberately carries no profile of its own: on both platforms the
    /// association is created under the profile the *request* asked for, not
    /// one the device advertises, so a profile here would be a field that
    /// looked authoritative and decided nothing.
    private static final class Candidate {
        private final String name;
        private final String address;
        private final String serviceUuid;

        private Candidate(String name, String address, String serviceUuid) {
            this.name = name;
            this.address = address;
            this.serviceUuid = serviceUuid;
        }

        private boolean matches(int kind, String value) {
            if (kind == DeviceFilter.KIND_BLE_SERVICE) {
                return serviceUuid != null
                        && serviceUuid.equalsIgnoreCase(value);
            }
            if (kind == DeviceFilter.KIND_ADDRESS) {
                return address != null && address.equalsIgnoreCase(value);
            }
            if (kind == DeviceFilter.KIND_NAME_PATTERN) {
                // Substring rather than a regular expression: the simulation
                // must not be more capable than the weakest real backend,
                // which is what AccessorySetupKit gives on iOS.
                return name != null && value != null
                        && name.toLowerCase().indexOf(value.toLowerCase()) >= 0;
            }
            return false;
        }
    }

    private static final class SimEndpoint {
        private final String id;
        private final String name;
        private String serviceId = "";

        private SimEndpoint(String id, String name) {
            this.id = id;
            this.name = name;
        }

        private String encode() {
            return NearbyWire.join(new String[] {id, name, serviceId});
        }
    }
}
