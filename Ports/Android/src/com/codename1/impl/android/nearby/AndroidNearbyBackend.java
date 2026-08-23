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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.companion.WifiDeviceFilter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.net.MacAddress;
import android.os.ParcelUuid;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.CodenameOneActivity;
import com.codename1.impl.android.IntentResultListener;
import com.codename1.nearby.NearbyAvailability;
import com.codename1.nearby.NearbyError;
import com.codename1.nearby.companion.CompanionDevices;
import com.codename1.nearby.spi.NearbyBridge;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/// The Android nearby implementation, compiled inside the generated app
/// rather than into the port jar.
///
/// It owns the companion-device half directly -- `CompanionDeviceManager` is
/// a framework class and needs no dependency, only an SDK newer than the one
/// the port jar is built against -- and reaches the other two halves
/// reflectively, for the same reason this class is itself reached
/// reflectively: an app that only associates accessories has neither
/// `androidx.core.uwb` nor `play-services-nearby` on its classpath, and the
/// builder has deleted the classes that would import them.
public class AndroidNearbyBackend implements NearbyBridge {

    /// Request code for the association chooser. Picked high to stay clear of
    /// the port's own IntentResultListener constants.
    private static final int ASSOCIATE_REQUEST = 0x4E42;

    private final Activity activity;
    private final NearbyBridge ranging;
    private final NearbyBridge transport;

    private int pendingAssociateRequest;

    public AndroidNearbyBackend(Activity activity) {
        this.activity = activity;
        this.ranging = load("com.codename1.impl.android.nearby."
                + "AndroidUwbRanging");
        this.transport = load("com.codename1.impl.android.nearby."
                + "AndroidNearbyTransport");
    }

    private NearbyBridge load(String className) {
        Object instance = null;
        try {
            Class<?> clazz = Class.forName(className);
            instance = clazz.getConstructor(Context.class)
                    .newInstance(activity);
        } catch (Throwable t) {
            // The builder deletes the half an app did not reference, so this
            // is the ordinary path rather than an error.
            instance = null;
        }
        // Guarded with instanceof rather than cast inside the catch: a failed
        // cast does not throw under ParparVM, so catching one is a handler
        // that never runs.
        return instance instanceof NearbyBridge ? (NearbyBridge) instance
                : null;
    }

    // ------------------------------------------------------------------
    // Shared
    // ------------------------------------------------------------------

    public boolean isRangingSupported() {
        return ranging != null && ranging.isRangingSupported();
    }

    public boolean isTransportSupported() {
        return transport != null && transport.isTransportSupported();
    }

    public boolean isCompanionSupported() {
        return Build.VERSION.SDK_INT >= 26 && manager() != null;
    }

    public int getRangingAvailability() {
        return ranging == null ? NearbyAvailability.NOT_SUPPORTED.ordinal()
                : ranging.getRangingAvailability();
    }

    public int getTransportAvailability() {
        return transport == null ? NearbyAvailability.NOT_SUPPORTED.ordinal()
                : transport.getTransportAvailability();
    }

    public int getCompanionAvailability() {
        return isCompanionSupported()
                ? NearbyAvailability.AVAILABLE.ordinal()
                : NearbyAvailability.NOT_SUPPORTED.ordinal();
    }

