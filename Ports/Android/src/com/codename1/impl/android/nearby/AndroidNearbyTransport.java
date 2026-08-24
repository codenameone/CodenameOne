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

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.ui.Display;

import com.codename1.nearby.NearbyAvailability;
import com.codename1.nearby.NearbyError;
import com.codename1.nearby.spi.NearbyBridge;
import com.codename1.nearby.transport.NearbyTransport;
import com.codename1.nearby.transport.PayloadStatus;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The nearby transport on Android, over Google's Nearby Connections.
///
/// Compiled inside the generated app, because `play-services-nearby` is on the
/// classpath only for an app that referenced
/// `com.codename1.nearby.transport`.
public class AndroidNearbyTransport implements NearbyBridge {

    /// The Nearby Connections limit for a BYTES payload.
    private static final int NEARBY_BYTES_LIMIT = 32 * 1024;

    /// What an app may actually send: the limit less the four-byte payload-id
    /// header this transport frames in. Reported rather than the raw limit,
    /// because an app that respects getMaxPayloadSize() must not then be
    /// rejected by Nearby for the header it never knew about.
    private static final int MAX_BYTES_PAYLOAD = NEARBY_BYTES_LIMIT - 4;

    private final Context context;
    private final Map<String, String> endpointNames =
            Collections.synchronizedMap(new HashMap<String, String>());
    private final Map<Long, Integer> payloadIds =
            Collections.synchronizedMap(new HashMap<Long, Integer>());
    /// Incoming FILE payloads between their announcement and the terminal
    /// update that says the bytes actually arrived.
    private final Map<Long, Payload> incomingFiles =
            Collections.synchronizedMap(new HashMap<Long, Payload>());
    /// How many recipients of an outgoing payload have yet to reach a
    /// terminal transfer state.
    private final Map<Long, Integer> payloadRecipients =
            Collections.synchronizedMap(new HashMap<Long, Integer>());

    /// The service each endpoint was NEGOTIATED through, when connected.
    ///
    /// Separate from endpointServices, which is what discovery saw. One map
    /// could not be both: an inbound connection through the advertised
    /// service has to label its own events with that service, and writing it
    /// over the discovery entry destroyed the pairing discovery owes its
    /// listener -- endpointFound reported A and the later endpointLost, after
    /// the connection had closed, reported B.
    private final Map<String, String> connectionServices =
            Collections.synchronizedMap(new HashMap<String, String>());

    /// The service each endpoint was found under.
    ///
    /// One shared field was wrong: an app advertising service B while
    /// discovering service A had the later call overwrite it, and endpoints
    /// found under A were then encoded as B -- which Endpoint.getServiceId()
    /// documents as the service they were found under.
    /// Endpoints discovery can currently see.
    ///
    /// A peer that arrived through ADVERTISING was never discovered, so a
    /// rejected or failed handshake leaves it with no callback coming at all
    /// -- no onEndpointLost, no onDisconnected. Only this tells such a peer
    /// from one discovery is still showing, whose metadata has to stay.
    private final java.util.Set<String> discoveredEndpoints =
            java.util.Collections.synchronizedSet(
                    new java.util.HashSet<String>());
    /// Endpoints currently connected.
    ///
    /// stop() has to clear the metadata of endpoints nothing will call back
    /// about, and leave alone the metadata onDisconnected still needs. Only
    /// this tells the two apart.
    private final java.util.Set<String> connectedEndpoints =
            java.util.Collections.synchronizedSet(
                    new java.util.HashSet<String>());
    private final Map<String, String> endpointServices =
            Collections.synchronizedMap(new HashMap<String, String>());
    /// Which start each asynchronous answer belongs to.
    ///
    /// A stop can land between a start and the platform's answer to it, and
    /// so can a second start. Without these the late answer resolved the
    /// caller's request as though the state it describes were still current.
    /// Read and written on the main thread, which is where Google delivers
    /// these listeners and where the portable API is called from.
    /// Guards the four fields below.
    ///
    /// They are written by the public API, which runs on Codename One's EDT,
    /// and read by Google's Task listeners, which run on Android's main
    /// thread -- two different threads, and the public API does not promise
    /// callers only one of them either. Unsynchronized, an increment could
    /// be lost or a callback could read a stale pair and let a stopped start
    /// report success, or stop the start that replaced it.
    private final Object transportLock = new Object();

    private int advertiseGeneration;
    private int discoverGeneration;

    /// Whether anyone still wants the radio doing this.
    ///
    /// A stale start has to tell "stopped, and nobody has asked since" from
    /// "superseded by a newer start". Google's stopAdvertising and
    /// stopDiscovery are GLOBAL -- there is one advertiser per client -- so
    /// undoing a stale start in the second case stopped the replacement that
    /// had just taken over, and that replacement then reported success on a
    /// radio this call had switched off.
    private boolean advertisingWanted;
    private boolean discoveringWanted;

    /// Whether the radio is on with NO resolved caller owning it.
    ///
    /// A start that succeeded after being superseded leaves exactly that: the
    /// platform is advertising, and the caller who asked was failed, because
    /// a newer start had taken the operation. If that newer start then fails
    /// too, both resources have failed and the radio is still on for nobody
    /// -- which is what this lets the failure notice and undo.
    private boolean unownedAdvertising;
    private boolean unownedDiscovering;

