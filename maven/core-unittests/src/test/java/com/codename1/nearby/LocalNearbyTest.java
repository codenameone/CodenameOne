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
package com.codename1.nearby;

import com.codename1.impl.nearby.LocalNearbyBridge;
import com.codename1.util.AsyncResource;
import com.codename1.impl.nearby.NearbyRequests;
import com.codename1.impl.nearby.SyntheticNearby;
import com.codename1.nearby.companion.AssociationRequest;
import com.codename1.nearby.companion.CompanionDevice;
import com.codename1.nearby.companion.CompanionDevices;
import com.codename1.nearby.companion.CompanionProfile;
import com.codename1.nearby.companion.DeviceFilter;
import com.codename1.nearby.companion.PresenceListener;
import com.codename1.nearby.ranging.Ranging;
import com.codename1.nearby.ranging.RangingCapabilities;
import com.codename1.nearby.ranging.RangingListener;
import com.codename1.nearby.ranging.RangingAdapter;
import com.codename1.nearby.ranging.RangingRemovalReason;
import com.codename1.nearby.ranging.RangingRole;
import com.codename1.nearby.ranging.RangingSession;
import com.codename1.nearby.ranging.RangingToken;
import com.codename1.nearby.ranging.RangingUnit;
import com.codename1.nearby.ranging.RangingUpdate;
import com.codename1.nearby.transport.Endpoint;
import com.codename1.nearby.transport.IncomingConnection;
import com.codename1.nearby.transport.NearbyTransport;
import com.codename1.nearby.transport.Payload;
import com.codename1.nearby.transport.PayloadStatus;
import com.codename1.nearby.transport.PayloadTransferUpdate;
import com.codename1.nearby.transport.TransportAdapter;
import com.codename1.nearby.transport.TransportStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.codename1.nearby.NearbyAwait.assertFailedWith;
import static com.codename1.nearby.NearbyAwait.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole stack against the simulated implementation: the three facades,
 * the wire codec, and the local bridge that backs the simulator, the desktop
 * ports and the JavaScript port.
 */
class LocalNearbyTest {

    private LocalNearbyBridge bridge;

    @BeforeEach
    void furnish() {
        bridge = new LocalNearbyBridge();
        SyntheticNearby.populate(bridge);
        NearbyRequests.resetForTest(bridge);
    }

    @AfterEach
    void clear() {
        NearbyRequests.resetForTest(null);
    }

    // ------------------------------------------------------------------
    // availability
    // ------------------------------------------------------------------

    @Test
    void everythingWorksAndSaysItIsNotReal() {
        assertTrue(Ranging.isSupported());
        assertTrue(CompanionDevices.isSupported());
        assertTrue(NearbyTransport.isSupported());
        // LOCAL_ONLY rather than AVAILABLE, so an app can tell the developer
        // the peers it is tracking exist only in this process.
        assertSame(NearbyAvailability.LOCAL_ONLY, Ranging.getAvailability());
        assertSame(NearbyAvailability.LOCAL_ONLY,
                CompanionDevices.getAvailability());
        assertSame(NearbyAvailability.LOCAL_ONLY,
                NearbyTransport.getAvailability());
    }

    @Test
    void capabilitiesReportWhatTheSimulationCanActuallyProduce() {
        RangingCapabilities c = Ranging.getCapabilities();
        assertTrue(c.isDistanceSupported());
        assertTrue(c.isDirectionSupported());
        assertTrue(c.isElevationSupported());
        assertTrue(c.isAccessoryRangingSupported());
        // Claimed by nothing here, because nothing here does them.
        assertFalse(c.isCameraAssistanceSupported());
        assertFalse(c.isBackgroundRangingSupported());
    }

    @Test
    void permissionsAreGrantedButNotInline() {
        assertTrue(value(Ranging.requestPermissions(NearbyPermission.RANGING))
                .booleanValue());
    }

    // ------------------------------------------------------------------
    // ranging
    // ------------------------------------------------------------------

    @Test
    void aPreparedSessionHasATokenAndIsNotYetRunning() {
        RangingSession s = value(Ranging.prepareSession(
                RangingRole.CONTROLLER));
        assertNotNull(s);
        assertSame(RangingRole.CONTROLLER, s.getRole());
        assertFalse(s.isRunning());
        RangingToken token = s.getLocalToken();
        assertNotNull(token);
        assertEquals(RangingToken.PLATFORM_SIMULATED, token.getPlatform());
        assertTrue(token.toByteArray().length > 0);
    }

    @Test
    void twoSessionsGetDifferentTokens() {
        RangingSession a = value(Ranging.prepareSession(RangingRole.CONTROLLER));
        RangingSession b = value(Ranging.prepareSession(RangingRole.CONTROLEE));
        assertFalse(a.getLocalToken().equals(b.getLocalToken()));
        assertSame(RangingRole.CONTROLEE, b.getRole());
    }

