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
import com.codename1.nearby.transport.TransportStrategy;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    /// Endpoints whose connection requests were rejected. Recorded so a
    /// test can tell an immediate refusal from silence.
    private final List<String> rejected = new ArrayList<String>();
    /// Endpoints invited and not yet answered.
    ///
    /// Counted with the connected ones when a strategy limit is enforced.
    /// Two requestConnection calls made before the first delayed acceptance
    /// ran both saw an empty connected list, so the simulator established two
    /// connections the real ports refuse -- which is exactly the topology bug
    /// a simulator exists to surface rather than hide.
    private final List<String> connecting = new ArrayList<String>();
    /// The topology each half was started with, as a TransportStrategy
    /// ordinal. CLUSTER is the default, which is also what a caller that
    /// passed no strategy is given.
    private int advertiseStrategy = TransportStrategy.CLUSTER.ordinal();
    private int discoverStrategy = TransportStrategy.CLUSTER.ordinal();
    /// Bumped when connections are dropped, so work queued by an earlier run
    /// of the transport can tell that it is answering for a transport that
    /// has since been stopped. Nothing in the simulation completes inline,
    /// which is the point -- and that means a delayed acceptance really can
    /// outlive the stop() that was supposed to have ended it.
    ///
    /// One counter per operation, because advertising, discovery and
    /// connections are independent. A single shared counter meant
    /// stopAdvertising() invalidated an unrelated discovery that was still
    /// starting, and an in-flight connection request with it -- failing calls
    /// the app never asked to stop.
    private int transportGeneration;
    private int discoverGeneration;
    private int advertiseGeneration;
    /// The id of the most recent acceptConnection, so a test can answer it
    /// the way a port would.
    ///
    /// @hidden not part of the public API; test-only.
    private int lastAcceptRequestId;
    /// Payload ids the app has cancelled, so a delivery already queued for
    /// one can report CANCELED instead of SUCCESS.
    private final Set<Integer> cancelledPayloads = new HashSet<Integer>();
    /// Payload id to the number of queued sends still carrying it.
    ///
    /// A cancel is only recorded for an id that is in here. Recorded
    /// unconditionally, a cancel for a transfer that had already completed --
    /// or an id that was never sent -- sat in the set for good, and reusing
    /// the same immutable Payload in a later send() consumed the stale marker
    /// and reported that perfectly good transfer as CANCELED.
    ///
    /// A count rather than a set, because the same Payload can be handed to
    /// several send() calls at once: one portable id, several pending sends,
    /// and a cancel has to stay in force until the last of them settles.
    private final Map<Integer, Integer> pendingPayloads =
            new HashMap<Integer, Integer>();
    /// Where delayed deliveries go while a test drives the clock, or null in
    /// normal operation.
    ///
    /// @hidden not part of the public API; test-only.
    private List<Runnable> deferred;

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
        // Nearby Connections allows 32K for a BYTES payload, and the Android
        // transport spends four of those bytes on the payload-id header it
        // frames in -- so 32764 is what an app may actually send there, and
        // the tightest of the real backends is the only honest number for a
        // simulator to advertise. Reporting the raw 32768 let an app size
        // itself against the simulator, pass, and then be refused on the
        // first device it ran on.
        return 32 * 1024 - 4;
    }

    @Override
    public void startAdvertising(final int requestId, String serviceId,
            String localName, int strategy) {
        advertising = true;
        advertiseStrategy = strategy;
        final int generation = advertiseGeneration;
        answer(new Runnable() {
            @Override
            public void run() {
                // Stopped before the start was answered, the same race
                // discovery has: the answer is queued, and a stop can land
                // in front of it.
                if (!advertising || generation != advertiseGeneration) {
                    NearbyTransport.deliverRequestFailed(requestId,
                            NearbyError.SESSION_INVALIDATED.ordinal(),
                            "advertising was stopped before it started");
                    return;
                }
                NearbyTransport.deliverRequestOk(requestId);
            }
        });
    }

    @Override
    public void stopAdvertising() {
        advertising = false;
        advertiseGeneration++;
    }

    @Override
    public void startDiscovery(final int requestId, final String serviceId,
            int strategy) {
        discovering = true;
        discoverStrategy = strategy;
        final int generation = discoverGeneration;
        answer(new Runnable() {
            @Override
            public void run() {
                // A stop between the call and this hop means there is no
                // discovery to report into. Reporting endpoints anyway had
                // the stopped simulator announcing peers nobody had asked
                // for, which is not what a device does.
                if (!discovering || generation != discoverGeneration) {
                    NearbyTransport.deliverRequestFailed(requestId,
                            NearbyError.SESSION_INVALIDATED.ordinal(),
                            "discovery was stopped before it started");
                    return;
                }
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
        // Bumped so a start still queued for this run of discovery can tell
        // that it has been stopped -- and only THIS counter, so an unrelated
        // advertise or connection in flight is left alone.
        discoverGeneration++;
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
        // The simulation refuses what the real platforms refuse. This device
        // is the one CONNECTING, and both STAR and POINT_TO_POINT allow it
        // exactly one peer -- under STAR it is one of the many, not the
        // centre. A simulator that let an app hold three connections under
        // POINT_TO_POINT would teach it a topology no device will honour.
        if (discoverStrategy != TransportStrategy.CLUSTER.ordinal()
                && !(connected.isEmpty() && connecting.isEmpty())) {
            answer(new TransportFailure(requestId, NearbyError.BUSY,
                    "this strategy allows one connection at a time;"
                    + " disconnect the current peer first"));
            return;
        }
        // Reserved before the first hop is queued, released when the request
        // settles either way.
        connecting.add(endpointId);
        final int generation = transportGeneration;
        answer(new Runnable() {
            @Override
            public void run() {
                if (generation != transportGeneration) {
                    // Failed, not dropped. Returning silently left the
                    // caller's AsyncResource pending for good -- a resource
                    // that never settles is worse than one that fails, which
                    // is the whole reason EdtResult exists.
                    connecting.remove(endpointId);
                    NearbyTransport.deliverRequestFailed(requestId,
                            NearbyError.SESSION_INVALIDATED.ordinal(),
                            "the transport was stopped before the connection"
                            + " was answered");
                    return;
                }
                NearbyTransport.deliverRequestOk(requestId);
                // The simulated peer always accepts, one hop later, so the
                // app sees the two-step shape the real platforms have.
                answer(new Runnable() {
                    @Override
                    public void run() {
                        // Checked again here, because THIS is the hop that
                        // outlives a stop(): the acceptance was already
                        // queued when the app stopped the transport, and
                        // adding the endpoint then reported a connection on
                        // a transport that had been stopped and never
                        // restarted.
                        //
                        // The reservation is also the claim on this
                        // acceptance. disconnect() takes it away, and
                        // without that check the acceptance went on to
                        // connect an endpoint the app had explicitly
                        // disconnected -- so the simulator was the one place
                        // a disconnect could be undone by the connection it
                        // was cancelling.
                        boolean reserved = connecting.remove(endpointId);
                        if (!reserved || generation != transportGeneration) {
                            return;
                        }
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
        lastAcceptRequestId = requestId;
        // POINT_TO_POINT bounds the advertiser too -- one connection on each
        // side. STAR does not: accepting many is what makes this device the
        // centre of the star.
        if (advertiseStrategy == TransportStrategy.POINT_TO_POINT.ordinal()
                && !(connected.isEmpty() && connecting.isEmpty())
                && !connected.contains(endpointId)) {
            rejectConnection(endpointId);
            answer(new TransportFailure(requestId, NearbyError.BUSY,
                    "POINT_TO_POINT allows one connection at a time;"
                    + " disconnect the current peer first"));
            return;
        }
        if (!connected.contains(endpointId)) {
            connected.add(endpointId);
        }
        answerOk(requestId);
        // The lifecycle event, which on a real platform arrives from the
        // connection callback and here had no other source. accept()
        // documents its outcome as connected or connectionFailed, and
        // answering the request alone left a listener waiting for an event
        // that was never going to come.
        final String accepted = endpointId;
        answer(new Runnable() {
            @Override
            public void run() {
                SimEndpoint e = findEndpoint(accepted);
                if (e != null && connected.contains(accepted)) {
                    NearbyTransport.deliverConnectionResult(e.encode(), true,
                            0, null);
                }
            }
        });
    }

    @Override
    public void rejectConnection(String endpointId) {
        connected.remove(endpointId);
        if (endpointId != null && !rejected.contains(endpointId)) {
            rejected.add(endpointId);
        }
    }

    /// The endpoints whose connection requests were turned down, newest last.
    ///
    /// #### Returns
    ///
    /// the rejected endpoint ids, never null
    public List<String> getRejectedEndpoints() {
        return new ArrayList<String>(rejected);
    }

    @Override
    public void sendPayload(final int requestId, final String[] endpointIds,
            final int payloadId, final int payloadType, final byte[] bytes,
            final String path) {
        Integer key = Integer.valueOf(payloadId);
        Integer outstanding = pendingPayloads.get(key);
        pendingPayloads.put(key, Integer.valueOf(
                outstanding == null ? 1 : outstanding.intValue() + 1));
        answer(new Runnable() {
            @Override
            public void run() {
                // Nobody to send to is a failure, not a success with nothing
                // in it. Answering ok and then skipping every recipient left
                // the caller holding a resolved resource and waiting for a
                // terminal payloadProgress that could never come, which is
                // exactly the state transfer UI hangs on.
                // Settled: one fewer send carrying this id. The cancel
                // marker outlives it and is dropped with the last one.
                Integer id = Integer.valueOf(payloadId);
                // Read BEFORE the count is decremented: the marker is
                // dropped with the last pending send, and this may be it.
                boolean cancelled = cancelledPayloads.contains(id);
                Integer left = pendingPayloads.get(id);
                int remaining = left == null ? 0 : left.intValue() - 1;
                if (remaining > 0) {
                    pendingPayloads.put(id, Integer.valueOf(remaining));
                } else {
                    pendingPayloads.remove(id);
                    cancelledPayloads.remove(id);
                }
                // EVERY requested endpoint has to be available, not just
                // one. Skipping the unavailable ones and answering
                // successfully left the omitted recipient with neither
                // delivery nor failure -- and let a desktop test pass for a
                // send the real ports refuse. The iOS transport rejects the
                // same case.
                List<String> unavailable = new ArrayList<String>();
                for (String endpointId : endpointIds) {
                    if (findEndpoint(endpointId) == null
                            || !connected.contains(endpointId)) {
                        unavailable.add(endpointId);
                    }
                }
                if (!unavailable.isEmpty()) {
                    NearbyTransport.deliverRequestFailed(requestId,
                            NearbyError.PEER_UNAVAILABLE.ordinal(),
                            "these endpoints are not connected: "
                            + unavailable);
                    return;
                }
                NearbyTransport.deliverRequestOk(requestId);
                for (String endpointId : endpointIds) {
                    final SimEndpoint e = findEndpoint(endpointId);
                    long total = payloadType == PAYLOAD_BYTES && bytes != null
                            ? bytes.length : -1;
                    if (cancelled) {
                        NearbyTransport.deliverPayloadProgress(e.encode(),
                                payloadId, 0, total,
                                PayloadStatus.CANCELED.ordinal());
                        continue;
                    }
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
        // Cancellation is real here, not a no-op. sendPayload is delayed like
        // everything else in this bridge, so an app CAN cancel while a send
        // is still in flight -- and doing nothing meant the queued delivery
        // went on to report SUCCESS and echo the payload, so the simulator
        // was the one place the public cancellation contract was never
        // exercised.
        //
        // Only for a send that is actually pending: cancelling something that
        // has finished, or an id that was never sent, is a no-op on a real
        // platform and must not leave a marker behind here either.
        if (pendingPayloads.containsKey(Integer.valueOf(payloadId))) {
            cancelledPayloads.add(Integer.valueOf(payloadId));
        }
    }

    @Override
    public void disconnect(String endpointId) {
        if (connected.remove(endpointId)) {
            SimEndpoint e = findEndpoint(endpointId);
            if (e != null) {
                NearbyTransport.deliverDisconnected(e.encode());
            }
            return;
        }
        // Not connected YET. The acceptance is queued behind this call, and
        // taking its reservation is what stops it: the request itself has
        // already been answered, so what would otherwise arrive is a
        // connection the app asked to drop.
        if (connecting.remove(endpointId)) {
            SimEndpoint e = findEndpoint(endpointId);
            if (e != null) {
                // Answered rather than dropped, so nothing waiting on the
                // connection outcome waits for good.
                NearbyTransport.deliverConnectionResult(e.encode(), false,
                        NearbyError.SESSION_INVALIDATED.ordinal(),
                        "the connection was disconnected before it"
                        + " completed");
            }
        }
    }

    @Override
    public void stopAllTransport() {
        advertising = false;
        discovering = false;
        // All three: stop() ends every operation, so anything queued for any
        // of them is answering for a transport that no longer exists.
        transportGeneration++;
        discoverGeneration++;
        advertiseGeneration++;
        connecting.clear();
        cancelledPayloads.clear();
        pendingPayloads.clear();
        List<String> doomed = new ArrayList<String>(connected);
        connected.clear();
        for (String id : doomed) {
            SimEndpoint e = findEndpoint(id);
            if (e != null) {
                NearbyTransport.deliverDisconnected(e.encode());
            }
        }
    }

    /// Parks every delayed delivery in `sink` instead of running it, so a
    /// test can decide when each one lands.
    ///
    /// Without a Display there is no timer, so deliveries otherwise run
    /// inline and no test can put anything BETWEEN the two hops of a
    /// simulated connection -- which is exactly where the interesting races
    /// are.
    ///
    /// @hidden not part of the public API; test-only.
    ///
    /// #### Parameters
    ///
    /// - `sink`: where to park deliveries, or null to run them as usual
    public void deferForTest(List<Runnable> sink) {
        deferred = sink;
    }

    /// The request id of the most recent [#acceptConnection].
    ///
    /// @hidden not part of the public API; test-only.
    ///
    /// #### Returns
    ///
    /// the id, or 0 when nothing has been accepted
    public int getLastAcceptRequestId() {
        return lastAcceptRequestId;
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
        List<Runnable> sink = deferred;
        if (sink != null) {
            // A test is driving the clock. Held until it says otherwise, so
            // the delayed ordering the simulation exists to reproduce can be
            // reproduced in a unit test too -- without a Display there is no
            // timer, and everything below runs inline.
            sink.add(delivery);
            return;
        }
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
