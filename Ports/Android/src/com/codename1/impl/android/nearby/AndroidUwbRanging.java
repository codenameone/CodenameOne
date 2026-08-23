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
package com.codename1.impl.android.nearby;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.uwb.RangingCapabilities;
import androidx.core.uwb.RangingMeasurement;
import androidx.core.uwb.RangingParameters;
import androidx.core.uwb.RangingPosition;
import androidx.core.uwb.RangingResult;
import androidx.core.uwb.UwbAddress;
import androidx.core.uwb.UwbClientSessionScope;
import androidx.core.uwb.UwbComplexChannel;
import androidx.core.uwb.UwbControleeSessionScope;
import androidx.core.uwb.UwbControllerSessionScope;
import androidx.core.uwb.UwbDevice;
import androidx.core.uwb.UwbManager;
import androidx.core.uwb.rxjava3.UwbClientSessionScopeRx;
import androidx.core.uwb.rxjava3.UwbManagerRx;

import com.codename1.nearby.NearbyAvailability;
import com.codename1.nearby.NearbyError;
import com.codename1.nearby.ranging.Ranging;
import com.codename1.nearby.ranging.RangingRemovalReason;
import com.codename1.nearby.ranging.RangingSession;
import com.codename1.nearby.ranging.RangingToken;
import com.codename1.nearby.spi.NearbyBridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/// Ultra-wideband ranging on Android, over Jetpack UWB.
///
/// #### Why the RxJava3 wrapper and not the base API
///
/// `androidx.core.uwb` is a Kotlin coroutines API: `prepareSession` returns a
/// `Flow` and every session getter is a suspend function. Consuming either
/// from the port's Java means hand-writing a `Continuation`, which is a lot of
/// machinery to get subtly wrong. `androidx.core.uwb:uwb-rxjava3` is the same
/// library's own Java-facing wrapper -- a `Single` for the session scope, an
/// `Observable` for the measurements -- so this file stays ordinary Java.
///
/// Only the classes this file names are needed at runtime; the builder adds
/// both artifacts together.
///
/// #### The token carries what the controlee has to join
///
/// Apple's Nearby Interaction negotiates channel and session parameters
/// itself, so its token is one opaque blob. Android's does not: the controller
/// picks the complex channel and the session id, and the controlee has to be
/// told both plus the controller's address. So the token minted here packs
/// address, channel, preamble index, session id and session key -- the same
/// shape `RangingToken.forUwbAddress` builds for an accessory.
public class AndroidUwbRanging implements NearbyBridge {

    private static final int DEFAULT_CHANNEL = 9;
    private static final int DEFAULT_PREAMBLE = 10;

    private final Context context;
    private final Map<Integer, Session> sessions =
            Collections.synchronizedMap(new HashMap<Integer, Session>());
    private final Random random = new Random();

    private UwbManager manager;

    private final Object capabilityLock = new Object();
    /// The capability bits the platform reported, or -1 while unknown. A
    /// probe that FAILS leaves this at -1 rather than caching zero: the usual
    /// reason it fails is that UWB_RANGING has not been granted yet, and
    /// remembering "no direction" from before the user said yes would make
    /// the answer wrong for the rest of the process.
    private int probedCapabilities = -1;
    private boolean probeInFlight;

    public AndroidUwbRanging(Context context) {
        this.context = context;
    }