    @Test
    void startingASessionMakesItRunAndDeliverMeasurements() {
        RangingSession s = value(Ranging.prepareSession(
                RangingRole.CONTROLLER));
        final List<RangingUpdate> updates = new ArrayList<RangingUpdate>();
        s.addRangingListener(new RangingAdapter() {
            @Override
            public void updated(RangingUpdate u) {
                updates.add(u);
            }
        });
        RangingSession started = value(s.start(peerToken()));
        assertSame(s, started);
        assertTrue(s.isRunning());
        assertFalse(updates.isEmpty(),
                "a started session must produce a measurement");
        RangingUpdate first = updates.get(0);
        assertTrue(first.hasDistance());
        assertTrue(first.getDistance(RangingUnit.METERS) > 0);
        assertTrue(first.getTimestamp() > 0);
    }

    @Test
    void aDistanceReadsTheSameNumberInDifferentUnits() {
        RangingSession s = running();
        bridge.setSimulatedDistance(handleOf(), 2.0);
        final AtomicReference<RangingUpdate> last =
                new AtomicReference<RangingUpdate>();
        s.addRangingListener(new RangingAdapter() {
            @Override
            public void updated(RangingUpdate u) {
                last.set(u);
            }
        });
        nudge(handleOf());
        RangingUpdate u = last.get();
        assertNotNull(u);
        double meters = u.getDistance(RangingUnit.METERS);
        assertEquals(meters * 100.0,
                u.getDistance(RangingUnit.CENTIMETERS), 1e-9);
        assertEquals(meters / 0.3048, u.getDistance(RangingUnit.FEET), 1e-9);
        assertEquals(meters / 0.0254, u.getDistance(RangingUnit.INCHES), 1e-9);
    }

    @Test
    void aDirectionVectorAgreesWithTheAnglesDerivedFromIt() {
        RangingSession s = running();
        final AtomicReference<RangingUpdate> withDirection =
                new AtomicReference<RangingUpdate>();
        s.addRangingListener(new RangingAdapter() {
            @Override
            public void updated(RangingUpdate u) {
                if (u.hasDirection() && withDirection.get() == null) {
                    withDirection.set(u);
                }
            }
        });
        bridge.setSimulatedDistance(handleOf(), 1.0);
        nudge(handleOf());
        RangingUpdate u = withDirection.get();
        assertNotNull(u, "a peer one metre away must have a direction");
        float[] v = u.getDirectionVector();
        assertNotNull(v);
        assertEquals(3, v.length);
        double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        assertEquals(1.0, len, 1e-4, "the direction vector must be a unit"
                + " vector, because that is what iOS produces");
        // The frame is x right, y up, forward is negative z -- so the azimuth
        // the API reports must come back out of atan2(x, -z).
        double azimuth = Math.toDegrees(Math.atan2(v[0], -v[2]));
        assertEquals(u.getAzimuth(), azimuth, 1e-3);
        double elevation = Math.toDegrees(Math.asin(v[1]));
        assertEquals(u.getElevation(), elevation, 1e-3);
    }

    @Test
    void aTokenFromAnotherPlatformIsRejectedHereRatherThanOnTheDevice() {
        RangingSession s = value(Ranging.prepareSession(
                RangingRole.CONTROLLER));
        assertFailedWith(NearbyError.INVALID_TOKEN, s.start(
                RangingToken.forPayload(RangingToken.PLATFORM_APPLE_NI,
                        new byte[] {1, 2, 3})));
    }

    @Test
    void aMissingTokenFailsRatherThanReachingTheBridge() {
        RangingSession s = value(Ranging.prepareSession(
                RangingRole.CONTROLLER));
        assertFailedWith(NearbyError.INVALID_TOKEN, s.start(null));
    }

    @Test
    void aSecondStartOnARunningSessionIsRefusedRatherThanQueued() {
        RangingSession s = running();
        assertFailedWith(NearbyError.BUSY, s.start(peerToken()));
    }

    @Test
    void aStoppedSessionCannotBeRestarted() {
        RangingSession s = running();
        s.stop();
        assertFalse(s.isRunning());
        assertFailedWith(NearbyError.SESSION_INVALIDATED, s.start(peerToken()));
    }

    @Test
    void stoppingTwiceIsHarmless() {
        RangingSession s = running();
        s.stop();
        s.stop();
    }

    @Test
    void noListenerHearsAnythingAfterStop() {
        RangingSession s = running();
        final AtomicInteger seen = new AtomicInteger();
        s.addRangingListener(new RangingAdapter() {
            @Override
            public void updated(RangingUpdate u) {
                seen.incrementAndGet();
            }
        });
        // Read the handle first: stop() deregisters the session, which is
        // itself part of the contract being tested here.
        int handle = handleOf();
        s.stop();
        assertEquals(0, bridge.getSessionHandles().length);
        int before = seen.get();
        bridge.dropPeer(handle);
        assertEquals(before, seen.get());
    }

