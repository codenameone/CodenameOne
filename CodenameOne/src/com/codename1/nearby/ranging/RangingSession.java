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
import com.codename1.impl.nearby.NearbyRequests;
import com.codename1.nearby.NearbyError;
import com.codename1.nearby.NearbyException;
import com.codename1.nearby.spi.NearbyBridge;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/// One ranging conversation with one peer or accessory, obtained from
/// [Ranging#prepareSession].
///
/// A prepared session has a [#getLocalToken()] to publish and is not yet
/// using the radio. It starts measuring when [#start] or [#startAccessory]
/// is called and keeps going until [#stop()], until the platform
/// invalidates it, or until the app exits.
///
/// Sessions are not reusable: once stopped or invalidated, prepare another.
public final class RangingSession {

    private static final AtomicInteger NEXT_HANDLE = new AtomicInteger(1);
    private static final Map<Integer, RangingSession> SESSIONS =
            new HashMap<Integer, RangingSession>();

    private final int handle;
    private final RangingRole role;
    private final RangingToken localToken;
    private final List<RangingListener> listeners =
            new ArrayList<RangingListener>();
    private boolean running;
    private boolean closed;
    private boolean starting;

    private RangingSession(int handle, RangingRole role,
            RangingToken localToken) {
        this.handle = handle;
        this.role = role;
        this.localToken = localToken;
    }

    /// The token to publish so the peer can range against this device. Never
    /// null, and available as soon as the session is prepared.
    ///
    /// #### Returns
    ///
    /// this device's token for this session
    public RangingToken getLocalToken() {
        return localToken;
    }

    /// Which end of the session this device is.
    ///
    /// #### Returns
    ///
    /// the role this session was prepared with
    public RangingRole getRole() {
        return role;
    }

    /// `true` while the radio is measuring. False before [#start] and after
    /// [#stop()], and false while the session is suspended.
    public boolean isRunning() {
        return running;
    }

    /// Starts ranging against a peer whose token arrived out of band.
    ///
    /// #### Parameters
    ///
    /// - `peerToken`: the peer's token, decoded from the bytes they
    ///   published
    ///
    /// #### Returns
    ///
    /// resolves with this session once the radio is measuring, or fails
    /// with a [NearbyException]
    public AsyncResource<RangingSession> start(RangingToken peerToken) {
        if (peerToken == null) {
            return failedSession(NearbyError.INVALID_TOKEN,
                    "a peer token is required");
        }
        NearbyException busy = checkStartable();
        if (busy != null) {
            EdtResult<RangingSession> out = new EdtResult<RangingSession>();
            out.error(busy);
            return out;
        }
        NearbyBridge b = NearbyRequests.bridge();
        int id = NearbyRequests.nextId();
        EdtResult<RangingSession> out = Ranging.pendingSessions().open(id);
        starting = true;
        b.startRanging(id, handle, peerToken.toByteArray());
        return out;
    }

    /// Starts ranging against a third-party UWB accessory.
    ///
    /// The accessory publishes a blob of configuration data over its own
    /// channel -- in practice a GATT characteristic. Hand those bytes here,
    /// and send whatever this resolves with back to the accessory: Apple's
    /// Nearby Interaction Accessory Protocol needs that second half of the
    /// handshake before the accessory begins ranging.
    ///
    /// Android has no equivalent protocol. There, an accessory simply names
    /// the channel and session to join, so build a token with
    /// [RangingToken#forUwbAddress] and call [#start] instead; this method
    /// fails with [NearbyError#NOT_SUPPORTED].
    ///
    /// #### Parameters
    ///
    /// - `accessoryConfigurationData`: what the accessory published
    ///
    /// #### Returns
    ///
    /// resolves with the bytes to send back to the accessory, empty where
    /// the platform needs no handshake
    public AsyncResource<byte[]> startAccessory(
            byte[] accessoryConfigurationData) {
        if (accessoryConfigurationData == null
                || accessoryConfigurationData.length == 0) {
            EdtResult<byte[]> out = new EdtResult<byte[]>();
            out.error(new NearbyException(NearbyError.INVALID_TOKEN,
                    "accessory configuration data is required"));
            return out;
        }
        NearbyException busy = checkStartable();
        if (busy != null) {
            EdtResult<byte[]> out = new EdtResult<byte[]>();
            out.error(busy);
            return out;
        }
        NearbyBridge b = NearbyRequests.bridge();
        int id = NearbyRequests.nextId();
        EdtResult<byte[]> out = Ranging.pendingAccessory().open(id);
        starting = true;
        b.startAccessoryRanging(id, handle, accessoryConfigurationData);
        return out;
    }

    /// Stops measuring and releases the radio. Idempotent, and safe to call
    /// on a session that never started. No further listener callback
    /// arrives afterwards.
    public void stop() {
        boolean wasOpen;
        synchronized (SESSIONS) {
            wasOpen = !closed;
            closed = true;
            running = false;
            SESSIONS.remove(Integer.valueOf(handle));
        }
        if (wasOpen) {
            NearbyBridge b = NearbyRequests.bridge();
            if (b != null) {
                b.stopRangingSession(handle);
            }
        }
        synchronized (listeners) {
            listeners.clear();
        }
    }

