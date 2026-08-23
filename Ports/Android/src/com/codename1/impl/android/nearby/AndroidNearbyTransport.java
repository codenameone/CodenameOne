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

    /// The service each endpoint was found under.
    ///
    /// One shared field was wrong: an app advertising service B while
    /// discovering service A had the later call overwrite it, and endpoints
    /// found under A were then encoded as B -- which Endpoint.getServiceId()
    /// documents as the service they were found under.
    private final Map<String, String> endpointServices =
            Collections.synchronizedMap(new HashMap<String, String>());
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
    private void add(ArrayList<String> perms, String permission) {
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
        this.advertisingServiceId = serviceId == null ? "" : serviceId;
        this.localName = localName == null ? "" : localName;
        AdvertisingOptions options = new AdvertisingOptions.Builder()
                .setStrategy(strategyFor(strategy))
                .build();
        client().startAdvertising(this.localName, this.advertisingServiceId,
                        connectionCallback(), options)
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

    public void stopAdvertising() {
        client().stopAdvertising();
    }

    public void startDiscovery(final int requestId, String serviceId,
            int strategy) {
        this.discoveryServiceId = serviceId == null ? "" : serviceId;
        DiscoveryOptions options = new DiscoveryOptions.Builder()
                .setStrategy(strategyFor(strategy))
                .build();
        client().startDiscovery(this.discoveryServiceId, discoveryCallback(),
                        options)
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

    public void stopDiscovery() {
        client().stopDiscovery();
    }

    public void requestConnection(final int requestId, String endpointId,
            String localName) {
        String name = localName == null || localName.length() == 0
                ? this.localName : localName;
        client().requestConnection(name, endpointId, connectionCallback())
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
        client().sendPayload(targets, payload)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    public void onSuccess(Void unused) {
                        NearbyTransport.deliverRequestOk(requestId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    public void onFailure(Exception e) {
                        NearbyTransport.deliverRequestFailed(requestId,
                                NearbyError.IO_ERROR.ordinal(), e.getMessage());
                    }
                });
    }

    public void cancelPayload(int payloadId) {
        synchronized (payloadIds) {
            for (Map.Entry<Long, Integer> e : payloadIds.entrySet()) {
                if (e.getValue().intValue() == payloadId) {
                    client().cancelPayload(e.getKey().longValue());
                    return;
                }
            }
        }
    }

    public void disconnect(String endpointId) {
        client().disconnectFromEndpoint(endpointId);
        endpointNames.remove(endpointId);
    }

    public void stopAllTransport() {
        client().stopAllEndpoints();
        endpointNames.clear();
        endpointServices.clear();
        payloadIds.clear();
        payloadRecipients.clear();
        incomingFiles.clear();
    }

    // ------------------------------------------------------------------
    // Callbacks
    // ------------------------------------------------------------------

    private EndpointDiscoveryCallback discoveryCallback() {
        return new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId,
                    DiscoveredEndpointInfo info) {
                endpointNames.put(endpointId, info.getEndpointName());
                endpointServices.put(endpointId, discoveryServiceId);
                NearbyTransport.deliverEndpointFound(
                        encode(endpointId, info.getEndpointName()), true);
            }

            @Override
            public void onEndpointLost(String endpointId) {
                NearbyTransport.deliverEndpointFound(
                        encode(endpointId, nameOf(endpointId)), false);
                endpointNames.remove(endpointId);
                // The service mapping goes with it. Nearby reuses endpoint
                // ids, so a peer lost under the discovery service could come
                // back by CONNECTING to this device's advertisement for a
                // different service -- and onConnectionInitiated leaves an
                // existing entry alone, so it kept reporting the old one.
                endpointServices.remove(endpointId);
            }
        };
    }

    private ConnectionLifecycleCallback connectionCallback() {
        return new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId,
                    ConnectionInfo info) {
                endpointNames.put(endpointId, info.getEndpointName());
                // An endpoint that arrives here without having been
                // discovered came in through advertising, so that is the
                // service it belongs to.
                if (!endpointServices.containsKey(endpointId)) {
                    endpointServices.put(endpointId, advertisingServiceId);
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
                NearbyTransport.deliverConnectionResult(
                        encode(endpointId, nameOf(endpointId)), ok,
                        ok ? 0 : NearbyError.SESSION_FAILED.ordinal(),
                        ok ? null : resolution.getStatus().getStatusMessage());
            }

            @Override
            public void onDisconnected(String endpointId) {
                NearbyTransport.deliverDisconnected(
                        encode(endpointId, nameOf(endpointId)));
                endpointNames.remove(endpointId);
                // Cleared with the name, for the reason onEndpointLost does.
                endpointServices.remove(endpointId);
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
                NearbyTransport.deliverPayloadProgress(
                        encode(endpointId, nameOf(endpointId)), id,
                        update.getBytesTransferred(), update.getTotalBytes(),
                        statusFor(update.getStatus()).ordinal());
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
                String path = localPathFor(file);
                if (path == null) {
                    // A payload whose only accessor cannot be used is worse
                    // than one that failed: getPath() is all a file Payload
                    // offers, so delivering it with a null path told the app
                    // the transfer succeeded and then gave it nothing to
                    // read. Reported as a failure instead, which is a state
                    // the API already documents.
                    NearbyTransport.deliverPayloadProgress(
                            encode(endpointId, nameOf(endpointId)),
                            senderIdOf(file), 0, update.getTotalBytes(),
                            PayloadStatus.FAILURE.ordinal());
                    return;
                }
                NearbyTransport.deliverPayloadReceived(
                        encode(endpointId, nameOf(endpointId)),
                        senderIdOf(file), NearbyBridge.PAYLOAD_FILE, null,
                        "file://" + path);
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

    private String encode(String endpointId, String name) {
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