    public void requestPermissions(int requestId, int permissionBits) {
        // Owned here rather than delegated to the two optional backends, for
        // two reasons that between them killed the previous arrangement.
        //
        // Delegating by "whichever half is loaded" handed an app that uses
        // both its discovery, advertise and connect bits to the UWB backend,
        // which knows only UWB_RANGING, ignores the rest and reports success
        // -- so the transport grants were never requested and the first
        // advertise failed for a permission the user never saw. Splitting the
        // request in two instead needs the two answers joined into the single
        // result the caller is waiting on, and neither backend can be asked
        // for a partial answer through an SPI whose only reply path is a
        // request id the caller owns.
        //
        // None of this needs an optional dependency: these are platform
        // permission strings and AndroidImplementation.checkForPermission is
        // in the always-compiled half of the port. So one list, one pass, one
        // answer.
        final ArrayList<String> perms = new ArrayList<String>();
        if ((permissionBits & NearbyBridge.PERMISSION_RANGING) != 0
                && Build.VERSION.SDK_INT >= 31) {
            add(perms, "android.permission.UWB_RANGING");
        }
        boolean transportBits = (permissionBits
                & (NearbyBridge.PERMISSION_DISCOVERY
                        | NearbyBridge.PERMISSION_ADVERTISE
                        | NearbyBridge.PERMISSION_CONNECT)) != 0;
        if (transportBits) {
            if (Build.VERSION.SDK_INT >= 31) {
                if ((permissionBits & NearbyBridge.PERMISSION_DISCOVERY) != 0) {
                    add(perms, "android.permission.BLUETOOTH_SCAN");
                }
                if ((permissionBits & NearbyBridge.PERMISSION_ADVERTISE) != 0) {
                    add(perms, "android.permission.BLUETOOTH_ADVERTISE");
                }
                if ((permissionBits & NearbyBridge.PERMISSION_CONNECT) != 0) {
                    add(perms, "android.permission.BLUETOOTH_CONNECT");
                }
            }
            if (Build.VERSION.SDK_INT >= 33) {
                add(perms, "android.permission.NEARBY_WIFI_DEVICES");
            } else {
                // Below 33 Nearby Connections genuinely refuses to start
                // without a location grant; it is not a scan-results
                // technicality there.
                add(perms, "android.permission.ACCESS_FINE_LOCATION");
            }
        }
        if (perms.isEmpty()) {
            // Nothing left to ask for -- everything is already granted, or the
            // request was for association, which needs no runtime permission
            // on any Android version because the chooser IS the consent.
            com.codename1.nearby.ranging.Ranging
                    .deliverPermissionResult(requestId, true);
            return;
        }
        // checkForPermission blocks through invokeAndBlock and must run on the
        // EDT.
        Display.getInstance().callSerially(
                permissionRunnable(requestId, perms));
    }

    /// Adds a permission the app has not already been granted.
    private void add(ArrayList<String> perms, String permission) {
        if (activity.checkSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(permission);
        }
    }

    /// Static so the Runnable carries no synthetic outer reference, which
    /// SpotBugs reports as SIC_INNER_SHOULD_BE_STATIC_ANON.
    private static Runnable permissionRunnable(final int requestId,
            final ArrayList<String> perms) {
        return new Runnable() {
            @Override
            public void run() {
                boolean all = true;
                for (String permission : perms) {
                    all = AndroidImplementation.checkForPermission(permission,
                            "This is required to find nearby devices") && all;
                }
                com.codename1.nearby.ranging.Ranging.deliverPermissionResult(
                        requestId, all);
            }
        };
    }

    // ------------------------------------------------------------------
    // Ranging
    // ------------------------------------------------------------------

    public int getRangingCapabilities() {
        return ranging == null ? 0 : ranging.getRangingCapabilities();
    }

    public void prepareRangingSession(int requestId, int sessionHandle,
            boolean controller) {
        if (ranging == null) {
            failRanging(requestId);
            return;
        }
        ranging.prepareRangingSession(requestId, sessionHandle, controller);
    }

    public void startRanging(int requestId, int sessionHandle,
            byte[] peerToken) {
        if (ranging == null) {
            failRanging(requestId);
            return;
        }
        ranging.startRanging(requestId, sessionHandle, peerToken);
    }

    public void startAccessoryRanging(int requestId, int sessionHandle,
            byte[] accessoryData) {
        if (ranging == null) {
            failRanging(requestId);
            return;
        }
        ranging.startAccessoryRanging(requestId, sessionHandle, accessoryData);
    }

    public void stopRangingSession(int sessionHandle) {
        if (ranging != null) {
            ranging.stopRangingSession(sessionHandle);
        }
    }

    // ------------------------------------------------------------------
    // Companion
    // ------------------------------------------------------------------