    /// Registers a listener. Callbacks arrive on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public void addRangingListener(RangingListener l) {
        if (l == null) {
            return;
        }
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    /// Removes a listener added by [#addRangingListener].
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public void removeRangingListener(RangingListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    // ------------------------------------------------------------------
    // Port entry points
    // ------------------------------------------------------------------

    /// Delivers one measurement.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `sessionHandle`: the session the measurement belongs to
    /// - `hasDistance`: whether a distance was measured
    /// - `distanceMeters`: the distance in metres
    /// - `hasDirection`: whether an azimuth was measured
    /// - `azimuth`: the horizontal angle in degrees
    /// - `hasElevation`: whether an elevation was measured
    /// - `elevation`: the vertical angle in degrees
    /// - `vector`: the raw unit direction vector, or null
    public static void deliverUpdate(int sessionHandle, boolean hasDistance,
            double distanceMeters, boolean hasDirection, double azimuth,
            boolean hasElevation, double elevation, float[] vector) {
        final RangingSession s = lookup(sessionHandle);
        if (s == null) {
            return;
        }
        final RangingUpdate u = new RangingUpdate(hasDistance, distanceMeters,
                hasDirection, azimuth, hasElevation, elevation, vector,
                System.currentTimeMillis());
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                s.running = true;
                RangingListener[] ls = s.snapshot();
                for (int i = 0; i < ls.length; i++) {
                    ls[i].updated(u);
                }
            }
        });
    }

    /// Reports that the peer stopped being ranged.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `sessionHandle`: the session
    /// - `reasonOrdinal`: the ordinal of a [RangingRemovalReason] constant
    public static void deliverPeerRemoved(int sessionHandle,
            int reasonOrdinal) {
        final RangingSession s = lookup(sessionHandle);
        if (s == null) {
            return;
        }
        RangingRemovalReason[] all = RangingRemovalReason.values();
        final RangingRemovalReason reason =
                reasonOrdinal >= 0 && reasonOrdinal < all.length
                        ? all[reasonOrdinal] : RangingRemovalReason.UNKNOWN;
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                RangingListener[] ls = s.snapshot();
                for (int i = 0; i < ls.length; i++) {
                    ls[i].peerRemoved(reason);
                }
            }
        });
    }

    /// Reports that the platform paused the session.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `sessionHandle`: the session
    public static void deliverSuspended(int sessionHandle) {
        final RangingSession s = lookup(sessionHandle);
        if (s == null) {
            return;
        }
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                s.running = false;
                RangingListener[] ls = s.snapshot();
                for (int i = 0; i < ls.length; i++) {
                    ls[i].suspended();
                }
            }
        });
    }

    /// Reports that a suspended session resumed.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `sessionHandle`: the session
    public static void deliverResumed(int sessionHandle) {
        final RangingSession s = lookup(sessionHandle);
        if (s == null) {
            return;
        }
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                s.running = true;
                RangingListener[] ls = s.snapshot();
                for (int i = 0; i < ls.length; i++) {
                    ls[i].resumed();
                }
            }
        });
    }

    /// Reports that the session died and cannot be restarted. The session is
    /// deregistered, so this is the last event it produces.
    ///
    /// @hidden not part of the public API; called by ports from any thread.
    ///
    /// #### Parameters
    ///
    /// - `sessionHandle`: the session
    /// - `errorOrdinal`: the ordinal of a `com.codename1.nearby.NearbyError`
    ///   constant
    /// - `message`: a human-readable detail, may be null
    public static void deliverInvalidated(int sessionHandle, int errorOrdinal,
            String message) {
        final RangingSession s;
        synchronized (SESSIONS) {
            s = SESSIONS.remove(Integer.valueOf(sessionHandle));
        }
        if (s == null) {
            return;
        }
        final NearbyException ex = Ranging.toException(errorOrdinal, message);
        NearbyRequests.onEdt(new Runnable() {
            public void run() {
                s.running = false;
                s.closed = true;
                RangingListener[] ls = s.snapshot();
                synchronized (s.listeners) {
                    s.listeners.clear();
                }
                for (int i = 0; i < ls.length; i++) {
                    ls[i].invalidated(ex);
                }
            }
        });
    }


    /// Forgets every session, so one test cannot see the sessions of the test
    /// that ran before it. Reached through
    /// `com.codename1.impl.nearby.NearbyRequests#resetForTest`.
    ///
    /// @hidden not part of the public API; test-only.
    public static void resetForTest() {
        synchronized (SESSIONS) {
            SESSIONS.clear();
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    static int nextHandle() {
        return NEXT_HANDLE.getAndIncrement();
    }

    static RangingSession create(int handle, RangingRole role,
            RangingToken localToken) {
        RangingSession s = new RangingSession(handle, role, localToken);
        synchronized (SESSIONS) {
            SESSIONS.put(Integer.valueOf(handle), s);
        }
        return s;
    }

    static RangingSession lookup(int handle) {
        synchronized (SESSIONS) {
            return SESSIONS.get(Integer.valueOf(handle));
        }
    }

    void markRunning() {
        running = true;
        starting = false;
    }

    private RangingListener[] snapshot() {
        synchronized (listeners) {
            return listeners.toArray(new RangingListener[listeners.size()]);
        }
    }

    private NearbyException checkStartable() {
        if (closed) {
            return new NearbyException(NearbyError.SESSION_INVALIDATED,
                    "this session has been stopped; prepare another");
        }
        if (running || starting) {
            return new NearbyException(NearbyError.BUSY,
                    "this session is already ranging");
        }
        if (NearbyRequests.bridge() == null) {
            return new NearbyException(NearbyError.NOT_SUPPORTED,
                    "this platform does not support precision ranging");
        }
        return null;
    }

    private AsyncResource<RangingSession> failedSession(NearbyError error,
            String message) {
        EdtResult<RangingSession> out = new EdtResult<RangingSession>();
        out.error(new NearbyException(error, message));
        return out;
    }
}