    /// This start owns the operation and should report success.
    private static final int START_CURRENT = 0;
    /// It was stopped and nothing has asked since: undo it.
    private static final int START_ORPHANED = 1;
    /// A newer start owns the operation: leave the radio alone.
    private static final int START_SUPERSEDED = 2;

    /// Claims a generation for a start that is about to be issued, replacing
    /// whatever was running.
    ///
    /// The stop is the point. Nearby has ONE advertiser and one discoverer
    /// per client and refuses a second start as "already advertising", so a
    /// start issued while an earlier one was live was rejected -- and the
    /// earlier one went on broadcasting the service the app had moved off.
    /// The generation guards could not save it either: the earlier start had
    /// already been answered, so nothing was left marked unowned for a
    /// failure to clean up. The simulated bridge and the iOS port both
    /// replace an existing start; this is Android doing the same.
    private int beginStart(boolean advertising) {
        synchronized (transportLock) {
            boolean live = advertising
                    ? (advertisingWanted || unownedAdvertising)
                    : (discoveringWanted || unownedDiscovering);
            if (live) {
                if (advertising) {
                    client().stopAdvertising();
                } else {
                    client().stopDiscovery();
                }
            }
            if (advertising) {
                advertisingWanted = true;
                unownedAdvertising = false;
                return ++advertiseGeneration;
            }
            discoveringWanted = true;
            unownedDiscovering = false;
            return ++discoverGeneration;
        }
    }

    /// What a start's answer means, decided from BOTH fields at once.
    ///
    /// One reading, under the lock. As two -- is it current, then does
    /// anyone want it -- a stop or a start landing between them could have
    /// this answer act on a state that never existed.
    private int classifyStart(boolean advertising, int generation) {
        synchronized (transportLock) {
            if (advertising) {
                if (generation == advertiseGeneration) {
                    return START_CURRENT;
                }
                return advertisingWanted ? START_SUPERSEDED : START_ORPHANED;
            }
            if (generation == discoverGeneration) {
                return START_CURRENT;
            }
            return discoveringWanted ? START_SUPERSEDED : START_ORPHANED;
        }
    }

    /// Records what a start's answer did, so a later failure knows whether
    /// the radio is still on for nobody.
    private void noteStartOutcome(boolean advertising, int state) {
        synchronized (transportLock) {
            // SUPERSEDED is the one outcome that leaves the platform running
            // with its caller failed. CURRENT has an owner, and ORPHANED was
            // just stopped.
            boolean unowned = state == START_SUPERSEDED;
            if (advertising) {
                unownedAdvertising = unowned;
                return;
            }
            unownedDiscovering = unowned;
        }
    }

    /// Fails the current start and cleans up after it, under the lock.
    ///
    /// The stop is issued from INSIDE the critical section. Deciding to stop
    /// and then releasing the lock left a window for another thread to begin
    /// a start, and the global stop that followed switched off that new
    /// operation instead -- without touching its generation, so its callback
    /// went on to report success for a radio this had just disabled. The
    /// lock is held across the platform call for exactly as long as it takes
    /// to issue it; Google answers it on the main looper, not here.
    private void failAndCleanUp(boolean advertising, int generation) {
        synchronized (transportLock) {
            if (!failStart(advertising, generation)) {
                return;
            }
            if (advertising) {
                client().stopAdvertising();
            } else {
                client().stopDiscovery();
            }
        }
    }

    /// Records that the current start failed, so nothing is wanted any more.
    ///
    /// #### Returns
    ///
    /// true when the radio has to be stopped as well, because a superseded
    /// start had left it running for a caller that was already failed
    private boolean failStart(boolean advertising, int generation) {
        synchronized (transportLock) {
            if (advertising) {
                if (generation != advertiseGeneration) {
                    return false;
                }
                advertisingWanted = false;
                boolean orphaned = unownedAdvertising;
                unownedAdvertising = false;
                return orphaned;
            }
            if (generation != discoverGeneration) {
                return false;
            }
            discoveringWanted = false;
            boolean orphaned = unownedDiscovering;
            unownedDiscovering = false;
            return orphaned;
        }
    }

    /// Ends the operation, invalidating any start still in flight.
    private void endStart(boolean advertising) {
        synchronized (transportLock) {
            if (advertising) {
                advertiseGeneration++;
                advertisingWanted = false;
                unownedAdvertising = false;
                return;
            }
            discoverGeneration++;
            discoveringWanted = false;
            unownedDiscovering = false;
        }
    }

    private String advertisingServiceId = "";
    private String discoveryServiceId = "";
    private String localName = "";

    public AndroidNearbyTransport(Context context) {
        this.context = context;
    }

    private ConnectionsClient client() {
        return Nearby.getConnectionsClient(context);
    }

    // ------------------------------------------------------------------
    // Capability
    // ------------------------------------------------------------------

    public boolean isTransportSupported() {
        return true;
    }

    public int getTransportAvailability() {
        // Reported honestly rather than as a flat AVAILABLE. Nearby
        // Connections needs Bluetooth and, depending on the level, nearby-WiFi
        // or location; without them advertising and discovery fail on the
        // first call. Saying AVAILABLE anyway made getAvailability() unable to
        // return the UNAUTHORIZED the public API documents, so an app showed
        // the feature as ready right up to the failure and had nothing to
        // prompt from.
        if (!NearbyPermissions.allGranted(context,
                NearbyPermissions.transportPermissions(context,
                        NearbyBridge.PERMISSION_DISCOVERY
                                | NearbyBridge.PERMISSION_ADVERTISE
                                | NearbyBridge.PERMISSION_CONNECT))) {
            return NearbyAvailability.UNAUTHORIZED.ordinal();
        }
        return NearbyAvailability.AVAILABLE.ordinal();
    }