    @Test
    void anUpdateAlreadyOnItsWayCannotRestartAStoppedSession() {
        // A native update queued from a background thread can reach the EDT
        // after stop() ran there. Delivering it set running back to true on a
        // session isRunning() had already promised was finished, and notified
        // a listener registered after the stop.
        RangingSession s = running();
        int handle = handleOf();
        s.stop();
        assertFalse(s.isRunning());

        final AtomicInteger seen = new AtomicInteger();
        s.addRangingListener(new RangingAdapter() {
            @Override
            public void updated(RangingUpdate u) {
                seen.incrementAndGet();
            }
        });
        RangingSession.deliverUpdate(handle, true, 1.5, false, 0, false, 0,
                null);
        RangingSession.deliverSuspended(handle);
        RangingSession.deliverResumed(handle);
        RangingSession.deliverInvalidated(handle,
                NearbyError.SESSION_FAILED.ordinal(), "too late");

        assertEquals(0, seen.get());
        assertFalse(s.isRunning());
    }

    @Test
    void aPeerCanWalkAwayWithoutKillingTheSession() {
        RangingSession s = running();
        final AtomicReference<RangingRemovalReason> reason =
                new AtomicReference<RangingRemovalReason>();
        s.addRangingListener(new RangingAdapter() {
            @Override
            public void peerRemoved(RangingRemovalReason r) {
                reason.set(r);
            }
        });
        bridge.dropPeer(handleOf());
        assertSame(RangingRemovalReason.TIMEOUT, reason.get());
        assertTrue(s.isRunning(), "losing the peer does not end the session");
    }

    @Test
    void suspendAndResumeAreReportedAndStopTheMeasurements() {
        RangingSession s = running();
        final List<String> events = new ArrayList<String>();
        final AtomicInteger updates = new AtomicInteger();
        s.addRangingListener(new RangingAdapter() {
            @Override
            public void suspended() {
                events.add("suspended");
            }

            @Override
            public void resumed() {
                events.add("resumed");
            }

            @Override
            public void updated(RangingUpdate u) {
                updates.incrementAndGet();
            }
        });
        bridge.suspendSession(handleOf());
        assertEquals(1, events.size());
        assertEquals("suspended", events.get(0));
        assertFalse(s.isRunning());
        int whileSuspended = updates.get();
        bridge.suspendSession(handleOf());
        assertEquals(1, events.size(), "suspending twice reports once");
        assertEquals(whileSuspended, updates.get(),
                "a suspended session produces no measurements");

        bridge.resumeSession(handleOf());
        assertEquals(2, events.size());
        assertEquals("resumed", events.get(1));
        assertTrue(s.isRunning());
        assertTrue(updates.get() > whileSuspended);
    }

    @Test
    void anAccessorySessionAnswersWithTheBytesToSendBack() {
        RangingSession s = value(Ranging.prepareSession(
                RangingRole.CONTROLLER));
        byte[] shareable = value(s.startAccessory(
                new byte[] {1, 2, 3, 4}));
        assertNotNull(shareable);
        assertTrue(shareable.length > 0, "an app that forgets to forward this"
                + " should have something to forget");
        assertTrue(s.isRunning());
    }

    @Test
    void anEmptyAccessoryConfigurationIsRefused() {
        RangingSession s = value(Ranging.prepareSession(
                RangingRole.CONTROLLER));
        assertFailedWith(NearbyError.INVALID_TOKEN,
                s.startAccessory(new byte[0]));
        assertFailedWith(NearbyError.INVALID_TOKEN, s.startAccessory(null));
    }

    @Test
    void theWalkIsReproducibleRunToRun() {
        // A test that asserts on the tenth measurement has to get the same
        // tenth measurement every time, or the simulation is a flake factory.
        double[] first = walk();
        NearbyRequests.resetForTest(null);
        bridge = new LocalNearbyBridge();
        SyntheticNearby.populate(bridge);
        NearbyRequests.resetForTest(bridge);
        double[] second = walk();
        assertEquals(first.length, second.length);
        for (int i = 0; i < first.length; i++) {
            assertEquals(first[i], second[i], 1e-12,
                    "measurement " + i + " differed between runs");
        }
    }

    @Test
    void theWalkStaysInsideItsBoundsAndKeepsMoving() {
        double[] w = walk();
        boolean moved = false;
        for (int i = 0; i < w.length; i++) {
            assertTrue(w[i] > 0, "a distance is positive");
            assertTrue(w[i] <= 14.0, "the peer stays in the simulated room");
            if (i > 0 && Math.abs(w[i] - w[i - 1]) > 1e-9) {
                moved = true;
            }
        }
        assertTrue(moved, "a peer that never moves would let an app ship a"
                + " label that flickers unreadably against real hardware");
    }

    // ------------------------------------------------------------------
    // companion
    // ------------------------------------------------------------------

