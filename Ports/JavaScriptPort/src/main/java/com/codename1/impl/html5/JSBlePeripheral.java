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
package com.codename1.impl.html5;

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.DeviceType;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattDescriptor;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.ConnectionOptions;
import com.codename1.bluetooth.le.ConnectionPriority;
import com.codename1.bluetooth.le.ConnectionState;
import com.codename1.bluetooth.le.L2capChannel;
import com.codename1.io.JSONParser;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A remote BLE peripheral backed by a main-thread Web Bluetooth
 * {@code BluetoothDevice}/{@code BluetoothRemoteGATTServer} pair. The
 * worker only holds the opaque device id ({@link #getAddress()}, the
 * per-origin Web Bluetooth {@code device.id}) plus per-attribute integer
 * handle ids (iids) assigned by the host during service discovery; every
 * {@code do*} operation is one {@code __cn1_bt_*__} host round-trip run on
 * a background thread so the EDT never blocks on the bridge.
 *
 * <p>Platform divergences (Web Bluetooth):</p>
 * <ul>
 *   <li><b>MTU is hidden</b> -- {@code requestMtu} resolves immediately
 *       with 512 (the ATT maximum attribute length, which is also the Web
 *       Bluetooth per-write limit) and {@code getMtu()} reports 512 once
 *       connected.</li>
 *   <li><b>No RSSI</b> on established connections -- {@code readRssi}
 *       fails with {@link BluetoothError#NOT_SUPPORTED}.</li>
 *   <li><b>Bonding is browser/OS managed</b> -- {@code createBond} fails
 *       with {@link BluetoothError#NOT_SUPPORTED}.</li>
 *   <li><b>No L2CAP channels</b> -- {@code openL2capChannel} fails with
 *       {@link BluetoothError#NOT_SUPPORTED}.</li>
 *   <li>Connection priority requests are accepted as successful no-ops
 *       (the browser manages connection parameters), mirroring iOS.</li>
 * </ul>
 */
public class JSBlePeripheral extends BlePeripheral {

    private final String deviceId;
    private String name;
    private final Object gattLock = new Object();
    private final HashMap<Integer, GattCharacteristic> charsByIid =
            new HashMap<Integer, GattCharacteristic>();
    private final HashMap<GattDescriptor, Integer> descriptorIds =
            new HashMap<GattDescriptor, Integer>();

    JSBlePeripheral(String deviceId, String name) {
        this.deviceId = deviceId;
        this.name = name;
    }

    void setDeviceName(String name) {
        if (name != null) {
            this.name = name;
        }
    }

    public String getAddress() {
        return deviceId;
    }

    public String getName() {
        return name;
    }

    public DeviceType getType() {
        return DeviceType.LE;
    }

    // ------------------------------------------------------------------
    // events routed from JSBluetooth.dispatchNativeEvent
    // ------------------------------------------------------------------

    void onNativeDisconnected() {
        if (getConnectionState() == ConnectionState.DISCONNECTING) {
            // app-requested teardown completing
            fireConnectionStateChanged(ConnectionState.DISCONNECTED, null);
        } else {
            fireConnectionStateChanged(ConnectionState.DISCONNECTED,
                    new BluetoothException(BluetoothError.CONNECTION_LOST,
                            "The GATT server disconnected"));
        }
    }

    void onNativeNotification(int iid, byte[] value) {
        GattCharacteristic c;
        synchronized (gattLock) {
            c = charsByIid.get(Integer.valueOf(iid));
        }
        if (c != null) {
            fireNotification(c, value == null ? new byte[0] : value);
        }
    }

    // ------------------------------------------------------------------
    // port SPI
    // ------------------------------------------------------------------

    protected void doConnect(ConnectionOptions options,
            final AsyncResource<BlePeripheral> out) {
        JSBluetooth.async(new Runnable() {
            public void run() {
                Map<String, Object> res = JSBluetooth.parseResult(
                        JSBluetooth.nativeBtConnect(deviceId));
                if (out.isDone()) {
                    // timed out / cancelled while the round-trip ran
                    return;
                }
                if (JSBluetooth.isOk(res)) {
                    // Web Bluetooth hides the negotiated ATT MTU; 512 is
                    // its per-write maximum, report that.
                    setMtu(512);
                    out.complete(JSBlePeripheral.this);
                } else {
                    out.error(JSBluetooth.toException(res,
                            BluetoothError.CONNECTION_FAILED, "connect"));
                }
            }
        });
    }

    protected void doDisconnect() {
        JSBluetooth.async(new Runnable() {
            public void run() {
                JSBluetooth.nativeBtDisconnect(deviceId);
                // gattserverdisconnected also reports this transition; the
                // state machine dedupes whichever arrives second.
                fireConnectionStateChanged(ConnectionState.DISCONNECTED, null);
            }
        });
    }

    protected void doDiscoverServices(
            final AsyncResource<List<GattService>> out) {
        JSBluetooth.async(new Runnable() {
            public void run() {
                Map<String, Object> res = JSBluetooth.parseResult(
                        JSBluetooth.nativeBtDiscoverServices(deviceId));
                if (out.isDone()) {
                    return;
                }
                if (!JSBluetooth.isOk(res)) {
                    out.error(JSBluetooth.toException(res,
                            BluetoothError.GATT_ERROR, "discoverServices"));
                    return;
                }
                try {
                    out.complete(buildGattDatabase(
                            JSONParser.asList(res.get("services"))));
                } catch (RuntimeException ex) {
                    out.error(new BluetoothException(BluetoothError.UNKNOWN,
                            "Malformed GATT database from the host bridge: "
                                    + ex, ex));
                }
            }
        });
    }

    /**
     * Maps the host's one-shot JSON GATT dump onto the core model,
     * rebuilding the iid handle tables used by reads/writes/notifications.
     */
    private List<GattService> buildGattDatabase(List<Object> services) {
        ArrayList<GattService> list = new ArrayList<GattService>();
        HashMap<Integer, GattCharacteristic> newChars =
                new HashMap<Integer, GattCharacteristic>();
        HashMap<GattDescriptor, Integer> newDescs =
                new HashMap<GattDescriptor, Integer>();
        if (services != null) {
            int sn = services.size();
            for (int i = 0; i < sn; i++) {
                Map<String, Object> sm = JSONParser.asMap(services.get(i));
                String sUuid = JSONParser.getString(sm, "uuid");
                if (sm == null || sUuid == null) {
                    continue;
                }
                GattService service = new GattService(this,
                        BluetoothUuid.fromString(sUuid),
                        JSONParser.getInt(sm, "primary", 1) == 1,
                        JSONParser.getInt(sm, "iid", 0));
                List<Object> chars =
                        JSONParser.asList(sm.get("characteristics"));
                int cn = chars == null ? 0 : chars.size();
                for (int j = 0; j < cn; j++) {
                    Map<String, Object> cm = JSONParser.asMap(chars.get(j));
                    String cUuid = JSONParser.getString(cm, "uuid");
                    if (cm == null || cUuid == null) {
                        continue;
                    }
                    int cIid = JSONParser.getInt(cm, "iid", 0);
                    GattCharacteristic characteristic = new GattCharacteristic(
                            service, BluetoothUuid.fromString(cUuid),
                            JSONParser.getInt(cm, "properties", 0), cIid);
                    service.addCharacteristic(characteristic);
                    newChars.put(Integer.valueOf(cIid), characteristic);
                    List<Object> descs =
                            JSONParser.asList(cm.get("descriptors"));
                    int dn = descs == null ? 0 : descs.size();
                    for (int k = 0; k < dn; k++) {
                        Map<String, Object> dm =
                                JSONParser.asMap(descs.get(k));
                        String dUuid = JSONParser.getString(dm, "uuid");
                        if (dm == null || dUuid == null) {
                            continue;
                        }
                        GattDescriptor descriptor = new GattDescriptor(
                                characteristic,
                                BluetoothUuid.fromString(dUuid));
                        characteristic.addDescriptor(descriptor);
                        newDescs.put(descriptor, Integer.valueOf(
                                JSONParser.getInt(dm, "iid", 0)));
                    }
                }
                list.add(service);
            }
        }
        synchronized (gattLock) {
            charsByIid.clear();
            charsByIid.putAll(newChars);
            descriptorIds.clear();
            descriptorIds.putAll(newDescs);
        }
        return list;
    }

    protected void doReadCharacteristic(final GattCharacteristic c,
            final AsyncResource<byte[]> out) {
        JSBluetooth.async(new Runnable() {
            public void run() {
                completeValueOp(out, JSBluetooth.parseResult(
                        JSBluetooth.nativeBtReadCharacteristic(deviceId,
                                c.getInstanceId())), "readCharacteristic");
            }
        });
    }

    protected void doWriteCharacteristic(final GattCharacteristic c,
            final byte[] value, final boolean withResponse,
            final AsyncResource<Boolean> out) {
        JSBluetooth.async(new Runnable() {
            public void run() {
                completeBooleanOp(out, JSBluetooth.parseResult(
                        JSBluetooth.nativeBtWriteCharacteristic(deviceId,
                                c.getInstanceId(), value, withResponse)),
                        "writeCharacteristic");
            }
        });
    }

    protected void doReadDescriptor(final GattDescriptor d,
            final AsyncResource<byte[]> out) {
        final Integer iid = descriptorId(d);
        if (iid == null) {
            out.error(unknownDescriptor());
            return;
        }
        JSBluetooth.async(new Runnable() {
            public void run() {
                completeValueOp(out, JSBluetooth.parseResult(
                        JSBluetooth.nativeBtReadDescriptor(deviceId,
                                iid.intValue())), "readDescriptor");
            }
        });
    }

    protected void doWriteDescriptor(final GattDescriptor d,
            final byte[] value, final AsyncResource<Boolean> out) {
        final Integer iid = descriptorId(d);
        if (iid == null) {
            out.error(unknownDescriptor());
            return;
        }
        JSBluetooth.async(new Runnable() {
            public void run() {
                completeBooleanOp(out, JSBluetooth.parseResult(
                        JSBluetooth.nativeBtWriteDescriptor(deviceId,
                                iid.intValue(), value)), "writeDescriptor");
            }
        });
    }

    protected void doSetNotifications(final GattCharacteristic c,
            final boolean enable, boolean indication,
            final AsyncResource<Boolean> out) {
        // Web Bluetooth's startNotifications() arms whichever of
        // notify/indicate the characteristic supports; the indication flag
        // needs no separate handling.
        JSBluetooth.async(new Runnable() {
            public void run() {
                completeBooleanOp(out, JSBluetooth.parseResult(
                        JSBluetooth.nativeBtSetNotifications(deviceId,
                                c.getInstanceId(), enable)),
                        enable ? "subscribe" : "unsubscribe");
            }
        });
    }

    protected void doReadRssi(AsyncResource<Integer> out) {
        out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                "Web Bluetooth does not expose the RSSI of a connection"));
    }

    protected void doRequestMtu(int mtu, AsyncResource<Integer> out) {
        // Web Bluetooth hides MTU negotiation entirely; 512 bytes is the
        // ATT maximum attribute length and the API's per-write cap, so
        // resolve immediately with that (mirrors the iOS behavior of
        // resolving with the OS-negotiated value).
        out.complete(Integer.valueOf(512));
    }

    protected void doRequestConnectionPriority(ConnectionPriority priority,
            AsyncResource<Boolean> out) {
        // the browser owns the connection parameters -- successful no-op,
        // mirroring iOS
        out.complete(Boolean.TRUE);
    }

    protected void doCreateBond(AsyncResource<Boolean> out) {
        out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                "Bonding is managed by the browser/OS on Web Bluetooth"));
    }

    protected void doOpenL2cap(int psm, boolean secure,
            AsyncResource<L2capChannel> out) {
        out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                "L2CAP channels are not available on Web Bluetooth"));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private Integer descriptorId(GattDescriptor d) {
        synchronized (gattLock) {
            return descriptorIds.get(d);
        }
    }

    private static BluetoothException unknownDescriptor() {
        return new BluetoothException(BluetoothError.UNKNOWN,
                "Unknown descriptor -- re-run discoverServices()");
    }

    private static void completeValueOp(AsyncResource<byte[]> out,
            Map<String, Object> res, String operation) {
        if (out.isDone()) {
            return;
        }
        if (!JSBluetooth.isOk(res)) {
            out.error(JSBluetooth.toException(res, BluetoothError.GATT_ERROR,
                    operation));
            return;
        }
        String b64 = JSONParser.getString(res, "value");
        out.complete(b64 == null || b64.length() == 0
                ? new byte[0] : Base64.decode(b64.getBytes()));
    }

    private static void completeBooleanOp(AsyncResource<Boolean> out,
            Map<String, Object> res, String operation) {
        if (out.isDone()) {
            return;
        }
        if (JSBluetooth.isOk(res)) {
            out.complete(Boolean.TRUE);
        } else {
            out.error(JSBluetooth.toException(res, BluetoothError.GATT_ERROR,
                    operation));
        }
    }
}