    public int getMaxPayloadSize() {
        return MAX_BYTES_PAYLOAD;
    }

    public boolean isRangingSupported() {
        return false;
    }

    public boolean isCompanionSupported() {
        return false;
    }

    public int getRangingAvailability() {
        return NearbyAvailability.NOT_SUPPORTED.ordinal();
    }

    public int getCompanionAvailability() {
        return NearbyAvailability.NOT_SUPPORTED.ordinal();
    }

    public int getRangingCapabilities() {
        return 0;
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

    /// Adds a permission the app has not already been granted.
    ///
    /// Below API 23 nothing is ever outstanding: permissions are granted at
    /// install time, and Context.checkSelfPermission does not exist there --
    /// calling it threw NoSuchMethodError rather than answering, which a
    /// transport app on Android 5.0 or 5.1 can reach, since the transport's
    /// minimum is 21.
    private void add(ArrayList<String> perms, String permission) {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        if (context.checkSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(permission);
        }
    }

    /// Static so the Runnable carries no synthetic outer reference, which
    /// SpotBugs reports as SIC_INNER_SHOULD_BE_STATIC_ANON.
    private static Runnable requestRunnable(final int requestId,
            final ArrayList<String> perms) {
        return new Runnable() {
            @Override
            public void run() {
                boolean all = true;
                for (int i = 0; i < perms.size(); i++) {
                    all = AndroidImplementation.checkForPermission(
                            perms.get(i),
                            "This is required to find and connect to nearby"
                                    + " devices") && all;
                }
                com.codename1.nearby.ranging.Ranging.deliverPermissionResult(
                        requestId, all);
            }
        };
    }

    // ------------------------------------------------------------------
    // Transport
    // ------------------------------------------------------------------

    public void startAdvertising(final int requestId, String serviceId,
            String localName, int strategy) {
        // Captured for THIS callback, for the reason startDiscovery does it.
        final String started = serviceId == null ? "" : serviceId;
        this.advertisingServiceId = started;
        this.localName = localName == null ? "" : localName;
        // The generation this start belongs to. Google answers the start
        // asynchronously, and a stopAdvertising can land in front of that
        // answer -- which then told the caller advertising was active AFTER
        // it had stopped it, and left the platform start running behind a
        // stop that had already returned. The simulated bridge has modelled
        // this race from the beginning; this is the same answer.
        final int generation = beginStart(true);
        AdvertisingOptions options = new AdvertisingOptions.Builder()
                .setStrategy(strategyFor(strategy))
                .build();
        client().startAdvertising(this.localName, started,
                        connectionCallback(started), options)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    public void onSuccess(Void unused) {
                        int state = classifyStart(true, generation);
                        noteStartOutcome(true, state);
                        if (state != START_CURRENT) {
                            // Stopped, so the platform is advertising for a
                            // caller that no longer wants it. Undone here,
                            // because the stop that ran before this had
                            // nothing to stop -- but ONLY when nobody has
                            // asked to advertise since. stopAdvertising is
                            // global, so calling it when a newer start has
                            // taken over stopped that one instead.
                            if (state == START_ORPHANED) {
                                client().stopAdvertising();
                            }
                            NearbyTransport.deliverRequestFailed(requestId,
                                    NearbyError.SESSION_INVALIDATED.ordinal(),
                                    "advertising was stopped before it"
                                    + " started");
                            return;
                        }
                        NearbyTransport.deliverRequestOk(requestId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    public void onFailure(Exception e) {
                        // Nothing is advertising and nothing is trying to.
                        // Leaving the flag set told an older start whose
                        // success is still on its way that a live replacement
                        // owned the radio, so it declined to undo itself --
                        // and went on advertising after both an explicit stop
                        // and this failure.
                        //
                        // And an EARLIER start that already succeeded after
                        // being superseded left the platform advertising with
                        // its caller failed. Both resources have failed by
                        // now, so nobody is left to stop it but this.
                        failAndCleanUp(true, generation);
                        NearbyTransport.deliverRequestFailed(requestId,
                                NearbyError.SESSION_FAILED.ordinal(),
                                e.getMessage());
                    }
                });
    }

    public void stopAdvertising() {
        endStart(true);
        client().stopAdvertising();
    }

    public void startDiscovery(final int requestId, String serviceId,
            int strategy) {
        // Captured for THIS callback rather than read back out of the field
        // when an endpoint turns up. Starting discovery for "files" while
        // "chat" was running overwrote the field, and the callback still
        // installed for chat then labelled chat's endpoints as files -- which
        // happens even when Google rejects the second start as already
        // discovering. The field remains for the state a later call needs.
        final String started = serviceId == null ? "" : serviceId;
        this.discoveryServiceId = started;
        // The generation this start belongs to, for the reason
        // startAdvertising keeps one.
        final int generation = beginStart(false);
        DiscoveryOptions options = new DiscoveryOptions.Builder()
                .setStrategy(strategyFor(strategy))
                .build();
        client().startDiscovery(started, discoveryCallback(started), options)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    public void onSuccess(Void unused) {
                        int state = classifyStart(false, generation);
                        noteStartOutcome(false, state);
                        if (state != START_CURRENT) {
                            // Only when nobody has asked since, for the
                            // reason the advertising branch gives.
                            if (state == START_ORPHANED) {
                                client().stopDiscovery();
                            }
                            NearbyTransport.deliverRequestFailed(requestId,
                                    NearbyError.SESSION_INVALIDATED.ordinal(),
                                    "discovery was stopped before it"
                                    + " started");
                            return;
                        }
                        NearbyTransport.deliverRequestOk(requestId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    public void onFailure(Exception e) {
                        // Cleared, and the radio stopped if it was left
                        // running for nobody, for the reasons the advertising
                        // failure gives.
                        failAndCleanUp(false, generation);
                        NearbyTransport.deliverRequestFailed(requestId,
                                NearbyError.SESSION_FAILED.ordinal(),
                                e.getMessage());
                    }
                });
    }

    public void stopDiscovery() {
        endStart(false);
        client().stopDiscovery();
    }

    public void requestConnection(final int requestId, String endpointId,
            String localName) {
        String name = localName == null || localName.length() == 0
                ? this.localName : localName;
        // Connecting OUT, so the endpoint belongs to whatever discovery
        // found it -- the field is the right source here, and the mapping
        // discoveryCallback recorded is left alone when one already exists.
        client().requestConnection(name, endpointId,
                        connectionCallback(discoveryServiceId))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    public void onSuccess(Void unused) {
                        NearbyTransport.deliverRequestOk(requestId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    public void onFailure(Exception e) {
                        NearbyTransport.deliverRequestFailed(requestId,
                                NearbyError.PEER_UNAVAILABLE.ordinal(),
                                e.getMessage());
                    }
                });
    }

    public void acceptConnection(final int requestId, String endpointId) {
        client().acceptConnection(endpointId, payloadCallback())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    public void onSuccess(Void unused) {
                        NearbyTransport.deliverRequestOk(requestId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    public void onFailure(Exception e) {
                        NearbyTransport.deliverRequestFailed(requestId,
                                NearbyError.SESSION_FAILED.ordinal(),
                                e.getMessage());
                    }
                });
    }

    public void rejectConnection(String endpointId) {
        client().rejectConnection(endpointId);
    }

    public void sendPayload(final int requestId, String[] endpointIds,
            int payloadId, int payloadType, byte[] bytes, String path) {
        if (endpointIds == null || endpointIds.length == 0) {
            NearbyTransport.deliverRequestFailed(requestId,
                    NearbyError.PEER_UNAVAILABLE.ordinal(),
                    "no endpoints given");
            return;
        }
        Payload payload;
        try {
            if (payloadType == NearbyBridge.PAYLOAD_FILE) {
                String p = path;
                if (p != null && p.startsWith("file://")) {
                    p = p.substring(7);
                }
                // The sender's payload id rides in the file NAME, because a
                // FILE payload has nowhere else to put it and getId() on the
                // receiving side is otherwise Google's own local id -- a
                // different number from the one the sender was handed, and a
                // long truncated into an int besides.
                File source = new File(p);
                payload = Payload.fromFile(source);
                payload.setFileName(ID_PREFIX + payloadId + "-"
                        + source.getName());
            } else {
                    // Framed with the sender's payload id. Nearby Connections
                // mints its own id on each side, so without this the
                // receiver saw Google's local id -- which is not the one the
                // sender was handed, may collide, and cannot be matched to
                // the sender's progress events. Payload.getId() documents
                // the sender's id, so it has to travel with the bytes. Both
                // ends are Codename One, so the framing is symmetric.
                byte[] body = bytes == null ? new byte[0] : bytes;
                byte[] framed = new byte[body.length + 4];
                framed[0] = (byte) ((payloadId >> 24) & 0xff);
                framed[1] = (byte) ((payloadId >> 16) & 0xff);
                framed[2] = (byte) ((payloadId >> 8) & 0xff);
                framed[3] = (byte) (payloadId & 0xff);
                System.arraycopy(body, 0, framed, 4, body.length);
                payload = Payload.fromBytes(framed);
            }
        } catch (Exception e) {
            NearbyTransport.deliverRequestFailed(requestId,
                    NearbyError.IO_ERROR.ordinal(), e.getMessage());
            return;
        }
        // Nearby Connections mints its own payload id, and progress arrives
        // keyed on that one. Mapping it back is what lets the portable API
        // report progress against the id the app was handed.
        payloadIds.put(Long.valueOf(payload.getId()),
                Integer.valueOf(payloadId));
        payloadRecipients.put(Long.valueOf(payload.getId()),
                Integer.valueOf(endpointIds.length));
        java.util.List<String> targets = java.util.Arrays.asList(endpointIds);
        final Long platformKey = Long.valueOf(payload.getId());
        client().sendPayload(targets, payload)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    public void onSuccess(Void unused) {
                        NearbyTransport.deliverRequestOk(requestId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    public void onFailure(Exception e) {
                        // The mappings go with the failure. Nearby rejected
                        // the handoff, so no transfer update will ever arrive
                        // to clear them -- and every failed send left a pair
                        // of entries behind for the life of the process, with
                        // cancelPayload scanning stale payloads for good
                        // measure.
                        payloadIds.remove(platformKey);
                        payloadRecipients.remove(platformKey);
                        NearbyTransport.deliverRequestFailed(requestId,
                                NearbyError.IO_ERROR.ordinal(), e.getMessage());
                    }
                });
    }

    public void cancelPayload(int payloadId) {
        List<Long> doomed = new ArrayList<Long>();
        synchronized (payloadIds) {
            for (Map.Entry<Long, Integer> e : payloadIds.entrySet()) {
                if (e.getValue().intValue() == payloadId) {
                    // Collected, not cancelled in place: cancelPayload can
                    // reach back into payloadIds through a transfer update,
                    // and mutating the map mid-iteration is not something to
                    // rely on.
                    doomed.add(e.getKey());
                }
            }
        }
        // EVERY transfer under this portable id, not the first. The same
        // immutable Payload can be handed to two send() calls, which mints
        // two platform ids for one portable id -- so returning after the
        // first left the other running, free to report SUCCESS after the app
        // had cancelled it.
        for (Long platformId : doomed) {
            client().cancelPayload(platformId.longValue());
        }
    }

    public void disconnect(String endpointId) {
        // The name stays until onDisconnected has used it. That callback
        // encodes the endpoint through nameOf(), and clearing the cache here
        // handed the listener an endpoint with an empty name instead of the
        // peer's advertised one -- and onDisconnected already removes both
        // mappings itself.
        client().disconnectFromEndpoint(endpointId);
    }

    public void stopAllTransport() {
        // All three, because they are three independent operations.
        // stopAllEndpoints disconnects peers and leaves advertising and
        // discovery running, so an app that closed its feature UI carried on
        // broadcasting and scanning -- burning the radio and still taking
        // endpoint and connection callbacks -- while the public stop()
        // documents exactly the opposite.
        // The generations go up here too. stopAdvertising() and
        // stopDiscovery() bump them so a start still in flight cannot come
        // back and report success into a stop that already returned -- and
        // this method, which is what the public stop() calls, went straight
        // to the client and left them alone. A start pending across it
        // therefore passed its check and resolved as though it had survived
        // the stop.
        endStart(true);
        endStart(false);
        client().stopAdvertising();
        client().stopDiscovery();
        client().stopAllEndpoints();
        // Only the endpoints nothing will call back about. stopAllEndpoints
        // disconnects asynchronously and onDisconnected encodes each endpoint
        // through nameOf(), so clearing everything handed those callbacks an
        // endpoint with an empty name and service id -- the same defect the
        // single-endpoint disconnect() path had. A connected endpoint's
        // metadata is removed by its own callback; a merely discovered one
        // has no callback coming and is dropped here.
        synchronized (connectedEndpoints) {
            List<String> discoveredOnly = new ArrayList<String>(
                    endpointNames.keySet());
            discoveredOnly.removeAll(connectedEndpoints);
            for (String id : discoveredOnly) {
                endpointNames.remove(id);
                endpointServices.remove(id);
                connectionServices.remove(id);
            }
            // Discovery visibility goes for EVERYTHING, connected or not.
            // Discovery has stopped, so no onEndpointLost is coming for any
            // of these -- and onDisconnected only clears an endpoint's
            // metadata when discovery is no longer watching it. Leaving a
            // connected endpoint in this set made that check answer "still
            // discovered" forever, so its name and service survived the stop
            // and a later reuse of the same endpoint id inherited them.
            discoveredEndpoints.clear();
        }
        // The transfer maps are NOT cleared here either. stopAllEndpoints
        // produces terminal payload callbacks asynchronously, and those
        // callbacks are what map a platform id back to the portable one and
        // what turn an incoming file into a delivered payload -- so clearing
        // now made an outgoing terminal update fall back to Google's id, and
        // an incoming file report SUCCESS with its entry already discarded so
        // payloadReceived never followed.
        //
        // Each entry is removed by its own terminal update. What a vanished
        // endpoint strands is bounded by the transfers in flight at the stop,
        // and a later send overwrites by platform id, so the residue is a
        // handful of Long-to-Integer entries rather than a leak worth racing
        // the callbacks to clear.
    }

    // ------------------------------------------------------------------
    // Callbacks
    // ------------------------------------------------------------------

    private EndpointDiscoveryCallback discoveryCallback(
            final String serviceId) {
        return new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId,
                    DiscoveredEndpointInfo info) {
                discoveredEndpoints.add(endpointId);
                endpointNames.put(endpointId, info.getEndpointName());
                endpointServices.put(endpointId, serviceId);
                NearbyTransport.deliverEndpointFound(
                        encodeDiscovered(endpointId,
                                info.getEndpointName()), true);
            }

            @Override
            public void onEndpointLost(String endpointId) {
                // The DISCOVERY service, so this names the same one the
                // endpointFound for it did -- even where a connection
                // through another service came and went in between.
                NearbyTransport.deliverEndpointFound(
                        encodeDiscovered(endpointId, nameOf(endpointId)),
                        false);
                discoveredEndpoints.remove(endpointId);
                if (connectedEndpoints.contains(endpointId)) {
                    // Still connected, so the metadata has to stay: payload
                    // callbacks and the eventual onDisconnected encode this
                    // endpoint through nameOf(), and dropping it here handed
                    // the listener an empty name for the rest of a live
                    // connection. onDisconnected does the final cleanup.
                    return;
                }
                endpointNames.remove(endpointId);
                // The service mapping goes with it. Nearby reuses endpoint
                // ids, so a peer lost under the discovery service could come
                // back by CONNECTING to this device's advertisement for a
                // different service -- and onConnectionInitiated leaves an
                // existing entry alone, so it kept reporting the old one.
                endpointServices.remove(endpointId);
                connectionServices.remove(endpointId);
            }
        };
    }

    private ConnectionLifecycleCallback connectionCallback(
            final String serviceId) {
        return new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId,
                    ConnectionInfo info) {
                endpointNames.put(endpointId, info.getEndpointName());
                // Which service this connection belongs to depends on who
                // started it, and Nearby says which.
                //
                // INCOMING means the peer answered THIS advertisement, so
                // the service is the one this callback was built for, even
                // when discovery had already seen the same peer under
                // another. Treating any existing mapping as authoritative
                // labelled that connection -- and every lifecycle and
                // payload event on it -- with the service it was discovered
                // under rather than the one it was negotiated through, which
                // in an app running two services routes it to the wrong
                // protocol.
                //
                // OUTGOING keeps the mapping discovery recorded, because
                // this callback carries the discoveryServiceId FIELD, which
                // may have moved on since the endpoint was found.
                connectionServices.put(endpointId, serviceId);
                if (!endpointServices.containsKey(endpointId)) {
                    // Never discovered, so this is also the only service
                    // anything knows it by -- which is what an endpointLost
                    // for it would have to report.
                    endpointServices.put(endpointId, serviceId);
                }
                NearbyTransport.deliverConnectionRequested(
                        encode(endpointId, info.getEndpointName()),
                        info.getAuthenticationDigits());
            }

            @Override
            public void onConnectionResult(String endpointId,
                    ConnectionResolution resolution) {
                boolean ok = resolution.getStatus().getStatusCode()
                        == ConnectionsStatusCodes.STATUS_OK;
                if (ok) {
                    connectedEndpoints.add(endpointId);
                } else {
                    connectedEndpoints.remove(endpointId);
                }
                // Encoded BEFORE anything is removed: this is the event that
                // names the endpoint, and clearing first handed the listener
                // an empty name for exactly the inbound failures the cleanup
                // below exists for.
                NearbyTransport.deliverConnectionResult(
                        encode(endpointId, nameOf(endpointId)), ok,
                        ok ? 0 : NearbyError.SESSION_FAILED.ordinal(),
                        ok ? null : resolution.getStatus().getStatusMessage());
                if (!ok && !discoveredEndpoints.contains(endpointId)) {
                    // Arrived through advertising and never connected, so
                    // nothing else will ever call back about it: neither
                    // onEndpointLost, which only fires for something
                    // discovery saw, nor onDisconnected, which needs a
                    // connection. Left behind, each failed request kept its
                    // name and service id for the life of the process -- and
                    // the containsKey guard in onConnectionInitiated then
                    // preserved that stale service id if the same endpoint id
                    // came back under another advertised service.
                    endpointNames.remove(endpointId);
                    endpointServices.remove(endpointId);
                    connectionServices.remove(endpointId);
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                NearbyTransport.deliverDisconnected(
                        encode(endpointId, nameOf(endpointId)));
                connectedEndpoints.remove(endpointId);
                // The negotiated service goes with the connection that had
                // it; discovery's own entry is a separate question below.
                connectionServices.remove(endpointId);
                // Only when discovery has ALSO lost sight of it. A connection
                // can close while the peer is still being advertised and
                // still in the discovered set -- the app disconnects, or the
                // link drops -- and dropping the name and service there meant
                // the later onEndpointLost for that same peer encoded it out
                // of empty maps, so the app was told an endpoint it knew by
                // name had been lost, with no name and no service on it.
                //
                // When discovery is not watching it, this is the last event
                // that will ever mention the endpoint, so the entries go now
                // or they never do.
                if (!discoveredEndpoints.contains(endpointId)) {
                    endpointNames.remove(endpointId);
                    endpointServices.remove(endpointId);
                }
            }
        };
    }

    private PayloadCallback payloadCallback() {
        return new PayloadCallback() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                if (payload.getType() == Payload.Type.BYTES) {
                    // A BYTES payload arrives complete -- Nearby delivers the
                    // whole array in this callback. The first four bytes are
                    // the sender's payload id; see sendPayload.
                    byte[] raw = payload.asBytes();
                    int senderId = 0;
                    byte[] body = raw == null ? new byte[0] : raw;
                    if (body.length >= 4) {
                        senderId = ((body[0] & 0xff) << 24)
                                | ((body[1] & 0xff) << 16)
                                | ((body[2] & 0xff) << 8) | (body[3] & 0xff);
                        byte[] trimmed = new byte[body.length - 4];
                        System.arraycopy(body, 4, trimmed, 0, trimmed.length);
                        body = trimmed;
                    }
                    // Recorded so the terminal transfer update for this
                    // payload reports the SENDER's id too. payloadIds was
                    // written only by our own sendPayload, so an incoming
                    // transfer fell back to Google's receiver-local id and
                    // PayloadTransferUpdate.getPayloadId() disagreed with the
                    // Payload.getId() the app had just been handed.
                    payloadIds.put(Long.valueOf(payload.getId()),
                            Integer.valueOf(senderId));
                    NearbyTransport.deliverPayloadReceived(
                            encode(endpointId, nameOf(endpointId)),
                            senderId, NearbyBridge.PAYLOAD_BYTES, body, null);
                    return;
                }
                if (payload.getType() == Payload.Type.FILE) {
                    // A FILE payload is ANNOUNCED here, not delivered: the
                    // transfer has only started and the file on disk is
                    // partial. Handing it to the app now breaks the
                    // complete-payload contract of payloadReceived -- a
                    // listener would read a half-written file, and would be
                    // told about one that later failed or was cancelled.
                    // Held until the terminal SUCCESS update names this id.
                    incomingFiles.put(Long.valueOf(payload.getId()), payload);
                    // Same reason as the BYTES branch: progress for an
                    // incoming file has to be reported under the id the
                    // sender framed into the name, which is the id the
                    // delivered Payload will carry.
                    payloadIds.put(Long.valueOf(payload.getId()),
                            Integer.valueOf(senderIdOf(payload)));
                }
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId,
                    PayloadTransferUpdate update) {
                Long key = Long.valueOf(update.getPayloadId());
                Integer mapped = payloadIds.get(key);
                int id = mapped == null ? (int) update.getPayloadId()
                        : mapped.intValue();
                // An incoming FILE that Nearby calls SUCCESS is not a success
                // yet: the copy into app storage below can still fail, and
                // reporting terminal SUCCESS here and terminal FAILURE a few
                // lines later gave the receiver two contradictory terminal
                // states for one transfer -- with the first one arriving
                // first, so anything that finalizes on terminal status
                // finalized on the wrong one. Held back and emitted once,
                // when the outcome is actually known.
                boolean incomingFileSuccess = incomingFiles.containsKey(key)
                        && update.getStatus()
                                == PayloadTransferUpdate.Status.SUCCESS;
                if (!incomingFileSuccess) {
                    NearbyTransport.deliverPayloadProgress(
                            encode(endpointId, nameOf(endpointId)), id,
                            update.getBytesTransferred(),
                            update.getTotalBytes(),
                            statusFor(update.getStatus()).ordinal());
                }
                if (update.getStatus()
                        == PayloadTransferUpdate.Status.IN_PROGRESS) {
                    return;
                }
                // Kept until EVERY recipient is done. One payload sent to
                // several endpoints produces a terminal update per endpoint
                // under the same Nearby id, so dropping the mapping on the
                // first meant later recipients' progress was reported under
                // Google's local id, and cancel() could no longer reach the
                // transfers still running.
                Integer left = payloadRecipients.get(key);
                int remaining = left == null ? 0 : left.intValue() - 1;
                if (remaining > 0) {
                    payloadRecipients.put(key, Integer.valueOf(remaining));
                    return;
                }
                payloadRecipients.remove(key);
                payloadIds.remove(key);
                // The terminal update is where an incoming file becomes real.
                // Anything other than SUCCESS means the app never hears about
                // it, which is the point: a failed or cancelled transfer is
                // not a payload.
                Payload file = incomingFiles.remove(key);
                if (file == null
                        || update.getStatus()
                                != PayloadTransferUpdate.Status.SUCCESS) {
                    return;
                }
                // On a WORKER thread, because localPathFor copies the whole
                // file when scoped storage gives only a content URI -- and
                // Nearby delivers this callback on the main thread. A file
                // payload is the one meant for large data, so copying it
                // here froze the UI for as long as the copy took and put a
                // big enough transfer within reach of an ANR.
                //
                // Everything the delivery needs is read out first: this
                // callback's arguments do not outlive it.
                final Payload received = file;
                final String peer = encode(endpointId, nameOf(endpointId));
                final int senderId = senderIdOf(file);
                final long moved = update.getBytesTransferred();
                final long total = update.getTotalBytes();
                new Thread(new Runnable() {
                    public void run() {
                        String path = localPathFor(received);
                        if (path == null) {
                            // A payload whose only accessor cannot be used is
                            // worse than one that failed: getPath() is all a
                            // file Payload offers, so delivering it with a
                            // null path told the app the transfer succeeded
                            // and then gave it nothing to read. Reported as a
                            // failure instead, which is a state the API
                            // already documents.
                            NearbyTransport.deliverPayloadProgress(peer,
                                    senderId, 0, total,
                                    PayloadStatus.FAILURE.ordinal());
                            return;
                        }
                        // The terminal SUCCESS held back above, now that it
                        // is true.
                        NearbyTransport.deliverPayloadProgress(peer, senderId,
                                moved, total,
                                PayloadStatus.SUCCESS.ordinal());
                        NearbyTransport.deliverPayloadReceived(peer, senderId,
                                NearbyBridge.PAYLOAD_FILE, null,
                                "file://" + path);
                    }
                }, "CN1 nearby file receive").start();
            }
        };
    }

    // ------------------------------------------------------------------
    // Unused halves
    // ------------------------------------------------------------------

    public void prepareRangingSession(int requestId, int sessionHandle,
            boolean controller) {
    }

    public void startRanging(int requestId, int sessionHandle,
            byte[] peerToken) {
    }

    public void startAccessoryRanging(int requestId, int sessionHandle,
            byte[] accessoryData) {
    }

    public void stopRangingSession(int sessionHandle) {
    }

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

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private String nameOf(String endpointId) {
        String name = endpointNames.get(endpointId);
        return name == null ? "" : name;
    }

    /// Encodes an endpoint for a CONNECTION or payload event.
    ///
    /// The negotiated service wins where there is one: that is the service
    /// this connection belongs to, whatever discovery happened to see the
    /// peer under first.
    private String encode(String endpointId, String name) {
        String service = connectionServices.get(endpointId);
        if (service == null) {
            service = endpointServices.get(endpointId);
        }
        return sanitize(endpointId) + '\t' + sanitize(name) + '\t'
                + sanitize(service == null ? "" : service);
    }

    /// Encodes an endpoint for a DISCOVERY event.
    ///
    /// Always the service discovery saw, so found and lost name the same one
    /// even when a connection through another service came and went in
    /// between.
    private String encodeDiscovered(String endpointId, String name) {
        String service = endpointServices.get(endpointId);
        return sanitize(endpointId) + '\t' + sanitize(name) + '\t'
                + sanitize(service == null ? "" : service);
    }

    /// A readable local path for a received file.
    ///
    /// asJavaFile() is the easy case and increasingly not the one that
    /// happens: under scoped storage Nearby hands the file over as a content
    /// Uri or a descriptor, and the app has no way to open either through the
    /// portable API, whose file payload carries a path and nothing else. So
    /// the content is copied into the app's own files directory and that path
    /// is returned.
    ///
    /// #### Parameters
    ///
    /// - `file`: the received payload
    ///
    /// #### Returns
    ///
    /// an absolute path the app can read, or null when the content could not
    /// be reached at all
    private String localPathFor(Payload file) {
        try {
            Payload.File f = file.asFile();
            if (f == null) {
                return null;
            }
            java.io.File local = f.asJavaFile();
            if (local != null && local.exists()) {
                return local.getAbsolutePath();
            }
            android.net.Uri uri = f.asUri();
            if (uri == null) {
                return null;
            }
            java.io.File out = new java.io.File(context.getFilesDir(),
                    "cn1nearby-" + file.getId() + "-"
                    + sanitizeFileName(uri.getLastPathSegment()));
            java.io.InputStream in =
                    context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            try {
                java.io.OutputStream os = new java.io.FileOutputStream(out);
                try {
                    byte[] buffer = new byte[8192];
                    int read = in.read(buffer);
                    while (read > 0) {
                        os.write(buffer, 0, read);
                        read = in.read(buffer);
                    }
                } finally {
                    os.close();
                }
            } finally {
                in.close();
            }
            return out.getAbsolutePath();
        } catch (Throwable unreadable) {
            return null;
        }
    }

    /// Reduces a remote-chosen name to something safe to append to a
    /// directory. The name crossed the wire, so it is untrusted.
    private static String sanitizeFileName(String name) {
        if (name == null || name.length() == 0) {
            return "payload";
        }
        StringBuilder out = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '.' || c == '-'
                    || c == '_') {
                out.append(c);
            }
        }
        String safe = out.toString();
        if (safe.length() == 0 || ".".equals(safe) || "..".equals(safe)) {
            return "payload";
        }
        return safe;
    }

    /// The marker that carries a sender's payload id in a file name.
    private static final String ID_PREFIX = "cn1id-";

    /// The sender's payload id for an incoming file, recovered from the name
    /// it was sent under.
    ///
    /// Falls back to Google's local id when the name carries no marker, which
    /// is what a file from an older build would look like -- wrong, but no
    /// worse than it was before, and better than zero.
    private static int senderIdOf(Payload file) {
        // Read from the received file's NAME, both ways it can be reached.
        //
        // It was suggested this should call Payload.getFileName() instead.
        // There is no such method: play-services-nearby 19.3.0 has
        // Payload.setFileName(String) and no getter for it, and neither
        // Payload nor Payload.File exposes the transmitted name under any
        // other name -- javap over the whole
        // com.google.android.gms.nearby.connection package finds no
        // getFileName at all. setFileName is what makes the RECEIVED file
        // carry the sender's name, so asJavaFile().getName() is where that
        // name arrives.
        //
        // asUri() is the second route and not a redundant one: under scoped
        // storage asJavaFile() returns null and the Uri is all there is.
        String name = null;
        try {
            Payload.File f = file.asFile();
            if (f != null) {
                java.io.File local = f.asJavaFile();
                if (local != null) {
                    name = local.getName();
                }
                if (name == null && f.asUri() != null) {
                    name = f.asUri().getLastPathSegment();
                }
            }
        } catch (Throwable t) {
            name = null;
        }
        if (name != null && name.startsWith(ID_PREFIX)) {
            int dash = name.indexOf('-', ID_PREFIX.length());
            if (dash > ID_PREFIX.length()) {
                try {
                    return Integer.parseInt(
                            name.substring(ID_PREFIX.length(), dash));
                } catch (NumberFormatException notAnId) {
                    // Fall through to the local id.
                }
            }
        }
        return (int) file.getId();
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static Strategy strategyFor(int ordinal) {
        // The ordinals of com.codename1.nearby.transport.TransportStrategy.
        if (ordinal == 1) {
            return Strategy.P2P_STAR;
        }
        if (ordinal == 2) {
            return Strategy.P2P_POINT_TO_POINT;
        }
        return Strategy.P2P_CLUSTER;
    }

    private static PayloadStatus statusFor(int status) {
        if (status == PayloadTransferUpdate.Status.SUCCESS) {
            return PayloadStatus.SUCCESS;
        }
        if (status == PayloadTransferUpdate.Status.FAILURE) {
            return PayloadStatus.FAILURE;
        }
        if (status == PayloadTransferUpdate.Status.CANCELED) {
            return PayloadStatus.CANCELED;
        }
        return PayloadStatus.IN_PROGRESS;
    }
}