    private CompanionDeviceManager manager() {
        if (Build.VERSION.SDK_INT < 26 || activity == null) {
            return null;
        }
        try {
            return (CompanionDeviceManager) activity.getSystemService(
                    Context.COMPANION_DEVICE_SERVICE);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressLint("MissingPermission")
    public void associate(final int requestId, int profile,
            boolean singleDevice, String[] filters) {
        final CompanionDeviceManager cdm = manager();
        if (cdm == null) {
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.NOT_SUPPORTED.ordinal(),
                    "companion association needs Android 8 or later");
            return;
        }
        if (pendingAssociateRequest != 0) {
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.BUSY.ordinal(),
                    "an association chooser is already open");
            return;
        }
        AssociationRequest.Builder request = new AssociationRequest.Builder();
        request.setSingleDevice(singleDevice);
        if (Build.VERSION.SDK_INT >= 31) {
            String deviceProfile = profileFor(profile);
            if (deviceProfile != null) {
                request.setDeviceProfile(deviceProfile);
            }
        }
        boolean anyFilter = false;
        for (int i = 0; filters != null && i < filters.length; i++) {
            if (addFilter(request, filters[i])) {
                anyFilter = true;
            }
        }
        if (!anyFilter) {
            // An unfiltered request is legal and shows everything the radios
            // can see. Left as is rather than refused: that is the same thing
            // an empty filter list means in the portable API.
            request.addDeviceFilter(new BluetoothLeDeviceFilter.Builder()
                    .build());
        }
        pendingAssociateRequest = requestId;
        listenForResult(requestId, cdm);
        cdm.associate(request.build(), new CompanionDeviceManager.Callback() {
            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                launch(chooserLauncher, requestId);
            }

            @Override
            public void onFailure(CharSequence error) {
                pendingAssociateRequest = 0;
                CompanionDevices.deliverRequestFailed(requestId,
                        NearbyError.PEER_UNAVAILABLE.ordinal(),
                        error == null ? null : error.toString());
            }
        }, new Handler(Looper.getMainLooper()));
    }

    private void launch(IntentSender chooserLauncher, int requestId) {
        try {
            activity.startIntentSenderForResult(chooserLauncher,
                    ASSOCIATE_REQUEST, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            pendingAssociateRequest = 0;
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.UNKNOWN.ordinal(), e.getMessage());
        }
    }

    private void listenForResult(final int requestId,
            final CompanionDeviceManager cdm) {
        if (!(activity instanceof CodenameOneActivity)) {
            return;
        }
        final CodenameOneActivity host = (CodenameOneActivity) activity;
        host.setIntentResultListener(new IntentResultListener() {
            public void onActivityResult(int requestCode, int resultCode,
                    Intent data) {
                if (requestCode != ASSOCIATE_REQUEST) {
                    return;
                }
                host.restoreIntentResultListener();
                pendingAssociateRequest = 0;
                if (resultCode != Activity.RESULT_OK) {
                    CompanionDevices.deliverRequestFailed(requestId,
                            NearbyError.USER_CANCELED.ordinal(),
                            "the user dismissed the chooser");
                    return;
                }
                String encoded = newestAssociation(cdm, data);
                if (encoded == null) {
                    CompanionDevices.deliverRequestFailed(requestId,
                            NearbyError.UNKNOWN.ordinal(),
                            "the chooser returned no device");
                } else {
                    CompanionDevices.deliverAssociated(requestId, encoded);
                }
            }
        });
    }

    /// The association the chooser just created.
    ///
    /// Read back from the platform rather than from the returned intent
    /// wherever possible: on API 33 and later the association carries an id
    /// and a display name the intent extra does not, and that id is what
    /// `disassociate` and presence observation take.
    @SuppressLint("MissingPermission")
    private String newestAssociation(CompanionDeviceManager cdm, Intent data) {
        if (Build.VERSION.SDK_INT >= 33) {
            List<AssociationInfo> all = cdm.getMyAssociations();
            if (all != null && !all.isEmpty()) {
                return encode(all.get(all.size() - 1), true);
            }
        }
        if (data != null) {
            Object extra = data.getParcelableExtra(
                    CompanionDeviceManager.EXTRA_DEVICE);
            if (extra instanceof BluetoothDevice) {
                BluetoothDevice d = (BluetoothDevice) extra;
                return encodeLegacy(d.getAddress(), d.getAddress(), true);
            }
        }
        List<String> legacy = cdm.getAssociations();
        if (legacy != null && !legacy.isEmpty()) {
            String mac = legacy.get(legacy.size() - 1);
            return encodeLegacy(mac, mac, true);
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    public String[] getAssociations() {
        CompanionDeviceManager cdm = manager();
        if (cdm == null) {
            return new String[0];
        }
        List<String> out = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= 33) {
            List<AssociationInfo> all = cdm.getMyAssociations();
            for (int i = 0; all != null && i < all.size(); i++) {
                out.add(encode(all.get(i), false));
            }
        } else {
            List<String> legacy = cdm.getAssociations();
            for (int i = 0; legacy != null && i < legacy.size(); i++) {
                out.add(encodeLegacy(legacy.get(i), legacy.get(i), false));
            }
        }
        return out.toArray(new String[out.size()]);
    }

    @SuppressLint("MissingPermission")
    public void disassociate(int requestId, String associationId) {
        CompanionDeviceManager cdm = manager();
        if (cdm == null || associationId == null) {
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.NOT_SUPPORTED.ordinal(), null);
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                List<AssociationInfo> all = cdm.getMyAssociations();
                for (int i = 0; all != null && i < all.size(); i++) {
                    if (idOf(all.get(i)).equals(associationId)) {
                        cdm.disassociate(all.get(i).getId());
                        CompanionDevices.deliverDisassociated(requestId);
                        return;
                    }
                }
                CompanionDevices.deliverRequestFailed(requestId,
                        NearbyError.PEER_UNAVAILABLE.ordinal(),
                        "no such association");
                return;
            }
            cdm.disassociate(associationId);
            CompanionDevices.deliverDisassociated(requestId);
        } catch (Throwable t) {
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.UNKNOWN.ordinal(), t.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    public boolean startObservingPresence(String associationId) {
        CompanionDeviceManager cdm = manager();
        if (cdm == null || associationId == null
                || Build.VERSION.SDK_INT < 31) {
            return false;
        }
        try {
            // Takes the MAC address on every version that has it, which is
            // what the encoded address field carries.
            cdm.startObservingDevicePresence(addressOf(cdm, associationId));
            CN1CompanionDeviceService.register(associationId);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    public void stopObservingPresence(String associationId) {
        CompanionDeviceManager cdm = manager();
        if (cdm == null || associationId == null
                || Build.VERSION.SDK_INT < 31) {
            return;
        }
        try {
            cdm.stopObservingDevicePresence(addressOf(cdm, associationId));
            CN1CompanionDeviceService.unregister(associationId);
        } catch (Throwable t) {
            // Nothing to report: the caller asked to stop and it is stopped
            // either way.
        }
    }

    // ------------------------------------------------------------------
    // Transport
    // ------------------------------------------------------------------

    public int getMaxPayloadSize() {
        return transport == null ? 0 : transport.getMaxPayloadSize();
    }

    public void startAdvertising(int requestId, String serviceId,
            String localName, int strategy) {
        if (transport == null) {
            failTransport(requestId);
            return;
        }
        transport.startAdvertising(requestId, serviceId, localName, strategy);
    }

    public void stopAdvertising() {
        if (transport != null) {
            transport.stopAdvertising();
        }
    }

    public void startDiscovery(int requestId, String serviceId, int strategy) {
        if (transport == null) {
            failTransport(requestId);
            return;
        }
        transport.startDiscovery(requestId, serviceId, strategy);
    }

    public void stopDiscovery() {
        if (transport != null) {
            transport.stopDiscovery();
        }
    }

    public void requestConnection(int requestId, String endpointId,
            String localName) {
        if (transport == null) {
            failTransport(requestId);
            return;
        }
        transport.requestConnection(requestId, endpointId, localName);
    }

    public void acceptConnection(int requestId, String endpointId) {
        if (transport == null) {
            failTransport(requestId);
            return;
        }
        transport.acceptConnection(requestId, endpointId);
    }

    public void rejectConnection(String endpointId) {
        if (transport != null) {
            transport.rejectConnection(endpointId);
        }
    }

    public void sendPayload(int requestId, String[] endpointIds, int payloadId,
            int payloadType, byte[] bytes, String path) {
        if (transport == null) {
            failTransport(requestId);
            return;
        }
        transport.sendPayload(requestId, endpointIds, payloadId, payloadType,
                bytes, path);
    }

    public void cancelPayload(int payloadId) {
        if (transport != null) {
            transport.cancelPayload(payloadId);
        }
    }

    public void disconnect(String endpointId) {
        if (transport != null) {
            transport.disconnect(endpointId);
        }
    }

    public void stopAllTransport() {
        if (transport != null) {
            transport.stopAllTransport();
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static String profileFor(int profile) {
        // The ordinals of com.codename1.nearby.companion.CompanionProfile.
        if (Build.VERSION.SDK_INT < 31) {
            return null;
        }
        switch (profile) {
            case 1:
                return AssociationRequest.DEVICE_PROFILE_WATCH;
            case 2:
                // GLASSES is API 34 and COMPUTER is 33 -- not the other way
                // round, which is the order the enum happens to declare them
                // in. Passing the platform a profile string it does not know
                // throws, so these two gates were checked against the SDK's
                // own api-versions.xml rather than guessed from the ordinal.
                return Build.VERSION.SDK_INT >= 34
                        ? AssociationRequest.DEVICE_PROFILE_GLASSES : null;
            case 3:
                return Build.VERSION.SDK_INT >= 33
                        ? AssociationRequest.DEVICE_PROFILE_COMPUTER : null;
            default:
                // GENERIC. Deliberately no profile at all rather than a
                // harmless-looking one: a profile is a request for elevated
                // privileges and shows the user a stronger prompt.
                return null;
        }
    }

    private static boolean addFilter(AssociationRequest.Builder request,
            String encoded) {
        String[] fields = encoded == null ? null : encoded.split("\t", -1);
        if (fields == null || fields.length < 2) {
            return false;
        }
        int kind;
        try {
            kind = Integer.parseInt(fields[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        String value = fields[1];
        // The kind constants of com.codename1.nearby.companion.DeviceFilter.
        if (kind == 0) {
            try {
                request.addDeviceFilter(new BluetoothLeDeviceFilter.Builder()
                        .setScanFilter(new android.bluetooth.le.ScanFilter
                                .Builder()
                                .setServiceUuid(ParcelUuid.fromString(
                                        expandUuid(value)))
                                .build())
                        .build());
                return true;
            } catch (Throwable t) {
                return false;
            }
        }
        if (kind == 1) {
            try {
                request.addDeviceFilter(new BluetoothLeDeviceFilter.Builder()
                        .setNamePattern(Pattern.compile(value))
                        .build());
                return true;
            } catch (Throwable t) {
                return false;
            }
        }
        if (kind == 2) {
            request.addDeviceFilter(
                    new android.companion.BluetoothDeviceFilter.Builder()
                            .setAddress(value)
                            .build());
            return true;
        }
        if (kind == 3) {
            request.addDeviceFilter(new WifiDeviceFilter.Builder()
                    .setNamePattern(Pattern.compile(Pattern.quote(value)))
                    .build());
            return true;
        }
        return false;
    }

    /// Expands the 16-bit short form of a Bluetooth UUID into the full one,
    /// which is what `ParcelUuid` requires. `"180D"` and the spelled-out
    /// 128-bit form must both work, because the portable API documents both.
    private static String expandUuid(String uuid) {
        String u = uuid.trim();
        if (u.length() == 4) {
            return "0000" + u + "-0000-1000-8000-00805F9B34FB";
        }
        if (u.length() == 8) {
            return u + "-0000-1000-8000-00805F9B34FB";
        }
        return u;
    }

    /// The association's MAC address as a string, or null when it has none.
    ///
    /// `AssociationInfo.getDeviceMacAddress()` returns an `android.net
    /// .MacAddress`, not a string -- the string-returning form is a hidden
    /// API that a normal app cannot call. Its `toString()` is the
    /// colon-separated lowercase form, which is what
    /// `startObservingDevicePresence` and `disassociate` take.
    private static String macOf(AssociationInfo info) {
        MacAddress address = info.getDeviceMacAddress();
        return address == null ? null : address.toString();
    }

    private static String idOf(AssociationInfo info) {
        String mac = macOf(info);
        return mac != null ? mac : Integer.toString(info.getId());
    }

    private static String encode(AssociationInfo info, boolean present) {
        String mac = macOf(info);
        CharSequence name = info.getDisplayName();
        return join(idOf(info), name == null ? "" : name.toString(),
                mac == null ? "" : mac, present);
    }

    private static String encodeLegacy(String id, String mac,
            boolean present) {
        return join(id, mac == null ? "" : mac, mac == null ? "" : mac,
                present);
    }

    /// Builds the record `com.codename1.impl.nearby.NearbyWire` decodes.
    ///
    /// The profile field is always zero: Android does not report back which
    /// profile an association was made under, and guessing would be worse
    /// than saying GENERIC.
    private static String join(String id, String name, String address,
            boolean present) {
        return sanitize(id) + '\t' + sanitize(name) + '\t' + sanitize(address)
                + "\t0\t" + (present ? '1' : '0');
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    @SuppressLint("MissingPermission")
    private static String addressOf(CompanionDeviceManager cdm, String id) {
        if (Build.VERSION.SDK_INT >= 33) {
            List<AssociationInfo> all = cdm.getMyAssociations();
            for (int i = 0; all != null && i < all.size(); i++) {
                if (idOf(all.get(i)).equals(id)) {
                    String mac = macOf(all.get(i));
                    if (mac != null) {
                        return mac;
                    }
                }
            }
        }
        return id;
    }

    private static void failRanging(int requestId) {
        com.codename1.nearby.ranging.Ranging.deliverRequestFailed(requestId,
                NearbyError.NOT_SUPPORTED.ordinal(),
                "this build does not include precision ranging");
    }

    private static void failTransport(int requestId) {
        com.codename1.nearby.transport.NearbyTransport.deliverRequestFailed(
                requestId, NearbyError.NOT_SUPPORTED.ordinal(),
                "this build does not include the nearby transport");
    }
}
