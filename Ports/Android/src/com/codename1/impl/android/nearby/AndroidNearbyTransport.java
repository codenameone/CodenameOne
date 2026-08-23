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
    private static final int MAX_BYTES_PAYLOAD = 32 * 1024;

    private final Context context;
    private final Map<String, String> endpointNames =
            Collections.synchronizedMap(new HashMap<String, String>());
    private final Map<Long, Integer> payloadIds =
            Collections.synchronizedMap(new HashMap<Long, Integer>());

    private String serviceId = "";
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
        this.serviceId = serviceId == null ? "" : serviceId;
        this.localName = localName == null ? "" : localName;
        AdvertisingOptions options = new AdvertisingOptions.Builder()
                .setStrategy(strategyFor(strategy))
                .build();
        client().startAdvertising(this.localName, this.serviceId,
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
        this.serviceId = serviceId == null ? "" : serviceId;
        DiscoveryOptions options = new DiscoveryOptions.Builder()
                .setStrategy(strategyFor(strategy))
                .build();
        client().startDiscovery(this.serviceId, discoveryCallback(), options)
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
                payload = Payload.fromFile(new File(p));
            } else {
                payload = Payload.fromBytes(bytes == null ? new byte[0]
                        : bytes);
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
        payloadIds.clear();
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
                NearbyTransport.deliverEndpointFound(
                        encode(endpointId, info.getEndpointName()), true);
            }

            @Override
            public void onEndpointLost(String endpointId) {
                NearbyTransport.deliverEndpointFound(
                        encode(endpointId, nameOf(endpointId)), false);
                endpointNames.remove(endpointId);
            }
        };
    }

    private ConnectionLifecycleCallback connectionCallback() {
        return new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId,
                    ConnectionInfo info) {
                endpointNames.put(endpointId, info.getEndpointName());
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
            }
        };
    }

    private PayloadCallback payloadCallback() {
        return new PayloadCallback() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                if (payload.getType() == Payload.Type.BYTES) {
                    NearbyTransport.deliverPayloadReceived(
                            encode(endpointId, nameOf(endpointId)),
                            (int) payload.getId(), NearbyBridge.PAYLOAD_BYTES,
                            payload.asBytes(), null);
                    return;
                }
                if (payload.getType() == Payload.Type.FILE
                        && payload.asFile() != null) {
                    java.io.File f = null;
                    try {
                        f = payload.asFile().asJavaFile();
                    } catch (Throwable t) {
                        // Older Play services return the file only through a
                        // ParcelFileDescriptor; nothing to hand the app then.
                    }
                    NearbyTransport.deliverPayloadReceived(
                            encode(endpointId, nameOf(endpointId)),
                            (int) payload.getId(), NearbyBridge.PAYLOAD_FILE,
                            null, f == null ? null
                                    : "file://" + f.getAbsolutePath());
                }
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId,
                    PayloadTransferUpdate update) {
                Integer mapped = payloadIds.get(
                        Long.valueOf(update.getPayloadId()));
                int id = mapped == null ? (int) update.getPayloadId()
                        : mapped.intValue();
                NearbyTransport.deliverPayloadProgress(
                        encode(endpointId, nameOf(endpointId)), id,
                        update.getBytesTransferred(), update.getTotalBytes(),
                        statusFor(update.getStatus()).ordinal());
                if (update.getStatus()
                        != PayloadTransferUpdate.Status.IN_PROGRESS) {
                    payloadIds.remove(Long.valueOf(update.getPayloadId()));
                }
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
        return sanitize(endpointId) + '\t' + sanitize(name) + '\t'
                + sanitize(serviceId);
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