    @Test
    void associatingWithNoFilterOffersTheFirstCandidate() {
        CompanionDevice d = value(CompanionDevices.associate(
                new AssociationRequest.Builder()
                        .profile(CompanionProfile.WATCH).build()));
        assertEquals("Simulated Watch", d.getDisplayName());
        assertSame(CompanionProfile.WATCH, d.getProfile());
        assertNotNull(d.getId());
        assertEquals(1, CompanionDevices.getAssociations().size());
    }

    @Test
    void aServiceFilterPicksTheDeviceAdvertisingIt() {
        CompanionDevice d = value(CompanionDevices.associate(
                new AssociationRequest.Builder()
                        .addFilter(DeviceFilter.bleService(
                                SyntheticNearby.HEART_RATE_SERVICE))
                        .build()));
        assertEquals("Simulated Heart Rate Strap", d.getDisplayName());
    }

    @Test
    void aFilterThatMatchesNothingReadsAsTheUserWalkingAway() {
        // There is no other honest answer: the chooser had nothing to show,
        // so from the app's point of view the user closed it.
        assertFailedWith(NearbyError.USER_CANCELED,
                CompanionDevices.associate(new AssociationRequest.Builder()
                        .addFilter(DeviceFilter.bleService("FFFF"))
                        .build()));
    }

    @Test
    void anAssociationSurvivesUntilItIsDropped() {
        CompanionDevice d = value(CompanionDevices.associate(
                new AssociationRequest.Builder().build()));
        List<CompanionDevice> held = CompanionDevices.getAssociations();
        assertEquals(1, held.size());
        assertEquals(d, held.get(0));

        assertTrue(value(CompanionDevices.disassociate(d.getId()))
                .booleanValue());
        assertTrue(CompanionDevices.getAssociations().isEmpty());
        assertFailedWith(NearbyError.PEER_UNAVAILABLE,
                CompanionDevices.disassociate(d.getId()));
    }

    @Test
    void presenceIsOnlyReportedForAnObservedAssociation() {
        final CompanionDevice d = value(CompanionDevices.associate(
                new AssociationRequest.Builder().build()));
        final List<String> events = new ArrayList<String>();
        CompanionDevices.addPresenceListener(new PresenceListener() {
            @Override
            public void deviceAppeared(CompanionDevice device) {
                events.add("appeared:" + device.getId());
            }

            @Override
            public void deviceDisappeared(CompanionDevice device) {
                events.add("disappeared:" + device.getId());
            }
        });

        // Not observed yet, so nothing is reported.
        bridge.setPresent(d.getId(), false);
        assertTrue(events.isEmpty());

        assertTrue(CompanionDevices.startObservingPresence(d.getId()));
        bridge.setPresent(d.getId(), false);
        bridge.setPresent(d.getId(), true);
        assertEquals(2, events.size());
        assertEquals("disappeared:" + d.getId(), events.get(0));
        assertEquals("appeared:" + d.getId(), events.get(1));

        CompanionDevices.stopObservingPresence(d.getId());
        bridge.setPresent(d.getId(), false);
        assertEquals(2, events.size());
    }

    @Test
    void aPresenceEventThatBeatsTheListenerIsReplayedRatherThanLost() {
        // The whole point of companion association is that the platform can
        // start the process purely to deliver this, which on Android happens
        // in a process where the app's init() has not run and no listener
        // exists yet. Dispatched straight through, the wake-up would be lost.
        CompanionDevices.deliverPresenceChanged(
                "cold\tCold Watch\t\t0\t1", true);
        CompanionDevices.deliverPresenceChanged(
                "cold\tCold Watch\t\t0\t0", false);

        final List<String> events = new ArrayList<String>();
        CompanionDevices.addPresenceListener(new PresenceListener() {
            @Override
            public void deviceAppeared(CompanionDevice device) {
                events.add("appeared:" + device.getId());
            }

            @Override
            public void deviceDisappeared(CompanionDevice device) {
                events.add("disappeared:" + device.getId());
            }
        });

        assertEquals(2, events.size());
        assertEquals("appeared:cold", events.get(0));
        assertEquals("disappeared:cold", events.get(1));

        // Drained, not merely copied -- a second listener does not see the
        // backlog a third time.
        final List<String> later = new ArrayList<String>();
        CompanionDevices.addPresenceListener(new PresenceListener() {
            @Override
            public void deviceAppeared(CompanionDevice device) {
                later.add("appeared:" + device.getId());
            }

            @Override
            public void deviceDisappeared(CompanionDevice device) {
                later.add("disappeared:" + device.getId());
            }
        });
        assertTrue(later.isEmpty());
    }

    @Test
    void observingSomethingThatIsNotAssociatedIsRefused() {
        assertFalse(CompanionDevices.startObservingPresence("nope"));
    }

    // ------------------------------------------------------------------
    // transport
    // ------------------------------------------------------------------

