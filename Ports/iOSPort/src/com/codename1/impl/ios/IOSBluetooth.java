/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.ios;

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.Bluetooth;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothPermission;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.le.AdvertisementData;
import com.codename1.bluetooth.le.L2capServer;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.bluetooth.le.server.BleAdvertisement;
import com.codename1.bluetooth.le.server.GattServer;
import com.codename1.io.Util;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * iOS / Mac Catalyst implementation of {@link Bluetooth} backed by
 * CoreBluetooth (CN1Bluetooth.m in nativeSources). BLE central and
 * peripheral roles plus L2CAP channels are supported; classic Bluetooth is
 * not exposed by iOS so {@link #isClassicSupported()} stays {@code false}
 * and the base no-op {@code getClassic()} instance is returned.
 *
 * <p>The CoreBluetooth managers are created lazily on the native side
 * (creating a CBCentralManager pops the OS permission dialog), driven by
 * {@link IOSNative#startBluetoothStateMonitor()} which fires on first use
 * of any adapter-state / permission / scan / connect entry point.</p>
 *
 * <p>The native side dispatches every event back through the static
 * {@code nativeBt*} methods below on a dedicated serial GCD queue. The
 * static initializer touches each callback so the ParparVM dead-code
 * eliminator cannot strip them from release builds.</p>
 */
public final class IOSBluetooth extends Bluetooth {

    // Error codes shared with CN1Bluetooth.m -- keep the two lists in sync.
    static final int ERR_UNKNOWN = 0;
    static final int ERR_NOT_SUPPORTED = 1;
    static final int ERR_POWERED_OFF = 2;
    static final int ERR_UNAUTHORIZED = 3;
    static final int ERR_SCAN_FAILED = 4;
    static final int ERR_ADVERTISE_FAILED = 5;
    static final int ERR_CONNECTION_FAILED = 6;
    static final int ERR_CONNECTION_LOST = 7;
    static final int ERR_NOT_CONNECTED = 8;
    static final int ERR_GATT = 9;
    static final int ERR_TIMEOUT = 10;
    static final int ERR_IO = 11;

    // CBManagerAuthorization values as reported by the native side.
    static final int AUTH_NOT_DETERMINED = 0;
    static final int AUTH_RESTRICTED = 1;
    static final int AUTH_DENIED = 2;
    static final int AUTH_ALLOWED = 3;

    /** Pending request-id registry -- see IOSNfc.REQUESTS for the pattern. */
    private static final Map<Integer, Object> REQUESTS =
            new HashMap<Integer, Object>();
    private static int nextRequestId = 1;

    /** Canonical peripheral instances keyed by identifier UUID string. */
    private static final Map<String, IOSBlePeripheral> PERIPHERALS =
            new HashMap<String, IOSBlePeripheral>();

    /** Published L2CAP servers keyed by PSM, for incoming-channel routing. */
    private static final Map<Integer, IOSL2capServer> L2CAP_SERVERS =
            new HashMap<Integer, IOSL2capServer>();

    /** Permission requests waiting for the CoreBluetooth auth to settle. */
    private static final List<AsyncResource<Boolean>> PENDING_PERMISSIONS =
            new ArrayList<AsyncResource<Boolean>>();

    private static volatile IOSBluetooth instance;

    static {
        // ---- do not remove: defeats ParparVM dead-code elimination ----
        // Placed after the static field initializers (class-init runs in
        // textual order) so the sentinel calls find live registries.
        nativeBtStateChanged(-1, -1);
        nativeBtScanResult(null, null, 0, false, null, null, null, null, -999);
        nativeBtConnected(null);
        nativeBtConnectFailed(null, 0, null);
        nativeBtDisconnected(null, 0, null);
        nativeBtServicesDiscovered(-1, null, null);
        nativeBtValue(-1, null);
        nativeBtNotification(null, null, 0, null, 0, null);
        nativeBtOperationComplete(-1);
        nativeBtRssi(-1, 0);
        nativeBtRequestError(-1, 0, null);
        nativeBtServicesInvalidated(null);
        nativeBtGattServerOpened(-1);
        nativeBtAdvertiseStarted(-1);
        nativeBtReadRequest(0, null, -1, -1, 0);
        nativeBtWriteRequest(0, null, -1, -1, null, 0, false);
        nativeBtSubscriptionChanged(null, 0, -1, false);
        nativeBtL2capOpened(-1, 0, 0);
        nativeBtL2capPublished(-1, 0);
        nativeBtL2capIncoming(0, 0);
    }

    final IOSNative nativeInstance;
    private final IOSBluetoothLE le;
    private volatile AdapterState adapterState = AdapterState.UNKNOWN;
    private volatile boolean monitorStarted;

    IOSBluetooth(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
        this.le = new IOSBluetoothLE(this);
        instance = this;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public boolean isLeSupported() {
        return true;
    }

    @Override
    public boolean isClassicSupported() {
        // iOS does not expose classic Bluetooth (RFCOMM) to apps; the base
        // class no-op getClassic() instance is intentionally kept.
        return false;
    }

    @Override
    public boolean isPeripheralModeSupported() {
        // CBPeripheralManager is unavailable on tvOS / watchOS; the native
        // capability check compiles the answer per target slice.
        return nativeInstance.isBlePeripheralSupported();
    }

    @Override
    public boolean isL2capSupported() {
        return true;
    }

    @Override
    public AdapterState getAdapterState() {
        // Creating the CBCentralManager is what pops the permission dialog,
        // so the monitor only starts when the app actually asks about
        // Bluetooth -- never as an app-startup side effect.
        ensureMonitor();
        return adapterState;
    }

    @Override
    public boolean hasPermission(BluetoothPermission permission) {
        // CoreBluetooth has a single privacy authorization covering scan,
        // connect and advertise alike; all three map to it.
        return nativeInstance.getBluetoothAuthorization() == AUTH_ALLOWED;
    }

    @Override
    public AsyncResource<Boolean> requestPermissions(
            BluetoothPermission... permissions) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        int auth = nativeInstance.getBluetoothAuthorization();
        if (auth != AUTH_NOT_DETERMINED) {
            r.complete(Boolean.valueOf(auth == AUTH_ALLOWED));
            return r;
        }
        synchronized (PENDING_PERMISSIONS) {
            PENDING_PERMISSIONS.add(r);
        }
        // Creating the manager triggers the system dialog; the resulting
        // state callback (auth != notDetermined) resolves the request.
        ensureMonitor();
        return r;
    }

    // requestEnable() intentionally keeps the base behavior: iOS offers no
    // programmatic enable flow, so it resolves false.

    @Override
    public com.codename1.bluetooth.le.BluetoothLE getLE() {
        return le;
    }

    // ------------------------------------------------------------------
    // package plumbing
    // ------------------------------------------------------------------

    void ensureMonitor() {
        if (!monitorStarted) {
            monitorStarted = true;
            nativeInstance.startBluetoothStateMonitor();
        }
    }

    IOSBluetoothLE getLEImpl() {
        return le;
    }

    static IOSBluetooth getIOSInstance() {
        return instance;
    }

    static int takeId(Object holder) {
        synchronized (REQUESTS) {
            int id = nextRequestId++;
            REQUESTS.put(Integer.valueOf(id), holder);
            return id;
        }
    }

    static Object take(int requestId) {
        synchronized (REQUESTS) {
            return REQUESTS.remove(Integer.valueOf(requestId));
        }
    }

    @SuppressWarnings("unchecked")
    static <T> AsyncResource<T> takeAsync(int requestId) {
        Object o = take(requestId);
        return o instanceof AsyncResource ? (AsyncResource<T>) o : null;
    }

    /** Returns the canonical peripheral for the identifier, creating it on
     * first sight; refreshes the cached name when a non-empty one arrives. */
    static IOSBlePeripheral peripheral(String peripheralId, String name) {
        synchronized (PERIPHERALS) {
            IOSBlePeripheral p = PERIPHERALS.get(peripheralId);
            if (p == null) {
                p = new IOSBlePeripheral(instance, peripheralId, name);
                PERIPHERALS.put(peripheralId, p);
            } else if (name != null && name.length() > 0) {
                p.updateName(name);
            }
            return p;
        }
    }

    static IOSBlePeripheral existingPeripheral(String peripheralId) {
        synchronized (PERIPHERALS) {
            return PERIPHERALS.get(peripheralId);
        }
    }

    static void registerL2capServer(int psm, IOSL2capServer server) {
        synchronized (L2CAP_SERVERS) {
            L2CAP_SERVERS.put(Integer.valueOf(psm), server);
        }
    }

    static void unregisterL2capServer(int psm) {
        synchronized (L2CAP_SERVERS) {
            L2CAP_SERVERS.remove(Integer.valueOf(psm));
        }
    }

    static BluetoothException mapError(int code, String msg) {
        BluetoothError err;
        switch (code) {
            case ERR_NOT_SUPPORTED:
                err = BluetoothError.NOT_SUPPORTED;
                break;
            case ERR_POWERED_OFF:
                err = BluetoothError.POWERED_OFF;
                break;
            case ERR_UNAUTHORIZED:
                err = BluetoothError.UNAUTHORIZED;
                break;
            case ERR_SCAN_FAILED:
                err = BluetoothError.SCAN_FAILED;
                break;
            case ERR_ADVERTISE_FAILED:
                err = BluetoothError.ADVERTISE_FAILED;
                break;
            case ERR_CONNECTION_FAILED:
                err = BluetoothError.CONNECTION_FAILED;
                break;
            case ERR_CONNECTION_LOST:
                err = BluetoothError.CONNECTION_LOST;
                break;
            case ERR_NOT_CONNECTED:
                err = BluetoothError.NOT_CONNECTED;
                break;
            case ERR_GATT:
                err = BluetoothError.GATT_ERROR;
                break;
            case ERR_TIMEOUT:
                err = BluetoothError.TIMEOUT;
                break;
            case ERR_IO:
                err = BluetoothError.IO_ERROR;
                break;
            default:
                err = BluetoothError.UNKNOWN;
                break;
        }
        return new BluetoothException(err, msg == null ? err.name() : msg);
    }

    private static AdapterState mapState(int cbManagerState) {
        switch (cbManagerState) {
            case 2:
                return AdapterState.UNSUPPORTED;
            case 3:
                return AdapterState.UNAUTHORIZED;
            case 4:
                return AdapterState.POWERED_OFF;
            case 5:
                return AdapterState.POWERED_ON;
            default: // 0 unknown, 1 resetting
                return AdapterState.UNKNOWN;
        }
    }

    static String bytesToHex(byte[] b) {
        if (b == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xFF;
            sb.append(Character.forDigit(v >> 4, 16));
            sb.append(Character.forDigit(v & 0xF, 16));
        }
        return sb.toString();
    }

    static byte[] hexToBytes(String s) {
        if (s == null || s.length() < 2) {
            return new byte[0];
        }
        int len = s.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int hi = Character.digit(s.charAt(i * 2), 16);
            int lo = Character.digit(s.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                return new byte[0];
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    /** Normalizes a UUID string coming from CBUUID (which may use the short
     * 4/8-character form) into the canonical 36-character representation. */
    static String normalizeUuid(String uuid) {
        try {
            return BluetoothUuid.fromString(uuid).toString();
        } catch (RuntimeException ex) {
            return uuid == null ? "" : uuid.toLowerCase();
        }
    }

    // ------------------------------------------------------------------
    // request holders for callbacks that resolve with rich objects
    // ------------------------------------------------------------------

    static final class PendingServer {
        final AsyncResource<GattServer> result;
        final IOSGattServer server;

        PendingServer(AsyncResource<GattServer> result, IOSGattServer server) {
            this.result = result;
            this.server = server;
        }
    }

    static final class PendingAdvertise {
        final AsyncResource<BleAdvertisement> result;
        final IOSBleAdvertisement advertisement;

        PendingAdvertise(AsyncResource<BleAdvertisement> result,
                IOSBleAdvertisement advertisement) {
            this.result = result;
            this.advertisement = advertisement;
        }
    }

    static final class PendingL2capServer {
        final AsyncResource<L2capServer> result;

        PendingL2capServer(AsyncResource<L2capServer> result) {
            this.result = result;
        }
    }

    // ---- Callbacks invoked from native code (do not rename) ----------------

    /** CBCentralManager state / authorization change. `state` uses the raw
     * CBManagerState values, `auth` the raw CBManagerAuthorization values. */
    public static void nativeBtStateChanged(int state, int auth) {
        if (state < 0 && auth < 0) {
            return; // static-initializer touch
        }
        if (auth != AUTH_NOT_DETERMINED && auth >= 0) {
            Object[] pending = null;
            synchronized (PENDING_PERMISSIONS) {
                if (!PENDING_PERMISSIONS.isEmpty()) {
                    pending = PENDING_PERMISSIONS.toArray();
                    PENDING_PERMISSIONS.clear();
                }
            }
            if (pending != null) {
                Boolean granted = Boolean.valueOf(auth == AUTH_ALLOWED);
                for (int i = 0; i < pending.length; i++) {
                    AsyncResource<Boolean> r =
                            (AsyncResource<Boolean>) pending[i];
                    if (!r.isDone()) {
                        r.complete(granted);
                    }
                }
            }
        }
        IOSBluetooth b = instance;
        if (b == null || state < 0) {
            return;
        }
        AdapterState mapped = mapState(state);
        if (mapped != b.adapterState) {
            b.adapterState = mapped;
            b.fireAdapterStateChanged(mapped);
        }
        if (mapped != AdapterState.POWERED_ON
                && mapped != AdapterState.UNKNOWN) {
            b.le.platformScanLostFromNative(mapped);
        }
    }

    /** One advertisement sighting from CBCentralManager
     * didDiscoverPeripheral. `serviceUuids` is comma-separated,
     * `manufacturerData` is the raw CoreBluetooth blob (2-byte little-endian
     * company id prefix), `serviceData` is `uuid=hex` pairs comma-separated
     * and `txPower` uses -999 when absent. */
    public static void nativeBtScanResult(String peripheralId, String name,
            int rssi, boolean connectable, String serviceUuids,
            String localName, byte[] manufacturerData, String serviceData,
            int txPower) {
        if (peripheralId == null) {
            return;
        }
        IOSBluetooth b = instance;
        if (b == null) {
            return;
        }
        IOSBlePeripheral p = peripheral(peripheralId, name);
        AdvertisementData ad = new AdvertisementData();
        if (localName != null && localName.length() > 0) {
            ad.setLocalName(localName);
        }
        if (serviceUuids != null && serviceUuids.length() > 0) {
            String[] uuids = Util.split(serviceUuids, ",");
            for (int i = 0; i < uuids.length; i++) {
                if (uuids[i].length() > 0) {
                    try {
                        ad.addServiceUuid(BluetoothUuid.fromString(uuids[i]));
                    } catch (RuntimeException ignore) {
                    }
                }
            }
        }
        if (manufacturerData != null && manufacturerData.length >= 2) {
            int companyId = (manufacturerData[0] & 0xFF)
                    | ((manufacturerData[1] & 0xFF) << 8);
            byte[] payload = new byte[manufacturerData.length - 2];
            System.arraycopy(manufacturerData, 2, payload, 0, payload.length);
            ad.addManufacturerData(companyId, payload);
        }
        if (serviceData != null && serviceData.length() > 0) {
            String[] entries = Util.split(serviceData, ",");
            for (int i = 0; i < entries.length; i++) {
                int eq = entries[i].indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                try {
                    ad.addServiceData(
                            BluetoothUuid.fromString(
                                    entries[i].substring(0, eq)),
                            hexToBytes(entries[i].substring(eq + 1)));
                } catch (RuntimeException ignore) {
                }
            }
        }
        if (txPower != -999) {
            ad.setTxPowerLevel(Integer.valueOf(txPower));
        }
        b.le.scanResultFromNative(new ScanResult(p, rssi, ad, connectable,
                System.currentTimeMillis()));
    }

    /** CBCentralManager didConnectPeripheral. */
    public static void nativeBtConnected(String peripheralId) {
        if (peripheralId == null) {
            return;
        }
        IOSBlePeripheral p = existingPeripheral(peripheralId);
        if (p != null) {
            p.connectedFromNative();
        }
    }

    /** CBCentralManager didFailToConnectPeripheral. */
    public static void nativeBtConnectFailed(String peripheralId, int code,
            String message) {
        if (peripheralId == null) {
            return;
        }
        IOSBlePeripheral p = existingPeripheral(peripheralId);
        if (p != null) {
            p.disconnectedFromNative(mapError(
                    code == 0 ? ERR_CONNECTION_FAILED : code, message));
        }
    }

    /** CBCentralManager didDisconnectPeripheral; `code` 0 means a clean,
     * app-requested disconnect. */
    public static void nativeBtDisconnected(String peripheralId, int code,
            String message) {
        if (peripheralId == null) {
            return;
        }
        IOSBlePeripheral p = existingPeripheral(peripheralId);
        if (p != null) {
            p.disconnectedFromNative(
                    code == 0 ? null : mapError(code, message));
        }
    }

    /** Full GATT database after the aggregated
     * services/characteristics/descriptors discovery pass. See
     * IOSBlePeripheral.parseGattDb for the line format. */
    public static void nativeBtServicesDiscovered(int requestId,
            String peripheralId, String gattDb) {
        AsyncResource<List<com.codename1.bluetooth.gatt.GattService>> r =
                takeAsync(requestId);
        if (r == null) {
            return;
        }
        IOSBlePeripheral p = peripheralId == null
                ? null : existingPeripheral(peripheralId);
        if (p == null) {
            if (!r.isDone()) {
                r.error(new BluetoothException(BluetoothError.UNKNOWN,
                        "Unknown peripheral in discovery result"));
            }
            return;
        }
        p.servicesDiscoveredFromNative(gattDb, r);
    }

    /** Characteristic / descriptor read result. */
    public static void nativeBtValue(int requestId, byte[] value) {
        AsyncResource<byte[]> r = takeAsync(requestId);
        if (r != null && !r.isDone()) {
            r.complete(value == null ? new byte[0] : value);
        }
    }

    /** Notification / indication outside a pending read. */
    public static void nativeBtNotification(String peripheralId,
            String serviceUuid, int serviceInstance, String charUuid,
            int charInstance, byte[] value) {
        if (peripheralId == null) {
            return;
        }
        IOSBlePeripheral p = existingPeripheral(peripheralId);
        if (p != null) {
            p.notificationFromNative(serviceUuid, serviceInstance, charUuid,
                    charInstance, value == null ? new byte[0] : value);
        }
    }

    /** Generic completion for writes, notify-state changes, server service
     * removal-free operations and sent notifications. */
    public static void nativeBtOperationComplete(int requestId) {
        AsyncResource<Boolean> r = takeAsync(requestId);
        if (r != null && !r.isDone()) {
            r.complete(Boolean.TRUE);
        }
    }

    /** didReadRSSI result. */
    public static void nativeBtRssi(int requestId, int rssi) {
        AsyncResource<Integer> r = takeAsync(requestId);
        if (r != null && !r.isDone()) {
            r.complete(Integer.valueOf(rssi));
        }
    }

    /** Failure path for any pending request id. */
    public static void nativeBtRequestError(int requestId, int code,
            String message) {
        Object o = take(requestId);
        if (o == null) {
            return;
        }
        BluetoothException ex = mapError(code, message);
        AsyncResource<?> r = null;
        if (o instanceof AsyncResource) {
            r = (AsyncResource<?>) o;
        } else if (o instanceof PendingServer) {
            r = ((PendingServer) o).result;
        } else if (o instanceof PendingAdvertise) {
            r = ((PendingAdvertise) o).result;
        } else if (o instanceof PendingL2capServer) {
            r = ((PendingL2capServer) o).result;
        }
        if (r != null && !r.isDone()) {
            r.error(ex);
        }
    }

    /** CBPeripheralDelegate didModifyServices. */
    public static void nativeBtServicesInvalidated(String peripheralId) {
        if (peripheralId == null) {
            return;
        }
        IOSBlePeripheral p = existingPeripheral(peripheralId);
        if (p != null) {
            p.servicesInvalidatedFromNative();
        }
    }

    /** CBPeripheralManager reached poweredOn for an openGattServer call. */
    public static void nativeBtGattServerOpened(int requestId) {
        Object o = take(requestId);
        if (!(o instanceof PendingServer)) {
            return;
        }
        PendingServer ps = (PendingServer) o;
        IOSBluetooth b = instance;
        if (b != null) {
            b.le.serverOpenedFromNative(ps.server);
        }
        if (!ps.result.isDone()) {
            ps.result.complete(ps.server);
        }
    }

    /** CBPeripheralManager didStartAdvertising without error. */
    public static void nativeBtAdvertiseStarted(int requestId) {
        Object o = take(requestId);
        if (!(o instanceof PendingAdvertise)) {
            return;
        }
        PendingAdvertise pa = (PendingAdvertise) o;
        pa.advertisement.startedFromNative();
        if (!pa.result.isDone()) {
            pa.result.complete(pa.advertisement);
        }
    }

    /** GATT server read request; the CBATTRequest is parked natively under
     * `requestHandle` until btRespondToReadRequest is called. `descLocalId`
     * is -1 for characteristic reads (iOS never surfaces descriptor
     * requests -- descriptors are static-value only). */
    public static void nativeBtReadRequest(long requestHandle,
            String centralId, int charLocalId, int descLocalId, int offset) {
        if (centralId == null) {
            return;
        }
        IOSBluetooth b = instance;
        IOSGattServer server = b == null ? null : b.le.getActiveServer();
        if (server == null) {
            if (b != null) {
                // 0x0e = unlikely error
                b.nativeInstance.btRespondToReadRequest(requestHandle, null,
                        0x0e);
            }
            return;
        }
        server.readRequestFromNative(requestHandle, centralId, charLocalId,
                descLocalId, offset);
    }

    /** GATT server write request; mirror of nativeBtReadRequest. */
    public static void nativeBtWriteRequest(long requestHandle,
            String centralId, int charLocalId, int descLocalId, byte[] value,
            int offset, boolean responseRequired) {
        if (centralId == null) {
            return;
        }
        IOSBluetooth b = instance;
        IOSGattServer server = b == null ? null : b.le.getActiveServer();
        if (server == null) {
            if (b != null && responseRequired) {
                b.nativeInstance.btRespondToWriteRequest(requestHandle, 0x0e);
            }
            return;
        }
        server.writeRequestFromNative(requestHandle, centralId, charLocalId,
                descLocalId, value == null ? new byte[0] : value, offset,
                responseRequired);
    }

    /** CBPeripheralManager central did(Un)SubscribeToCharacteristic. */
    public static void nativeBtSubscriptionChanged(String centralId,
            int centralMtu, int charLocalId, boolean subscribed) {
        if (centralId == null) {
            return;
        }
        IOSBluetooth b = instance;
        IOSGattServer server = b == null ? null : b.le.getActiveServer();
        if (server != null) {
            server.subscriptionFromNative(centralId, centralMtu, charLocalId,
                    subscribed);
        }
    }

    /** Central-role CBPeripheral didOpenL2CAPChannel. */
    public static void nativeBtL2capOpened(int requestId, int psm,
            long channelHandle) {
        AsyncResource<com.codename1.bluetooth.le.L2capChannel> r =
                takeAsync(requestId);
        if (r == null) {
            return;
        }
        IOSBluetooth b = instance;
        if (b == null) {
            return;
        }
        if (!r.isDone()) {
            r.complete(new IOSL2capChannel(b, psm, channelHandle));
        }
    }

    /** CBPeripheralManager didPublishL2CAPChannel. */
    public static void nativeBtL2capPublished(int requestId, int psm) {
        Object o = take(requestId);
        if (!(o instanceof PendingL2capServer)) {
            return;
        }
        PendingL2capServer pl = (PendingL2capServer) o;
        IOSBluetooth b = instance;
        if (b == null) {
            return;
        }
        IOSL2capServer server = new IOSL2capServer(b, psm);
        registerL2capServer(psm, server);
        if (!pl.result.isDone()) {
            pl.result.complete(server);
        }
    }

    /** Peripheral-role CBPeripheralManager didOpenL2CAPChannel (incoming). */
    public static void nativeBtL2capIncoming(int psm, long channelHandle) {
        if (channelHandle == 0) {
            return;
        }
        IOSBluetooth b = instance;
        if (b == null) {
            return;
        }
        IOSL2capServer server;
        synchronized (L2CAP_SERVERS) {
            server = L2CAP_SERVERS.get(Integer.valueOf(psm));
        }
        if (server != null) {
            server.incomingFromNative(new IOSL2capChannel(b, psm,
                    channelHandle));
        } else {
            b.nativeInstance.btL2capClose(channelHandle);
        }
    }
}