    /// Asks the platform for its ranging capabilities, off the calling thread.
    ///
    /// Android reports them only through a session scope, and opening one
    /// binds to the UWB system service -- seconds, on a cold radio. Started
    /// once, as early as anything touches this bridge, so that by the time an
    /// app asks the answer is usually already here.
    private void startCapabilityProbe() {
        synchronized (capabilityLock) {
            if (probedCapabilities >= 0 || probeInFlight) {
                return;
            }
            probeInFlight = true;
        }
        try {
            UwbManagerRx.clientSessionScopeSingle(managerOrThrow())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new io.reactivex.rxjava3.functions.Consumer<
                            UwbClientSessionScope>() {
                        public void accept(UwbClientSessionScope scope) {
                            int bits = 0;
                            try {
                                RangingCapabilities caps =
                                        scope.getRangingCapabilities();
                                if (caps.isAzimuthalAngleSupported()) {
                                    bits |= NearbyBridge.CAPABILITY_DIRECTION;
                                }
                                if (caps.isElevationAngleSupported()) {
                                    bits |= NearbyBridge.CAPABILITY_ELEVATION;
                                }
                                if (caps.isBackgroundRangingSupported()) {
                                    bits |= NearbyBridge.CAPABILITY_BACKGROUND;
                                }
                            } catch (Throwable unreadable) {
                                bits = 0;
                            }
                            settleProbe(bits);
                        }
                    }, new io.reactivex.rxjava3.functions.Consumer<
                            Throwable>() {
                        public void accept(Throwable error) {
                            settleProbe(-1);
                        }
                    });
        } catch (Throwable noRadio) {
            settleProbe(-1);
        }
    }

    /// Records the probe's answer and wakes anything waiting for it.
    ///
    /// #### Parameters
    ///
    /// - `bits`: the capability bits, or -1 when the probe failed and the
    ///   next caller should try again
    private void settleProbe(int bits) {
        synchronized (capabilityLock) {
            if (bits >= 0) {
                probedCapabilities = bits;
            }
            probeInFlight = false;
            capabilityLock.notifyAll();
        }
    }

    // ------------------------------------------------------------------
    // Capability
    // ------------------------------------------------------------------

    public boolean isRangingSupported() {
        if (Build.VERSION.SDK_INT < 31) {
            return false;
        }
        PackageManager pm = context.getPackageManager();
        // FEATURE_UWB rather than trying to create a UwbManager: creating one
        // on a device without the radio throws, and a capability query must
        // not.
        return pm != null && pm.hasSystemFeature("android.hardware.uwb");
    }

    public int getRangingAvailability() {
        if (!isRangingSupported()) {
            return NearbyAvailability.NOT_SUPPORTED.ordinal();
        }
        // UNAUTHORIZED is the whole reason getAvailability() exists beside
        // isSupported(): a phone with a UWB radio whose owner has not granted
        // (or has revoked) UWB_RANGING is supported and unusable, and the
        // documented answer tells the app to ask rather than to hide the
        // feature. Reporting AVAILABLE here let an app show ranging as ready
        // until session preparation failed.
        if (Build.VERSION.SDK_INT >= 31
                && context.checkSelfPermission("android.permission.UWB_RANGING")
                        != PackageManager.PERMISSION_GRANTED) {
            return NearbyAvailability.UNAUTHORIZED.ordinal();
        }
        // Warmed here, where the permission is known to be granted, so the
        // scope is usually open by the time an app asks what it can measure.
        startCapabilityProbe();
        return NearbyAvailability.AVAILABLE.ordinal();
    }

    public int getRangingCapabilities() {
        if (!isRangingSupported()) {
            return 0;
        }
        // Reported without opening a session where possible. Where the
        // platform will only answer through a session scope, the conservative
        // answer is distance alone: claiming direction a device cannot
        // produce would have an app draw an arrow that never moves.
        int bits = NearbyBridge.CAPABILITY_DISTANCE;
        // The scope that carries the answer is opened by a background probe,
        // not here: opening one on the calling thread blocks it on the UWB
        // system service binding, and this is called from the EDT. The wait
        // below is bounded and only ever happens on a call that beats the
        // probe; once it lands the answer is cached and every later call is
        // free.
        startCapabilityProbe();
        int probed;
        long deadline = System.currentTimeMillis() + 400;
        synchronized (capabilityLock) {
            while (probedCapabilities < 0 && probeInFlight) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    break;
                }
                try {
                    capabilityLock.wait(left);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            probed = probedCapabilities;
        }
        if (probed > 0) {
            bits |= probed;
        }
        // An accessory is ranged here by joining the session it names, which
        // is the same code path as a peer -- so if ranging works at all,
        // accessory ranging does.
        bits |= NearbyBridge.CAPABILITY_ACCESSORY;
        return bits;
    }

    public boolean isCompanionSupported() {
        return false;
    }

    public boolean isTransportSupported() {
        return false;
    }

    public int getCompanionAvailability() {
        return NearbyAvailability.NOT_SUPPORTED.ordinal();
    }

    public int getTransportAvailability() {
        return NearbyAvailability.NOT_SUPPORTED.ordinal();
    }

    public void requestPermissions(int requestId, int permissionBits) {
        // AndroidNearbyBackend owns the permission flow for both halves: the
        // strings are platform permissions, needing no optional dependency,
        // and an app using ranging AND transport needs ONE answer covering
        // both -- which no single backend can give. Reached only through that
        // coordinator, so this is unreachable; it answers rather than hanging
        // in case a future caller finds another way in.
        com.codename1.nearby.ranging.Ranging.deliverPermissionResult(requestId,
                true);
    }

    // ------------------------------------------------------------------
    // Sessions
    // ------------------------------------------------------------------

    public void prepareRangingSession(final int requestId,
            final int sessionHandle, final boolean controller) {
        if (!isRangingSupported()) {
            fail(requestId, NearbyError.NOT_SUPPORTED,
                    "this device has no ultra-wideband radio");
            return;
        }
        // Subscribed, never blockingGet(). Opening a session scope binds to
        // the UWB system service and negotiates a local address, and this is
        // called from the EDT -- so blocking on it froze the UI for as long as
        // the service took to come up, which on a cold radio is long enough to
        // be an ANR rather than a stutter. The answer arrives on an io thread
        // and Ranging hops it back to the EDT itself.
        try {
            final Session session = new Session(sessionHandle, controller);
            UwbManager uwb = managerOrThrow();
            if (controller) {
                UwbManagerRx.controllerSessionScopeSingle(uwb)
                        .subscribeOn(Schedulers.io())
                        .subscribe(new io.reactivex.rxjava3.functions.Consumer<
                                UwbControllerSessionScope>() {
                            public void accept(UwbControllerSessionScope scope) {
                                session.scope = scope;
                                session.channel = scope.getUwbComplexChannel();
                                finishPrepare(requestId, session);
                            }
                        }, new io.reactivex.rxjava3.functions.Consumer<
                                Throwable>() {
                            public void accept(Throwable error) {
                                fail(requestId, NearbyError.SESSION_FAILED,
                                        message(error));
                            }
                        });
                return;
            }
            UwbManagerRx.controleeSessionScopeSingle(uwb)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new io.reactivex.rxjava3.functions.Consumer<
                            UwbControleeSessionScope>() {
                        public void accept(UwbControleeSessionScope scope) {
                            session.scope = scope;
                            finishPrepare(requestId, session);
                        }
                    }, new io.reactivex.rxjava3.functions.Consumer<
                            Throwable>() {
                        public void accept(Throwable error) {
                            fail(requestId, NearbyError.SESSION_FAILED,
                                    message(error));
                        }
                    });
        } catch (Throwable t) {
            fail(requestId, NearbyError.SESSION_FAILED, message(t));
        }
    }

    /// Finishes a prepare once the session scope has been opened. Runs on the
    /// RxJava io thread the scope was delivered on, never on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the request to answer
    /// - `session`: the session whose scope is now set
    private void finishPrepare(int requestId, Session session) {
        try {
            session.localAddress = session.scope.getLocalAddress();
            session.sessionId = random.nextInt(Integer.MAX_VALUE - 1) + 1;
            session.sessionKey = new byte[8];
            random.nextBytes(session.sessionKey);
            sessions.put(Integer.valueOf(session.handle), session);
            Ranging.deliverSessionPrepared(requestId, session.handle,
                    session.controller, RangingToken.PLATFORM_ANDROID_UWB,
                    session.token());
        } catch (Throwable t) {
            fail(requestId, NearbyError.SESSION_FAILED, message(t));
        }
    }

    public void startRanging(final int requestId, final int sessionHandle,
            byte[] peerToken) {
        final Session session = sessions.get(Integer.valueOf(sessionHandle));
        if (session == null) {
            fail(requestId, NearbyError.SESSION_INVALIDATED, "no such session");
            return;
        }
        Peer peer;
        try {
            peer = Peer.decode(peerToken);
        } catch (IllegalArgumentException e) {
            fail(requestId, NearbyError.INVALID_TOKEN, e.getMessage());
            return;
        }
        run(requestId, session, peer);
    }

    public void startAccessoryRanging(int requestId, int sessionHandle,
            byte[] accessoryData) {
        // An accessory on Android is not a protocol, it is a set of session
        // parameters the accessory published out of band -- which is exactly
        // what a token is. So the two paths are the same one, and the public
        // API documents building the token with RangingToken.forUwbAddress.
        startRanging(requestId, sessionHandle, accessoryData);
    }

    private void run(final int requestId, final Session session,
            final Peer peer) {
        try {
            List<UwbDevice> peers = new ArrayList<UwbDevice>();
            peers.add(new UwbDevice(new UwbAddress(peer.address)));
            UwbComplexChannel channel = session.controller
                    ? session.channel
                    : new UwbComplexChannel(peer.channel, peer.preamble);
            int sessionId = session.controller ? session.sessionId
                    : peer.sessionId;
            byte[] key = session.controller ? session.sessionKey
                    : peer.sessionKey;
            RangingParameters params = new RangingParameters(
                    RangingParameters.CONFIG_UNICAST_DS_TWR,
                    sessionId,
                    0,
                    key,
                    null,
                    channel,
                    peers,
                    RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC);
            // Answered by the subscription, not before it. subscribeOn puts
            // the actual startRanging on an io thread, so resolving here said
            // "ranging started" while the radio had not been asked yet -- and
            // a rejected channel, address or key then arrived as an
            // invalidation AFTER the caller had already been told it
            // succeeded, which is the opposite of what start() documents.
            //
            // Whichever comes first wins: the first measurement (ranging is
            // demonstrably running), the first error (it is not), or the
            // grace timer. The timer is the backstop for the case that has no
            // signal of its own -- a session that starts cleanly and simply
            // has nothing in range to measure yet.
            session.startRequest.set(requestId);
            session.subscription = UwbClientSessionScopeRx
                    .rangingResultsObservable(session.scope, params)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new io.reactivex.rxjava3.functions.Consumer<
                            RangingResult>() {
                        public void accept(RangingResult result) {
                            settleStarted(session);
                            deliver(session.handle, result);
                        }
                    }, new io.reactivex.rxjava3.functions.Consumer<
                            Throwable>() {
                        public void accept(Throwable error) {
                            int pending = session.startRequest.getAndSet(0);
                            if (pending != 0) {
                                // It never started, so the caller is told
                                // that rather than being told it started and
                                // then invalidated.
                                //
                                // The backend session STAYS. A failed start
                                // leaves the facade session open and
                                // retryable -- Ranging.deliverRequestFailed
                                // only clears the in-progress flag -- so
                                // dropping it here meant the retry the facade
                                // invites answered "no such session" for a
                                // session isClosed() still reported as open.
                                // The scope is still valid; only this
                                // subscription failed.
                                fail(pending, NearbyError.SESSION_FAILED,
                                        message(error));
                                return;
                            }
                            RangingSession.deliverInvalidated(
                                    session.handle,
                                    NearbyError.SESSION_INVALIDATED.ordinal(),
                                    message(error));
                            sessions.remove(Integer.valueOf(session.handle));
                        }
                    });
            scheduleStartGrace(session);
        } catch (Throwable t) {
            fail(requestId, NearbyError.SESSION_FAILED, message(t));
        }
    }

    /// Answers the start request if it is still waiting.
    private static void settleStarted(Session session) {
        int pending = session.startRequest.getAndSet(0);
        if (pending != 0) {
            Ranging.deliverSessionStarted(pending, session.handle);
        }
    }

    /// How long a start is given to fail before it is called a success.
    private static final long START_GRACE_MILLIS = 500;

    /// Answers a start that produced neither a measurement nor an error.
    ///
    /// A session can start perfectly well and have nothing in range to
    /// measure, which produces no signal at all -- so without this the
    /// caller's AsyncResource would wait for a peer that may never appear.
    private void scheduleStartGrace(final Session session) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        settleStarted(session);
                    }
                }, START_GRACE_MILLIS);
    }

    private static void deliver(int handle, RangingResult result) {
        if (result instanceof RangingResult.RangingResultPeerDisconnected) {
            RangingSession.deliverPeerRemoved(handle,
                    RangingRemovalReason.TIMEOUT.ordinal());
            return;
        }
        if (!(result instanceof RangingResult.RangingResultPosition)) {
            return;
        }
        RangingPosition position =
                ((RangingResult.RangingResultPosition) result).getPosition();
        // Degrees already -- NOT radians, and NOT converted here.
        //
        // It was suggested these arrive in radians and need Math.toDegrees.
        // androidx.core.uwb's own KDoc on RangingPosition says otherwise:
        // "The azimuth angle in degrees of the ranging device", and the same
        // for elevation. The library does no unit conversion of its own
        // either -- disassembling UwbClientSessionScopeAospImpl shows the
        // backend's float copied straight into androidx RangingMeasurement --
        // so whatever it is called, it is what the library documents.
        // Converting would turn a 90-degree bearing into 1.57.
        //
        // The RANGES do differ from iOS and the portable documentation says
        // so: Android reports azimuth in [-90, 90], which cannot tell a peer
        // in front from one behind, while Apple's direction vector yields
        // [-180, 180].
        RangingMeasurement distance = position.getDistance();
        RangingMeasurement azimuth = position.getAzimuth();
        RangingMeasurement elevation = position.getElevation();
        RangingSession.deliverUpdate(handle,
                distance != null, distance == null ? 0 : distance.getValue(),
                azimuth != null, azimuth == null ? 0 : azimuth.getValue(),
                elevation != null, elevation == null ? 0
                        : elevation.getValue(),
                // No vector: Android reports the angles and never the unit
                // vector iOS produces, and synthesising one from two angles
                // would invent a precision the platform did not report.
                null);
    }

    public void stopRangingSession(int sessionHandle) {
        Session session = sessions.remove(Integer.valueOf(sessionHandle));
        if (session != null && session.subscription != null) {
            session.subscription.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Unused halves
    // ------------------------------------------------------------------

    public void associate(int requestId, int profile, boolean singleDevice,
            String[] filters) {
    }

    public String[] getAssociations() {
        return new String[0];
    }

    public void disassociate(int requestId, String associationId) {
    }

    public boolean startObservingPresence(String associationId) {
        return false;
    }

    public void stopObservingPresence(String associationId) {
    }

    public int getMaxPayloadSize() {
        return 0;
    }

    public void startAdvertising(int requestId, String serviceId,
            String localName, int strategy) {
    }

    public void stopAdvertising() {
    }

    public void startDiscovery(int requestId, String serviceId, int strategy) {
    }

    public void stopDiscovery() {
    }

    public void requestConnection(int requestId, String endpointId,
            String localName) {
    }

    public void acceptConnection(int requestId, String endpointId) {
    }

    public void rejectConnection(String endpointId) {
    }

    public void sendPayload(int requestId, String[] endpointIds, int payloadId,
            int payloadType, byte[] bytes, String path) {
    }

    public void cancelPayload(int payloadId) {
    }

    public void disconnect(String endpointId) {
    }

    public void stopAllTransport() {
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private UwbManager managerOrThrow() {
        if (manager == null) {
            manager = UwbManager.Companion.createInstance(context);
        }
        return manager;
    }

    private static void fail(int requestId, NearbyError error, String message) {
        Ranging.deliverRequestFailed(requestId, error.ordinal(), message);
    }

    private static String message(Throwable t) {
        return t == null ? null
                : (t.getMessage() != null ? t.getMessage()
                        : t.getClass().getName());
    }

    private static final class Session {
        private final int handle;
        private final boolean controller;
        private UwbClientSessionScope scope;
        private UwbAddress localAddress;
        private UwbComplexChannel channel;
        private int sessionId;
        private byte[] sessionKey;
        private Disposable subscription;
        /// The start request still waiting for an answer, or 0 once it has
        /// been answered. Answered exactly once, by whichever of the first
        /// measurement, the first error, or the grace timer gets there.
        private final java.util.concurrent.atomic.AtomicInteger startRequest =
                new java.util.concurrent.atomic.AtomicInteger();

        private Session(int handle, boolean controller) {
            this.handle = handle;
            this.controller = controller;
        }

        /// The payload half of the token, without the framing
        /// `RangingToken.forPayload` adds.
        private byte[] token() {
            byte[] address = localAddress.getAddress();
            int channelNumber = channel == null ? DEFAULT_CHANNEL
                    : channel.getChannel();
            int preamble = channel == null ? DEFAULT_PREAMBLE
                    : channel.getPreambleIndex();
            byte[] out = new byte[4 + address.length + 12 + 4
                    + sessionKey.length];
            int p = writeInt(out, 0, address.length);
            System.arraycopy(address, 0, out, p, address.length);
            p += address.length;
            p = writeInt(out, p, channelNumber);
            p = writeInt(out, p, preamble);
            p = writeInt(out, p, sessionId);
            p = writeInt(out, p, sessionKey.length);
            System.arraycopy(sessionKey, 0, out, p, sessionKey.length);
            return out;
        }
    }

    /// The decoded far side of a token.
    private static final class Peer {
        private byte[] address;
        private int channel;
        private int preamble;
        private int sessionId;
        private byte[] sessionKey;

        private static Peer decode(byte[] framed) {
            if (framed == null || framed.length < 10
                    || framed[0] != 'C' || framed[1] != 'N' || framed[2] != '1'
                    || framed[3] != 'R') {
                throw new IllegalArgumentException(
                        "the peer token is not a Codename One token");
            }
            if ((framed[5] & 0xff) != RangingToken.PLATFORM_ANDROID_UWB) {
                throw new IllegalArgumentException(
                        "this token was minted by another platform");
            }
            int length = readInt(framed, 6, "payload length");
            if (length < 0 || 10 + length > framed.length) {
                throw new IllegalArgumentException("truncated ranging token");
            }
            // Every read is bounds-checked, including the four-byte ints.
            // RangingToken.fromByteArray only validates the OUTER frame, so a
            // peer can hand over a well-formed envelope whose payload is two
            // bytes long -- and an ArrayIndexOutOfBoundsException from in here
            // is not an IllegalArgumentException, so it would escape
            // startRanging's handler into application code and leave the start
            // resource pending forever.
            Peer peer = new Peer();
            int p = 10;
            int addressLength = readInt(framed, p, "address length");
            p += 4;
            if (addressLength < 0 || addressLength > framed.length - p) {
                throw new IllegalArgumentException("truncated ranging token");
            }
            peer.address = new byte[addressLength];
            System.arraycopy(framed, p, peer.address, 0, addressLength);
            p += addressLength;
            peer.channel = readInt(framed, p, "channel");
            p += 4;
            peer.preamble = readInt(framed, p, "preamble index");
            p += 4;
            peer.sessionId = readInt(framed, p, "session id");
            p += 4;
            int keyLength = readInt(framed, p, "session key length");
            p += 4;
            if (keyLength < 0 || keyLength > framed.length - p) {
                throw new IllegalArgumentException("truncated ranging token");
            }
            peer.sessionKey = new byte[keyLength];
            System.arraycopy(framed, p, peer.sessionKey, 0, keyLength);
            return peer;
        }
    }

    private static int writeInt(byte[] b, int p, int v) {
        b[p] = (byte) ((v >> 24) & 0xff);
        b[p + 1] = (byte) ((v >> 16) & 0xff);
        b[p + 2] = (byte) ((v >> 8) & 0xff);
        b[p + 3] = (byte) (v & 0xff);
        return p + 4;
    }

    /// Reads four bytes, refusing rather than running off the end.
    ///
    /// The `what` is in the message because a truncated token is something a
    /// developer debugging an out-of-band exchange has to locate, and "field
    /// X starts past the end" says where to look.
    private static int readInt(byte[] b, int p, String what) {
        if (p < 0 || p > b.length - 4) {
            throw new IllegalArgumentException(
                    "truncated ranging token: " + what + " starts past the end");
        }
        return ((b[p] & 0xff) << 24) | ((b[p + 1] & 0xff) << 16)
                | ((b[p + 2] & 0xff) << 8) | (b[p + 3] & 0xff);
    }
}