    @Test
    void sendingToNobodyFailsRatherThanResolvingWithNothingInIt() {
        // Answering ok and then skipping every recipient left the caller
        // holding a resolved resource and waiting for a terminal
        // payloadProgress that could never come -- the state transfer UI
        // hangs on.
        final AtomicReference<Endpoint> found = new AtomicReference<Endpoint>();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void endpointFound(Endpoint e) {
                found.compareAndSet(null, e);
            }
        });
        value(NearbyTransport.startDiscovery("chat", TransportStrategy.STAR));
        Endpoint e = found.get();
        assertNotNull(e);
        // Discovered but never connected.
        assertFailedWith(NearbyError.PEER_UNAVAILABLE,
                NearbyTransport.send(e, Payload.fromBytes(new byte[] {1})));
    }

    @Test
    void cancellingAPayloadInFlightReportsCanceledAndSendsNothing() {
        // sendPayload is delayed like everything else here, so an app really
        // can cancel while a send is in flight -- and the simulator used to
        // ignore it, report SUCCESS and echo the payload anyway, which made
        // it the one place the public cancellation contract was never
        // exercised.
        final List<PayloadTransferUpdate> progress =
                new ArrayList<PayloadTransferUpdate>();
        final List<Payload> received = new ArrayList<Payload>();
        final AtomicReference<Endpoint> found = new AtomicReference<Endpoint>();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void endpointFound(Endpoint e) {
                found.compareAndSet(null, e);
            }

            @Override
            public void payloadProgress(Endpoint e, PayloadTransferUpdate u) {
                progress.add(u);
            }

            @Override
            public void payloadReceived(Endpoint e, Payload p) {
                received.add(p);
            }
        });
        value(NearbyTransport.startDiscovery("chat", TransportStrategy.STAR));
        Endpoint e = found.get();
        value(NearbyTransport.requestConnection(e, "me"));

        List<Runnable> queue = new ArrayList<Runnable>();
        bridge.deferForTest(queue);
        Payload p = Payload.fromBytes(new byte[] {1, 2, 3});
        NearbyTransport.send(e, p);
        NearbyTransport.cancel(p.getId());
        drain(queue);

        assertTrue(received.isEmpty(),
                "a cancelled payload must not be delivered: " + received);
        assertEquals(1, progress.size());
        assertSame(PayloadStatus.CANCELED, progress.get(0).getStatus());
    }

    @Test
    void stoppingBeforeDiscoveryStartsReportsNoEndpoints() {
        // The queued start had no idea discovery had been stopped, so a
        // stopped simulator announced peers nobody had asked for -- and
        // resolved the start as though discovery were running.
        final List<Endpoint> found = new ArrayList<Endpoint>();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void endpointFound(Endpoint e) {
                found.add(e);
            }
        });
        List<Runnable> queue = new ArrayList<Runnable>();
        bridge.deferForTest(queue);
        AsyncResource<Boolean> pending = NearbyTransport.startDiscovery("chat",
                TransportStrategy.CLUSTER);
        NearbyTransport.stop();
        drain(queue);

        assertTrue(found.isEmpty(),
                "a stopped simulator must not announce peers: " + found);
        assertFailedWith(NearbyError.SESSION_INVALIDATED, pending);
    }

    @Test
    void stoppingBeforeTheAcceptanceLandsLeavesTheTransportStopped() {
        // Nothing in the simulation completes inline, which is the point --
        // and it means the delayed acceptance really can outlive the stop()
        // that was supposed to have ended the transport. Adding the endpoint
        // then reported a connection on a transport nobody had restarted.
        final List<Endpoint> connected = new ArrayList<Endpoint>();
        final AtomicReference<Endpoint> found = new AtomicReference<Endpoint>();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void endpointFound(Endpoint e) {
                found.compareAndSet(null, e);
            }

            @Override
            public void connected(Endpoint e) {
                connected.add(e);
            }
        });
        value(NearbyTransport.startDiscovery("chat", TransportStrategy.STAR));
        Endpoint e = found.get();
        assertNotNull(e);

        // From here the test drives the clock, so a stop() can land between
        // the request and the acceptance the way it does on a real timer.
        List<Runnable> queue = new ArrayList<Runnable>();
        bridge.deferForTest(queue);
        AsyncResource<Boolean> pending = NearbyTransport.requestConnection(e,
                "me");
        NearbyTransport.stop();
        drain(queue);

        assertTrue(connected.isEmpty(),
                "a stopped transport must not connect: " + connected);
        // Failed rather than left hanging: a resource that never settles is
        // worse than one that fails.
        assertFailedWith(NearbyError.SESSION_INVALIDATED, pending);
    }

    @Test
    void anAcceptanceThatBeatsTheStopStillConnects() {
        // The other side of the same guard: a connection that completed
        // before the stop is a real connection, not one to suppress.
        final List<Endpoint> connected = new ArrayList<Endpoint>();
        final AtomicReference<Endpoint> found = new AtomicReference<Endpoint>();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void endpointFound(Endpoint e) {
                found.compareAndSet(null, e);
            }

            @Override
            public void connected(Endpoint e) {
                connected.add(e);
            }
        });
        value(NearbyTransport.startDiscovery("chat", TransportStrategy.STAR));
        Endpoint e = found.get();

        List<Runnable> queue = new ArrayList<Runnable>();
        bridge.deferForTest(queue);
        NearbyTransport.requestConnection(e, "me");
        drain(queue);
        assertEquals(1, connected.size());
    }

    /// Runs every parked delivery, including any the deliveries themselves
    /// park, until nothing is left.
    private static void drain(List<Runnable> queue) {
        while (!queue.isEmpty()) {
            Runnable next = queue.remove(0);
            next.run();
        }
    }

    @Test
    void pointToPointRefusesASecondConnectionInsteadOfAllowingIt() {
        // TransportStrategy documents "exactly one connection on each side",
        // and a simulator that let an app hold three would teach it a
        // topology no device will honour.
        List<Endpoint> found = discoverAll(TransportStrategy.POINT_TO_POINT);
        assertTrue(found.size() >= 2, "need two synthetic peers to test this");
        assertTrue(value(NearbyTransport.requestConnection(found.get(0), "me"))
                .booleanValue());
        assertFailedWith(NearbyError.BUSY,
                NearbyTransport.requestConnection(found.get(1), "me"));
    }

    @Test
    void clusterAllowsTheSecondConnectionPointToPointRefuses() {
        List<Endpoint> found = discoverAll(TransportStrategy.CLUSTER);
        assertTrue(value(NearbyTransport.requestConnection(found.get(0), "me"))
                .booleanValue());
        assertTrue(value(NearbyTransport.requestConnection(found.get(1), "me"))
                .booleanValue());
    }

    /// Starts discovery with a strategy and hands back every endpoint it saw.
    private List<Endpoint> discoverAll(TransportStrategy strategy) {
        final List<Endpoint> found = new ArrayList<Endpoint>();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void endpointFound(Endpoint e) {
                found.add(e);
            }
        });
        value(NearbyTransport.startDiscovery("chat", strategy));
        return found;
    }

    @Test
    void aListenerMayAnswerAConnectionRequestAfterItReturns() {
        // The documented flow: show getAuthenticationToken() on both screens,
        // ask the user whether the two match, and accept when they say yes.
        // That cannot finish inside the callback, and rejecting a request the
        // listener had not answered YET made the later accept() a no-op --
        // so the one handshake worth trusting could never connect.
        final AtomicReference<IncomingConnection> held =
                new AtomicReference<IncomingConnection>();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void connectionRequested(IncomingConnection request) {
                held.set(request);
            }
        });
        NearbyTransport.deliverConnectionRequested(
                "peer-1\tA Phone\tchat", "1234");

        IncomingConnection r = held.get();
        assertNotNull(r);
        assertFalse(r.isAnswered(),
                "a listener that has not answered must not be answered for it");
        r.accept();
        assertTrue(r.isAnswered());
    }

    @Test
    void anAcceptanceThePlatformRefusesIsReportedAsAConnectionFailure() {
        // accept() returns void and its outcome is documented to arrive as
        // connected or connectionFailed, so there is no AsyncResource for a
        // port to fail. The port's failure used to be dropped: the id it
        // reported had no pending entry, so an acceptance the platform
        // refused produced no callback at all and the app waited forever.
        final AtomicReference<IncomingConnection> held =
                new AtomicReference<IncomingConnection>();
        final List<NearbyException> failures = new ArrayList<NearbyException>();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void connectionRequested(IncomingConnection request) {
                held.set(request);
            }

            @Override
            public void connectionFailed(Endpoint e, NearbyException error) {
                failures.add(error);
            }
        });
        NearbyTransport.deliverConnectionRequested(
                "peer-9\tA Phone\tchat", "4321");
        // The clock is held so the bridge's own success cannot settle the
        // request before the refusal below, which is the ordering a real
        // port produces: acceptConnection returns and fails later.
        List<Runnable> queue = new ArrayList<Runnable>();
        bridge.deferForTest(queue);
        held.get().accept();

        // The port refuses it after the fact, naming the request id it was
        // handed -- which is the id accept() recorded.
        NearbyTransport.deliverRequestFailed(bridge.getLastAcceptRequestId(),
                NearbyError.PEER_UNAVAILABLE.ordinal(), "it went away");
        assertEquals(1, failures.size());
        assertSame(NearbyError.PEER_UNAVAILABLE, failures.get(0).getError());
    }

    @Test
    void aConnectionRequestNobodyHeardIsRejectedRatherThanLeftHanging() {
        // With no listener at all nobody will ever answer, and the far side
        // would sit in its connecting state until it timed out.
        NearbyTransport.deliverConnectionRequested(
                "peer-2\tAnother Phone\tchat", "5678");
        assertTrue(bridge.getRejectedEndpoints().contains("peer-2"),
                "expected an immediate reject, got "
                        + bridge.getRejectedEndpoints());
    }

    @Test
    void discoveryFindsTheSyntheticEndpoints() {
        final List<Endpoint> found = new ArrayList<Endpoint>();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void endpointFound(Endpoint e) {
                found.add(e);
            }
        });
        assertTrue(value(NearbyTransport.startDiscovery("chat",
                TransportStrategy.CLUSTER)).booleanValue());
        assertEquals(2, found.size());
        assertEquals("chat", found.get(0).getServiceId());
        assertTrue(NearbyTransport.getMaxPayloadSize() > 0);
    }

    @Test
    void aConnectionOpensInTwoStepsTheWayARealOneDoes() {
        final List<Endpoint> connected = new ArrayList<Endpoint>();
        final AtomicReference<Endpoint> found = new AtomicReference<Endpoint>();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void endpointFound(Endpoint e) {
                found.compareAndSet(null, e);
            }

            @Override
            public void connected(Endpoint e) {
                connected.add(e);
            }
        });
        value(NearbyTransport.startDiscovery("chat", TransportStrategy.STAR));
        Endpoint e = found.get();
        assertNotNull(e);
        // Resolving means the request was sent, not that we are connected.
        assertTrue(value(NearbyTransport.requestConnection(e, "me"))
                .booleanValue());
        assertEquals(1, connected.size());
        assertEquals(e, connected.get(0));
    }

    @Test
    void aPayloadReportsProgressAndComesBackFromTheEcho() {
        final List<PayloadTransferUpdate> progress =
                new ArrayList<PayloadTransferUpdate>();
        final List<Payload> received = new ArrayList<Payload>();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void payloadProgress(Endpoint e,
                    PayloadTransferUpdate u) {
                progress.add(u);
            }

            @Override
            public void payloadReceived(Endpoint e, Payload p) {
                received.add(p);
            }
        });
        Endpoint e = connectedEndpoint();
        byte[] data = {1, 2, 3, 4, 5};
        assertTrue(value(NearbyTransport.send(e, Payload.fromBytes(data)))
                .booleanValue());
        assertEquals(1, progress.size());
        assertSame(PayloadStatus.SUCCESS, progress.get(0).getStatus());
        assertEquals(5L, progress.get(0).getTotalBytes());
        assertEquals(1, received.size());
        assertEquals(Payload.TYPE_BYTES, received.get(0).getType());
        assertEquals(5, received.get(0).getBytes().length);
    }

    @Test
    void theEchoCanBeTurnedOffForATestThatCountsDeliveries() {
        bridge.setEchoPayloads(false);
        final AtomicInteger received = new AtomicInteger();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void payloadReceived(Endpoint e, Payload p) {
                received.incrementAndGet();
            }
        });
        value(NearbyTransport.send(connectedEndpoint(),
                Payload.fromBytes(new byte[] {1})));
        assertEquals(0, received.get());
    }

    @Test
    void aBytedPayloadOverTheLimitIsRefusedBeforeItReachesTheRadio() {
        Endpoint e = connectedEndpoint();
        byte[] tooBig = new byte[NearbyTransport.getMaxPayloadSize() + 1];
        assertFailedWith(NearbyError.IO_ERROR,
                NearbyTransport.send(e, Payload.fromBytes(tooBig)));
    }

    @Test
    void sendingToNobodyIsRefused() {
        assertFailedWith(NearbyError.PEER_UNAVAILABLE,
                NearbyTransport.send(new Endpoint[0],
                        Payload.fromBytes(new byte[] {1})));
        assertFailedWith(NearbyError.PEER_UNAVAILABLE,
                NearbyTransport.send(new Endpoint[] {null},
                        Payload.fromBytes(new byte[] {1})));
    }

    @Test
    void connectingToAnEndpointThatIsNotThereFails() {
        assertFailedWith(NearbyError.PEER_UNAVAILABLE,
                NearbyTransport.requestConnection(
                        new Endpoint("ghost", "Ghost", "chat"), "me"));
    }

    @Test
    void disconnectingIsReportedAndIsIdempotent() {
        final AtomicInteger drops = new AtomicInteger();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void disconnected(Endpoint e) {
                drops.incrementAndGet();
            }
        });
        Endpoint e = connectedEndpoint();
        NearbyTransport.disconnect(e);
        assertEquals(1, drops.get());
        NearbyTransport.disconnect(e);
        assertEquals(1, drops.get());
    }

    @Test
    void stoppingEverythingDropsEveryConnection() {
        final AtomicInteger drops = new AtomicInteger();
        NearbyTransport.addTransportListener(new TransportAdapter() {
            @Override
            public void disconnected(Endpoint e) {
                drops.incrementAndGet();
            }
        });
        connectedEndpoint();
        value(NearbyTransport.startAdvertising("chat", "me",
                TransportStrategy.CLUSTER));
        assertTrue(bridge.isAdvertising());
        NearbyTransport.stop();
        assertEquals(1, drops.get());
        assertFalse(bridge.isAdvertising());
        assertFalse(bridge.isDiscovering());
    }

    @Test
    void anUnansweredConnectionRequestIsRejectedRatherThanLeftHanging() {
        // Nobody registers a listener, so nobody answers. The far side must
        // learn that immediately instead of timing out.
        NearbyTransport.deliverConnectionRequested(
                "ep-x\tSomebody\tchat", "1234");
        // Reaching here without an exception is the assertion: the framework
        // answered on the app's behalf.
    }

    @Test
    void aRemovedListenerStopsHearingThings() {
        final AtomicInteger seen = new AtomicInteger();
        TransportAdapter l = new TransportAdapter() {
            @Override
            public void endpointFound(Endpoint e) {
                seen.incrementAndGet();
            }
        };
        NearbyTransport.addTransportListener(l);
        value(NearbyTransport.startDiscovery("chat", TransportStrategy.CLUSTER));
        int after = seen.get();
        assertTrue(after > 0);
        NearbyTransport.removeTransportListener(l);
        value(NearbyTransport.startDiscovery("chat", TransportStrategy.CLUSTER));
        assertEquals(after, seen.get());
    }

    @Test
    void transportPermissionsSettleRatherThanHanging() {
        // Regression: NearbyTransport.requestPermissions parked its resource
        // in the transport's own pending map while every bridge answers
        // through Ranging.deliverPermissionResult, which only searched the
        // ranging map. The id was dropped and the caller waited forever --
        // the exact failure the SPI documentation calls worse than an error.
        assertTrue(value(NearbyTransport.requestPermissions(
                NearbyPermission.DISCOVERY, NearbyPermission.CONNECT))
                .booleanValue());
    }

    @Test
    void aFailedStartLeavesTheSessionUsable() {
        // Regression: the flag that makes a concurrent start answer BUSY was
        // set before the bridge call and cleared only on success, so a
        // rejected token wedged the session permanently -- and retrying after
        // a bad token exchange is the obvious thing to do.
        RangingSession s = value(Ranging.prepareSession(
                RangingRole.CONTROLLER));
        assertFailedWith(NearbyError.INVALID_TOKEN, s.start(
                RangingToken.forPayload(RangingToken.PLATFORM_APPLE_NI,
                        new byte[] {1, 2, 3})));
        // The retry must reach the bridge, not bounce off BUSY.
        RangingSession started = value(s.start(peerToken()));
        assertSame(s, started);
        assertTrue(s.isRunning());
    }

    @Test
    void aFailedAccessoryStartAlsoLeavesTheSessionUsable() {
        RangingSession s = value(Ranging.prepareSession(
                RangingRole.CONTROLLER));
        assertFailedWith(NearbyError.INVALID_TOKEN,
                s.startAccessory(new byte[0]));
        assertNotNull(value(s.startAccessory(new byte[] {1, 2, 3})));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private RangingToken peerToken() {
        return RangingToken.forPayload(RangingToken.PLATFORM_SIMULATED,
                new byte[] {'p', 'e', 'e', 'r'});
    }

    private RangingSession running() {
        RangingSession s = value(Ranging.prepareSession(
                RangingRole.CONTROLLER));
        value(s.start(peerToken()));
        return s;
    }

    /**
     * Drives one more measurement out of a running session.
     *
     * <p>The simulation only re-arms its own timer when there is an event loop
     * to re-arm it on, so under a unit test each trigger produces exactly one
     * measurement. Suspending and resuming is the trigger.</p>
     */
    private void nudge(int handle) {
        bridge.suspendSession(handle);
        bridge.resumeSession(handle);
    }

    private int handleOf() {
        int[] handles = bridge.getSessionHandles();
        assertEquals(1, handles.length,
                "these helpers assume exactly one live session");
        return handles[0];
    }

    private double[] walk() {
        RangingSession s = value(Ranging.prepareSession(
                RangingRole.CONTROLLER));
        final List<Double> seen = new ArrayList<Double>();
        s.addRangingListener(new RangingAdapter() {
            @Override
            public void updated(RangingUpdate u) {
                seen.add(Double.valueOf(u.getDistance(RangingUnit.METERS)));
            }
        });
        value(s.start(peerToken()));
        int handle = handleOf();
        for (int i = 0; i < 12; i++) {
            nudge(handle);
        }
        s.stop();
        double[] out = new double[seen.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = seen.get(i).doubleValue();
        }
        return out;
    }

    private Endpoint connectedEndpoint() {
        final AtomicReference<Endpoint> found = new AtomicReference<Endpoint>();
        TransportAdapter finder = new TransportAdapter() {
            @Override
            public void endpointFound(Endpoint e) {
                found.compareAndSet(null, e);
            }
        };
        NearbyTransport.addTransportListener(finder);
        value(NearbyTransport.startDiscovery("chat", TransportStrategy.CLUSTER));
        NearbyTransport.removeTransportListener(finder);
        Endpoint e = found.get();
        assertNotNull(e);
        value(NearbyTransport.requestConnection(e, "me"));
        return e;
    }
}
