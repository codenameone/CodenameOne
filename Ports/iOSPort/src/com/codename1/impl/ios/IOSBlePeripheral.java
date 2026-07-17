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
import com.codename1.io.Util;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A remote peripheral over a retained CBPeripheral. The "address" is the
 * CoreBluetooth identifier UUID string (iOS never exposes the MAC), stable
 * per app install and usable with
 * {@link com.codename1.bluetooth.le.BluetoothLE#getPeripheral(String)}.
 *
 * <p>The core {@link BlePeripheral} serializes GATT operations one at a
 * time per peripheral, so each do* method simply registers its
 * AsyncResource in the shared request registry and forwards to the native
 * bridge; the matching nativeBt* callback resolves it.</p>
 */
class IOSBlePeripheral extends BlePeripheral {

    private final IOSBluetooth bt;
    private final String id;
    private volatile String name;

    /** Canonical characteristic instances from the last discovery pass,
     * keyed by serviceUuid|serviceInstance|charUuid|charInstance -- used to
     * route notifications back to the objects the app subscribed on. */
    private final HashMap<String, GattCharacteristic> charIndex =
            new HashMap<String, GattCharacteristic>();

    IOSBlePeripheral(IOSBluetooth bt, String id, String name) {
        this.bt = bt;
        this.id = id;
        this.name = name;
    }

    @Override
    public String getAddress() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DeviceType getType() {
        return DeviceType.LE;
    }

    void updateName(String name) {
        this.name = name;
    }

    // ------------------------------------------------------------------
    // connection lifecycle SPI
    // ------------------------------------------------------------------

    @Override
    protected void doConnect(ConnectionOptions options,
            AsyncResource<BlePeripheral> out) {
        // iOS connect attempts never time out on their own; the core layer
        // arms the ConnectionOptions timeout and calls doDisconnect on
        // expiry, which cancels the native attempt. autoConnect has no
        // CoreBluetooth equivalent (attempts are already persistent).
        bt.ensureMonitor();
        bt.nativeInstance.btConnect(id);
        // resolution happens through nativeBtConnected /
        // nativeBtConnectFailed -> fireConnectionStateChanged
    }

    @Override
    protected void doDisconnect() {
        bt.nativeInstance.btDisconnect(id);
    }

    void connectedFromNative() {
        // seed the MTU before the CONNECTED transition resolves the
        // pending connect handle
        int payload = bt.nativeInstance.btGetMaxWriteLength(id, false);
        if (payload > 0) {
            setMtu(payload + 3);
        }
        fireConnectionStateChanged(ConnectionState.CONNECTED, null);
    }

    void disconnectedFromNative(BluetoothException reason) {
        fireConnectionStateChanged(ConnectionState.DISCONNECTED, reason);
    }

    void servicesInvalidatedFromNative() {
        synchronized (charIndex) {
            charIndex.clear();
        }
        fireServicesInvalidated();
    }

    // ------------------------------------------------------------------
    // GATT client SPI
    // ------------------------------------------------------------------

    @Override
    protected void doDiscoverServices(AsyncResource<List<GattService>> out) {
        int rid = IOSBluetooth.takeId(out);
        bt.nativeInstance.btDiscoverServices(rid, id);
    }

    @Override
    protected void doReadCharacteristic(GattCharacteristic c,
            AsyncResource<byte[]> out) {
        int rid = IOSBluetooth.takeId(out);
        bt.nativeInstance.btReadCharacteristic(rid, id,
                c.getService().getUuid().toString(),
                c.getService().getInstanceId(),
                c.getUuid().toString(), c.getInstanceId());
    }

    @Override
    protected void doWriteCharacteristic(GattCharacteristic c, byte[] value,
            boolean withResponse, AsyncResource<Boolean> out) {
        int rid = IOSBluetooth.takeId(out);
        bt.nativeInstance.btWriteCharacteristic(rid, id,
                c.getService().getUuid().toString(),
                c.getService().getInstanceId(),
                c.getUuid().toString(), c.getInstanceId(),
                value == null ? new byte[0] : value, withResponse);
    }

    @Override
    protected void doReadDescriptor(GattDescriptor d,
            AsyncResource<byte[]> out) {
        GattCharacteristic c = d.getCharacteristic();
        int rid = IOSBluetooth.takeId(out);
        bt.nativeInstance.btReadDescriptor(rid, id,
                c.getService().getUuid().toString(),
                c.getService().getInstanceId(),
                c.getUuid().toString(), c.getInstanceId(),
                d.getUuid().toString());
    }

    @Override
    protected void doWriteDescriptor(GattDescriptor d, byte[] value,
            AsyncResource<Boolean> out) {
        GattCharacteristic c = d.getCharacteristic();
        int rid = IOSBluetooth.takeId(out);
        bt.nativeInstance.btWriteDescriptor(rid, id,
                c.getService().getUuid().toString(),
                c.getService().getInstanceId(),
                c.getUuid().toString(), c.getInstanceId(),
                d.getUuid().toString(), value == null ? new byte[0] : value);
    }

    @Override
    protected void doSetNotifications(GattCharacteristic c, boolean enable,
            boolean indication, AsyncResource<Boolean> out) {
        // setNotifyValue covers both notifications and indications on iOS
        // (CoreBluetooth writes the CCCD itself), so `indication` is moot.
        int rid = IOSBluetooth.takeId(out);
        bt.nativeInstance.btSetNotify(rid, id,
                c.getService().getUuid().toString(),
                c.getService().getInstanceId(),
                c.getUuid().toString(), c.getInstanceId(), enable);
    }

    @Override
    protected void doReadRssi(AsyncResource<Integer> out) {
        int rid = IOSBluetooth.takeId(out);
        bt.nativeInstance.btReadRssi(rid, id);
    }

    @Override
    protected void doRequestMtu(int mtu, AsyncResource<Integer> out) {
        // iOS negotiates the MTU itself; report the current effective value
        // (maximumWriteValueLength for write-without-response + the 3-byte
        // ATT header) immediately.
        int payload = bt.nativeInstance.btGetMaxWriteLength(id, false);
        int att = payload > 0 ? payload + 3 : getMtu();
        setMtu(att);
        if (!out.isDone()) {
            out.complete(Integer.valueOf(att));
        }
    }

    @Override
    protected void doRequestConnectionPriority(ConnectionPriority priority,
            AsyncResource<Boolean> out) {
        // iOS manages connection intervals itself -- a successful no-op
        if (!out.isDone()) {
            out.complete(Boolean.TRUE);
        }
    }

    @Override
    protected void doCreateBond(AsyncResource<Boolean> out) {
        // Pairing on iOS is OS-managed, triggered by access to encrypted
        // characteristics -- report success without user interaction.
        if (!out.isDone()) {
            out.complete(Boolean.TRUE);
        }
    }

    @Override
    protected void doOpenL2cap(int psm, boolean secure,
            AsyncResource<L2capChannel> out) {
        // `secure` has no central-side switch on iOS; the peripheral chose
        // the encryption requirement when publishing the PSM.
        if (getConnectionState() != ConnectionState.CONNECTED) {
            out.error(new BluetoothException(BluetoothError.NOT_CONNECTED,
                    "iOS requires a connected peripheral to open L2CAP"));
            return;
        }
        int rid = IOSBluetooth.takeId(out);
        bt.nativeInstance.btOpenL2cap(rid, id, psm);
    }

    // ------------------------------------------------------------------
    // native event plumbing
    // ------------------------------------------------------------------

    void notificationFromNative(String serviceUuid, int serviceInstance,
            String charUuid, int charInstance, byte[] value) {
        GattCharacteristic c;
        synchronized (charIndex) {
            c = charIndex.get(charKey(serviceUuid, serviceInstance, charUuid,
                    charInstance));
        }
        if (c != null) {
            fireNotification(c, value);
        }
    }

    /** Parses the discovery result and rebuilds the canonical
     * characteristic index. Line format (fields separated by '|'):
     * <pre>
     * S|uuid|primary(0/1)|instanceId
     * C|uuid|properties|instanceId      (owned by the last S)
     * D|uuid                            (owned by the last C)
     * </pre> */
    void servicesDiscoveredFromNative(String gattDb,
            AsyncResource<List<GattService>> out) {
        ArrayList<GattService> services = new ArrayList<GattService>();
        HashMap<String, GattCharacteristic> index =
                new HashMap<String, GattCharacteristic>();
        if (gattDb != null && gattDb.length() > 0) {
            String[] lines = Util.split(gattDb, "\n");
            GattService curService = null;
            GattCharacteristic curChar = null;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.length() < 3) {
                    continue;
                }
                String[] f = Util.split(line, "|");
                try {
                    if (line.charAt(0) == 'S' && f.length >= 4) {
                        curService = new GattService(this,
                                BluetoothUuid.fromString(f[1]),
                                "1".equals(f[2]), Integer.parseInt(f[3]));
                        curChar = null;
                        services.add(curService);
                    } else if (line.charAt(0) == 'C' && f.length >= 4
                            && curService != null) {
                        curChar = new GattCharacteristic(curService,
                                BluetoothUuid.fromString(f[1]),
                                Integer.parseInt(f[2]),
                                Integer.parseInt(f[3]));
                        curService.addCharacteristic(curChar);
                        index.put(charKey(
                                curService.getUuid().toString(),
                                curService.getInstanceId(),
                                curChar.getUuid().toString(),
                                curChar.getInstanceId()), curChar);
                    } else if (line.charAt(0) == 'D' && f.length >= 2
                            && curChar != null) {
                        curChar.addDescriptor(new GattDescriptor(curChar,
                                BluetoothUuid.fromString(f[1])));
                    }
                } catch (RuntimeException ignore) {
                    // skip malformed lines rather than failing discovery
                }
            }
        }
        synchronized (charIndex) {
            charIndex.clear();
            charIndex.putAll(index);
        }
        if (!out.isDone()) {
            out.complete(services);
        }
    }

    private static String charKey(String serviceUuid, int serviceInstance,
            String charUuid, int charInstance) {
        return IOSBluetooth.normalizeUuid(serviceUuid) + "|" + serviceInstance
                + "|" + IOSBluetooth.normalizeUuid(charUuid) + "|"
                + charInstance;
    }
}
